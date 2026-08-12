package com.amanp20.securevault.data.model

sealed class DatabaseStatus {
    data object Ready : DatabaseStatus()
    data class Error(val message: String) : DatabaseStatus()
}
