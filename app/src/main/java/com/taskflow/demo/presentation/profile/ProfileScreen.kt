package com.taskflow.demo.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.demo.core.ui.components.FullScreenError
import com.taskflow.demo.core.ui.components.InitialBadge
import com.taskflow.demo.core.ui.components.PanelCard
import com.taskflow.demo.core.ui.components.TaskListSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hồ sơ", fontWeight = FontWeight.Bold)
                        Text("Thiết lập cá nhân", style = MaterialTheme.typography.labelMedium)
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
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Đăng xuất")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> TaskListSkeleton(padding = padding)
            state.errorMessage != null && state.profile == null -> FullScreenError(state.errorMessage ?: "Không tải được hồ sơ", onRetry = viewModel::load)
            else -> ProfileContent(padding = padding, state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun ProfileContent(
    padding: PaddingValues,
    state: ProfileState,
    viewModel: ProfileViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        InitialBadge(text = state.fullName.ifBlank { "U" })
                        Column {
                            Text("Thông tin cá nhân", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Cập nhật hồ sơ demo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(
                        value = state.fullName,
                        onValueChange = viewModel::onFullNameChanged,
                        label = { Text("Họ tên") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.jobTitle,
                        onValueChange = viewModel::onJobTitleChanged,
                        label = { Text("Vai trò / chức danh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChanged,
                        label = { Text("Số điện thoại") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.isSaving) "Đang lưu..." else "Lưu hồ sơ")
                    }
                }
            }
        }
        item {
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Notification settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingRow(
                        title = "Nhắc task sắp đến hạn",
                        checked = state.dueSoonEnabled,
                        onCheckedChange = viewModel::onDueSoonChanged
                    )
                    SettingRow(
                        title = "Badge comment mới",
                        checked = state.commentBadgeEnabled,
                        onCheckedChange = viewModel::onCommentBadgeChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
