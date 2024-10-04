package com.hoker.intra.domain

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import com.hoker.intra.di.NfcModule
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

open class NfcAdapterController @Inject constructor(
    private val nfcAdapter: NfcAdapter?,
    private val nfcControllerFactory: NfcModule.NfcControllerFactory
) {
    private var onTagDiscoveredListener: ((Tag, NfcController) -> Unit)? = null
    private val listenerMap = LinkedHashMap<String, Pair<String, (Tag, NfcController) -> Unit>>()
    private val _onScanChannel = Channel<Unit>(Channel.BUFFERED)
    val scanEvent = _onScanChannel.receiveAsFlow()

    fun enableNfc(activity: NfcActivity) {
        nfcAdapter?.let { adapter ->
            val flags = NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)

            adapter.enableReaderMode(
                activity,
                { tag ->
                    when (val result = nfcControllerFactory.getController(tag)) {
                        is OperationResult.Success -> {
                            _onScanChannel.trySend(Unit)
                            onTagDiscoveredListener?.invoke(tag, result.data)
                        }
                        is OperationResult.Failure -> {
                            Log.i(this@NfcAdapterController::class.simpleName, "There was an error constructing the NfcController")
                        }
                    }
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
        listener: (Tag, NfcController) -> Unit
    ) {
        listenerMap.remove(uuid)
        listenerMap[uuid] = className to listener
        updateListener()
        logCurrentListeners()
    }

    fun setOnTagDiscoveredListener(
        listener: (Tag, NfcController) -> Unit
    ) {
        listenerMap.clear()
        onTagDiscoveredListener = listener
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