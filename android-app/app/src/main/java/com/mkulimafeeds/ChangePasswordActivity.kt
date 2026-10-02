package com.mkulimafeeds

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.remote.dto.auth.ChangePasswordRequestDto
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {


    private lateinit var tokenManager: TokenManager

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnChangePassword: MaterialButton
    private lateinit var progressChangePassword: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_change_password
        )

        tokenManager =
            TokenManager(this)

        setupViews()
        setupToolbar()
        setupButton()
    }

    private fun setupViews() {

        etCurrentPassword =
            findViewById(
                R.id.etCurrentPassword
            )

        etNewPassword =
            findViewById(
                R.id.etNewPassword
            )

        etConfirmPassword =
            findViewById(
                R.id.etConfirmPassword
            )

        btnChangePassword =
            findViewById(
                R.id.btnChangePassword
            )

        progressChangePassword =
            findViewById(
                R.id.progressChangePassword
            )
    }

    private fun setupToolbar() {

        findViewById<MaterialToolbar>(
            R.id.toolbarChangePassword
        ).setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButton() {

        btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {

        val currentPassword =
            etCurrentPassword.text
                ?.toString()
                ?.trim()
                ?: ""

        val newPassword =
            etNewPassword.text
                ?.toString()
                ?: ""

        val confirmPassword =
            etConfirmPassword.text
                ?.toString()
                ?: ""

        clearErrors()

        if (currentPassword.isBlank()) {

            etCurrentPassword.error =
                "Current password is required"

            etCurrentPassword.requestFocus()

            return
        }

        if (newPassword.isBlank()) {

            etNewPassword.error =
                "New password is required"

            etNewPassword.requestFocus()

            return
        }

        if (newPassword.length < 8) {

            etNewPassword.error =
                "Password must be at least 8 characters"

            etNewPassword.requestFocus()

            return
        }

        if (confirmPassword.isBlank()) {

            etConfirmPassword.error =
                "Please confirm your new password"

            etConfirmPassword.requestFocus()

            return
        }

        if (newPassword != confirmPassword) {

            etConfirmPassword.error =
                "Passwords do not match"

            etConfirmPassword.requestFocus()

            return
        }

        if (currentPassword == newPassword) {

            etNewPassword.error =
                "New password must be different"

            etNewPassword.requestFocus()

            return
        }

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        setLoading(true)

        lifecycleScope.launch {

            try {

                NetworkModule.apiService
                    .changePassword(
                        request =
                            ChangePasswordRequestDto(
                                currentPassword =
                                    currentPassword,
                                newPassword =
                                    newPassword
                            ),
                        token = token
                    )

                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Password changed successfully",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } catch (e: Exception) {

                val message =
                    extractErrorMessage(e)

                Toast.makeText(
                    this@ChangePasswordActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                setLoading(false)
            }
        }
    }

    private fun clearErrors() {

        etCurrentPassword.error = null
        etNewPassword.error = null
        etConfirmPassword.error = null
    }

    private fun setLoading(
        loading: Boolean
    ) {

        progressChangePassword.visibility =
            if (loading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

        btnChangePassword.isEnabled =
            !loading

        btnChangePassword.text =
            if (loading) {
                "Changing Password..."
            } else {
                "Change Password"
            }
    }

    private fun extractErrorMessage(
        exception: Exception
    ): String {

        val message =
            exception.message
                ?.trim()

        return when {

            !message.isNullOrBlank() ->
                message

            else ->
                "Unable to change password. Please try again."
        }
    }


}
