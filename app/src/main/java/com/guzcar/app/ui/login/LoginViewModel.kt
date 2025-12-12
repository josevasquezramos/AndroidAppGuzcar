package com.guzcar.app.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guzcar.app.data.api.RetrofitClient
import com.guzcar.app.data.model.LoginRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val token: String, val user: Any?) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    val loginState = MutableLiveData<LoginState>()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                loginState.postValue(LoginState.Loading)

                val response = RetrofitClient.api.login(LoginRequest(email, password))
                loginState.postValue(LoginState.Success(response.token, response.user))

            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    401 -> "Credenciales incorrectas"
                    422 -> "Datos inválidos"
                    else -> "Error inesperado en el servidor"
                }
                loginState.postValue(LoginState.Error(msg))

            } catch (e: Exception) {
                loginState.postValue(LoginState.Error("No se pudo conectar con el servidor"))
            }
        }
    }
}
