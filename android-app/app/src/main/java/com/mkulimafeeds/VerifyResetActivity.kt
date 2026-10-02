package com.mkulimafeeds

import android.content.Context
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
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AuthRepository
import kotlinx.coroutines.launch

class VerifyResetActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_IDENTIFIER = "EXTRA_IDENTIFIER"
        private const val EXTRA_EMAIL_DESTINATION = "EXTRA_EMAIL_DESTINATION"
        private const val EXTRA_PHONE_DESTINATION = "EXTRA_PHONE_DESTINATION"

        fun newIntent(
            context: Context,
            identifier: String,
            emailDestination: String?,
            phoneDestination: String?
        ): Intent {
            return Intent(context, VerifyResetActivity::class.java).apply {
                putExtra(EXTRA_IDENTIFIER, identifier)
                putExtra(EXTRA_EMAIL_DESTINATION, emailDestination)
                putExtra(EXTRA_PHONE_DESTINATION, phoneDestination)
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
    private var emailDestination: String? = null
    private var phoneDestination: String? = null

    private lateinit var tvSubTitle: TextView
    private lateinit var layoutEmailCode: LinearLayout
    private lateinit var layoutPhoneCode: LinearLayout
    private lateinit var labelEmailCode: TextView
    private lateinit var labelPhoneCode: TextView
    private lateinit var etEmailCode: EditText
    private lateinit var etPhoneCode: EditText
    private lateinit var btnVerifyCodes: MaterialButton
    private lateinit var btnBack: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_reset)

        // ---------------------------------------------
        // Read intent extras
        // ---------------------------------------------
        identifier = intent.getStringExtra(EXTRA_IDENTIFIER).orEmpty()
        emailDestination = intent.getStringExtra(EXTRA_EMAIL_DESTINATION)
        phoneDestination = intent.getStringExtra(EXTRA_PHONE_DESTINATION)

        if (identifier.isBlank()) {
            toast("Something went wrong. Please start over.")
            finish()
            return
        }

        val rootView = findViewById<View>(R.id.verifyResetRoot)
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
        configureFields()
        setupVerifyButton()
        setupBackButton()
    }

    private fun initializeViews() {
        tvSubTitle = findViewById(R.id.subTitleText)
        layoutEmailCode = findViewById(R.id.layoutEmailCode)
        layoutPhoneCode = findViewById(R.id.layoutPhoneCode)
        labelEmailCode = findViewById(R.id.labelEmailCode)
        labelPhoneCode = findViewById(R.id.labelPhoneCode)
        etEmailCode = findViewById(R.id.etEmailCode)
        etPhoneCode = findViewById(R.id.etPhoneCode)
        btnVerifyCodes = findViewById(R.id.btnVerifyCodes)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun configureFields() {

        val hasEmail = !emailDestination.isNullOrBlank()
        val hasPhone = !phoneDestination.isNullOrBlank()

        val descriptions = mutableListOf<String>()

        if (hasEmail) {
            layoutEmailCode.visibility = View.VISIBLE
            labelEmailCode.text = "EMAIL CODE SENT TO ${emailDestination!!.uppercase()}"
            descriptions.add("email")
        }

        if (hasPhone) {
            layoutPhoneCode.visibility = View.VISIBLE
            labelPhoneCode.text = "PHONE CODE SENT TO ${phoneDestination!!.uppercase()}"
            descriptions.add("phone")
        }

        tvSubTitle.text = when (descriptions.size) {
            0 -> "We couldn't find codes to verify. Please start over."
            1 -> "Enter the 6-digit code we sent to your ${descriptions[0]}."
            else -> "Enter the 6-digit codes we sent to your email and phone."
        }
    }

    private fun setupVerifyButton() {
        btnVerifyCodes.setOnClickListener {
            submitVerify()
        }
    }

    private fun submitVerify() {

        val hasEmail = !emailDestination.isNullOrBlank()
        val hasPhone = !phoneDestination.isNullOrBlank()

        val emailCode = if (hasEmail)
            etEmailCode.text?.toString()?.trim().orEmpty()
        else null

        val phoneCode = if (hasPhone)
            etPhoneCode.text?.toString()?.trim().orEmpty()
        else null

        // ---------------------------------------------
        // Client validation
        // ---------------------------------------------
        if (hasEmail && (emailCode == null || emailCode.length != 6)) {
            toast("Please enter the 6-digit email code")
            etEmailCode.requestFocus()
            return
        }
        if (hasPhone && (phoneCode == null || phoneCode.length != 6)) {
            toast("Please enter the 6-digit phone code")
            etPhoneCode.requestFocus()
            return
        }

        btnVerifyCodes.isEnabled = false
        btnVerifyCodes.text = "Verifying..."

        lifecycleScope.launch {
            try {

                val response = authRepository.forgotPasswordVerify(
                    identifier = identifier,
                    emailCode = emailCode,
                    phoneCode = phoneCode
                )

                val resetToken = response["resetToken"]
                if (resetToken.isNullOrBlank()) {
                    toast("Verification failed. Please try again.")
                    btnVerifyCodes.isEnabled = true
                    btnVerifyCodes.text = "Verify Codes"
                    return@launch
                }

                // ---------------------------------------------
                // Navigate to password reset screen
                // ---------------------------------------------
                startActivity(
                    ResetPasswordActivity.newIntent(
                        context = this@VerifyResetActivity,
                        identifier = identifier,
                        resetToken = resetToken
                    )
                )
                finish()

            } catch (e: Exception) {
                toast(e.message ?: "Invalid or expired codes")
                btnVerifyCodes.isEnabled = true
                btnVerifyCodes.text = "Verify Codes"
            }
        }
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}