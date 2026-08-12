package com.amanp20.securevault.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.databinding.FragmentLockBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.utils.BiometricCapability
import com.amanp20.securevault.utils.EventObserver
import com.amanp20.securevault.viewmodel.LockViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class LockFragment : BaseBindingFragment<FragmentLockBinding>() {

    private val viewModel: LockViewModel by viewModels {
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }

    private var biometricPrompt: BiometricPrompt? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLockBinding {
        return FragmentLockBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
        showBiometricIfAvailable()
    }

    private fun setupListeners() {
        binding.pinInput.doOnTextChanged { _, _, _, _ -> binding.pinLayout.error = null }
        binding.unlockButton.setOnClickListener { submitPin() }
        binding.usePinButton.setOnClickListener {
            binding.biometricContainer.visibility = View.GONE
            binding.pinContainer.visibility = View.VISIBLE
        }
    }

    private fun submitPin() {
        val pin = binding.pinInput.text?.toString().orEmpty()
        if (pin.isBlank()) {
            binding.pinLayout.error = getString(R.string.error_pin_required)
            return
        }
        viewModel.verifyPin(pin)
    }

    private fun observeViewModel() {
        viewModel.authenticated.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_lockFragment_to_homeFragment)
        })

        viewModel.pinError.observe(viewLifecycleOwner, EventObserver { errorMessage ->
            binding.pinLayout.error = errorMessage
        })
    }

    private fun showBiometricIfAvailable() {
        if (!BiometricCapability.isAvailable(requireContext())) {
            binding.biometricUnavailableText.visibility = View.VISIBLE
            binding.biometricContainer.visibility = View.GONE
            binding.pinContainer.visibility = View.VISIBLE
            return
        }

        if ((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository.isBiometricEnabled()) {
            startBiometricPrompt()
        } else {
            binding.biometricContainer.visibility = View.GONE
            binding.pinContainer.visibility = View.VISIBLE
        }
    }

    private fun startBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                findNavController().navigate(R.id.action_lockFragment_to_homeFragment)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                binding.biometricMessage.text = errString
                binding.pinContainer.visibility = View.VISIBLE
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                binding.biometricMessage.text = getString(R.string.error_biometric_failed)
            }
        })

        biometricPrompt?.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.use_pin))
                .build()
        )
    }
}
