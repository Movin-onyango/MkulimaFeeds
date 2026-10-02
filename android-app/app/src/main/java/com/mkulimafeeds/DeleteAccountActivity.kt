package com.mkulimafeeds

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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

class DeleteAccountActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { UserProfileRepository() }

    private lateinit var etPassword: EditText
    private lateinit var etConfirmation: EditText
    private lateinit var btnDelete: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_account)

        val rootView = findViewById<View>(R.id.deleteAccountRoot)
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
        setupToolbar()
        setupDeleteButton()
        setupCancelButton()
    }

    private fun initializeViews() {
        etPassword = findViewById(R.id.etPassword)
        etConfirmation = findViewById(R.id.etConfirmation)
        btnDelete = findViewById(R.id.btnDeleteAccount)
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupCancelButton() {
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }

    private fun setupDeleteButton() {
        btnDelete.setOnClickListener {
            attemptDelete()
        }
    }

    private fun attemptDelete() {

        val password = etPassword.text?.toString().orEmpty()
        val confirmation = etConfirmation.text?.toString()?.trim().orEmpty()

        // -----------------------------------------------------
        // Client-side validation
        // -----------------------------------------------------
        if (password.isBlank()) {
            toast("Please enter your password")
            etPassword.requestFocus()
            return
        }

        if (confirmation.uppercase() != "DELETE") {
            toast("Please type DELETE to confirm")
            etConfirmation.requestFocus()
            return
        }

        // -----------------------------------------------------
        // Final confirmation dialog
        // -----------------------------------------------------
        AlertDialog.Builder(this)
            .setTitle("Delete account?")
            .setMessage(
                "This is your last chance to cancel. " +
                        "Once deleted, you will be signed out immediately and cannot recover your account."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes, delete") { _, _ ->
                performDelete(password, confirmation)
            }
            .show()
    }

    private fun performDelete(
        password: String,
        confirmation: String
    ) {

        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        btnDelete.isEnabled = false
        btnDelete.text = "Deleting..."

        lifecycleScope.launch {
            try {
                repository.deleteMyAccount(
                    password = password,
                    confirmation = confirmation,
                    token = token
                )

                // ---------------------------------------------
                // Clear local session and return to login
                // ---------------------------------------------
                tokenManager.clearToken()

                toast("Your account has been deleted.")

                val intent = Intent(this@DeleteAccountActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                toast(e.message ?: "Failed to delete account")
                btnDelete.isEnabled = true
                btnDelete.text = "Delete My Account"
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}