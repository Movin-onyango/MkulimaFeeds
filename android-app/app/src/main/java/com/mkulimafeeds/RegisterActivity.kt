package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

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
    // STATE
    // =========================================================

    private enum class IdentifierMode { EMAIL, PHONE }

    private var identifierMode = IdentifierMode.EMAIL

    /** Filled in after successful registration initiate. */
    private var pendingDestination: String? = null

    private var resendTimer: CountDownTimer? = null

    // =========================================================
    // VIEWS — STEP 1
    // =========================================================

    private lateinit var layoutFormStep: LinearLayout
    private lateinit var tabEmail: TextView
    private lateinit var tabPhone: TextView
    private lateinit var layoutEmailInput: LinearLayout
    private lateinit var layoutPhoneInput: LinearLayout
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnContinue: MaterialButton
    private lateinit var tvLogin: TextView

    // =========================================================
    // VIEWS — STEP 2
    // =========================================================

    private lateinit var layoutOtpStep: LinearLayout
    private lateinit var tvOtpSubtitle: TextView
    private lateinit var etOtp: EditText
    private lateinit var tvResendTimer: TextView
    private lateinit var tvResendCode: TextView
    private lateinit var btnVerify: MaterialButton
    private lateinit var tvBackToForm: TextView

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val rootView = findViewById<View>(R.id.register_root)
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
        setupTabs()
        setupContinueButton()
        setupVerifyButton()
        setupBackToForm()
        setupLoginLink()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private fun initializeViews() {

        layoutFormStep = findViewById(R.id.layoutFormStep)
        tabEmail = findViewById(R.id.tabEmail)
        tabPhone = findViewById(R.id.tabPhone)
        layoutEmailInput = findViewById(R.id.layoutEmailInput)
        layoutPhoneInput = findViewById(R.id.layoutPhoneInput)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnContinue = findViewById(R.id.btnContinue)
        tvLogin = findViewById(R.id.tvLogin)

        layoutOtpStep = findViewById(R.id.layoutOtpStep)
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle)
        etOtp = findViewById(R.id.etOtp)
        tvResendTimer = findViewById(R.id.tvResendTimer)
        tvResendCode = findViewById(R.id.tvResendCode)
        btnVerify = findViewById(R.id.btnVerify)
        tvBackToForm = findViewById(R.id.tvBackToForm)

        // Auto-dismiss keyboard when 6 digits entered
        etOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    etOtp.clearFocus()
                }
            }
        })
    }

    // =========================================================
    // TABS — Email / Phone
    // =========================================================

    private fun setupTabs() {
        tabEmail.setOnClickListener { selectMode(IdentifierMode.EMAIL) }
        tabPhone.setOnClickListener { selectMode(IdentifierMode.PHONE) }
    }

    private fun selectMode(mode: IdentifierMode) {
        identifierMode = mode
        when (mode) {
            IdentifierMode.EMAIL -> {
                tabEmail.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.brand_dark_green)
                )
                tabEmail.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                tabEmail.setTypeface(null, android.graphics.Typeface.BOLD)

                tabPhone.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
                tabPhone.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
                tabPhone.setTypeface(null, android.graphics.Typeface.NORMAL)

                layoutEmailInput.visibility = View.VISIBLE
                layoutPhoneInput.visibility = View.GONE
            }
            IdentifierMode.PHONE -> {
                tabPhone.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.brand_dark_green)
                )
                tabPhone.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                tabPhone.setTypeface(null, android.graphics.Typeface.BOLD)

                tabEmail.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
                tabEmail.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
                tabEmail.setTypeface(null, android.graphics.Typeface.NORMAL)

                layoutEmailInput.visibility = View.GONE
                layoutPhoneInput.visibility = View.VISIBLE
            }
        }
    }

    // =========================================================
    // CONTINUE (register/initiate)
    // =========================================================

    private fun setupContinueButton() {
        btnContinue.setOnClickListener {
            submitRegistrationForm()
        }
    }

    private fun submitRegistrationForm() {

        val name = etName.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

        // ---------------------------------------------
        // Validate common fields
        // ---------------------------------------------
        if (name.length < 2) {
            toast("Please enter your full name")
            etName.requestFocus()
            return
        }
        if (password.length < 8) {
            toast("Password must be at least 8 characters")
            etPassword.requestFocus()
            return
        }
        if (confirmPassword.isEmpty()) {
            toast("Please confirm your password")
            etConfirmPassword.requestFocus()
            return
        }
        if (password != confirmPassword) {
            toast("Passwords do not match")
            etConfirmPassword.requestFocus()
            return
        }
        if (!cbTerms.isChecked) {
            toast("Please agree to the Terms of Service")
            return
        }

        // ---------------------------------------------
        // Validate identifier based on mode
        // ---------------------------------------------
        val email: String?
        val phone: String?

        when (identifierMode) {
            IdentifierMode.EMAIL -> {
                email = etEmail.text?.toString()?.trim().orEmpty()
                phone = null
                if (email.isBlank()) {
                    toast("Please enter your email")
                    etEmail.requestFocus()
                    return
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    toast("Please enter a valid email address")
                    etEmail.requestFocus()
                    return
                }
            }
            IdentifierMode.PHONE -> {
                email = null
                phone = etPhone.text?.toString()?.trim().orEmpty()
                if (phone.isBlank()) {
                    toast("Please enter your phone number")
                    etPhone.requestFocus()
                    return
                }
                if (!isValidKenyanPhone(phone)) {
                    toast("Enter a valid Kenyan number (e.g., 0712345678)")
                    etPhone.requestFocus()
                    return
                }
            }
        }

        // ---------------------------------------------
        // Disable form while request is in flight
        // ---------------------------------------------
        setFormEnabled(false)

        lifecycleScope.launch {
            try {

                val response = authRepository.registerInitiate(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password
                )

                pendingDestination = response.destination

                showOtpStep(response.destination, response.channel)

            } catch (e: Exception) {

                toast(e.message ?: "Registration failed. Please try again.")
                setFormEnabled(true)
            }
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        btnContinue.isEnabled = enabled
        btnContinue.text = if (enabled) "Continue" else "Sending code..."
    }

    // =========================================================
    // OTP STEP
    // =========================================================

    private fun showOtpStep(destination: String, channel: String) {

        layoutFormStep.visibility = View.GONE
        layoutOtpStep.visibility = View.VISIBLE

        val friendlyChannel = if (channel.equals("EMAIL", ignoreCase = true)) {
            "email"
        } else {
            "phone"
        }

        tvOtpSubtitle.text =
            "Enter the 6-digit code we sent to your $friendlyChannel ($destination)."

        etOtp.setText("")
        etOtp.requestFocus()

        startResendTimer()
    }

    private fun setupVerifyButton() {
        btnVerify.setOnClickListener {
            submitOtp()
        }
    }

    private fun submitOtp() {

        val destination = pendingDestination ?: run {
            toast("Session expired. Please start over.")
            returnToForm()
            return
        }

        val code = etOtp.text?.toString()?.trim().orEmpty()

        if (code.length != 6 || !code.all { it.isDigit() }) {
            toast("Enter the 6-digit code")
            return
        }

        btnVerify.isEnabled = false
        btnVerify.text = "Verifying..."

        lifecycleScope.launch {
            try {

                val response = authRepository.registerVerify(
                    destination = destination,
                    code = code
                )

                toast("Welcome, ${response.user.name}!")

                val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {

                toast(e.message ?: "Verification failed")
                btnVerify.isEnabled = true
                btnVerify.text = "Verify & Continue"
            }
        }
    }

    private fun setupBackToForm() {
        tvBackToForm.setOnClickListener {
            resendTimer?.cancel()
            returnToForm()
        }
    }

    private fun returnToForm() {
        layoutOtpStep.visibility = View.GONE
        layoutFormStep.visibility = View.VISIBLE
        etOtp.setText("")
        setFormEnabled(true)
    }

    // =========================================================
    // RESEND TIMER
    // =========================================================

    private fun startResendTimer() {

        resendTimer?.cancel()

        tvResendCode.visibility = View.GONE

        resendTimer = object : CountDownTimer(60_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                tvResendTimer.text = "Resend code in ${seconds}s"
                tvResendTimer.visibility = View.VISIBLE
            }

            override fun onFinish() {
                tvResendTimer.visibility = View.GONE
                tvResendCode.visibility = View.VISIBLE
                setupResendListener()
            }
        }.start()
    }

    private fun setupResendListener() {

        tvResendCode.setOnClickListener {

            val destination = pendingDestination ?: return@setOnClickListener

            tvResendCode.isEnabled = false

            val name = etName.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString().orEmpty()
            val email = if (identifierMode == IdentifierMode.EMAIL)
                etEmail.text?.toString()?.trim() else null
            val phone = if (identifierMode == IdentifierMode.PHONE)
                etPhone.text?.toString()?.trim() else null

            lifecycleScope.launch {
                try {
                    authRepository.registerInitiate(
                        name = name,
                        email = email,
                        phone = phone,
                        password = password
                    )
                    toast("New code sent to $destination")
                    startResendTimer()
                } catch (e: Exception) {
                    toast(e.message ?: "Could not resend code")
                    tvResendCode.isEnabled = true
                }
            }
        }
    }

    // =========================================================
    // LOGIN LINK
    // =========================================================

    private fun setupLoginLink() {
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Basic Kenyan phone validation matching the backend.
     */
    private fun isValidKenyanPhone(input: String): Boolean {
        val cleaned = input.trim()
        val patterns = listOf(
            Regex("^0[17]\\d{8}$"),
            Regex("^\\+254[17]\\d{8}$"),
            Regex("^254[17]\\d{8}$")
        )
        return patterns.any { it.matches(cleaned) }
    }
}