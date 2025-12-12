package com.guzcar.app.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.guzcar.app.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.guzcar.app.data.api.TokenManager
import com.guzcar.app.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Revisar si ya hay token guardado
        val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)

        if (savedToken != null) {
            // 2. Configurar TokenManager para que Retrofit lo use
            TokenManager.token = savedToken

            // 3. Ir directo al Main y cerrar Login
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Si NO hay token, recién mostrar la pantalla de login
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.emailEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString().trim()

            binding.emailLayout.error = null
            binding.passwordLayout.error = null

            viewModel.login(email, password)
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {

                is LoginState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.progress.visibility = android.view.View.VISIBLE
                }

                is LoginState.Success -> {
                    binding.btnLogin.isEnabled = true
                    binding.progress.visibility = android.view.View.GONE

                    // Guardar token en SharedPreferences
                    getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                        .edit()
                        .putString("token", state.token)
                        .apply()

                    // Ir a pantalla principal
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

                is LoginState.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.progress.visibility = android.view.View.GONE

                    binding.emailLayout.error = state.message
                    Snackbar.make(binding.btnLogin, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
