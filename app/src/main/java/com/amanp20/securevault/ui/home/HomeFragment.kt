package com.amanp20.securevault.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.data.repository.ImportResult
import com.amanp20.securevault.databinding.FragmentHomeBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.viewmodel.HomeViewModel
import com.amanp20.securevault.viewmodel.SecureVaultViewModel
import com.amanp20.securevault.viewmodel.SecureVaultViewModelFactory
import com.amanp20.securevault.viewmodel.ViewModelFactory

class HomeFragment :
    BaseBindingFragment<FragmentHomeBinding>() {

    private val applicationState: SecureVaultApplication
        get() =
            requireActivity()
                .application as SecureVaultApplication

    private val homeViewModel:
        HomeViewModel by viewModels {
            ViewModelFactory(
                applicationState
                    .appContainer
                    .secureVaultRepository
            )
        }

    private val vaultViewModel:
        SecureVaultViewModel by viewModels {
            SecureVaultViewModelFactory(
                applicationState
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
    ): FragmentHomeBinding {

        return FragmentHomeBinding.inflate(
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

        setupGreeting()
        observeVaultStats()
        setupNavigation()
    }

    private fun setupGreeting() {

        binding.greetingText.setText(
            homeViewModel.greeting
        )
    }

    private fun observeVaultStats() {

        homeViewModel.documentCount.observe(
            viewLifecycleOwner
        ) { count ->

            binding.documentsCountText.text =
                count.toString()
        }

        homeViewModel.totalStorageUsed.observe(
            viewLifecycleOwner
        ) { bytes ->

            binding.storageUsedText.text =
                formatStorage(bytes)
        }
    }

    private fun setupNavigation() {

        binding.documentsCard.setOnClickListener {

            findNavController().navigate(
                R.id.action_homeFragment_to_documentsFragment
            )
        }

        binding.settingsCard.setOnClickListener {

            findNavController().navigate(
                R.id.action_homeFragment_to_settingsFragment
            )
        }

        binding.addFileButton.setOnClickListener {

            picker.launch(
                arrayOf("*/*")
            )
        }
    }

    private fun importFile(
        uri: Uri
    ) {

        vaultViewModel.importFile(
            uri = uri,

            onResult = { result ->

                if (!isAdded) {
                    return@importFile
                }

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

                if (!isAdded) {
                    return@importFile
                }

                showMessage(
                    error.message
                        ?: "Unable to import file"
                )
            }
        )
    }

    private fun showMessage(
        message: String
    ) {

        if (!isAdded) {
            return
        }

        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun formatStorage(
        bytes: Long
    ): String {

        if (bytes <= 0L) {
            return "0 B"
        }

        val units = arrayOf(
            "B",
            "KB",
            "MB",
            "GB",
            "TB"
        )

        var value =
            bytes.toDouble()

        var index = 0

        while (
            value >= 1024 &&
            index < units.lastIndex
        ) {
            value /= 1024
            index++
        }

        return if (index == 0) {

            "${value.toLong()} ${units[index]}"

        } else {

            String.format(
                "%.1f %s",
                value,
                units[index]
            )
        }
    }
}