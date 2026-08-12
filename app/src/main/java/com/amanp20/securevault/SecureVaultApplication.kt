package com.amanp20.securevault

import android.app.Application
import com.amanp20.securevault.utils.AppContainer

class SecureVaultApplication : Application() {

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
