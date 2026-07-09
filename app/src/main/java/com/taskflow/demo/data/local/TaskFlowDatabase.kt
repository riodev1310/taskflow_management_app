package com.taskflow.demo.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.taskflow.demo.domain.model.Project
import com.taskflow.demo.domain.model.Task
import com.taskflow.demo.domain.model.TaskAttachment
import com.taskflow.demo.domain.model.TaskComment
import com.taskflow.demo.domain.model.UserProfile
import com.taskflow.demo.domain.model.Workspace
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "local_users")
data class LocalUserEntity(
    @PrimaryKey val userId: String = UUID.randomUUID().toString(),
    val email: String,
    val password: String,
    val fullName: String,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val fullName: String = "",
    val avatarUrl: String? = null,
    val phone: String? = null,
    val jobTitle: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val workspaceId: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val iconUrl: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "workspace_members")
data class WorkspaceMemberEntity(
    @PrimaryKey val memberId: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val userId: String,
    val role: String = "member",
    val joinedAt: String = ""
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val projectId: String,
    val workspaceId: String,
    val name: String,
    val description: String? = null,
    val status: String = "active",
    val colorHex: String? = null,
    val dueDate: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val projectId: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val assigneeId: String? = null,
    val reporterId: String,
    val dueAt: String? = null,
    val completedAt: String? = null,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "task_comments")
data class TaskCommentEntity(
    @PrimaryKey val commentId: String,
    val taskId: String,
    val userId: String,
    val content: String,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "task_attachments")
data class TaskAttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val taskId: String,
    val uploadedBy: String,
    val fileName: String,
    val fileUrl: String,
    val fileType: String? = null,
    val sizeBytes: Long? = null,
    val createdAt: String = ""
)

@Entity(tableName = "task_drafts")
data class TaskDraftEntity(
    @PrimaryKey val draftId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val description: String?,
    val priority: String = "medium",
    val assigneeId: String?,
    val dueAt: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_projects")
data class CachedProjectEntity(
    @PrimaryKey val projectId: String,
    val workspaceId: String,
    val name: String,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface LocalUserDao {
    @Query("SELECT * FROM local_users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): LocalUserEntity?

    @Query("SELECT * FROM local_users WHERE userId = :userId LIMIT 1")
    suspend fun getById(userId: String): LocalUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: LocalUserEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getById(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId IN (:userIds)")
    suspend fun getByIds(userIds: List<String>): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface WorkspaceDao {
    @Query(
        """
        SELECT workspaces.* FROM workspaces
        INNER JOIN workspace_members ON workspaces.workspaceId = workspace_members.workspaceId
        WHERE workspace_members.userId = :userId
        ORDER BY workspaces.updatedAt DESC
        """
    )
    suspend fun getForUser(userId: String): List<WorkspaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE workspaceId = :workspaceId")
    suspend fun deleteById(workspaceId: String)
}

@Dao
interface WorkspaceMemberDao {
    @Query("SELECT * FROM workspace_members WHERE workspaceId = :workspaceId ORDER BY joinedAt ASC")
    suspend fun getByWorkspace(workspaceId: String): List<WorkspaceMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: WorkspaceMemberEntity)

    @Query("DELETE FROM workspace_members WHERE workspaceId = :workspaceId")
    suspend fun deleteByWorkspace(workspaceId: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    suspend fun getById(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    suspend fun getByWorkspace(workspaceId: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE workspaceId = :workspaceId AND status = 'active' ORDER BY updatedAt DESC")
    suspend fun getActiveByWorkspace(workspaceId: String): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE projectId = :projectId")
    suspend fun deleteById(projectId: String)

    @Query("DELETE FROM projects WHERE workspaceId = :workspaceId")
    suspend fun deleteByWorkspace(workspaceId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getByProject(projectId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE projectId IN (:projectIds)")
    suspend fun getByProjectIds(projectIds: List<String>): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun updateStatus(taskId: String, status: String, completedAt: String?, updatedAt: String)

    @Query("UPDATE tasks SET assigneeId = :assigneeId, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun updateAssignee(taskId: String, assigneeId: String?, updatedAt: String)

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)

    @Query("DELETE FROM tasks WHERE projectId IN (:projectIds)")
    suspend fun deleteByProjectIds(projectIds: List<String>)
}

@Dao
interface TaskCommentDao {
    @Query("SELECT * FROM task_comments WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTask(taskId: String): List<TaskCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comment: TaskCommentEntity)

    @Query("DELETE FROM task_comments WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)

    @Query("DELETE FROM task_comments WHERE taskId IN (:taskIds)")
    suspend fun deleteByTaskIds(taskIds: List<String>)
}

@Dao
interface TaskAttachmentDao {
    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY createdAt DESC")
    suspend fun getByTask(taskId: String): List<TaskAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attachment: TaskAttachmentEntity)

    @Query("DELETE FROM task_attachments WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)

    @Query("DELETE FROM task_attachments WHERE taskId IN (:taskIds)")
    suspend fun deleteByTaskIds(taskIds: List<String>)
}

@Dao
interface TaskDraftDao {
    @Query("SELECT * FROM task_drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TaskDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: TaskDraftEntity)

    @Query("DELETE FROM task_drafts WHERE draftId = :draftId")
    suspend fun deleteById(draftId: String)

    @Query("DELETE FROM task_drafts")
    suspend fun clearAll()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY createdAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(query: SearchHistoryEntity)
}

@Dao
interface CachedProjectDao {
    @Query("SELECT * FROM cached_projects WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun observeByWorkspace(workspaceId: String): Flow<List<CachedProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(projects: List<CachedProjectEntity>)
}

@Database(
    entities = [
        LocalUserEntity::class,
        UserProfileEntity::class,
        WorkspaceEntity::class,
        WorkspaceMemberEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        TaskCommentEntity::class,
        TaskAttachmentEntity::class,
        TaskDraftEntity::class,
        SearchHistoryEntity::class,
        CachedProjectEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TaskFlowDatabase : RoomDatabase() {
    abstract fun localUserDao(): LocalUserDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceMemberDao(): WorkspaceMemberDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun taskCommentDao(): TaskCommentDao
    abstract fun taskAttachmentDao(): TaskAttachmentDao
    abstract fun taskDraftDao(): TaskDraftDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun cachedProjectDao(): CachedProjectDao

    companion object {
        const val DATABASE_NAME = "taskflow_local.db"
    }
}

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        userId = userId,
        fullName = fullName,
        avatarUrl = avatarUrl,
        phone = phone,
        jobTitle = jobTitle,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WorkspaceEntity.toDomain(): Workspace {
    return Workspace(
        workspaceId = workspaceId,
        name = name,
        description = description,
        ownerId = ownerId,
        iconUrl = iconUrl,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ProjectEntity.toDomain(): Project {
    return Project(
        projectId = projectId,
        workspaceId = workspaceId,
        name = name,
        description = description,
        status = status,
        colorHex = colorHex,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaskEntity.toDomain(): Task {
    return Task(
        taskId = taskId,
        projectId = projectId,
        title = title,
        description = description,
        status = status,
        priority = priority,
        assigneeId = assigneeId,
        reporterId = reporterId,
        dueAt = dueAt,
        completedAt = completedAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaskCommentEntity.toDomain(): TaskComment {
    return TaskComment(
        commentId = commentId,
        taskId = taskId,
        userId = userId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaskAttachmentEntity.toDomain(): TaskAttachment {
    return TaskAttachment(
        attachmentId = attachmentId,
        taskId = taskId,
        uploadedBy = uploadedBy,
        fileName = fileName,
        fileUrl = fileUrl,
        fileType = fileType,
        sizeBytes = sizeBytes,
        createdAt = createdAt
    )
}
