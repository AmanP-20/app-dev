package com.amanp20.securevault

import android.app.Application
import com.amanp20.securevault.utils.AppContainer

class SecureVaultApplication : Application() {

    @Volatile
    var isSessionAuthenticated: Boolean = false

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
