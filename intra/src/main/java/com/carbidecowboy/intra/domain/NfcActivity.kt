package com.carbidecowboy.intra.domain

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
abstract class NfcActivity : ComponentActivity() {

    @Inject lateinit var nfcAdapterController: NfcAdapterController

    override fun onResume() {
        super.onResume()
        nfcAdapterController.enableNfc(this)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapterController.disableNfc(this)
    }

    suspend fun cycleNfcAdapter() {
        nfcAdapterController.disableNfc(this)
        delay(500)
        nfcAdapterController.enableNfc(this)
    }
}