package com.taskflow.demo.presentation.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.demo.core.util.AuthErrorMapper
import com.taskflow.demo.core.util.DateFormatter
import com.taskflow.demo.data.repository.ProjectRepository
import com.taskflow.demo.data.repository.TaskRepository
import com.taskflow.demo.data.repository.WorkspaceRepository
import com.taskflow.demo.domain.model.CreateProjectRequest
import com.taskflow.demo.domain.model.CreateTaskRequest
import com.taskflow.demo.domain.model.Project
import com.taskflow.demo.domain.model.Task
import com.taskflow.demo.domain.model.TaskPriority
import com.taskflow.demo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectBoardState(
    val workspaceId: String = "",
    val projects: List<Project> = emptyList(),
    val selectedProjectId: String? = null,
    val tasks: List<Task> = emptyList(),
    val members: List<UserProfile> = emptyList(),
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val deletingProjectId: String? = null,
    val errorMessage: String? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val projectDueDate: String = "",
    val taskTitle: String = "",
    val taskDescription: String = "",
    val taskPriority: String = TaskPriority.Medium.wire,
    val taskAssigneeId: String? = null,
    val taskDueDate: String = ""
) {
    val selectedProject: Project?
        get() = projects.firstOrNull { it.projectId == selectedProjectId }
}

@HiltViewModel
class ProjectBoardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
    private val workspaceId: String = checkNotNull(savedStateHandle["workspaceId"])

    private val _uiState = MutableStateFlow(ProjectBoardState(workspaceId = workspaceId))
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val projects = projectRepository.getProjects(workspaceId)
                val selected = _uiState.value.selectedProjectId?.takeIf { id -> projects.any { it.projectId == id } }
                    ?: projects.firstOrNull()?.projectId
                val tasks = selected?.let { taskRepository.getTasks(it) }.orEmpty()
                val membersResult = runCatching { workspaceRepository.getWorkspaceMembers(workspaceId) }
                BoardLoadResult(
                    projects = projects,
                    selectedProjectId = selected,
                    tasks = tasks,
                    members = membersResult.getOrDefault(emptyList()),
                    memberWarning = membersResult.exceptionOrNull()?.message
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        projects = result.projects,
                        selectedProjectId = result.selectedProjectId,
                        tasks = result.tasks,
                        members = result.members,
                        isLoading = false,
                        errorMessage = result.memberWarning?.let { warning ->
                            "Board đã tải được, nhưng chưa tải được danh sách thành viên: ${AuthErrorMapper.map(warning)}"
                        }
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AuthErrorMapper.map(throwable.message))
                }
            }
        }
    }

    fun selectProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedProjectId = projectId, isLoading = true, errorMessage = null) }
            runCatching { taskRepository.getTasks(projectId) }
                .onSuccess { tasks -> _uiState.update { it.copy(tasks = tasks, isLoading = false) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
                }
        }
    }

    fun onProjectNameChanged(value: String) = _uiState.update { it.copy(projectName = value, errorMessage = null) }
    fun onProjectDescriptionChanged(value: String) = _uiState.update { it.copy(projectDescription = value, errorMessage = null) }
    fun onProjectDueDateChanged(value: String) = _uiState.update { it.copy(projectDueDate = value, errorMessage = null) }

    fun createProject(onCreated: () -> Unit) {
        val state = _uiState.value
        if (state.projectName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên project") }
            return
        }

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                projectRepository.createProject(
                    CreateProjectRequest(
                        workspaceId = workspaceId,
                        name = state.projectName.trim(),
                        description = state.projectDescription.trim().ifBlank { null },
                        dueDate = state.projectDueDate.trim().ifBlank { null }
                    )
                )
            }.onSuccess { project ->
                _uiState.update {
                    it.copy(
                        projects = listOf(project) + it.projects,
                        selectedProjectId = project.projectId,
                        tasks = emptyList(),
                        projectName = "",
                        projectDescription = "",
                        projectDueDate = "",
                        isMutating = false
                    )
                }
                onCreated()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isMutating = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
            }
        }
    }

    fun deleteSelectedProject() {
        val project = _uiState.value.selectedProject ?: return
        viewModelScope.launch {
            val previous = _uiState.value
            runCatching {
                _uiState.update { it.copy(deletingProjectId = project.projectId, errorMessage = null) }
                projectRepository.deleteProject(project.projectId)
            }.onSuccess {
                val remaining = previous.projects.filterNot { it.projectId == project.projectId }
                val nextSelected = remaining.firstOrNull()?.projectId
                val nextTasks = nextSelected?.let { projectId ->
                    runCatching { taskRepository.getTasks(projectId) }.getOrDefault(emptyList())
                }.orEmpty()
                _uiState.update {
                    it.copy(
                        projects = remaining,
                        selectedProjectId = nextSelected,
                        tasks = nextTasks,
                        deletingProjectId = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    previous.copy(
                        deletingProjectId = null,
                        errorMessage = AuthErrorMapper.map(throwable.message)
                    )
                }
            }
        }
    }

    fun onTaskTitleChanged(value: String) = _uiState.update { it.copy(taskTitle = value, errorMessage = null) }
    fun onTaskDescriptionChanged(value: String) = _uiState.update { it.copy(taskDescription = value, errorMessage = null) }
    fun onTaskPriorityChanged(value: String) = _uiState.update { it.copy(taskPriority = value) }
    fun onTaskAssigneeChanged(value: String?) = _uiState.update { it.copy(taskAssigneeId = value) }
    fun onTaskDueDateChanged(value: String) = _uiState.update { it.copy(taskDueDate = value, errorMessage = null) }

    fun createTask(onCreated: () -> Unit) {
        val state = _uiState.value
        val projectId = state.selectedProjectId ?: return
        if (state.taskTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tiêu đề task") }
            return
        }

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                taskRepository.createTask(
                    CreateTaskRequest(
                        projectId = projectId,
                        title = state.taskTitle.trim(),
                        description = state.taskDescription.trim().ifBlank { null },
                        priority = state.taskPriority,
                        assigneeId = state.taskAssigneeId,
                        dueAt = DateFormatter.toDueAt(state.taskDueDate)
                    )
                )
            }.onSuccess { task ->
                _uiState.update {
                    it.copy(
                        tasks = listOf(task) + it.tasks,
                        taskTitle = "",
                        taskDescription = "",
                        taskPriority = TaskPriority.Medium.wire,
                        taskAssigneeId = null,
                        taskDueDate = "",
                        isMutating = false
                    )
                }
                onCreated()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isMutating = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
            }
        }
    }

    fun updateStatus(task: Task, status: String) {
        viewModelScope.launch {
            val previous = _uiState.value.tasks
            _uiState.update {
                it.copy(tasks = it.tasks.map { item -> if (item.taskId == task.taskId) item.copy(status = status) else item })
            }
            runCatching { taskRepository.updateStatus(task.taskId, status) }
                .onFailure { throwable ->
                    _uiState.update { it.copy(tasks = previous, errorMessage = AuthErrorMapper.map(throwable.message)) }
                }
        }
    }
}

private data class BoardLoadResult(
    val projects: List<Project>,
    val selectedProjectId: String?,
    val tasks: List<Task>,
    val members: List<UserProfile>,
    val memberWarning: String?
)
