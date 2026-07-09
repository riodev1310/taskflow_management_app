package com.taskflow.demo.data.repository

import android.content.Context
import android.net.Uri
import com.taskflow.demo.core.config.DataSourceConfig
import com.taskflow.demo.data.local.CachedProjectDao
import com.taskflow.demo.data.local.CachedProjectEntity
import com.taskflow.demo.data.local.LocalUserDao
import com.taskflow.demo.data.local.LocalUserEntity
import com.taskflow.demo.data.local.ProjectDao
import com.taskflow.demo.data.local.ProjectEntity
import com.taskflow.demo.data.local.TaskAttachmentDao
import com.taskflow.demo.data.local.TaskAttachmentEntity
import com.taskflow.demo.data.local.TaskCommentDao
import com.taskflow.demo.data.local.TaskCommentEntity
import com.taskflow.demo.data.local.TaskDao
import com.taskflow.demo.data.local.TaskEntity
import com.taskflow.demo.data.local.UserProfileDao
import com.taskflow.demo.data.local.UserProfileEntity
import com.taskflow.demo.data.local.WorkspaceDao
import com.taskflow.demo.data.local.WorkspaceEntity
import com.taskflow.demo.data.local.WorkspaceMemberDao
import com.taskflow.demo.data.local.WorkspaceMemberEntity
import com.taskflow.demo.data.local.toDomain
import com.taskflow.demo.domain.model.CreateProjectRequest
import com.taskflow.demo.domain.model.CreateTaskRequest
import com.taskflow.demo.domain.model.CreateWorkspaceRequest
import com.taskflow.demo.domain.model.DashboardStats
import com.taskflow.demo.domain.model.Project
import com.taskflow.demo.domain.model.Task
import com.taskflow.demo.domain.model.TaskAttachment
import com.taskflow.demo.domain.model.TaskComment
import com.taskflow.demo.domain.model.TaskStatus
import com.taskflow.demo.domain.model.UserProfile
import com.taskflow.demo.domain.model.Workspace
import com.taskflow.demo.domain.model.WorkspaceMember
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val localUserDao: LocalUserDao,
    private val userProfileDao: UserProfileDao
) {
    private val sessionPrefs = context.getSharedPreferences("taskflow_local_session", Context.MODE_PRIVATE)

    suspend fun signUp(email: String, password: String, fullName: String) {
        if (!DataSourceConfig.isLocal) {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
            return
        }

        val normalizedEmail = email.trim().lowercase()
        if (localUserDao.getByEmail(normalizedEmail) != null) {
            error("Email này đã tồn tại trên thiết bị")
        }
        val now = Clock.System.now().toString()
        val userId = UUID.randomUUID().toString()
        localUserDao.upsert(
            LocalUserEntity(
                userId = userId,
                email = normalizedEmail,
                password = password,
                fullName = fullName,
                createdAt = now,
                updatedAt = now
            )
        )
        userProfileDao.upsert(
            UserProfileEntity(
                userId = userId,
                fullName = fullName,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun signIn(email: String, password: String) {
        if (!DataSourceConfig.isLocal) {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            return
        }

        val normalizedEmail = email.trim().lowercase()
        val user = localUserDao.getByEmail(normalizedEmail)
            ?: error("Không tìm thấy tài khoản local. Hãy đăng ký trước.")
        if (user.password != password) {
            error("Email hoặc mật khẩu không đúng")
        }
        sessionPrefs.edit()
            .putString(KEY_USER_ID, user.userId)
            .putString(KEY_EMAIL, user.email)
            .apply()
    }

    suspend fun signInWithGoogle(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (DataSourceConfig.isLocal) {
            error("Google sign-in chỉ dùng cho Supabase mode")
        }
        supabase.auth.signInWith(Google, redirectUrl = "taskflow://login") {
            scopes.add("email")
            scopes.add("profile")
        }
    }

    suspend fun signOut() {
        if (DataSourceConfig.isLocal) {
            sessionPrefs.edit().clear().apply()
        } else {
            supabase.auth.signOut()
        }
    }

    fun currentUserId(): String? {
        return if (DataSourceConfig.isLocal) {
            sessionPrefs.getString(KEY_USER_ID, null)
        } else {
            supabase.auth.currentUserOrNull()?.id
        }
    }

    fun currentEmail(): String? {
        return if (DataSourceConfig.isLocal) {
            sessionPrefs.getString(KEY_EMAIL, null)
        } else {
            supabase.auth.currentUserOrNull()?.email
        }
    }

    val authState: Flow<SessionStatus> = supabase.auth.sessionStatus

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
    }
}

@Singleton
class WorkspaceRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val workspaceDao: WorkspaceDao,
    private val workspaceMemberDao: WorkspaceMemberDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val commentDao: TaskCommentDao,
    private val attachmentDao: TaskAttachmentDao,
    private val userProfileDao: UserProfileDao
) {
    suspend fun getMyWorkspaces(): List<Workspace> {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId() ?: return emptyList()
            return workspaceDao.getForUser(userId).map { it.toDomain() }
        }

        val userId = authRepository.currentUserId() ?: return emptyList()
        return supabase.postgrest["workspaces"]
            .select(columns = Columns.raw("*, workspace_members!inner(*)")) {
                filter { eq("workspace_members.user_id", userId) }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId()
                ?: error("User must login before creating workspace")
            val now = Clock.System.now().toString()
            val workspaceId = UUID.randomUUID().toString()
            val workspace = WorkspaceEntity(
                workspaceId = workspaceId,
                name = request.name,
                description = request.description,
                ownerId = userId,
                createdAt = now,
                updatedAt = now
            )
            workspaceDao.upsert(workspace)
            workspaceMemberDao.upsert(
                WorkspaceMemberEntity(
                    workspaceId = workspaceId,
                    userId = userId,
                    role = "owner",
                    joinedAt = now
                )
            )
            return workspace.toDomain()
        }

        val userId = authRepository.currentUserId()
            ?: error("User must login before creating workspace")
        val workspaceId = UUID.randomUUID().toString()
        val now = Clock.System.now().toString()

        supabase.postgrest["workspaces"].insert(
            value = buildJsonObject {
                put("workspace_id", workspaceId)
                put("name", request.name)
                request.description?.let { put("description", it) }
                put("owner_id", userId)
            }
        )

        supabase.postgrest["workspace_members"].insert(
            value = buildJsonObject {
                put("workspace_id", workspaceId)
                put("user_id", userId)
                put("role", "owner")
            }
        )

        return Workspace(
            workspaceId = workspaceId,
            name = request.name,
            description = request.description,
            ownerId = userId,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun deleteWorkspace(workspaceId: String) {
        if (DataSourceConfig.isLocal) {
            val projects = projectDao.getByWorkspace(workspaceId)
            val projectIds = projects.map { it.projectId }
            if (projectIds.isNotEmpty()) {
                val taskIds = taskDao.getByProjectIds(projectIds).map { it.taskId }
                if (taskIds.isNotEmpty()) {
                    commentDao.deleteByTaskIds(taskIds)
                    attachmentDao.deleteByTaskIds(taskIds)
                }
                taskDao.deleteByProjectIds(projectIds)
            }
            projectDao.deleteByWorkspace(workspaceId)
            workspaceMemberDao.deleteByWorkspace(workspaceId)
            workspaceDao.deleteById(workspaceId)
            return
        }

        supabase.postgrest["workspaces"].delete {
            filter { eq("workspace_id", workspaceId) }
        }
    }

    suspend fun getWorkspaceMembers(workspaceId: String): List<UserProfile> {
        if (DataSourceConfig.isLocal) {
            val members = workspaceMemberDao.getByWorkspace(workspaceId)
            if (members.isEmpty()) return emptyList()
            val profiles = userProfileDao.getByIds(members.map { it.userId }).associateBy { it.userId }
            return members.mapNotNull { member -> profiles[member.userId]?.toDomain() }
        }

        val memberships: List<WorkspaceMember> = supabase.postgrest["workspace_members"]
            .select {
                filter { eq("workspace_id", workspaceId) }
                order("joined_at", Order.ASCENDING)
            }
            .decodeList()

        return memberships.mapNotNull { member ->
            runCatching {
                supabase.postgrest["user_profiles"]
                    .select {
                        filter { eq("user_id", member.userId) }
                        single()
                    }
                    .decodeAs<UserProfile>()
            }.getOrNull()
        }
    }
}

@Singleton
class ProjectRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val cachedProjectDao: CachedProjectDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val commentDao: TaskCommentDao,
    private val attachmentDao: TaskAttachmentDao
) {
    suspend fun getProject(projectId: String): Project {
        if (DataSourceConfig.isLocal) {
            return projectDao.getById(projectId)?.toDomain()
                ?: error("Project not found on local database")
        }

        return supabase.postgrest["projects"]
            .select {
                filter { eq("project_id", projectId) }
                single()
            }
            .decodeAs()
    }

    suspend fun getProjects(workspaceId: String): List<Project> {
        if (DataSourceConfig.isLocal) {
            return projectDao.getActiveByWorkspace(workspaceId).map { it.toDomain() }
        }

        val projects: List<Project> = supabase.postgrest["projects"]
            .select {
                filter {
                    eq("workspace_id", workspaceId)
                    eq("status", "active")
                }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList()

        cachedProjectDao.upsertAll(
            projects.map {
                CachedProjectEntity(
                    projectId = it.projectId,
                    workspaceId = it.workspaceId,
                    name = it.name,
                    status = it.status
                )
            }
        )
        return projects
    }

    suspend fun createProject(request: CreateProjectRequest): Project {
        if (DataSourceConfig.isLocal) {
            val projectId = UUID.randomUUID().toString()
            val now = Clock.System.now().toString()
            val project = ProjectEntity(
                projectId = projectId,
                workspaceId = request.workspaceId,
                name = request.name,
                description = request.description,
                status = "active",
                colorHex = request.colorHex,
                dueDate = request.dueDate,
                createdAt = now,
                updatedAt = now
            )
            projectDao.upsert(project)
            return project.toDomain()
        }

        val projectId = UUID.randomUUID().toString()
        val now = Clock.System.now().toString()
        supabase.postgrest["projects"].insert(
            value = buildJsonObject {
                put("project_id", projectId)
                put("workspace_id", request.workspaceId)
                put("name", request.name)
                request.description?.let { put("description", it) }
                put("status", "active")
                request.colorHex?.let { put("color_hex", it) }
                request.dueDate?.let { put("due_date", it) }
            }
        )

        val project = Project(
            projectId = projectId,
            workspaceId = request.workspaceId,
            name = request.name,
            description = request.description,
            colorHex = request.colorHex,
            dueDate = request.dueDate,
            createdAt = now,
            updatedAt = now
        )
        cachedProjectDao.upsertAll(
            listOf(CachedProjectEntity(projectId = projectId, workspaceId = request.workspaceId, name = request.name, status = "active"))
        )
        return project
    }

    suspend fun deleteProject(projectId: String) {
        if (DataSourceConfig.isLocal) {
            val taskIds = taskDao.getByProject(projectId).map { it.taskId }
            if (taskIds.isNotEmpty()) {
                commentDao.deleteByTaskIds(taskIds)
                attachmentDao.deleteByTaskIds(taskIds)
            }
            taskDao.deleteByProject(projectId)
            projectDao.deleteById(projectId)
            return
        }

        supabase.postgrest["projects"].delete {
            filter { eq("project_id", projectId) }
        }
    }
}

@Singleton
class TaskRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val taskDao: TaskDao,
    private val commentDao: TaskCommentDao,
    private val attachmentDao: TaskAttachmentDao
) {
    suspend fun getTasks(projectId: String): List<Task> {
        if (DataSourceConfig.isLocal) {
            return taskDao.getByProject(projectId).map { it.toDomain() }
        }

        return supabase.postgrest["tasks"]
            .select {
                filter { eq("project_id", projectId) }
                order("sort_order", Order.ASCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun getTask(taskId: String): Task {
        if (DataSourceConfig.isLocal) {
            return taskDao.getById(taskId)?.toDomain()
                ?: error("Task not found on local database")
        }

        return supabase.postgrest["tasks"]
            .select {
                filter { eq("task_id", taskId) }
                single()
            }
            .decodeAs()
    }

    suspend fun createTask(request: CreateTaskRequest): Task {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId()
                ?: error("User must login before creating task")
            val taskId = UUID.randomUUID().toString()
            val now = Clock.System.now().toString()
            val sortOrder = (System.currentTimeMillis() / 1000L).toInt()
            val task = TaskEntity(
                taskId = taskId,
                projectId = request.projectId,
                title = request.title,
                description = request.description,
                status = TaskStatus.Todo.wire,
                priority = request.priority,
                assigneeId = request.assigneeId,
                reporterId = userId,
                dueAt = request.dueAt,
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now
            )
            taskDao.upsert(task)
            return task.toDomain()
        }

        val userId = authRepository.currentUserId()
            ?: error("User must login before creating task")
        val taskId = UUID.randomUUID().toString()
        val now = Clock.System.now().toString()
        val sortOrder = (System.currentTimeMillis() / 1000L).toInt()

        supabase.postgrest["tasks"].insert(
            value = buildJsonObject {
                put("task_id", taskId)
                put("project_id", request.projectId)
                put("title", request.title)
                request.description?.let { put("description", it) }
                put("status", TaskStatus.Todo.wire)
                put("priority", request.priority)
                if (request.assigneeId == null) put("assignee_id", JsonNull) else put("assignee_id", request.assigneeId)
                put("reporter_id", userId)
                if (request.dueAt == null) put("due_at", JsonNull) else put("due_at", request.dueAt)
                put("sort_order", sortOrder)
            }
        )

        return Task(
            taskId = taskId,
            projectId = request.projectId,
            title = request.title,
            description = request.description,
            priority = request.priority,
            assigneeId = request.assigneeId,
            reporterId = userId,
            dueAt = request.dueAt,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun updateStatus(taskId: String, status: String) {
        if (DataSourceConfig.isLocal) {
            val completedAt = if (status == TaskStatus.Done.wire) Clock.System.now().toString() else null
            taskDao.updateStatus(taskId, status, completedAt, Clock.System.now().toString())
            return
        }

        supabase.postgrest["tasks"].update(
            value = buildJsonObject {
                put("status", status)
                put("updated_at", Clock.System.now().toString())
                if (status == TaskStatus.Done.wire) {
                    put("completed_at", Clock.System.now().toString())
                } else {
                    put("completed_at", JsonNull)
                }
            }
        ) {
            filter { eq("task_id", taskId) }
        }
    }

    suspend fun updateAssignee(taskId: String, assigneeId: String?): Task {
        if (DataSourceConfig.isLocal) {
            taskDao.updateAssignee(taskId, assigneeId, Clock.System.now().toString())
            return getTask(taskId)
        }

        return supabase.postgrest["tasks"].update(
            value = buildJsonObject {
                if (assigneeId == null) put("assignee_id", JsonNull) else put("assignee_id", assigneeId)
                put("updated_at", Clock.System.now().toString())
            }
        ) {
            filter { eq("task_id", taskId) }
            select()
        }.decodeSingle()
    }

    suspend fun deleteTask(taskId: String) {
        if (DataSourceConfig.isLocal) {
            commentDao.deleteByTask(taskId)
            attachmentDao.deleteByTask(taskId)
            taskDao.deleteById(taskId)
            return
        }

        supabase.postgrest["tasks"].delete {
            filter { eq("task_id", taskId) }
        }
    }
}

@Singleton
class CommentRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val commentDao: TaskCommentDao
) {
    suspend fun getComments(taskId: String): List<TaskComment> {
        if (DataSourceConfig.isLocal) {
            return commentDao.getByTask(taskId).map { it.toDomain() }
        }

        return supabase.postgrest["task_comments"]
            .select {
                filter { eq("task_id", taskId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()
    }

    suspend fun addComment(taskId: String, content: String): TaskComment {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId()
                ?: error("User must login before commenting")
            val now = Clock.System.now().toString()
            val comment = TaskCommentEntity(
                commentId = UUID.randomUUID().toString(),
                taskId = taskId,
                userId = userId,
                content = content,
                createdAt = now,
                updatedAt = now
            )
            commentDao.upsert(comment)
            return comment.toDomain()
        }

        val userId = authRepository.currentUserId()
            ?: error("User must login before commenting")
        val commentId = UUID.randomUUID().toString()
        val now = Clock.System.now().toString()

        supabase.postgrest["task_comments"].insert(
            value = buildJsonObject {
                put("comment_id", commentId)
                put("task_id", taskId)
                put("user_id", userId)
                put("content", content)
            }
        )

        return TaskComment(
            commentId = commentId,
            taskId = taskId,
            userId = userId,
            content = content,
            createdAt = now,
            updatedAt = now
        )
    }
}

@Singleton
class AttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val attachmentDao: TaskAttachmentDao
) {
    suspend fun getAttachments(taskId: String): List<TaskAttachment> {
        if (DataSourceConfig.isLocal) {
            return attachmentDao.getByTask(taskId).map { it.toDomain() }
        }

        return supabase.postgrest["task_attachments"]
            .select {
                filter { eq("task_id", taskId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun uploadTaskAttachment(taskId: String, bytes: ByteArray, fileName: String, mimeType: String?): TaskAttachment {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId()
                ?: error("User must login before uploading")
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val directory = File(context.filesDir, "attachments/$taskId").apply { mkdirs() }
            val file = File(directory, "${System.currentTimeMillis()}_$safeName").apply { writeBytes(bytes) }
            val now = Clock.System.now().toString()
            val attachment = TaskAttachmentEntity(
                attachmentId = UUID.randomUUID().toString(),
                taskId = taskId,
                uploadedBy = userId,
                fileName = fileName,
                fileUrl = file.toURI().toString(),
                fileType = mimeType,
                sizeBytes = bytes.size.toLong(),
                createdAt = now
            )
            attachmentDao.upsert(attachment)
            return attachment.toDomain()
        }

        val userId = authRepository.currentUserId()
            ?: error("User must login before uploading")
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val path = "tasks/$taskId/${System.currentTimeMillis()}_$safeName"
        supabase.storage["task-attachments"].upload(path = path, data = bytes)
        val publicUrl = supabase.storage["task-attachments"].publicUrl(path)

        val attachmentId = UUID.randomUUID().toString()
        val now = Clock.System.now().toString()
        supabase.postgrest["task_attachments"].insert(
            value = buildJsonObject {
                put("attachment_id", attachmentId)
                put("task_id", taskId)
                put("uploaded_by", userId)
                put("file_name", fileName)
                put("file_url", publicUrl)
                mimeType?.let { put("file_type", it) }
                put("size_bytes", bytes.size.toLong())
            }
        )

        return TaskAttachment(
            attachmentId = attachmentId,
            taskId = taskId,
            uploadedBy = userId,
            fileName = fileName,
            fileUrl = publicUrl,
            fileType = mimeType,
            sizeBytes = bytes.size.toLong(),
            createdAt = now
        )
    }
}

@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val localUserDao: LocalUserDao,
    private val userProfileDao: UserProfileDao
) {
    suspend fun getMyProfile(): UserProfile {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId()
                ?: error("User not logged in")
            val existing = userProfileDao.getById(userId)
            if (existing != null) return existing.toDomain()

            val now = Clock.System.now().toString()
            val user = localUserDao.getById(userId)
            val fallback = UserProfileEntity(
                userId = userId,
                fullName = user?.fullName.orEmpty(),
                createdAt = now,
                updatedAt = now
            )
            userProfileDao.upsert(fallback)
            return fallback.toDomain()
        }

        val userId = authRepository.currentUserId()
            ?: error("User not logged in")
        return supabase.postgrest["user_profiles"]
            .select {
                filter { eq("user_id", userId) }
                single()
            }
            .decodeAs()
    }

    suspend fun updateProfile(fullName: String, phone: String?, jobTitle: String?): UserProfile {
        if (DataSourceConfig.isLocal) {
            val userId = authRepository.currentUserId()
                ?: error("User not logged in")
            val now = Clock.System.now().toString()
            val currentProfile = userProfileDao.getById(userId)
            val profile = UserProfileEntity(
                userId = userId,
                fullName = fullName,
                avatarUrl = currentProfile?.avatarUrl,
                phone = phone,
                jobTitle = jobTitle,
                createdAt = currentProfile?.createdAt ?: now,
                updatedAt = now
            )
            userProfileDao.upsert(profile)
            localUserDao.getById(userId)?.let { user ->
                localUserDao.upsert(user.copy(fullName = fullName, updatedAt = now))
            }
            return profile.toDomain()
        }

        val userId = authRepository.currentUserId()
            ?: error("User not logged in")
        return supabase.postgrest["user_profiles"]
            .update(
                value = buildJsonObject {
                    put("full_name", fullName)
                    if (phone.isNullOrBlank()) put("phone", JsonNull) else put("phone", phone)
                    if (jobTitle.isNullOrBlank()) put("job_title", JsonNull) else put("job_title", jobTitle)
                    put("updated_at", Clock.System.now().toString())
                }
            ) {
                filter { eq("user_id", userId) }
            }
            .decodeSingle()
    }
}

@Singleton
class DashboardRepository @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend fun getStats(workspaceId: String): DashboardStats {
        val projects = projectRepository.getProjects(workspaceId)
        val tasks = projects.flatMap { project -> taskRepository.getTasks(project.projectId) }
        val members = workspaceRepository.getWorkspaceMembers(workspaceId).associateBy { it.userId }
        val today = LocalDate.now()
        val overdue = tasks.count { task ->
            task.status != TaskStatus.Done.wire && task.dueAt?.let { parseDate(it)?.isBefore(today) } == true
        }
        val workload = tasks
            .filter { it.status != TaskStatus.Done.wire }
            .groupingBy { task ->
                val assignee = task.assigneeId
                members[assignee]?.fullName?.takeIf { it.isNotBlank() } ?: "Chưa phân công"
            }
            .eachCount()

        return DashboardStats(
            totalTasks = tasks.size,
            completedTasks = tasks.count { it.status == TaskStatus.Done.wire },
            inProgressTasks = tasks.count { it.status == TaskStatus.InProgress.wire },
            overdueTasks = overdue,
            reviewTasks = tasks.count { it.status == TaskStatus.Review.wire },
            workloadByAssignee = workload
        )
    }

    private fun parseDate(raw: String): LocalDate? {
        return runCatching { OffsetDateTime.parse(raw).toLocalDate() }
            .recoverCatching { LocalDate.parse(raw.take(10)) }
            .getOrNull()
    }
}

fun Uri.displayNameFallback(): String = lastPathSegment?.substringAfterLast('/') ?: "attachment_${System.currentTimeMillis()}"
