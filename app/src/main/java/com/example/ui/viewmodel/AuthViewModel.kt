package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserEntity
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val user: UserEntity) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    init {
        // Pre-carrega dados estáticos do banco local e Firebase
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Preencha e-mail e senha para entrar.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = repository.findAndAuthenticateUser(email, pass)
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Error("Usuário não encontrado ou e-mail/senha incorretos. Verifique os dados ou cadastre-se.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Falha na autenticação.")
            }
        }
    }

    fun signup(name: String, email: String, pass: String, role: String, adminCode: String = "") {
        if (name.isBlank() || email.isBlank() || pass.length < 6) {
            _authState.value = AuthState.Error("A senha deve conter no mínimo 6 caracteres e o nome ser válido.")
            return
        }

        if (role == "admin" && adminCode.trim() != "2003") {
            _authState.value = AuthState.Error("Código secreto do Administrador incorreto! Verifique o código de autorização.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val existing = repository.getUserByEmail(email)
                if (existing != null) {
                    _authState.value = AuthState.Error("Este e-mail já está cadastrado. Faça login na sua conta.")
                    return@launch
                }

                val uid = "user_" + UUID.randomUUID().toString().take(8)
                val user = repository.registerNewUser(uid, name, email, role, pass)
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Erro ao criar conta: ${e.message}")
            }
        }
    }

    fun loginWithDemoUser(role: String, adminCode: String = "") {
        if (role == "admin" && adminCode.trim() != "2003") {
            _authState.value = AuthState.Error("Código secreto de Administrador incorreto! Verifique o código de autorização.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val demoUser = if (role == "admin") {
                UserEntity("admin_master_101", "Administrador Principal", "admin@biblioteca.com", "admin")
            } else {
                UserEntity("reader_user_202", "Lucas Mendes (Leitor)", "lucas.mendes@email.com", "reader")
            }
            repository.registerNewUser(demoUser.uid, demoUser.name, demoUser.email, demoUser.role, "123456")
            _currentUser.value = demoUser
            _authState.value = AuthState.Authenticated(demoUser)
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }
}
