package com.taskflow.demo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.demo.core.util.AuthErrorMapper
import com.taskflow.demo.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = authRepository.currentUserId() != null
}

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginFormState())
    val uiState = _uiState.asStateFlow()

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess = _loginSuccess.asSharedFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập email và mật khẩu") }
            return
        }

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                authRepository.signIn(state.email.trim(), state.password)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _loginSuccess.emit(Unit)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AuthErrorMapper.map(throwable.message))
                }
            }
        }
    }
}

data class RegisterFormState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterFormState())
    val uiState = _uiState.asStateFlow()

    private val _registerSuccess = MutableSharedFlow<Unit>()
    val registerSuccess = _registerSuccess.asSharedFlow()

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun register() {
        val state = _uiState.value
        when {
            state.fullName.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Vui lòng nhập họ tên") }
                return
            }
            state.email.isBlank() || state.password.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Vui lòng nhập email và mật khẩu") }
                return
            }
            state.password.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Mật khẩu tối thiểu 6 ký tự") }
                return
            }
            state.password != state.confirmPassword -> {
                _uiState.update { it.copy(errorMessage = "Xác nhận mật khẩu không khớp") }
                return
            }
        }

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                authRepository.signUp(state.email.trim(), state.password, state.fullName.trim())
                runCatching { authRepository.signIn(state.email.trim(), state.password) }
                    .onFailure { throwable ->
                        if (AuthErrorMapper.isEmailNotConfirmed(throwable.message)) {
                            throw IllegalStateException("Đăng ký thành công. Vui lòng xác thực email trước khi đăng nhập.")
                        } else {
                            throw throwable
                        }
                    }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _registerSuccess.emit(Unit)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AuthErrorMapper.map(throwable.message))
                }
            }
        }
    }
}
