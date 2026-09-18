package com.amanp20.securevault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amanp20.securevault.data.repository.SecureVaultRepository

class ViewModelFactory(
    private val repository: SecureVaultRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(repository)
            modelClass.isAssignableFrom(CreatePinViewModel::class.java) -> CreatePinViewModel(repository)
            modelClass.isAssignableFrom(LockViewModel::class.java) -> LockViewModel(repository)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository)
            modelClass.isAssignableFrom(SecureDocumentViewModel::class.java) -> SecureDocumentViewModel(repository)
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
