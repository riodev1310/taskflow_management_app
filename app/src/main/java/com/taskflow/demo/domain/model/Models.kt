package com.taskflow.demo.domain.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
    @SerialName("job_title") val jobTitle: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class Workspace(
    @SerialName("workspace_id") val workspaceId: String,
    val name: String,
    val description: String? = null,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class WorkspaceMember(
    @SerialName("member_id") val memberId: String = "",
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member",
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
data class Project(
    @SerialName("project_id") val projectId: String,
    @SerialName("workspace_id") val workspaceId: String,
    val name: String,
    val description: String? = null,
    val status: String = "active",
    @SerialName("color_hex") val colorHex: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class Task(
    @SerialName("task_id") val taskId: String,
    @SerialName("project_id") val projectId: String,
    val title: String,
    val description: String? = null,
    val status: String = TaskStatus.Todo.wire,
    val priority: String = TaskPriority.Medium.wire,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class TaskComment(
    @SerialName("comment_id") val commentId: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class TaskAttachment(
    @SerialName("attachment_id") val attachmentId: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("uploaded_by") val uploadedBy: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("created_at") val createdAt: String = ""
)

data class CreateWorkspaceRequest(
    val name: String,
    val description: String?
)

data class CreateProjectRequest(
    val workspaceId: String,
    val name: String,
    val description: String?,
    val colorHex: String? = "#2563EB",
    val dueDate: String? = null
)

data class CreateTaskRequest(
    val projectId: String,
    val title: String,
    val description: String?,
    val priority: String,
    val assigneeId: String?,
    val dueAt: String?
)

data class DashboardStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val inProgressTasks: Int = 0,
    val overdueTasks: Int = 0,
    val reviewTasks: Int = 0,
    val workloadByAssignee: Map<String, Int> = emptyMap()
) {
    val completionRate: Float
        get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()
}

sealed class TaskStatus(val wire: String, val label: String, val color: Color) {
    data object Todo : TaskStatus("todo", "To Do", Color(0xFF94A3B8))
    data object InProgress : TaskStatus("in_progress", "In Progress", Color(0xFF2563EB))
    data object Review : TaskStatus("review", "Review", Color(0xFFF97316))
    data object Done : TaskStatus("done", "Done", Color(0xFF16A34A))
    data object Cancelled : TaskStatus("cancelled", "Cancelled", Color(0xFFDC2626))

    companion object {
        val boardStatuses: List<TaskStatus>
            get() = listOf(Todo, InProgress, Review, Done)

        val all: List<TaskStatus>
            get() = listOf(Todo, InProgress, Review, Done, Cancelled)

        fun fromWire(value: String?): TaskStatus {
            return when (value) {
                Todo.wire -> Todo
                InProgress.wire -> InProgress
                Review.wire -> Review
                Done.wire -> Done
                Cancelled.wire -> Cancelled
                else -> Todo
            }
        }
    }
}

sealed class TaskPriority(
    val wire: String,
    val label: String,
    val containerColor: Color,
    val contentColor: Color
) {
    data object Low : TaskPriority("low", "Low", Color(0xFFE0F2FE), Color(0xFF075985))
    data object Medium : TaskPriority("medium", "Medium", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
    data object High : TaskPriority("high", "High", Color(0xFFFFEDD5), Color(0xFFC2410C))
    data object Urgent : TaskPriority("urgent", "Urgent", Color(0xFFFEE2E2), Color(0xFFB91C1C))

    companion object {
        val all: List<TaskPriority>
            get() = listOf(Low, Medium, High, Urgent)

        fun fromWire(value: String?): TaskPriority {
            return when (value) {
                Low.wire -> Low
                Medium.wire -> Medium
                High.wire -> High
                Urgent.wire -> Urgent
                else -> Medium
            }
        }
    }
}
