package com.hoker.intra_example.presentation

import android.nfc.Tag
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.hoker.intra.domain.NfcAdapterController
import com.hoker.intra.domain.NfcController
import com.hoker.intra.domain.NfcViewModel
import com.hoker.intra.domain.OperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntraExampleViewModel @Inject constructor(
    nfcAdapterController: NfcAdapterController
): NfcViewModel(nfcAdapterController) {

    private val _jwtText: MutableState<String?> = mutableStateOf(null)
    val jwtText: String?
        get() { return _jwtText.value }

    private val _errorChannel = Channel<String>(Channel.BUFFERED)
    var errorFlow = _errorChannel.receiveAsFlow()

    override fun onNfcTagDiscovered(tag: Tag, nfcController: NfcController) {
        viewModelScope.launch(Dispatchers.IO) {
            nfcController.withConnection(tag) {
                when (val result = nfcController.getVivokeyJwt(tag)) {
                    is OperationResult.Success -> {
                        _jwtText.value = result.data
                    }
                    is OperationResult.Failure -> {
                        _jwtText.value = null
                        _errorChannel.trySend(result.exception?.message ?: "Error getting JWT")
                    }
                }
            }
        }
    }
}