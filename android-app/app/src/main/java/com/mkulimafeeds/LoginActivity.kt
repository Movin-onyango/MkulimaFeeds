package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var etIdentifier: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    // =========================================================
    // REPOSITORY
    // =========================================================

    private val authRepository by lazy {
        AuthRepository(
            apiService = NetworkModule.apiService,
            tokenManager = TokenManager(applicationContext)
        )
    }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val rootView = findViewById<View>(R.id.login_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        initializeViews()
        setupLoginButton()
        setupForgotPassword()
        setupRegisterLink()
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private fun initializeViews() {
        etIdentifier = findViewById(R.id.etIdentifier)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister = findViewById(R.id.tvRegister)
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private fun setupLoginButton() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {

        val identifier = etIdentifier.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()

        // -----------------------------------------------------
        // Basic client validation
        // -----------------------------------------------------
        if (identifier.isBlank()) {
            toast("Please enter your email or phone")
            etIdentifier.requestFocus()
            return
        }
        if (password.isBlank()) {
            toast("Please enter your password")
            etPassword.requestFocus()
            return
        }

        // -----------------------------------------------------
        // Disable UI while request is in flight
        // -----------------------------------------------------
        setLoading(true)

        lifecycleScope.launch {
            try {

                val response = authRepository.login(
                    identifier = identifier,
                    password = password
                )

                // Token is saved inside AuthRepository.login().
                toast("Welcome back, ${response.user.name}!")

                openDashboard(response.user.role)

            } catch (e: Exception) {

                toast(e.message ?: "Login failed. Please try again.")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "Logging in..." else "Log In"
        etIdentifier.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    // =========================================================
    // ROLE-BASED NAVIGATION
    // =========================================================

    private fun openDashboard(role: String?) {

        val destination = when (role?.trim()?.uppercase()) {
            "ADMIN" -> AdminDashboardActivity::class.java
            "DEALER" -> DealerDashboardActivity::class.java
            "STAFF" -> AdminDashboardActivity::class.java
            "CUSTOMER" -> HomeActivity::class.java
            else -> {
                toast("Unknown user role: $role")
                setLoading(false)
                return
            }
        }

        val intent = Intent(this, destination)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    private fun setupForgotPassword() {
        tvForgotPassword.setOnClickListener {
            startActivity(
                Intent(this, ForgotPasswordActivity::class.java)
            )
        }
    }

    // =========================================================
    // REGISTER LINK
    // =========================================================

    private fun setupRegisterLink() {
        tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
            finish()
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}