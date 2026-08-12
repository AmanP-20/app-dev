package com.amanp20.securevault.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.databinding.FragmentSplashBinding
import com.amanp20.securevault.ui.common.BaseBindingFragment
import com.amanp20.securevault.viewmodel.SplashDestination
import com.amanp20.securevault.viewmodel.SplashViewModel
import com.amanp20.securevault.viewmodel.ViewModelFactory

class SplashFragment : BaseBindingFragment<FragmentSplashBinding>() {

    private val viewModel: SplashViewModel by viewModels {
        ViewModelFactory((requireActivity().application as SecureVaultApplication).appContainer.secureVaultRepository)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSplashBinding {
        return FragmentSplashBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.destination.observe(viewLifecycleOwner) { destination ->
            when (destination) {
                SplashDestination.CreatePin -> findNavController().navigate(R.id.action_splashFragment_to_createPinFragment)
                SplashDestination.Lock -> findNavController().navigate(R.id.action_splashFragment_to_lockFragment)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ viewModel.decideDestination() }, 2000)
        binding.logo.setImageResource(R.drawable.ic_secure_vault_logo)
    }
}
