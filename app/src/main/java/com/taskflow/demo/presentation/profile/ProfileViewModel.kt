package com.taskflow.demo.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.demo.core.util.AuthErrorMapper
import com.taskflow.demo.data.repository.AuthRepository
import com.taskflow.demo.data.repository.ProfileRepository
import com.taskflow.demo.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val profile: UserProfile? = null,
    val fullName: String = "",
    val phone: String = "",
    val jobTitle: String = "",
    val dueSoonEnabled: Boolean = true,
    val commentBadgeEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { profileRepository.getMyProfile() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            fullName = profile.fullName,
                            phone = profile.phone.orEmpty(),
                            jobTitle = profile.jobTitle.orEmpty(),
                            isLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
                }
        }
    }

    fun onFullNameChanged(value: String) = _uiState.update { it.copy(fullName = value, errorMessage = null) }
    fun onPhoneChanged(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }
    fun onJobTitleChanged(value: String) = _uiState.update { it.copy(jobTitle = value, errorMessage = null) }
    fun onDueSoonChanged(value: Boolean) = _uiState.update { it.copy(dueSoonEnabled = value) }
    fun onCommentBadgeChanged(value: Boolean) = _uiState.update { it.copy(commentBadgeEnabled = value) }

    fun save() {
        val state = _uiState.value
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập họ tên") }
            return
        }
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                profileRepository.updateProfile(
                    fullName = state.fullName.trim(),
                    phone = state.phone.trim().ifBlank { null },
                    jobTitle = state.jobTitle.trim().ifBlank { null }
                )
            }.onSuccess { profile ->
                _uiState.update { it.copy(profile = profile, isSaving = false) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false, errorMessage = AuthErrorMapper.map(throwable.message)) }
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
