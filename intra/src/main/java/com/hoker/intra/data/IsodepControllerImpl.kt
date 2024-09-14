package com.hoker.intra.data

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.Log
import com.hoker.intra.di.IntraAuthApiService
import com.hoker.intra.domain.ApduUtils
import com.hoker.intra.domain.AuthApiService
import com.hoker.intra.domain.NfcController
import com.hoker.intra.domain.OperationResult
import com.hoker.intra.domain.Timer
import com.hoker.intra.domain.request.ChallengeRequest
import com.hoker.intra.domain.request.SessionRequest
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
import kotlin.experimental.xor

class IsodepControllerImpl @Inject constructor(
    @IntraAuthApiService private val authApiService: AuthApiService,
    private val timer: Timer
) : NfcController {

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus: StateFlow<Boolean>
        get() = _connectionStatus.asStateFlow()

    private var isoDep: IsoDep? = null
    private var timerJob: Job? = null

    companion object {
        private val NDEF_SEL: ByteArray = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x0C.toByte(), 0x07.toByte(), 0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte())
        private val WRONG_RNDB: ByteArray = byteArrayOf(0x91.toByte(), 0xAE.toByte())
    }

    override suspend fun connect(tag: Tag): OperationResult<Unit> {
        close()
        isoDep = IsoDep.get(tag)
        return try {
            isoDep?.let {
                it.connect()
                it.timeout = 20000
                _connectionStatus.emit(true)
                startConnectionCheckJob()
                OperationResult.Success(Unit)
            }
            OperationResult.Failure(Exception("IsoDep.connect() came back as null"))
        } catch (e: Exception) {
            return OperationResult.Failure(e)
        }
    }

    override suspend fun close() {
        try {
            isoDep?.close()
        } catch(e: Exception) {
            Log.d(this::class.java.simpleName, "Tag was out of date")
        }
        isoDep = null
        stopConnectionCheckJob()
        _connectionStatus.emit(false)
    }

    override suspend fun transceive(data: ByteArray): OperationResult<ByteArray> {
        return try {
            Log.i("${this@IsodepControllerImpl::class.simpleName}.transceive", "Transceive data: ${Hex.encodeHexString(data)}")
            val result = isoDep?.transceive(data) ?: return OperationResult.Failure(Exception("transceive(): IsoDep was null"))
            Log.i("${this@IsodepControllerImpl::class.simpleName}.transceive", "Transceive result: ${Hex.encodeHexString(result)}")
            OperationResult.Success(result)
        } catch (e: Exception) {
            close()
            OperationResult.Failure(e)
        }
    }

    override suspend fun getAts(): OperationResult<ByteArray?> {
        isoDep?.let {
            return OperationResult.Success(it.historicalBytes)
        }
        return OperationResult.Failure()
    }

    override suspend fun getAtr(): OperationResult<ByteArray?> {
        isoDep?.let {
            try {
                if (!it.isConnected) {
                    return OperationResult.Failure()
                }

                val historicalBytes = it.historicalBytes

                val atr = ByteArray(4 + historicalBytes.size + 1)
                atr[0] = 0x3b.toByte()
                atr[1] = (0x80.toByte() + historicalBytes.size).toByte()
                atr[2] = 0x80.toByte()
                atr[3] = 0x01.toByte()
                System.arraycopy(historicalBytes, 0, atr, 4, historicalBytes.size)

                var tck = atr[1]
                for (idx in 2 until atr.size) {
                    tck = tck.xor(atr[idx])
                }
                atr[atr.size - 1] = tck

                Log.i("ATR:", Hex.encodeHexString(atr))
                return OperationResult.Success(atr)
            } catch(e: Exception) {
                close()
                return OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override suspend fun getVivokeyJwt(
        tag: Tag,
    ): OperationResult<String> {
        return try {
            withContext(Dispatchers.IO) {

                // part 1 command
                var command = ByteArray(8)
                command[0] = 0x90.toByte()
                // "opcode" for authenticate part 1 command
                command[1] = 0x71.toByte()
                command[2] = 0x00.toByte()
                command[3] = 0x00.toByte()
                command[4] = 0x02.toByte()
                command[5] = 0x02.toByte()
                command[6] = 0x00.toByte()
                command[7] = 0x00.toByte()

                val isoDep = IsoDep.get(tag)
                isoDep.connect()

                //TODO: Add error check on this result
                isoDep.transceive(NDEF_SEL)

                // send part 1 command
                val part1Result = isoDep.transceive(command)

                var piccChallenge = part1Result
                piccChallenge = piccChallenge.copyOfRange(0, 16)
                println("PICC Challenge:\n${Hex.encodeHexString(piccChallenge)}\n\n")

                val challengeRequest = ChallengeRequest(
                    scheme = 2,
                    message = Hex.encodeHexString(piccChallenge),
                    uid = Hex.encodeHexString(tag.id)
                )

                val challengeResponse = async {
                    authApiService.postChallenge(challengeRequest).body()
                }.await()

                if (challengeResponse == null) {
                    OperationResult.Failure()
                }

                val pcdChallengeBytes = Hex.decodeHex(challengeResponse!!.payload)

                // part 2 command
                command = ByteArray(38)
                command[0] = 0x90.toByte()
                command[1] = 0xAF.toByte()
                command[2] = 0x00.toByte()
                command[3] = 0x00.toByte()
                command[4] = 0x20.toByte()
                pcdChallengeBytes.copyInto(command, 5, 0)
                command[37] = 0x00.toByte()

                Log.i("Part 2 Command", Hex.encodeHexString(command))
                val response = isoDep.transceive(command)
                isoDep.close()
                Log.i("Response", Hex.encodeHexString(response))

                val responseString = Hex.encodeHexString(response)

                val sessionRequest = SessionRequest(
                    uid = Hex.encodeHexString(tag.id),
                    response = responseString,
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
                    Log.i("JWT:", jwt)
                    return@withContext OperationResult.Success(jwt)
                }

                OperationResult.Failure()
            }
        } catch (e: Exception) {
            Log.i(this@IsodepControllerImpl::class.java.name, e.message.toString())
            OperationResult.Failure(e)
        }
    }

    override suspend fun issueApdu(instruction: Byte, p1: Byte, p2: Byte, data: ByteBuffer.() -> Unit): OperationResult<ByteBuffer> {
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
                Log.i("${this@IsodepControllerImpl::class.simpleName}", "APDU request: ${Hex.encodeHexString(apdu)}")
                var response = splitApduResponse(isoDep!!.transceive(apdu))
                while (response.statusCode != ApduUtils.APDU_OK) {
                    if ((response.statusCode shr 8).toByte() == ApduUtils.APDU_DATA_REMAINING.toByte()) {
                        put(response.data)
                        response = splitApduResponse(
                            isoDep!!.transceive(
                                byteArrayOf(
                                    0,
                                    ApduUtils.SEND_REMAINING_INS.toByte(),
                                    0,
                                    0
                                )
                            )
                        )
                    } else {
                        Log.i("${this@IsodepControllerImpl::class.simpleName}.issueApdu", "APDU response: ERROR!: ${Hex.encodeHexString(response.data)}")
                        return OperationResult.Failure(Exception(response.statusCode.toString(16)))
                    }
                }
                put(response.data).limit(position()).rewind()
            }
            Log.i("${this@IsodepControllerImpl::class.simpleName}.issueApdu", "APDU response: ${Hex.encodeHexString(buffer)}")
            return OperationResult.Success(buffer)
        } catch(e: Exception) {
            close()
            Log.i("${this@IsodepControllerImpl::class.simpleName}.issueApdu", "EXCEPTION ISSUING APDU: ${e.message.toString()}")
            return OperationResult.Failure(e)
        }
    }

    private fun splitApduResponse(resp: ByteArray): ApduUtils.Companion.ApduResponse {
        return ApduUtils.Companion.ApduResponse(
            resp.copyOfRange(0, resp.size - 2),
            ((0xff and resp[resp.size - 2].toInt()) shl 8) or (0xff and resp[resp.size - 1].toInt())
        )
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

    override suspend fun getNdefMessage(ndef: Ndef): OperationResult<NdefMessage?> {
        return try {
            val result = ndef.cachedNdefMessage
            OperationResult.Success(result)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override suspend fun checkConnection(): OperationResult<Boolean> {
        return try {
            OperationResult.Success(isoDep?.isConnected ?: false)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }

    private suspend fun startConnectionCheckJob() {
        timerJob?.cancel()
        timerJob = timer.repeatEverySecond {
            Log.i("ConnectionCheck", "ISODEP CONNECTION CHECK")
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
}