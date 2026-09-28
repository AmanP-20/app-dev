package com.amanp20.securevault

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.amanp20.securevault.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val applicationState: SecureVaultApplication
        get() = application as SecureVaultApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
            ?.navController ?: return
        if (!applicationState.isSessionAuthenticated &&
            applicationState.appContainer.secureVaultRepository.hasStoredPin() &&
            navController.currentDestination?.id !in setOf(
                R.id.splashFragment,
                R.id.lockFragment,
                R.id.createPinFragment
            )
        ) {
            navController.navigate(R.id.lockFragment)
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations && applicationState.isSessionAuthenticated) {
            applicationState.isSessionAuthenticated = false
        }
        super.onStop()
    }
}
