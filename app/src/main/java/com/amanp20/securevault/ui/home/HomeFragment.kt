package com.amanp20.securevault.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.databinding.FragmentHomeBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.viewmodel.HomeViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class HomeFragment : BaseBindingFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory(
            (requireActivity().application as SecureVaultApplication)
                .appContainer
                .secureVaultRepository
        )
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
            viewModel.greeting
        )
    }

    private fun observeVaultStats() {

        viewModel.documentCount.observe(
            viewLifecycleOwner
        ) { count ->

            binding.documentsCountText.text =
                count.toString()
        }

        viewModel.totalStorageUsed.observe(
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

        var value = bytes.toDouble()
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