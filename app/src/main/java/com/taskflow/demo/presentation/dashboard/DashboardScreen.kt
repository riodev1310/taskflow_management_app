package com.taskflow.demo.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.demo.core.ui.components.FullScreenError
import com.taskflow.demo.core.ui.components.MetricCard
import com.taskflow.demo.core.ui.components.PanelCard
import com.taskflow.demo.core.ui.components.SectionHeader
import com.taskflow.demo.core.ui.components.TaskListSkeleton
import com.taskflow.demo.core.util.UiState
import com.taskflow.demo.domain.model.DashboardStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard", fontWeight = FontWeight.Bold)
                        Text("Tiến độ workspace", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Tải lại")
                    }
                }
            )
        }
    ) { padding ->
        when (val dashboard = state) {
            UiState.Loading, UiState.Idle -> TaskListSkeleton(padding = padding)
            is UiState.Error -> FullScreenError(dashboard.message, onRetry = viewModel::load)
            is UiState.Success -> DashboardContent(padding = padding, stats = dashboard.data)
        }
    }
}

@Composable
private fun DashboardContent(padding: PaddingValues, stats: DashboardStats) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Tổng quan tiến độ",
                subtitle = "Theo dõi tốc độ hoàn thành và việc đang mở"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Tổng task", stats.totalTasks.toString(), modifier = Modifier.weight(1f))
                MetricCard("Hoàn thành", stats.completedTasks.toString(), modifier = Modifier.weight(1f), accent = androidx.compose.ui.graphics.Color(0xFF16A34A))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Đang làm", stats.inProgressTasks.toString(), modifier = Modifier.weight(1f), accent = androidx.compose.ui.graphics.Color(0xFF2563EB))
                MetricCard("Quá hạn", stats.overdueTasks.toString(), modifier = Modifier.weight(1f), accent = androidx.compose.ui.graphics.Color(0xFFDC2626))
            }
        }
        item {
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Completion rate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { stats.completionRate }, modifier = Modifier.fillMaxWidth())
                Text("${(stats.completionRate * 100).toInt()}% task đã Done", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionHeader("Workload theo assignee", "Phân bổ task chưa hoàn thành")
        }
        if (stats.workloadByAssignee.isEmpty()) {
            item { Text("Chưa có task đang mở", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(stats.workloadByAssignee.entries.toList(), key = { it.key }) { entry ->
                PanelCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(entry.key, fontWeight = FontWeight.SemiBold)
                            Text("${entry.value} task", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { entry.value.toFloat() / stats.totalTasks.coerceAtLeast(1).toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
