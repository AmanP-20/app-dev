package com.amanp20.securevault.utils

import android.content.Context
import com.amanp20.securevault.data.repository.SecureVaultRepository
import com.amanp20.securevault.data.repository.SecureVaultRepositoryImpl

class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    val secureVaultRepository: SecureVaultRepository by lazy {
        SecureVaultRepositoryImpl(applicationContext)
    }
}
