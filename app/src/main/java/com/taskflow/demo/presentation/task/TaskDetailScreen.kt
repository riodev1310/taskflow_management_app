package com.taskflow.demo.presentation.task

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflow.demo.core.ui.components.FullScreenError
import com.taskflow.demo.core.ui.components.InitialBadge
import com.taskflow.demo.core.ui.components.PanelCard
import com.taskflow.demo.core.ui.components.PriorityBadge
import com.taskflow.demo.core.ui.components.SectionHeader
import com.taskflow.demo.core.ui.components.StatusChip
import com.taskflow.demo.core.ui.components.TaskListSkeleton
import com.taskflow.demo.core.util.DateFormatter
import com.taskflow.demo.data.repository.displayNameFallback
import com.taskflow.demo.domain.model.TaskAttachment
import com.taskflow.demo.domain.model.TaskComment
import com.taskflow.demo.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteTask by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.uploadAttachment(
                    bytes = bytes,
                    fileName = uri.displayNameFallback(),
                    mimeType = context.contentResolver.getType(uri)
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Task detail", fontWeight = FontWeight.Bold)
                        Text("Comment và attachment", style = MaterialTheme.typography.labelMedium)
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
                    if (state.task != null) {
                        IconButton(
                            onClick = { showDeleteTask = true },
                            enabled = !state.isDeletingTask
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Xoá task",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Tải lại")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> TaskListSkeleton(padding = padding)
            state.errorMessage != null && state.task == null -> FullScreenError(state.errorMessage ?: "Không tải được task", onRetry = viewModel::load)
            state.task != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        TaskSummaryCard(state = state)
                    }
                    item {
                        AssigneeEditor(
                            state = state,
                            onAssigneeSelected = viewModel::updateAssignee
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { attachmentPicker.launch("*/*") },
                                enabled = !state.isUploading
                            ) {
                                Icon(Icons.Outlined.AttachFile, contentDescription = null)
                                Text(if (state.isUploading) "Đang upload..." else "Đính kèm")
                            }
                        }
                        state.errorMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    item {
                        SectionHeader("Attachments", "File liên quan đến task")
                    }
                    if (state.attachments.isEmpty()) {
                        item { Text("Chưa có file đính kèm", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(state.attachments, key = { it.attachmentId }) { attachment ->
                            AttachmentRow(attachment)
                        }
                    }
                    item {
                        SectionHeader("Comment thread", "Trao đổi theo ngữ cảnh task")
                    }
                    items(state.comments, key = { it.commentId }) { comment ->
                        CommentRow(comment)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = state.commentText,
                                onValueChange = viewModel::onCommentChanged,
                                label = { Text("Viết comment") },
                                modifier = Modifier.weight(1f),
                                minLines = 1
                            )
                            Button(onClick = viewModel::sendComment, enabled = !state.isSending) {
                                Icon(Icons.Outlined.Send, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteTask) {
        ConfirmDeleteDialog(
            title = "Xoá task?",
            message = "Task \"${state.task?.title.orEmpty()}\" sẽ bị xoá khỏi project.",
            confirmText = "Xoá task",
            onDismiss = { showDeleteTask = false },
            onConfirm = {
                showDeleteTask = false
                viewModel.deleteTask(onDeleted)
            }
        )
    }
}

@Composable
private fun AssigneeEditor(
    state: TaskDetailState,
    onAssigneeSelected: (String?) -> Unit
) {
    val task = state.task ?: return
    val selected = state.members.firstOrNull { it.userId == task.assigneeId }

    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "Assignee",
                subtitle = selected?.displayName() ?: "Task này chưa được phân công"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = task.assigneeId == null,
                        onClick = { onAssigneeSelected(null) },
                        enabled = !state.isUpdatingAssignee,
                        label = { Text("Chưa phân công") }
                    )
                }
                items(state.members, key = { it.userId }) { member ->
                    FilterChip(
                        selected = task.assigneeId == member.userId,
                        onClick = { onAssigneeSelected(member.userId) },
                        enabled = !state.isUpdatingAssignee,
                        label = { Text(member.displayName()) }
                    )
                }
            }
            if (state.isUpdatingAssignee) {
                Text("Đang cập nhật assignee...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (state.members.isEmpty()) {
                Text(
                    "Workspace chưa có member đọc được để phân công.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskSummaryCard(state: TaskDetailState) {
    val task = state.task ?: return
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialBadge(text = task.title)
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Hạn: ${DateFormatter.dueLabel(task.dueAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!task.description.isNullOrBlank()) {
                Text(task.description, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(task.status)
                PriorityBadge(task.priority)
            }
        }
    }
}

private fun UserProfile.displayName(): String = fullName.ifBlank { userId.take(8) }

@Composable
private fun CommentRow(comment: TaskComment) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            Text(comment.createdAt.take(16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AttachmentRow(attachment: TaskAttachment) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null)
            Column {
                Text(attachment.fileName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${attachment.fileType ?: "file"} - ${attachment.sizeBytes ?: 0} bytes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
