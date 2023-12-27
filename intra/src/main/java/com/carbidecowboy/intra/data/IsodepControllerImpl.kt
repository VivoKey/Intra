package com.carbidecowboy.intra.data

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.Log
import com.carbidecowboy.intra.domain.ApduUtils
import com.carbidecowboy.intra.domain.AuthApiService
import com.carbidecowboy.intra.domain.NfcController
import com.carbidecowboy.intra.domain.OperationResult
import com.carbidecowboy.intra.domain.request.ChallengeRequest
import com.carbidecowboy.intra.domain.request.SessionRequest
import kotlinx.coroutines.Dispatchers
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
    private val authApiService: AuthApiService
) : NfcController {

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus: StateFlow<Boolean>
        get() = _connectionStatus.asStateFlow()

    private var isoDep: IsoDep? = null

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
                _connectionStatus.emit(true)
                OperationResult.Success(Unit)
            }
            OperationResult.Failure(Exception("IsoDep.connect() came back as null"))
        } catch (e: Exception) {
            return OperationResult.Failure(e)
        }
    }

    override suspend fun close() {
        isoDep?.close()
        isoDep = null
        _connectionStatus.emit(false)
    }

    override suspend fun transceive(data: ByteArray): OperationResult<ByteArray> {
        return try {
            val result = isoDep?.transceive(data) ?: return OperationResult.Failure(Exception("transceive(): IsoDep was null"))
            OperationResult.Success(result)
        } catch (e: Exception) {
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
                command[5] = 0x01.toByte()
                command[6] = 0x00.toByte()
                command[7] = 0x00.toByte()

                val isoDep = IsoDep.get(tag)
                isoDep.connect()

                val ndefResult = isoDep.transceive(NDEF_SEL)

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

                val response = isoDep.transceive(command)
                isoDep.close()

                val responseString = Hex.encodeHexString(response)

                val sessionRequest = SessionRequest(
                    uid = Hex.encodeHexString(tag.id),
                    response = responseString,
                    token = challengeResponse.token
                )

                val sessionResponse = async {
                    authApiService.postSession(sessionRequest).body()
                }.await()

                if (sessionResponse == null) {
                    OperationResult.Failure()
                }

                OperationResult.Success(sessionResponse!!.token)
            }
        } catch (e: Exception) {
            OperationResult.Failure()
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

    override suspend fun getNdefMessage(ndef: Ndef): OperationResult<NdefMessage> {
        return try {
            val result = ndef.cachedNdefMessage
            OperationResult.Success(result)
        } catch (e: Exception) {
            OperationResult.Failure(e)
        }
    }
}