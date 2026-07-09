package com.taskflow.demo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.demo.core.ui.theme.FlowInk
import com.taskflow.demo.core.ui.theme.FlowYellow

@Composable
fun TaskFlowLogo(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true,
    large: Boolean = false
) {
    val markSize = if (large) 76.dp else 46.dp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (large) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(markSize)
                .clip(RoundedCornerShape(if (large) 24.dp else 15.dp))
                .background(FlowYellow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TF",
                color = FlowInk,
                style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
        if (showWordmark) {
            Text(
                text = "TaskFlow",
                color = MaterialTheme.colorScheme.onSurface,
                style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}
