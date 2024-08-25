package com.hoker.intra.domain

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import kotlinx.coroutines.flow.StateFlow
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
    suspend fun getNdefMessage(ndef: Ndef): OperationResult<NdefMessage>
    suspend fun checkConnection(): OperationResult<Boolean>
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
}