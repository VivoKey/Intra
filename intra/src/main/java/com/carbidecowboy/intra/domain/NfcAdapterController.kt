package com.carbidecowboy.intra.domain

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import javax.inject.Inject

open class NfcAdapterController @Inject constructor(
    private val nfcAdapter: NfcAdapter?
) {
    private var onTagDiscoveredListener: ((Tag?) -> Unit)? = null
    private val listenersStack = ArrayDeque<(Tag?) -> Unit>()

    fun enableNfc(activity: Activity) {
        nfcAdapter?.let { adapter ->
            val flags = NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_A
            val options = Bundle()

            adapter.enableReaderMode(
                activity,
                { tag ->
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
        listener: (Tag?) -> Unit
    ) {
        listenersStack.addLast(listener)
        updateListener()
    }

    fun removeOnTagDiscoveredListener() {
        listenersStack.removeLast()
        updateListener()
    }

    private fun updateListener() {
        val currentListener = listenersStack.last()
        this.onTagDiscoveredListener = currentListener
    }
}