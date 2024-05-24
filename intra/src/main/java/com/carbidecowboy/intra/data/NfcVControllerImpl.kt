package com.carbidecowboy.intra.data

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcV
import android.util.Log
import com.carbidecowboy.intra.di.IntraAuthApiService
import com.carbidecowboy.intra.domain.ApduUtils
import com.carbidecowboy.intra.domain.AuthApiService
import com.carbidecowboy.intra.domain.NfcController
import com.carbidecowboy.intra.domain.OperationResult
import com.carbidecowboy.intra.domain.Timer
import com.carbidecowboy.intra.domain.request.ChallengeRequest
import com.carbidecowboy.intra.domain.request.SessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.codec.binary.Hex
import java.nio.ByteBuffer
import javax.inject.Inject

class NfcVControllerImpl @Inject constructor(
    @IntraAuthApiService private val authApiService: AuthApiService,
    private val timer: Timer
): NfcController {

    companion object {
        private const val UID_BYTE_LENGTH = 8
    }

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus: StateFlow<Boolean>
        get() = _connectionStatus.asStateFlow()

    private var nfcV: NfcV? = null
    private var timerJob: Job? = null

    override suspend fun connect(tag: Tag): OperationResult<Unit> {
        return try {
            close()
            nfcV = NfcV.get(tag)
            nfcV?.connect()
            startConnectionCheckJob()
            _connectionStatus.emit(true)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override suspend fun close() {
        try {
            nfcV?.close()
        } catch(e: Exception) {
            Log.d(this::class.java.simpleName, "Tag was out of date")
        }
        stopConnectionCheckJob()
        nfcV = null
        _connectionStatus.emit(false)
    }

    override suspend fun getAts(): OperationResult<ByteArray?> {
        return OperationResult.Failure()
    }

    override suspend fun getAtr(): OperationResult<ByteArray?> {
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
                var response = splitApduResponse(nfcV!!.transceive(apdu))
                while (response.statusCode != ApduUtils.APDU_OK) {
                    if ((response.statusCode shr 8).toByte() == ApduUtils.APDU_DATA_REMAINING.toByte()) {
                        put(response.data)
                        response = splitApduResponse(
                            nfcV!!.transceive(
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

    override suspend fun transceive(data: ByteArray): OperationResult<ByteArray> {
        return try {
            OperationResult.Success(nfcV!!.transceive(data))
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

    override suspend fun getNdefCapacity(ndef: Ndef): OperationResult<Int> {
        return try {
            val result = ndef.maxSize
            OperationResult.Success(result)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override suspend fun getVivokeyJwt(tag: Tag): OperationResult<String> {
        return try {

            withContext(Dispatchers.IO) {

                //get challenge from
                val challengeRequest = ChallengeRequest(1)
                val challengeResponse = async {
                    authApiService.postChallenge(challengeRequest).body()
                }.await()

                if (challengeResponse == null) {
                    OperationResult.Failure()
                }

                val nfcV = NfcV.get(tag)
                nfcV.connect()

                // truncate challenge to 10 bytes
                // challenge string into hex
                val challengeBytes: ByteArray =
                    Hex.decodeHex(challengeResponse!!.payload.substring(0, 20))
                val command = ByteArray(15 + UID_BYTE_LENGTH)
                // Spark 1 flag mode (addressed command)
                command[0] = 0x20
                // authentication command code
                command[1] = 0x35
                // copy into command byte array
                tag.id.copyInto(command, 2, 0)
                // CSI (AES = 0x00)
                command[UID_BYTE_LENGTH + 2] = 0x00
                // RFU
                command[UID_BYTE_LENGTH + 3] = 0x00
                // Key slot as byte (only supporting slot 2 for now)
                command[UID_BYTE_LENGTH + 4] = 0x02
                // copy the challenge
                challengeBytes.copyInto(command, UID_BYTE_LENGTH + 5, 0)
                // connect and send command
                Log.i("Command", Hex.encodeHexString(command))
                val response = nfcV.transceive(command)
                nfcV.close()
                Log.i("Response", Hex.encodeHexString(response))

                val sessionRequest = SessionRequest(
                    uid = Hex.encodeHexString(tag.id!!.reversedArray()),
                    response = Hex.encodeHexString(response),
                    token = challengeResponse.token
                )

                val sessionResponse = async {
                    authApiService.postSession(sessionRequest)
                }.await()

                if (!sessionResponse.isSuccessful) {
                    OperationResult.Failure()
                }

                val result = sessionResponse.body()
                result?.token?.let { jwt ->
                    println(jwt)
                    return@withContext OperationResult.Success(jwt)
                }

                OperationResult.Failure()
            }
        } catch (e: Exception) {
            Log.i(this@NfcVControllerImpl::class.java.name, e.message.toString())
            OperationResult.Failure(e)
        }
    }

    override suspend fun getNdefMessage(ndef: Ndef): OperationResult<NdefMessage> {
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
            OperationResult.Success(nfcV?.isConnected ?: false)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }
}