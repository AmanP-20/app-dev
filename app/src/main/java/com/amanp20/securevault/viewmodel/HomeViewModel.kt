package com.amanp20.securevault.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.amanp20.securevault.R
import com.amanp20.securevault.data.repository.SecureVaultRepository
import java.util.Calendar

class HomeViewModel(
    repository: SecureVaultRepository
) : ViewModel() {

    val documentCount: LiveData<Int> = repository.documentCount
    val noteCount: LiveData<Int> = repository.noteCount

    val greeting: Int = buildGreeting()

    val databaseStatus = repository.databaseStatus

    private fun buildGreeting(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..20 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }
    }
}
