package com.hoker.intra_example.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.hoker.intra_example.presentation.components.OperationTypeBottomBar
import com.hoker.supra.presentation.scaffolds.SupraGyroScaffold
import com.hoker.supra.presentation.scaffolds.SupraScaffold
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

            SupraScaffold(
                borderColor = Color.DarkGray,
                contentBackgroundColor = Color.Black,
                bottomBar = {
                    OperationTypeBottomBar(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(),
                        onSelectOperationClicked = {
                            viewModel.showOperationOptions = !viewModel.showOperationOptions
                        },
                        showOperationOptions = viewModel.showOperationOptions,
                        onOperationTypeClicked = { operationType ->
                            viewModel.output = null
                            viewModel.selectedCommand = operationType
                            viewModel.showOperationOptions = false
                        }
                    )
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (viewModel.output == null) {
                        Text(
                            text = "Selected Command: ${viewModel.selectedCommand.displayName}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    viewModel.output?.let { output ->
                        Text(
                            text = "${viewModel.selectedCommand.displayName}: ",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = output,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}