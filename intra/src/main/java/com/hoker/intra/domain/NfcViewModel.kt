package com.hoker.intra.domain

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import com.hoker.intra.di.NfcModule
import java.util.UUID

abstract class NfcViewModel(
    private val nfcAdapterController: NfcAdapterController,
    private val nfcControllerFactory: NfcModule.NfcControllerFactory,
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
        nfcAdapterController.setOnTagDiscoveredListener(uuid, className) { tag ->
            tag?.let {
                val nfcControllerResult = nfcControllerFactory.getController(tag)
                if (nfcControllerResult is OperationResult.Success) {
                    onNfcTagDiscovered(tag, nfcControllerResult.data)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nfcAdapterController.removeOnTagDiscoveredListener(uuid)
    }
}