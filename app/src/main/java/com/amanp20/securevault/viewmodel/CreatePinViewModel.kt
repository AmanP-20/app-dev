package com.amanp20.securevault.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.repository.SecureVaultRepository
import com.amanp20.securevault.utils.Event
import kotlinx.coroutines.launch

class CreatePinViewModel(
    private val repository: SecureVaultRepository
) : ViewModel() {

    private val createdPinMutable = MutableLiveData<Event<Unit>>()
    val createdPin: LiveData<Event<Unit>> = createdPinMutable

    private val errorMutable = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> = errorMutable

    fun createPin(pin: String, pinLength: Int) {
        viewModelScope.launch {
            try {
                repository.saveNewPin(pin, pinLength)
                createdPinMutable.value = Event(Unit)
            } catch (exception: Exception) {
                errorMutable.value = Event(exception.message.orEmpty())
            }
        }
    }
}
