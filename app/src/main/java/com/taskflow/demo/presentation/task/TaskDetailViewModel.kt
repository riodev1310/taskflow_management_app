package com.taskflow.demo.presentation.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.demo.core.util.AuthErrorMapper
import com.taskflow.demo.data.repository.AttachmentRepository
import com.taskflow.demo.data.repository.CommentRepository
import com.taskflow.demo.data.repository.ProjectRepository
import com.taskflow.demo.data.repository.TaskRepository
import com.taskflow.demo.data.repository.WorkspaceRepository
import com.taskflow.demo.domain.model.Task
import com.taskflow.demo.domain.model.TaskAttachment
import com.taskflow.demo.domain.model.TaskComment
import com.taskflow.demo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailState(
    val task: Task? = null,
    val members: List<UserProfile> = emptyList(),
    val comments: List<TaskComment> = emptyList(),
    val attachments: List<TaskAttachment> = emptyList(),
    val commentText: String = "",
    val isLoading: Boolean = true,
    val isUpdatingAssignee: Boolean = false,
    val isDeletingTask: Boolean = false,
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val commentRepository: CommentRepository,
    private val attachmentRepository: AttachmentRepository
) : ViewModel() {
    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(TaskDetailState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val task = taskRepository.getTask(taskId)
                val project = projectRepository.getProject(task.projectId)
                TaskDetailLoadResult(
                    task = task,
                    members = runCatching { workspaceRepository.getWorkspaceMembers(project.workspaceId) }.getOrDefault(emptyList()),
                    comments = commentRepository.getComments(taskId),
                    attachments = attachmentRepository.getAttachments(taskId)
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        task = result.task,
                        members = result.members,
                        comments = result.comments,
                        attachments = result.attachments,
                        isLoading = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
            }
        }
    }

    fun updateAssignee(assigneeId: String?) {
        viewModelScope.launch {
            val previous = _uiState.value.task
            runCatching {
                _uiState.update {
                    it.copy(
                        task = it.task?.copy(assigneeId = assigneeId),
                        isUpdatingAssignee = true,
                        errorMessage = null
                    )
                }
                taskRepository.updateAssignee(taskId, assigneeId)
            }.onSuccess { task ->
                _uiState.update { it.copy(task = task, isUpdatingAssignee = false) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        task = previous,
                        isUpdatingAssignee = false,
                        errorMessage = AuthErrorMapper.map(throwable.message)
                    )
                }
            }
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isDeletingTask = true, errorMessage = null) }
                taskRepository.deleteTask(taskId)
            }.onSuccess {
                _uiState.update { it.copy(isDeletingTask = false) }
                onDeleted()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isDeletingTask = false, errorMessage = AuthErrorMapper.map(throwable.message))
                }
            }
        }
    }

    fun onCommentChanged(value: String) {
        _uiState.update { it.copy(commentText = value, errorMessage = null) }
    }

    fun sendComment() {
        val content = _uiState.value.commentText.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isSending = true, errorMessage = null) }
                commentRepository.addComment(taskId, content)
            }.onSuccess { comment ->
                _uiState.update {
                    it.copy(comments = it.comments + comment, commentText = "", isSending = false)
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSending = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
            }
        }
    }

    fun uploadAttachment(bytes: ByteArray, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isUploading = true, errorMessage = null) }
                attachmentRepository.uploadTaskAttachment(taskId, bytes, fileName, mimeType)
            }.onSuccess { attachment ->
                _uiState.update {
                    it.copy(attachments = listOf(attachment) + it.attachments, isUploading = false)
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isUploading = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
            }
        }
    }
}

private data class TaskDetailLoadResult(
    val task: Task,
    val members: List<UserProfile>,
    val comments: List<TaskComment>,
    val attachments: List<TaskAttachment>
)
