package com.amanp20.securevault.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

fun <T> MutableLiveData<T>.addSourceLiveData(source: LiveData<T>) {
    source.observeForever(object : Observer<T> {
        override fun onChanged(value: T) {
            this@addSourceLiveData.value = value
        }
    })
}
