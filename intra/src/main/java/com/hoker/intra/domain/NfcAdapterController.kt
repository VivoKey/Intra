package com.hoker.intra.domain

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

    fun enableNfc(activity: NfcActivity) {
        nfcAdapter?.let { adapter ->
            val flags = NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)

            adapter.enableReaderMode(
                activity,
                { tag ->
                    activity.onScanStartedCallback?.invoke()
                    onTagDiscoveredListener?.invoke(tag)
                    activity.onScanEndedCallback?.invoke()
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
                appendLine("Class: ${it.first}, HashCode: ${it.second.hashCode()}")
            }
        }
        Log.i(this@NfcAdapterController::class.simpleName, log)
    }
}