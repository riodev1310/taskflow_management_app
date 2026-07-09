package com.taskflow.demo.presentation.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.demo.core.util.AuthErrorMapper
import com.taskflow.demo.core.util.UiState
import com.taskflow.demo.data.repository.DashboardRepository
import com.taskflow.demo.domain.model.DashboardStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {
    private val workspaceId: String = checkNotNull(savedStateHandle["workspaceId"])

    private val _uiState = MutableStateFlow<UiState<DashboardStats>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            runCatching { dashboardRepository.getStats(workspaceId) }
                .onSuccess { stats -> _uiState.update { UiState.Success(stats) } }
                .onFailure { throwable -> _uiState.update { UiState.Error(AuthErrorMapper.map(throwable.message)) } }
        }
    }
}
