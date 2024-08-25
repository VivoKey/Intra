package com.hoker.intra_example.presentation

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hoker.intra.domain.NfcActivity
import com.carbidecowboy.supra.presentation.scaffolds.SupraGyroScaffold
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntraExampleActivity: NfcActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SupraGyroScaffold(
                borderColor = Color.Black,
                backgroundColor = Color.DarkGray
            ) {

            }
        }
    }
}