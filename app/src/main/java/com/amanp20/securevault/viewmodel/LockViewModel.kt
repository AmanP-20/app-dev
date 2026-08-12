package com.amanp20.securevault.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.repository.SecureVaultRepository
import com.amanp20.securevault.utils.Event
import kotlinx.coroutines.launch

class LockViewModel(
    private val repository: SecureVaultRepository
) : ViewModel() {

    private val authenticatedMutable = MutableLiveData<Event<Unit>>()
    val authenticated: LiveData<Event<Unit>> = authenticatedMutable

    private val pinErrorMutable = MutableLiveData<Event<String>>()
    val pinError: LiveData<Event<String>> = pinErrorMutable

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            val isValid = repository.verifyPin(pin)
            if (isValid) {
                authenticatedMutable.value = Event(Unit)
            } else {
                pinErrorMutable.value = Event("Wrong PIN")
            }
        }
    }
}
