package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor (
    private val appAuth: AppAuth,
    ): ViewModel() { // ViewModel для проверки авторизации (ViewModel сохраняет данные)

    val state: StateFlow<AuthState?> = appAuth
        .authState

        val isAuthenticated: Boolean
            get() = appAuth.authState.value != null // проверка на вход в приложение


    private val _refreshEvents = MutableStateFlow<Unit?>(null)
    val refreshEvents = _refreshEvents.filterNotNull()


init {

    state.drop(1)
        .onEach {

            _refreshEvents.value = Unit

        }.launchIn(viewModelScope)

}


    fun onRefresh() {
        _refreshEvents.value = null
    }

}
