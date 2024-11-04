package com.hoker.intra.domain

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import kotlinx.coroutines.flow.StateFlow
import org.apache.commons.codec.binary.Hex
import java.nio.ByteBuffer

interface NfcController {
    val connectionStatus: StateFlow<Boolean>
    suspend fun connect(tag: Tag): OperationResult<Unit>
    suspend fun close()
    suspend fun getAts(): OperationResult<ByteArray?>
    suspend fun getAtr(): OperationResult<ByteArray?>
    suspend fun issueApdu(instruction: Byte, p1: Byte = 0, p2: Byte = 0, data: ByteBuffer.() -> Unit = {}): OperationResult<ByteBuffer>
    suspend fun transceive(data: ByteArray): OperationResult<ByteArray>
    suspend fun writeNdefMessage(tag: Tag, message: NdefMessage): OperationResult<Unit>
    suspend fun getVivokeyJwt(tag: Tag): OperationResult<String>
    suspend fun getNdefCapacity(ndef: Ndef): OperationResult<Int>
    suspend fun getNdefMessage(ndef: Ndef): OperationResult<NdefMessage?>
    suspend fun checkConnection(): OperationResult<Boolean>
    fun getMaxTransceiveLength(): Int?
    suspend fun withNdefConnection(tag: Tag, operations: suspend (ndef: Ndef) -> Unit): OperationResult<Unit> {
        return try {
            close()
            val ndef = Ndef.get(tag)
            ndef.connect()
            operations(ndef)
            ndef.close()
            OperationResult.Success(Unit)
        } catch(e: Exception) {
            OperationResult.Failure(e)
        }
    }

    suspend fun withConnection(tag: Tag, operations: suspend () -> Unit): OperationResult<Unit> {
        return try {
            connect(tag)
            operations()
            close()
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    suspend fun getVersion(): OperationResult<ByteArray> {
        return try {
            val getVersionCommand = byteArrayOf(0x60.toByte())
            when (val response = transceive(getVersionCommand)) {
                is OperationResult.Success -> {
                    if (Hex.encodeHexString(response.data) == Consts.WRONG_LENGTH) {
                        val getVersionApduResult = transceive(byteArrayOf(
                            0x90.toByte(),
                            0x60.toByte(),
                            0x00.toByte(),
                            0x00.toByte(),
                            0x00.toByte()
                        ))
                        when (getVersionApduResult) {
                            is OperationResult.Success -> {
                                OperationResult.Success(getVersionApduResult.data)
                            }

                            is OperationResult.Failure -> {
                                OperationResult.Failure(getVersionApduResult.exception)
                            }
                        }
                    } else {
                        OperationResult.Success(response.data)
                    }
                }
                is OperationResult.Failure -> {
                    OperationResult.Failure(response.exception)
                }
            }
        } catch(e: Exception) {
            OperationResult.Failure(e)
        }
    }
}