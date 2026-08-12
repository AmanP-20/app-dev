package com.amanp20.securevault.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.repository.SecureVaultRepository
import com.amanp20.securevault.utils.Event
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SecureVaultRepository
) : ViewModel() {

    val biometricEnabled: Boolean
        get() = repository.isBiometricEnabled()

    val databaseStatus = repository.databaseStatus

    private val biometricSavedMutable = MutableLiveData<Event<Unit>>()
    val biometricSaved: LiveData<Event<Unit>> = biometricSavedMutable

    private val changePinSuccessMutable = MutableLiveData<Event<Unit>>()
    val changePinSuccess: LiveData<Event<Unit>> = changePinSuccessMutable

    private val settingsErrorMutable = MutableLiveData<Event<String>>()
    val settingsError: LiveData<Event<String>> = settingsErrorMutable

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
            biometricSavedMutable.value = Event(Unit)
        }
    }

    fun changePin(currentPin: String, newPin: String, newPinLength: Int) {
        viewModelScope.launch {
            try {
                repository.changePin(currentPin, newPin, newPinLength)
                changePinSuccessMutable.value = Event(Unit)
            } catch (exception: IllegalStateException) {
                settingsErrorMutable.value = Event(exception.message.orEmpty())
            } catch (exception: Exception) {
                settingsErrorMutable.value = Event(exception.message.orEmpty())
            }
        }
    }
}
