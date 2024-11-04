package com.hoker.intra_example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hoker.intra_example.domain.models.FontSizes
import com.hoker.intra_example.domain.models.OperationType

@Composable
fun OperationTypeBottomBar(
    modifier: Modifier = Modifier,
    onSelectOperationClicked: () -> Unit,
    showOperationOptions: Boolean,
    onOperationTypeClicked: (operation: OperationType) -> Unit,
) {

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearOutSlowInEasing
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onSelectOperationClicked()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp),
                imageVector = Icons.Default.Nfc,
                contentDescription = "nfc icon",
                tint = if (showOperationOptions) Color.Yellow else Color.White
            )
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "Select Command",
                fontSize = FontSizes.medium,
                color = if (showOperationOptions) Color.Yellow else Color.White
            )
        }

        AnimatedVisibility(showOperationOptions) {
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                itemsIndexed(OperationType.entries) { index, operationType ->
                    val isLastElement = index == OperationType.entries.size - 1
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 32.dp,
                                end = 32.dp,
                                bottom = if (isLastElement) 0.dp else 16.dp
                            )
                            .clickable {
                                onOperationTypeClicked(operationType)
                            },
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color.Black
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = operationType.displayName,
                            color = Color.White,
                            fontSize = FontSizes.medium
                        )
                    }
                }
            }
        }
    }
}