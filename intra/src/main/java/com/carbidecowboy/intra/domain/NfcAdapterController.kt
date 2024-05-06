package com.carbidecowboy.intra.domain

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import javax.inject.Inject

open class NfcAdapterController @Inject constructor(
    private val nfcAdapter: NfcAdapter?
) {
    private var onTagDiscoveredListener: ((Tag?) -> Unit)? = null
    private val listenerMap = LinkedHashMap<String, Pair<String, (Tag?) -> Unit>>()

    fun enableNfc(activity: Activity) {
        nfcAdapter?.let { adapter ->
            val flags = NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_A
            val options = Bundle()

            adapter.enableReaderMode(
                activity,
                { tag ->
                    Log.d(this@NfcAdapterController::class.simpleName,"onTagDiscoveredListener: ${onTagDiscoveredListener.hashCode()}")
                    onTagDiscoveredListener?.invoke(tag)
                },
                flags,
                options
            )
        }
    }

    fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }

    fun disableNfc(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    fun setOnTagDiscoveredListener(
        uuid: String,
        className: String,
        listener: (Tag?) -> Unit
    ) {
        listenerMap.remove(uuid)
        listenerMap[uuid] = className to listener
        updateListener()
        logCurrentListeners()
    }

    fun removeOnTagDiscoveredListener(uuid: String) {
        listenerMap.remove(uuid)
        updateListener()
        logCurrentListeners()
    }

    private fun updateListener() {
        val lastEntry = listenerMap.values.lastOrNull()
        onTagDiscoveredListener = lastEntry?.second
    }

    private fun logCurrentListeners() {
        val log = buildString {
            append("Current listener queue: ")
            listenerMap.values.forEach {
                append("Class: ${it.first}, HashCode: ${it.second.hashCode()}")
            }
        }
        Log.i(this@NfcAdapterController::class.simpleName, log)
    }
}