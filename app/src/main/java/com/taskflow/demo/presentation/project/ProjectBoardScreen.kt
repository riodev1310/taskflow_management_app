package com.taskflow.demo.presentation.project

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.taskflow.demo.core.ui.components.EmptyState
import com.taskflow.demo.core.ui.components.FullScreenError
import com.taskflow.demo.core.ui.components.InitialBadge
import com.taskflow.demo.core.ui.components.PanelCard
import com.taskflow.demo.core.ui.components.PriorityBadge
import com.taskflow.demo.core.ui.components.SectionHeader
import com.taskflow.demo.core.ui.components.StatusChip
import com.taskflow.demo.core.ui.components.TaskListSkeleton
import com.taskflow.demo.core.util.DateFormatter
import com.taskflow.demo.domain.model.Project
import com.taskflow.demo.domain.model.Task
import com.taskflow.demo.domain.model.TaskPriority
import com.taskflow.demo.domain.model.TaskStatus
import com.taskflow.demo.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectBoardScreen(
    viewModel: ProjectBoardViewModel,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenDashboard: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateProject by remember { mutableStateOf(false) }
    var showCreateTask by remember { mutableStateOf(false) }
    var showDeleteProject by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.selectedProject?.name ?: "Project board", fontWeight = FontWeight.Bold)
                        Text("Kanban board", style = MaterialTheme.typography.labelMedium)
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
                    if (state.selectedProject != null) {
                        IconButton(
                            onClick = { showDeleteProject = true },
                            enabled = state.deletingProjectId == null
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Xoá project",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { onOpenDashboard(state.workspaceId) }) {
                        Icon(Icons.Outlined.Dashboard, contentDescription = "Dashboard")
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Tải lại")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Hồ sơ")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { showCreateProject = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Tạo project")
                }
                if (state.selectedProjectId != null) {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateTask = true },
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("Task") }
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> TaskListSkeleton(padding = padding)
            state.errorMessage != null && state.projects.isEmpty() -> {
                FullScreenError(state.errorMessage ?: "Không tải được board", onRetry = viewModel::load)
            }
            state.projects.isEmpty() -> {
                EmptyState(
                    title = "Chưa có project",
                    subtitle = "Tạo project đầu tiên trong workspace để bắt đầu quản lý task.",
                    actionText = "Tạo project",
                    onAction = { showCreateProject = true }
                )
            }
            else -> {
                BoardContent(
                    padding = padding,
                    state = state,
                    onSelectProject = viewModel::selectProject,
                    onOpenTask = onOpenTask,
                    onStatusChange = viewModel::updateStatus
                )
            }
        }
    }

    if (showCreateProject) {
        CreateProjectDialog(
            state = state,
            onDismiss = { showCreateProject = false },
            onNameChange = viewModel::onProjectNameChanged,
            onDescriptionChange = viewModel::onProjectDescriptionChanged,
            onDueDateChange = viewModel::onProjectDueDateChanged,
            onCreate = { viewModel.createProject { showCreateProject = false } }
        )
    }

    if (showCreateTask) {
        CreateTaskDialog(
            state = state,
            onDismiss = { showCreateTask = false },
            onTitleChange = viewModel::onTaskTitleChanged,
            onDescriptionChange = viewModel::onTaskDescriptionChanged,
            onPriorityChange = viewModel::onTaskPriorityChanged,
            onAssigneeChange = viewModel::onTaskAssigneeChanged,
            onDueDateChange = viewModel::onTaskDueDateChanged,
            onCreate = { viewModel.createTask { showCreateTask = false } }
        )
    }

    if (showDeleteProject) {
        val projectName = state.selectedProject?.name.orEmpty()
        ConfirmDeleteDialog(
            title = "Xoá project?",
            message = "Project \"$projectName\" sẽ bị xoá cùng toàn bộ task bên trong.",
            confirmText = "Xoá project",
            onDismiss = { showDeleteProject = false },
            onConfirm = {
                showDeleteProject = false
                viewModel.deleteSelectedProject()
            }
        )
    }
}

@Composable
private fun BoardContent(
    padding: PaddingValues,
    state: ProjectBoardState,
    onSelectProject: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onStatusChange: (Task, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ProjectSummary(state = state)
        }

        item {
            ProjectSelector(
                projects = state.projects,
                selectedProjectId = state.selectedProjectId,
                onSelectProject = onSelectProject
            )
            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        TaskStatus.boardStatuses.forEach { status ->
            val tasks = state.tasks.filter { it.status == status.wire }
            item(key = "section-${status.wire}") {
                StatusProgressSection(status = status, taskCount = tasks.size) {
                    if (tasks.isEmpty()) {
                        Text(
                            "Chưa có task trong cột này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tasks.forEach { task ->
                                TaskCard(
                                    task = task,
                                    members = state.members,
                                    onOpen = { onOpenTask(task.taskId) },
                                    onStatusChange = { newStatus -> onStatusChange(task, newStatus) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusProgressSection(
    status: TaskStatus,
    taskCount: Int,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = status.color.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(
                title = status.label,
                subtitle = when (status.wire) {
                    TaskStatus.Todo.wire -> "Việc cần bắt đầu"
                    TaskStatus.InProgress.wire -> "Đang triển khai"
                    TaskStatus.Review.wire -> "Đang chờ review"
                    else -> "Đã hoàn thành"
                },
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = status.color,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = taskCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            )
            content()
        }
    }
}

@Composable
private fun ProjectSummary(state: ProjectBoardState) {
    val project = state.selectedProject
    val total = state.tasks.size
    val done = state.tasks.count { it.status == TaskStatus.Done.wire }
    val overdue = state.tasks.count { it.status != TaskStatus.Done.wire && DateFormatter.dueLabel(it.dueAt).startsWith("Quá hạn") }

    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                InitialBadge(text = project?.name ?: "P", color = MaterialTheme.colorScheme.secondary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(project?.name ?: "Chọn project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        project?.description ?: "Theo dõi task theo từng trạng thái",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill("Task", total.toString(), Modifier.weight(1f))
                SummaryPill("Done", done.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                SummaryPill("Overdue", overdue.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.09f),
        contentColor = color
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ProjectSelector(
    projects: List<Project>,
    selectedProjectId: String?,
    onSelectProject: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(projects, key = { it.projectId }) { project ->
            FilterChip(
                selected = selectedProjectId == project.projectId,
                onClick = { onSelectProject(project.projectId) },
                label = { Text(project.name) }
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    members: List<UserProfile>,
    onOpen: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val assignee = members.firstOrNull { it.userId == task.assigneeId }
    val statuses = TaskStatus.boardStatuses
    val currentIndex = statuses.indexOfFirst { it.wire == task.status }.coerceAtLeast(0)
    val previous = statuses.getOrNull(currentIndex - 1)
    val next = statuses.getOrNull(currentIndex + 1)

    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                InitialBadge(text = task.title, color = TaskStatus.fromWire(task.status).color)
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Hạn: ${DateFormatter.dueLabel(task.dueAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!task.description.isNullOrBlank()) {
                Text(task.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(task.status)
                PriorityBadge(task.priority)
            }
            Text(
                "Assignee: ${assignee?.fullName?.ifBlank { assignee.userId.take(8) } ?: "Chưa phân công"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                OutlinedButton(onClick = onOpen) {
                    Text("Chi tiết")
                }
                previous?.let {
                    StatusMoveButton(
                        label = "< ${it.label}",
                        status = it,
                        filled = false,
                        onClick = { onStatusChange(it.wire) }
                    )
                }
                next?.let {
                    StatusMoveButton(
                        label = "${it.label} >",
                        status = it,
                        filled = true,
                        onClick = { onStatusChange(it.wire) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMoveButton(
    label: String,
    status: TaskStatus,
    filled: Boolean,
    onClick: () -> Unit
) {
    if (filled) {
        Button(
            onClick = onClick,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = status.color,
                contentColor = if (status == TaskStatus.Todo) Color(0xFF111827) else Color.White
            )
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(1.dp, status.color),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = status.color
            )
        ) {
            Text(label)
        }
    }
}

@Composable
private fun CreateProjectDialog(
    state: ProjectBoardState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(state.projectName, onNameChange, label = { Text("Tên project") }, singleLine = true)
                OutlinedTextField(state.projectDescription, onDescriptionChange, label = { Text("Mô tả") }, minLines = 2)
                OutlinedTextField(state.projectDueDate, onDueDateChange, label = { Text("Due date yyyy-mm-dd") }, singleLine = true)
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = !state.isMutating) {
                Text(if (state.isMutating) "Đang tạo..." else "Tạo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun CreateTaskDialog(
    state: ProjectBoardState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onAssigneeChange: (String?) -> Unit,
    onDueDateChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo task") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(state.taskTitle, onTitleChange, label = { Text("Tiêu đề") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(state.taskDescription, onDescriptionChange, label = { Text("Mô tả") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Text("Priority", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TaskPriority.all) { priority ->
                            FilterChip(
                                selected = state.taskPriority == priority.wire,
                                onClick = { onPriorityChange(priority.wire) },
                                label = { Text(priority.label) }
                            )
                        }
                    }
                }
                item {
                    Text("Assignee", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.taskAssigneeId == null,
                                onClick = { onAssigneeChange(null) },
                                label = { Text("Chưa phân công") }
                            )
                        }
                        items(state.members, key = { it.userId }) { member ->
                            FilterChip(
                                selected = state.taskAssigneeId == member.userId,
                                onClick = { onAssigneeChange(member.userId) },
                                label = { Text(member.fullName.ifBlank { member.userId.take(8) }) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(state.taskDueDate, onDueDateChange, label = { Text("Due date yyyy-mm-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                state.errorMessage?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = !state.isMutating) {
                Text(if (state.isMutating) "Đang tạo..." else "Tạo task")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
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
