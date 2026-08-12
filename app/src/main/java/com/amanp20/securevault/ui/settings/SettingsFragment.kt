package com.amanp20.securevault.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.databinding.FragmentSettingsBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.utils.EventObserver
import com.amanp20.securevault.viewmodel.SettingsViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class SettingsFragment : BaseBindingFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.biometricSwitch.isChecked = viewModel.biometricEnabled
        binding.biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBiometricEnabled(isChecked)
        }
        binding.changePinCard.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_changePinFragment)
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.settingsError.observe(viewLifecycleOwner, EventObserver {
            binding.aboutText.text = it
        })
    }
}
