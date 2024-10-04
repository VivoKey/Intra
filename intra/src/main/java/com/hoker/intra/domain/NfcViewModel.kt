package com.hoker.intra.domain

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import java.util.UUID

abstract class NfcViewModel(
    private val nfcAdapterController: NfcAdapterController,
    setAsActiveOnInjection: Boolean = true,
): ViewModel() {

    private val uuid = UUID.randomUUID().toString()
    private val className = this::class.simpleName ?: "UnknownViewModel"

    abstract fun onNfcTagDiscovered(tag: Tag, nfcController: NfcController)

    init {
        if (setAsActiveOnInjection) {
            setAsActiveListener()
        }
    }

    fun setAsActiveListener() {
        nfcAdapterController.setOnTagDiscoveredListener(uuid, className) { tag, nfcController ->
            onNfcTagDiscovered(tag, nfcController)
        }
    }

    override fun onCleared() {
        super.onCleared()
        nfcAdapterController.removeOnTagDiscoveredListener(uuid)
    }
}