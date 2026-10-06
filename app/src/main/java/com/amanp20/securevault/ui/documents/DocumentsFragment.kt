package com.amanp20.securevault.ui.documents

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.repository.ImportResult
import com.amanp20.securevault.databinding.FragmentDocumentsBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.viewmodel.SecureVaultViewModel
import com.amanp20.securevault.viewmodel.SecureVaultViewModelFactory

class DocumentsFragment :
    BaseBindingFragment<FragmentDocumentsBinding>() {

    private lateinit var adapter: DocumentAdapter

    private val viewModel: SecureVaultViewModel by viewModels {
        SecureVaultViewModelFactory(
            (requireActivity().application as SecureVaultApplication)
                .appContainer
                .secureVaultRepository
        )
    }

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isEmpty()) {
                return@registerForActivityResult
            }

            uris.forEach { uri ->
                importFile(uri)
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDocumentsBinding {

        return FragmentDocumentsBinding.inflate(
            inflater,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        setupToolbar()
        setupMenu()
        setupRecyclerView()
        setupFilePicker()
        observeDocuments()
    }

    private fun setupToolbar() {

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMenu() {

        requireActivity().addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater
                ) {

                    menuInflater.inflate(
                        R.menu.documents_menu,
                        menu
                    )

                    val searchItem =
                        menu.findItem(
                            R.id.action_search
                        )

                    val searchView =
                        searchItem.actionView
                            as? SearchView
                            ?: return

                    searchView.queryHint =
                        getString(
                            R.string.search_documents_hint
                        )

                    searchView.setOnQueryTextListener(
                        object :
                            SearchView.OnQueryTextListener {

                            override fun onQueryTextSubmit(
                                query: String?
                            ): Boolean {
                                return true
                            }

                            override fun onQueryTextChange(
                                newText: String?
                            ): Boolean {

                                filterDocuments(
                                    newText.orEmpty()
                                )

                                return true
                            }
                        }
                    )
                }

                override fun onMenuItemSelected(
                    item: MenuItem
                ): Boolean {

                    return when (item.itemId) {

                        R.id.action_sort -> {
                            showSortDialog()
                            true
                        }

                        R.id.action_filter -> {
                            showFilterDialog()
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun setupRecyclerView() {

        adapter = DocumentAdapter(
            ::openDocument,
            ::showDocumentMenu
        )

        binding.documentsRecyclerView.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.documentsRecyclerView.adapter =
            adapter
    }

    private fun setupFilePicker() {

        binding.addFileButton.setOnClickListener {

            picker.launch(
                arrayOf("*/*")
            )
        }
    }

    private fun observeDocuments() {

        viewModel.documents.observe(
            viewLifecycleOwner
        ) { documents ->

            adapter.submitList(
                documents
            )

            updateEmptyState(
                documents.isEmpty()
            )
        }
    }

    private fun filterDocuments(
        query: String
    ) {

        val allDocuments =
            viewModel.documents.value
                ?: emptyList()

        val filtered =
            if (query.isBlank()) {

                allDocuments

            } else {

                allDocuments.filter {

                    it.originalName.contains(
                        query,
                        ignoreCase = true
                    )
                }
            }

        adapter.submitList(
            filtered
        )

        updateEmptyState(
            filtered.isEmpty()
        )
    }

    private fun updateEmptyState(
        empty: Boolean
    ) {

        binding.emptyState.visibility =
            if (empty) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.documentsRecyclerView.visibility =
            if (empty) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun importFile(
        uri: Uri
    ) {

        viewModel.importFile(
            uri = uri,

            onResult = { result ->

                when (result) {

                    ImportResult.Added -> {
                        showMessage(
                            "File encrypted and added"
                        )
                    }

                    ImportResult.Duplicate -> {
                        showMessage(
                            "A file with this name already exists"
                        )
                    }
                }
            },

            onError = { error ->

                showMessage(
                    error.message
                        ?: "Unable to import file"
                )
            }
        )
    }

    private fun openDocument(
        document: SecureDocument
    ) {

        val action =
            DocumentsFragmentDirections
                .actionDocumentsFragmentToDocumentViewerFragment(
                    document.id
                )

        findNavController().navigate(
            action
        )
    }

    private fun showDocumentMenu(
        document: SecureDocument
    ) {

        val options = arrayOf(
            getString(R.string.open_action),
            "Rename",
            getString(R.string.delete_action)
        )

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setItems(options) { _, which ->

                when (which) {

                    0 -> openDocument(
                        document
                    )

                    1 -> showRenameDialog(
                        document
                    )

                    2 -> confirmDelete(
                        document
                    )
                }
            }
            .show()
    }

    private fun showRenameDialog(
        document: SecureDocument
    ) {

        val input =
            EditText(requireContext())

        input.setText(
            document.originalName
        )

        input.selectAll()

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle("Rename file")
            .setView(input)
            .setNegativeButton(
                R.string.cancel_action,
                null
            )
            .setPositiveButton(
                R.string.save_action
            ) { _, _ ->

                val newName =
                    input.text
                        .toString()
                        .trim()

                if (newName.isBlank()) {

                    showMessage(
                        "File name cannot be empty"
                    )

                    return@setPositiveButton
                }

                viewModel.renameDocument(
                    document,
                    newName,

                    onResult = {
                        showMessage(
                            "File renamed"
                        )
                    },

                    onError = { error ->
                        showMessage(
                            error.message
                                ?: "Unable to rename file"
                        )
                    }
                )
            }
            .show()
    }

    private fun confirmDelete(
        document: SecureDocument
    ) {

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle("Delete file?")
            .setMessage(
                "This permanently removes the encrypted file from your vault."
            )
            .setNegativeButton(
                R.string.cancel_action,
                null
            )
            .setPositiveButton(
                R.string.delete_action
            ) { _, _ ->

                viewModel.deleteDocument(
                    document,

                    onResult = {
                        showMessage(
                            "File deleted"
                        )
                    },

                    onError = { error ->
                        showMessage(
                            error.message
                                ?: "Unable to delete file"
                        )
                    }
                )
            }
            .show()
    }

    private fun showSortDialog() {

        val options = arrayOf(
            "Newest first",
            "Oldest first",
            "Name A-Z",
            "Name Z-A",
            "Largest first",
            "Smallest first"
        )

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle("Sort files")
            .setItems(options) { _, which ->

                val documents =
                    viewModel.documents.value
                        ?: emptyList()

                val sorted =
                    when (which) {

                        0 ->
                            documents.sortedByDescending {
                                it.dateAdded
                            }

                        1 ->
                            documents.sortedBy {
                                it.dateAdded
                            }

                        2 ->
                            documents.sortedBy {
                                it.originalName.lowercase()
                            }

                        3 ->
                            documents.sortedByDescending {
                                it.originalName.lowercase()
                            }

                        4 ->
                            documents.sortedByDescending {
                                it.fileSize
                            }

                        else ->
                            documents.sortedBy {
                                it.fileSize
                            }
                    }

                adapter.submitList(
                    sorted
                )
            }
            .show()
    }

    private fun showFilterDialog() {

        val categories = arrayOf(
            "All",
            "Images",
            "Videos",
            "Audio",
            "Documents",
            "Other"
        )

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle("Filter files")
            .setItems(categories) { _, which ->

                val documents =
                    viewModel.documents.value
                        ?: emptyList()

                val filtered =
                    if (which == 0) {

                        documents

                    } else {

                        documents.filter {
                            it.category.equals(
                                categories[which],
                                ignoreCase = true
                            )
                        }
                    }

                adapter.submitList(
                    filtered
                )
            }
            .show()
    }

    private fun showMessage(
        message: String
    ) {

        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}