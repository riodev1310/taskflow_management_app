package com.taskflow.demo.presentation.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.demo.core.ui.components.EmptyState
import com.taskflow.demo.core.ui.components.FullScreenError
import com.taskflow.demo.core.ui.components.InitialBadge
import com.taskflow.demo.core.ui.components.PanelCard
import com.taskflow.demo.core.ui.components.SectionHeader
import com.taskflow.demo.core.ui.components.TaskFlowLogo
import com.taskflow.demo.core.ui.components.TaskListSkeleton
import com.taskflow.demo.core.util.UiState
import com.taskflow.demo.domain.model.Workspace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceListScreen(
    viewModel: WorkspaceListViewModel,
    onOpenWorkspace: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var workspacePendingDelete by remember { mutableStateOf<Workspace?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    TaskFlowLogo()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Hồ sơ")
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Đăng xuất")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Tạo workspace")
            }
        }
    ) { padding ->
        when (val workspaces = state.workspaces) {
            UiState.Loading, UiState.Idle -> TaskListSkeleton(padding = padding)
            is UiState.Error -> FullScreenError(workspaces.message, onRetry = viewModel::load)
            is UiState.Success -> {
                if (workspaces.data.isEmpty()) {
                    EmptyState(
                        title = "Chưa có workspace",
                        subtitle = "Tạo workspace đầu tiên để quản lý project, task và comment.",
                        actionText = "Tạo workspace",
                        onAction = { showCreate = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Không gian làm việc",
                                subtitle = "${workspaces.data.size} workspace đang hoạt động"
                            )
                            state.errorMessage?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        items(workspaces.data, key = { it.workspaceId }) { workspace ->
                            WorkspaceCard(
                                workspace = workspace,
                                isDeleting = state.deletingWorkspaceId == workspace.workspaceId,
                                onOpen = { onOpenWorkspace(workspace.workspaceId) },
                                onDelete = { workspacePendingDelete = workspace }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateWorkspaceDialog(
            state = state,
            onDismiss = { showCreate = false },
            onNameChange = viewModel::onCreateNameChanged,
            onDescriptionChange = viewModel::onCreateDescriptionChanged,
            onCreate = {
                viewModel.createWorkspace { workspaceId ->
                    showCreate = false
                    onOpenWorkspace(workspaceId)
                }
            }
        )
    }

    workspacePendingDelete?.let { workspace ->
        ConfirmDeleteDialog(
            title = "Xoá workspace?",
            message = "Workspace \"${workspace.name}\" sẽ bị xoá cùng toàn bộ project và task bên trong.",
            confirmText = "Xoá workspace",
            onDismiss = { workspacePendingDelete = null },
            onConfirm = {
                workspacePendingDelete = null
                viewModel.deleteWorkspace(workspace)
            }
        )
    }
}

@Composable
private fun WorkspaceCard(
    workspace: Workspace,
    isDeleting: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InitialBadge(text = workspace.name)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(workspace.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Icon(
                        Icons.Outlined.Workspaces,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(16.dp)
                    )
                }
                Text(
                    workspace.description ?: "Workspace quản lý task demo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onOpen, enabled = !isDeleting) {
                    Text("Mở")
                }
                IconButton(onClick = onDelete, enabled = !isDeleting) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Xoá workspace",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateWorkspaceDialog(
    state: WorkspaceListState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.createName,
                    onValueChange = onNameChange,
                    label = { Text("Tên workspace") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.createDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Mô tả") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = !state.isCreating) {
                Text(if (state.isCreating) "Đang tạo..." else "Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
