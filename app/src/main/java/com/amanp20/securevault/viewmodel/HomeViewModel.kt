package com.amanp20.securevault.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.amanp20.securevault.data.repository.SecureVaultRepository
import java.util.Calendar

class HomeViewModel(
    repository: SecureVaultRepository
) : ViewModel() {

    val documentCount: LiveData<Int> = repository.documentCount
    val noteCount: LiveData<Int> = repository.noteCount

    val greeting: String = buildGreeting()

    val databaseStatus = repository.databaseStatus

    private fun buildGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Welcome back"
        }
    }
}
