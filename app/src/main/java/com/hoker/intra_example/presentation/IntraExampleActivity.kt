package com.hoker.intra_example.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hoker.intra.domain.NfcActivity
import com.hoker.supra.presentation.scaffolds.SupraGyroScaffold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class IntraExampleActivity: NfcActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val viewModel: IntraExampleViewModel = hiltViewModel()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                viewModel.errorFlow.collectLatest { errorMessage ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            SupraGyroScaffold(
                borderColor = Color.DarkGray,
                backgroundColor = Color.Black
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (viewModel.jwtText == null) {
                        Text(
                            text = "Scan to get JWT",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    viewModel.jwtText?.let { jwt ->
                        Text(
                            text = "JWT:",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = jwt,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}