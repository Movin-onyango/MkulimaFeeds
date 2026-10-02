package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private val authRepository by lazy {
        AuthRepository(
            apiService = NetworkModule.apiService,
            tokenManager = TokenManager(applicationContext)
        )
    }

    private lateinit var etIdentifier: EditText
    private lateinit var btnSendCode: MaterialButton
    private lateinit var btnBackLogin: LinearLayout
    private lateinit var cardInfoBanner: MaterialCardView
    private lateinit var tvInfoBanner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val rootView = findViewById<View>(R.id.forgot_root)
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
        setupSendCode()
        setupBackToLogin()
    }

    private fun initializeViews() {
        etIdentifier = findViewById(R.id.etIdentifier)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnBackLogin = findViewById(R.id.btnBackLogin)
        cardInfoBanner = findViewById(R.id.cardInfoBanner)
        tvInfoBanner = findViewById(R.id.tvInfoBanner)
    }

    private fun setupSendCode() {
        btnSendCode.setOnClickListener {
            submitInitiate()
        }
    }

    private fun submitInitiate() {

        val identifier = etIdentifier.text?.toString()?.trim().orEmpty()

        if (identifier.isBlank()) {
            toast("Please enter your email or phone")
            etIdentifier.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {

                val response = authRepository.forgotPasswordInitiate(identifier)

                // ---------------------------------------------
                // Always "succeeds" (no user enumeration)
                // ---------------------------------------------
                val hasEmail = !response.emailDestination.isNullOrBlank()
                val hasPhone = !response.phoneDestination.isNullOrBlank()

                if (!hasEmail && !hasPhone) {
                    // Account doesn't exist OR has no contacts.
                    // We still transition to the verify screen but
                    // show a generic message; the user will fail
                    // verification anyway.

                    toast(response.message)

                    startActivity(
                        VerifyResetActivity.newIntent(
                            this@ForgotPasswordActivity,
                            identifier = identifier,
                            emailDestination = null,
                            phoneDestination = null
                        )
                    )
                    finish()
                    return@launch
                }

                val message = buildString {
                    append("A code was sent to ")
                    val parts = mutableListOf<String>()
                    if (hasEmail) parts.add(response.emailDestination!!)
                    if (hasPhone) parts.add(response.phoneDestination!!)
                    append(parts.joinToString(" and "))
                    append(".")
                }

                tvInfoBanner.text = message
                cardInfoBanner.visibility = View.VISIBLE

                // Small delay so the user sees the banner before navigation
                etIdentifier.postDelayed({
                    startActivity(
                        VerifyResetActivity.newIntent(
                            this@ForgotPasswordActivity,
                            identifier = identifier,
                            emailDestination = response.emailDestination,
                            phoneDestination = response.phoneDestination
                        )
                    )
                    finish()
                }, 800)

            } catch (e: Exception) {
                toast(e.message ?: "Something went wrong. Please try again.")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSendCode.isEnabled = !loading
        btnSendCode.text = if (loading) "Sending..." else "Send Code"
    }

    private fun setupBackToLogin() {
        btnBackLogin.setOnClickListener {
            finish()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}