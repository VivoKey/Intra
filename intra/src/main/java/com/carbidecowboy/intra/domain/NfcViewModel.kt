package com.carbidecowboy.intra.domain

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import com.carbidecowboy.intra.di.NfcModule

abstract class NfcViewModel(
    setAsActiveOnInjection: Boolean = true,
    private val nfcAdapterController: NfcAdapterController,
    private val nfcControllerFactory: NfcModule.NfcControllerFactory
): ViewModel() {

    abstract fun onNfcTagDiscovered(tag: Tag, nfcController: NfcController)

    init {
        if (setAsActiveOnInjection) {
            setAsActiveListener()
        }
    }

    fun setAsActiveListener() {
        nfcAdapterController.setOnTagDiscoveredListener { tag ->
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
        nfcAdapterController.removeOnTagDiscoveredListener()
    }
}