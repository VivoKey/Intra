package com.hoker.intra.data

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.util.Log
import com.hoker.intra.domain.ApduUtils
import com.hoker.intra.domain.NfcController
import com.hoker.intra.domain.OperationResult
import com.hoker.intra.domain.Timer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.experimental.xor

class NfcAControllerImpl @Inject constructor(
    private val timer: Timer
): NfcController {

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus: StateFlow<Boolean>
        get() = _connectionStatus.asStateFlow()

    private var nfcA: NfcA? = null
    private var timerJob: Job? = null

    override suspend fun connect(tag: Tag): OperationResult<Unit> {
        return try {
            close()
            nfcA = NfcA.get(tag)
            nfcA?.let {
                it.connect()
                it.timeout = 20000
                Log.i("ApexConnection", "----NFC_A CONNECTED")
                startConnectionCheckJob()
                _connectionStatus.emit(true)
                OperationResult.Success(Unit)
            }
            OperationResult.Failure(Exception("NfcA.connect() came back as null"))
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override suspend fun close() {
        try {
            nfcA?.close()
        } catch(e: Exception) {
            Log.d(this::class.java.simpleName, "Tag was out of date")
        }
        stopConnectionCheckJob()
        nfcA = null
        Log.i("ApexConnection", "----NFC_A CLOSED")
        _connectionStatus.emit(false)
    }

    override suspend fun getAtr(): OperationResult<ByteArray?> {
        nfcA?.let {
            try {
                val atr = mutableListOf<Byte>()
                //Initial Header
                atr.add(0x3b.toByte())
                //T0
                atr.add(0x8f.toByte())
                //TD1
                atr.add(0x80.toByte())
                //TD2
                atr.add(0x01.toByte())
                //T1
                atr.add(0x80.toByte())
                //Application identifier presence indicator
                atr.add(0x4f.toByte())
                //length
                atr.add(0x0c.toByte())
                //RID
                atr.addAll(
                    byteArrayOf(
                        0xa0.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x03.toByte(),
                        0x06.toByte()
                    ).toList()
                )
                //Standard
                atr.add(0x03.toByte())
                //Card name
                atr.addAll(byteArrayOf(0x00.toByte(), 0x00.toByte()).toList())
                //RFU
                atr.addAll(
                    byteArrayOf(
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte()
                    ).toList()
                )
                //TCK
                var tck = atr[1]
                for (idx in 2 until atr.size) {
                    tck = tck.xor(atr[idx])
                }
                atr.add(tck)

                return OperationResult.Success(atr.toByteArray())
            } catch(e: Exception) {
                return OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override suspend fun issueApdu(
        instruction: Byte,
        p1: Byte,
        p2: Byte,
        data: ByteBuffer.() -> Unit
    ): OperationResult<ByteBuffer> {
        try {
            val apdu = ByteBuffer
                .allocate(256)
                .put(0)
                .put(instruction)
                .put(p1)
                .put(p2)
                .put(0)
                .apply(data)
                .let {
                    it.put(4, (it.position() - 5).toByte()).array()
                        .copyOfRange(0, it.position())
                }

            val buffer = ByteBuffer.allocate(4096).apply {
                var response = splitApduResponse(nfcA!!.transceive(apdu))
                while (response.statusCode != ApduUtils.APDU_OK) {
                    if ((response.statusCode shr 8).toByte() == ApduUtils.APDU_DATA_REMAINING.toByte()) {
                        put(response.data)
                        response = splitApduResponse(
                            nfcA!!.transceive(
                                byteArrayOf(
                                    0,
                                    ApduUtils.SEND_REMAINING_INS.toByte(),
                                    0,
                                    0
                                )
                            )
                        )
                    } else {
                        return OperationResult.Failure()
                    }
                }
                put(response.data).limit(position()).rewind()
            }
            return OperationResult.Success(buffer)
        } catch(e: Exception) {
            close()
            return OperationResult.Failure(e)
        }
    }

    private fun splitApduResponse(resp: ByteArray): ApduUtils.Companion.ApduResponse {
        return ApduUtils.Companion.ApduResponse(
            resp.copyOfRange(0, resp.size - 2),
            ((0xff and resp[resp.size - 2].toInt()) shl 8) or (0xff and resp[resp.size - 1].toInt())
        )
    }

    override suspend fun getAts(): OperationResult<ByteArray?> {
        nfcA?.let {
            return try {
                val uid = it.tag.id
                val sak = it.sak
                val sakByte = sak.toByte()
                OperationResult.Success(uid + sakByte)
            } catch(e: Exception) {
                OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override suspend fun transceive(data: ByteArray): OperationResult<ByteArray> {
        return try {
            OperationResult.Success(nfcA!!.transceive(data))
        } catch(e: Exception) {
            close()
            OperationResult.Failure(e)
        }
    }

    override suspend fun writeNdefMessage(tag: Tag, message: NdefMessage): OperationResult<Unit> {
        return try {
            val ndef = Ndef.get(tag)
            ndef.connect()
            ndef.writeNdefMessage(message)
            ndef.close()
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override fun getMaxTransceiveLength(): Int? {
        return nfcA?.maxTransceiveLength
    }

    override suspend fun getNdefCapacity(ndef: Ndef): OperationResult<Int> {
        return try {
            val result = ndef.maxSize
            OperationResult.Success(result)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override suspend fun getVivokeyJwt(
        tag: Tag,
        cid: String?
    ): OperationResult<String> {
        return OperationResult.Failure(Exception("This operation is not currently supported with NfcA"))
    }

    override suspend fun getNdefMessage(ndef: Ndef): OperationResult<NdefMessage?> {
        return try {
            val result = ndef.cachedNdefMessage
            OperationResult.Success(result)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    private suspend fun startConnectionCheckJob() {
        timerJob?.cancel()
        timerJob = timer.repeatEverySecond {
            Log.i("ConnectionCheck", "CONNECTION CHECK")
            when (val isConnected = checkConnection()) {
                is OperationResult.Success -> {
                    if (!isConnected.data) {
                        close()
                    }
                }
                is OperationResult.Failure -> {
                    close()
                }
            }
        }
    }

    private fun stopConnectionCheckJob() {
        timerJob?.cancel()
    }

    override suspend fun checkConnection(): OperationResult<Boolean> {
        return try {
            OperationResult.Success(nfcA?.isConnected ?: false)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }
}