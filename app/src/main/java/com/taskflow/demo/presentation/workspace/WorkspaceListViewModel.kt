package com.taskflow.demo.presentation.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.demo.core.util.AuthErrorMapper
import com.taskflow.demo.core.util.UiState
import com.taskflow.demo.data.repository.AuthRepository
import com.taskflow.demo.data.repository.WorkspaceRepository
import com.taskflow.demo.domain.model.CreateWorkspaceRequest
import com.taskflow.demo.domain.model.Workspace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceListState(
    val workspaces: UiState<List<Workspace>> = UiState.Loading,
    val createName: String = "",
    val createDescription: String = "",
    val isCreating: Boolean = false,
    val deletingWorkspaceId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class WorkspaceListViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkspaceListState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(workspaces = UiState.Loading, errorMessage = null) }
            runCatching { workspaceRepository.getMyWorkspaces() }
                .onSuccess { workspaces -> _uiState.update { it.copy(workspaces = UiState.Success(workspaces)) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(workspaces = UiState.Error(AuthErrorMapper.map(throwable.message)))
                    }
                }
        }
    }

    fun onCreateNameChanged(value: String) {
        _uiState.update { it.copy(createName = value, errorMessage = null) }
    }

    fun onCreateDescriptionChanged(value: String) {
        _uiState.update { it.copy(createDescription = value, errorMessage = null) }
    }

    fun createWorkspace(onCreated: (String) -> Unit) {
        val state = _uiState.value
        if (state.createName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên workspace") }
            return
        }

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isCreating = true, errorMessage = null) }
                workspaceRepository.createWorkspace(
                    CreateWorkspaceRequest(
                        name = state.createName.trim(),
                        description = state.createDescription.trim().ifBlank { null }
                    )
                )
            }.onSuccess { workspace ->
                _uiState.update {
                    val current = (it.workspaces as? UiState.Success)?.data.orEmpty()
                    it.copy(
                        workspaces = UiState.Success(listOf(workspace) + current),
                        createName = "",
                        createDescription = "",
                        isCreating = false
                    )
                }
                onCreated(workspace.workspaceId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isCreating = false, errorMessage = AuthErrorMapper.map(throwable.message))
                }
            }
        }
    }

    fun deleteWorkspace(workspace: Workspace) {
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(deletingWorkspaceId = workspace.workspaceId, errorMessage = null) }
                workspaceRepository.deleteWorkspace(workspace.workspaceId)
            }.onSuccess {
                _uiState.update { state ->
                    val current = (state.workspaces as? UiState.Success)?.data.orEmpty()
                    state.copy(
                        workspaces = UiState.Success(current.filterNot { it.workspaceId == workspace.workspaceId }),
                        deletingWorkspaceId = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(deletingWorkspaceId = null, errorMessage = AuthErrorMapper.map(throwable.message))
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            onDone()
        }
    }
}
