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
import com.amanp20.securevault.databinding.FragmentCreatePinBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.utils.EventObserver
import com.amanp20.securevault.utils.PinValidator
import com.amanp20.securevault.viewmodel.CreatePinViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class CreatePinFragment : BaseBindingFragment<FragmentCreatePinBinding>() {

    private val viewModel: CreatePinViewModel by viewModels {
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreatePinBinding {
        return FragmentCreatePinBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPinLengthSelector()
        setupListeners()
        observeViewModel()
    }

    private fun setupPinLengthSelector() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.pin_lengths,
            android.R.layout.simple_list_item_1
        )
        binding.pinLengthDropdown.setAdapter(adapter)
        binding.pinLengthDropdown.setText(getString(R.string.pin_length_4), false)
    }

    private fun setupListeners() {
        binding.pinInput.doOnTextChanged { _, _, _, _ -> clearErrors() }
        binding.confirmPinInput.doOnTextChanged { _, _, _, _ -> clearErrors() }
        binding.createPinButton.setOnClickListener { submitPin() }
    }

    private fun submitPin() {
        val pinLength = selectedPinLength()
        val pin = PinValidator.normalizedPin(binding.pinInput.text?.toString().orEmpty())
        val confirmPin = PinValidator.normalizedPin(binding.confirmPinInput.text?.toString().orEmpty())

        if (!PinValidator.isValidPin(pin, pinLength)) {
            binding.pinLayout.error = getString(R.string.error_pin_length)
            return
        }

        if (pin != confirmPin) {
            binding.confirmPinLayout.error = getString(R.string.error_pin_mismatch)
            return
        }

        viewModel.createPin(pin, pinLength)
    }

    private fun observeViewModel() {
        viewModel.createdPin.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_createPinFragment_to_homeFragment)
        })

        viewModel.error.observe(viewLifecycleOwner, EventObserver { errorMessage ->
            binding.confirmPinLayout.error = errorMessage
        })
    }

    private fun selectedPinLength(): Int {
        return if (binding.pinLengthDropdown.text?.toString() == getString(R.string.pin_length_6)) 6 else 4
    }

    private fun clearErrors() {
        binding.pinLayout.error = null
        binding.confirmPinLayout.error = null
    }
}
