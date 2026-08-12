package com.amanp20.securevault.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.databinding.FragmentChangePinBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.utils.EventObserver
import com.amanp20.securevault.utils.PinValidator
import com.amanp20.securevault.viewmodel.SettingsViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class ChangePinFragment : BaseBindingFragment<FragmentChangePinBinding>() {

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChangePinBinding {
        return FragmentChangePinBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSelector()
        setupListeners()
        observeViewModel()
    }

    private fun setupSelector() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.pin_lengths,
            android.R.layout.simple_list_item_1
        )
        binding.newPinLengthDropdown.setAdapter(adapter)
        binding.newPinLengthDropdown.setText(getString(R.string.pin_length_4), false)
    }

    private fun setupListeners() {
        binding.currentPinInput.doOnTextChanged { _, _, _, _ -> clearErrors() }
        binding.newPinInput.doOnTextChanged { _, _, _, _ -> clearErrors() }
        binding.confirmNewPinInput.doOnTextChanged { _, _, _, _ -> clearErrors() }
        binding.savePinButton.setOnClickListener { submitChangePin() }
    }

    private fun submitChangePin() {
        val currentPin = PinValidator.normalizedPin(binding.currentPinInput.text?.toString().orEmpty())
        val newPin = PinValidator.normalizedPin(binding.newPinInput.text?.toString().orEmpty())
        val confirmPin = PinValidator.normalizedPin(binding.confirmNewPinInput.text?.toString().orEmpty())
        val newPinLength = if (binding.newPinLengthDropdown.text?.toString() == getString(R.string.pin_length_6)) 6 else 4

        if (!PinValidator.isValidPin(newPin, newPinLength)) {
            binding.newPinLayout.error = getString(R.string.error_pin_length)
            return
        }

        if (newPin != confirmPin) {
            binding.confirmNewPinLayout.error = getString(R.string.error_pin_mismatch)
            return
        }

        viewModel.changePin(currentPin, newPin, newPinLength)
    }

    private fun observeViewModel() {
        viewModel.changePinSuccess.observe(viewLifecycleOwner, EventObserver {
            findNavController().popBackStack()
        })
        viewModel.settingsError.observe(viewLifecycleOwner, EventObserver { errorMessage ->
            binding.currentPinLayout.error = errorMessage
        })
    }

    private fun clearErrors() {
        binding.currentPinLayout.error = null
        binding.newPinLayout.error = null
        binding.confirmNewPinLayout.error = null
    }
}
