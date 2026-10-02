package com.mkulimafeeds

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_IDENTIFIER = "EXTRA_IDENTIFIER"
        private const val EXTRA_RESET_TOKEN = "EXTRA_RESET_TOKEN"

        fun newIntent(
            context: Context,
            identifier: String,
            resetToken: String
        ): Intent {
            return Intent(context, ResetPasswordActivity::class.java).apply {
                putExtra(EXTRA_IDENTIFIER, identifier)
                putExtra(EXTRA_RESET_TOKEN, resetToken)
            }
        }
    }

    private val authRepository by lazy {
        AuthRepository(
            apiService = NetworkModule.apiService,
            tokenManager = TokenManager(applicationContext)
        )
    }

    private lateinit var identifier: String
    private lateinit var resetToken: String

    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        identifier = intent.getStringExtra(EXTRA_IDENTIFIER).orEmpty()
        resetToken = intent.getStringExtra(EXTRA_RESET_TOKEN).orEmpty()

        if (identifier.isBlank() || resetToken.isBlank()) {
            toast("Session expired. Please start over.")
            finish()
            return
        }

        val rootView = findViewById<View>(R.id.resetPasswordRoot)
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
        setupResetButton()
    }

    private fun initializeViews() {
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
    }

    private fun setupResetButton() {
        btnResetPassword.setOnClickListener {
            submitReset()
        }
    }

    private fun submitReset() {

        val newPassword = etNewPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

        if (newPassword.length < 8) {
            toast("Password must be at least 8 characters")
            etNewPassword.requestFocus()
            return
        }
        if (confirmPassword.isEmpty()) {
            toast("Please confirm your password")
            etConfirmPassword.requestFocus()
            return
        }
        if (newPassword != confirmPassword) {
            toast("Passwords do not match")
            etConfirmPassword.requestFocus()
            return
        }

        btnResetPassword.isEnabled = false
        btnResetPassword.text = "Resetting..."

        lifecycleScope.launch {
            try {

                authRepository.forgotPasswordReset(
                    identifier = identifier,
                    resetToken = resetToken,
                    newPassword = newPassword
                )

                toast("Password reset successfully. Please log in.")

                // ---------------------------------------------
                // Clear any stored token and return to login
                // ---------------------------------------------
                TokenManager(applicationContext).clearToken()

                val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                toast(e.message ?: "Could not reset password. Please try again.")
                btnResetPassword.isEnabled = true
                btnResetPassword.text = "Reset Password"
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}