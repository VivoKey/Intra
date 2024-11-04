package com.hoker.intra_example.presentation

import android.nfc.Tag
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.hoker.intra.domain.NfcAdapterController
import com.hoker.intra.domain.NfcController
import com.hoker.intra.domain.NfcViewModel
import com.hoker.intra.domain.OperationResult
import com.hoker.intra_example.domain.models.OperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Hex
import javax.inject.Inject

@HiltViewModel
class IntraExampleViewModel @Inject constructor(
    nfcAdapterController: NfcAdapterController
): NfcViewModel(nfcAdapterController) {

    private val _selectedCommand = mutableStateOf(OperationType.JWT)
    var selectedCommand: OperationType
        get() { return _selectedCommand.value }
        set(value) { _selectedCommand.value = value }

    private val _output: MutableState<String?> = mutableStateOf(null)
    var output: String?
        get() { return _output.value }
        set(value) { _output.value = value }

    private val _errorChannel = Channel<String>(Channel.BUFFERED)
    var errorFlow = _errorChannel.receiveAsFlow()

    private val _showOperationOptions = mutableStateOf(false)
    var showOperationOptions: Boolean
        get() { return _showOperationOptions.value }
        set(value) { _showOperationOptions.value = value }

    override fun onNfcTagDiscovered(tag: Tag, nfcController: NfcController) {
        viewModelScope.launch(Dispatchers.IO) {
            nfcController.withConnection(tag) {
                when (_selectedCommand.value) {
                    OperationType.JWT -> {
                        when (val result = nfcController.getVivokeyJwt(tag)) {
                            is OperationResult.Success -> {
                                _output.value = result.data
                            }
                            is OperationResult.Failure -> {
                                _output.value = "ERROR:\n${result.exception?.message ?: "Unknown error"}"
                                _errorChannel.trySend(result.exception?.message ?: "Error getting JWT")
                            }
                        }
                    }
                    OperationType.GET_VERSION -> {
                        when (val result = nfcController.getVersion()) {
                            is OperationResult.Success -> {
                                _output.value = Hex.encodeHexString(result.data)
                            }
                            is OperationResult.Failure -> {
                                _output.value = "ERROR:\n${result.exception?.message ?: "Unknown error"}"
                                _errorChannel.trySend(result.exception?.message ?: "Error executing Get Version")
                            }
                        }
                    }
                    OperationType.ATR -> {
                        when (val result = nfcController.getAtr()) {
                            is OperationResult.Success -> {
                                _output.value = Hex.encodeHexString(result.data)
                            }
                            is OperationResult.Failure -> {
                                _output.value = "ERROR:\n${result.exception?.message ?: "Unknown error"}"
                                _errorChannel.trySend(result.exception?.message ?: "Error getting ATR")
                            }
                        }
                    }
                }
            }
        }
    }
}