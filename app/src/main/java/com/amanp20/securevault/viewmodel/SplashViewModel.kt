package com.amanp20.securevault.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.repository.SecureVaultRepository
import kotlinx.coroutines.launch

sealed class SplashDestination {
    data object CreatePin : SplashDestination()
    data object Lock : SplashDestination()
}

class SplashViewModel(
    private val repository: SecureVaultRepository
) : ViewModel() {

    private val destinationMutable = MutableLiveData<SplashDestination>()
    val destination: LiveData<SplashDestination> = destinationMutable

    fun decideDestination() {
        viewModelScope.launch {
            destinationMutable.value = if (repository.hasStoredPin()) {
                SplashDestination.Lock
            } else {
                SplashDestination.CreatePin
            }
        }
    }
}
