package com.hoker.intra.domain

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
abstract class NfcActivity : ComponentActivity() {

    @Inject lateinit var nfcAdapterController: NfcAdapterController

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        if (!nfcAdapterController.isNfcSupported()) {
            Toast.makeText(this, "Your device does not support NFC which is required by this application.", Toast.LENGTH_LONG).show()
        }
    }

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