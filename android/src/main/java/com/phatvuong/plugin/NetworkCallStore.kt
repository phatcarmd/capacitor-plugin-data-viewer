package com.phatvuong.plugin

import androidx.lifecycle.MutableLiveData

object NetworkCallStore {
    private const val MAX_CALLS = 200

    val calls = MutableLiveData<List<NetworkCall>>(emptyList())

    @Synchronized
    fun add(call: NetworkCall) {
        val current = calls.value?.toMutableList() ?: mutableListOf()
        current.add(0, call)
        if (current.size > MAX_CALLS) current.removeAt(current.size - 1)
        calls.postValue(current)
    }

    fun clear() {
        calls.postValue(emptyList())
    }
}
