package com.mkulimafeeds

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { UserProfileRepository() }

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        val rootView = findViewById<View>(R.id.editProfileRoot)
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
        setupBackButton()
        loadCurrentProfile()
        setupSaveButton()
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentProfile() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val profile = repository.getMyProfile(token)
                etName.setText(profile.user.name)
                etEmail.setText(profile.user.email)
                etPhone.setText(profile.user.phone)
            } catch (e: Exception) {
                toast("Failed to load profile: ${e.message}")
                finish()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()

        // -----------------------------------------------------
        // Client-side validation
        // -----------------------------------------------------
        if (name.length < 2) {
            toast("Please enter your full name")
            etName.requestFocus()
            return
        }

        // Email is optional but must be valid if provided
        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Please enter a valid email address")
            etEmail.requestFocus()
            return
        }

        // Phone is optional but must be Kenyan format if provided
        if (phone.isNotBlank() && !isValidKenyanPhone(phone)) {
            toast("Enter a valid Kenyan number (e.g., 0712345678)")
            etPhone.requestFocus()
            return
        }

        // Must have at least one contact method
        if (email.isBlank() && phone.isBlank()) {
            toast("Please provide at least one contact method")
            return
        }

        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                repository.updateMyProfile(
                    name = name,
                    email = email,
                    phone = phone,
                    token = token
                )
                toast("Profile updated successfully")
                finish()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to update profile")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSave.isEnabled = !loading
        btnSave.text = if (loading) "Saving..." else "Save Changes"
    }

    private fun isValidKenyanPhone(input: String): Boolean {
        val cleaned = input.trim()
        val patterns = listOf(
            Regex("^0[17]\\d{8}$"),
            Regex("^\\+254[17]\\d{8}$"),
            Regex("^254[17]\\d{8}$")
        )
        return patterns.any { it.matches(cleaned) }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}