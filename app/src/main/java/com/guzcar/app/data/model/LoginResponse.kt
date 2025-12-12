package com.guzcar.app.data.model

data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserResponse
)