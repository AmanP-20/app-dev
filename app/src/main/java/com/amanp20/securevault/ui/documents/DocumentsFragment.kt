package com.amanp20.securevault.ui.documents

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.databinding.FragmentDocumentsBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.viewmodel.DocumentSort
import com.amanp20.securevault.viewmodel.SecureDocumentViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class DocumentsFragment : BaseBindingFragment<FragmentDocumentsBinding>() {
    private val viewModel: SecureDocumentViewModel by viewModels {
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importUris(requireContext().contentResolver, uris)
    }
    private lateinit var adapter: DocumentAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDocumentsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setHasOptionsMenu(true)
        adapter = DocumentAdapter(::openDocument, ::showDocumentMenu)
        binding.documentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.documentsRecyclerView.adapter = adapter
        binding.addFileButton.setOnClickListener {
            picker.launch(
                arrayOf(
                    "application/pdf", "text/*", "image/*", "video/*", "audio/*",
                    "application/msword", "application/vnd.openxmlformats-officedocument.*"
                )
            )
        }
        viewModel.documents.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyState.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            binding.documentsRecyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: LayoutInflater) {
        inflater.inflate(R.menu.documents_menu, menu)
        val search = menu.findItem(R.id.action_search).actionView as SearchView
        search.queryHint = getString(R.string.search_documents_hint)
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) = viewModel.setQuery(newText.orEmpty()).let { true }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_sort -> {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort_documents)
                .setItems(R.array.document_sort_options) { _, which ->
                    viewModel.setSort(DocumentSort.values()[which])
                }.show()
            true
        }
        R.id.action_filter -> {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter_documents)
                .setSingleChoiceItems(resources.getStringArray(R.array.document_categories), 0) { dialog, which ->
                    viewModel.setCategory(resources.getStringArray(R.array.document_categories)[which])
                    dialog.dismiss()
                }.show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openDocument(document: SecureDocument) {
        if (document.isLocked) {
            authenticateLockedDocument(document)
            return
        }
        openUri(Uri.parse(document.uri), document.mimeType)
    }

    private fun openUri(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }
            .onFailure { Snackbar.make(binding.root, R.string.no_app_to_open_file, Snackbar.LENGTH_LONG).show() }
    }

    private fun showDocumentMenu(document: SecureDocument) {
        val options = if (document.isLocked) {
            arrayOf(getString(R.string.open_action), getString(R.string.unlock_action), getString(R.string.change_lock_action), getString(R.string.delete_action))
        } else {
            arrayOf(getString(R.string.open_action), getString(R.string.lock_action), getString(R.string.delete_action))
        }
        MaterialAlertDialogBuilder(requireContext())
            .setItems(options) { _, which ->
                when {
                    !document.isLocked && which == 0 -> openDocument(document)
                    !document.isLocked && which == 1 -> configureLock(document)
                    !document.isLocked -> confirmDelete(document)
                    which == 0 -> openDocument(document)
                    which == 1 -> removeLock(document)
                    which == 2 -> changeLock(document)
                    else -> confirmDelete(document)
                }
            }.show()
    }

    private fun configureLock(document: SecureDocument) {
        val choices = arrayOf(getString(R.string.password_lock), getString(R.string.biometric_lock), getString(R.string.combined_lock))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.protect_document)
            .setSingleChoiceItems(choices, 0) { dialog, which ->
                val type = arrayOf("PASSWORD", "BIOMETRIC", "PIN_AND_BIOMETRIC")[which]
                if (type == "BIOMETRIC" && !biometricsAvailable()) {
                    Snackbar.make(binding.root, R.string.biometric_unavailable, Snackbar.LENGTH_LONG).show()
                } else if (type == "BIOMETRIC") {
                    viewModel.lock(document, type)
                } else {
                    dialog.dismiss()
                    passwordDialog(document, type)
                }
                if (type == "BIOMETRIC") dialog.dismiss()
            }.show()
    }

    private fun changeLock(document: SecureDocument) {
        authenticateLockedDocumentForChange(document) {
            viewModel.removeProtection(document) {
                configureLock(document.copy(isLocked = false, lockType = "NONE", passwordHash = null))
            }
        }
    }

    private fun passwordDialog(document: SecureDocument, type: String) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.document_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmation = EditText(requireContext()).apply {
            hint = getString(R.string.confirm_document_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val fields = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)
            addView(input)
            addView(confirmation)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.document_password_title)
            .setView(fields)
            .setNegativeButton(R.string.cancel_action, null)
            .setPositiveButton(R.string.save_action) { _, _ ->
                if (input.text.length < 4) {
                    Snackbar.make(binding.root, R.string.document_password_too_short, Snackbar.LENGTH_LONG).show()
                } else if (input.text.toString() != confirmation.text.toString()) {
                    Snackbar.make(binding.root, R.string.error_pin_mismatch, Snackbar.LENGTH_LONG).show()
                } else {
                    viewModel.lock(document, type, input.text.toString())
                }
            }.show()
    }

    private fun authenticateLockedDocument(document: SecureDocument) {
        if (document.lockType.contains("BIOMETRIC")) showBiometric(document) else passwordUnlock(document)
    }

    private fun passwordUnlock(document: SecureDocument) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.document_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.enter_document_password)
            .setView(input)
            .setNegativeButton(R.string.cancel_action, null)
            .setPositiveButton(R.string.unlock_action) { _, _ ->
                viewModel.unlock(document, input.text.toString()) { uri -> openUri(uri, document.mimeType) }
            }.show()
    }

    private fun showBiometric(document: SecureDocument) {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (document.lockType == "PIN_AND_BIOMETRIC") passwordUnlock(document)
                else viewModel.unlock(document, null) { uri -> openUri(uri, document.mimeType) }
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_document))
                .setSubtitle(getString(R.string.biometric_document_subtitle))
                .setNegativeButtonText(getString(R.string.cancel_action))
                .build()
        )
    }

    private fun removeLock(document: SecureDocument) {
        authenticateLockedDocumentForChange(document) {
            viewModel.removeProtection(document)
        }
    }

    private fun authenticateLockedDocumentForChange(document: SecureDocument, onAuthenticated: () -> Unit) {
        if (document.lockType.contains("BIOMETRIC")) {
            val executor = ContextCompat.getMainExecutor(requireContext())
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (document.passwordHash != null) passwordDialogForAction(document, onAuthenticated) else onAuthenticated()
                }
            }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.unlock_document))
                .setNegativeButtonText(getString(R.string.cancel_action)).build())
        } else passwordDialogForAction(document, onAuthenticated)
    }

    private fun passwordDialogForAction(document: SecureDocument, onAuthenticated: () -> Unit) {
        val input = EditText(requireContext()).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.enter_document_password).setView(input)
            .setNegativeButton(R.string.cancel_action, null).setPositiveButton(R.string.unlock_action) { _, _ ->
                viewModel.unlock(document, input.text.toString()) { onAuthenticated() }
            }.show()
    }

    private fun biometricsAvailable(): Boolean =
        BiometricManager.from(requireContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun confirmDelete(document: SecureDocument) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_document_message)
            .setNegativeButton(R.string.cancel_action, null)
            .setPositiveButton(R.string.delete_action) { _, _ -> viewModel.delete(document) }
            .show()
    }
}
