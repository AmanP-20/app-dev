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
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.greetingText.text = viewModel.greeting
        viewModel.documentCount.observe(viewLifecycleOwner) { count ->
            binding.documentsCountText.text = count.toString()
        }
        viewModel.noteCount.observe(viewLifecycleOwner) { count ->
            binding.notesCountText.text = count.toString()
        }
        binding.settingsCard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }
    }
}
