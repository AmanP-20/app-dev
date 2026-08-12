package com.amanp20.securevault.utils

object PinValidator {

    fun isSupportedLength(length: Int): Boolean {
        return length == 4 || length == 6
    }

    fun isValidPin(pin: String, length: Int): Boolean {
        return isSupportedLength(length) && pin.length == length && pin.all(Char::isDigit)
    }

    fun normalizedPin(pin: String): String {
        return pin.filter(Char::isDigit)
    }
}
