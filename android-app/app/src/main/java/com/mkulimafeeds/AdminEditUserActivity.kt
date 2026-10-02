package com.mkulimafeeds

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.remote.dto.CreateAdminUserRequestDto
import com.mkulimafeeds.data.remote.dto.UpdateAdminUserRequestDto
import com.mkulimafeeds.data.repository.AdminUserRepository
import kotlinx.coroutines.launch

class AdminEditUserActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var tilUserPassword: TextInputLayout
    private lateinit var spinnerTier: AutoCompleteTextView

    private lateinit var tvTitle: TextView
    private lateinit var btnSave: MaterialButton

    private val tokenManager by lazy {
        TokenManager(applicationContext)
    }

    private val adminUserRepository by lazy {
        AdminUserRepository(
            NetworkModule.apiService
        )
    }

    private var isEditMode = false
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_admin_edit_user
        )

        val rootView =
            findViewById<View>(
                R.id.edit_user_root
            )

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )

            insets
        }

        initializeViews()
        setupTierSpinner()

        isEditMode =
            intent.getBooleanExtra(
                "IS_EDIT",
                false
            )

        userId =
            intent.getStringExtra(
                "USER_ID"
            )

        if (isEditMode && userId != null) {

            tvTitle.text =
                getString(
                    R.string.admin_edit_user_title
                )

            tilUserPassword.visibility =
                View.GONE

            loadUser(
                userId!!
            )

        } else {

            tvTitle.text =
                getString(
                    R.string.admin_add_user_title
                )

            tilUserPassword.visibility =
                View.VISIBLE
        }

        findViewById<ImageView>(
            R.id.btnBackEditUser
        ).setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveUser()
        }
    }

    private fun initializeViews() {

        etName =
            findViewById(
                R.id.etUserName
            )

        etEmail =
            findViewById(
                R.id.etUserEmail
            )

        etPhone =
            findViewById(
                R.id.etUserPhone
            )

        etPassword =
            findViewById(
                R.id.etUserPassword
            )

        tilUserPassword =
            findViewById(
                R.id.tilUserPassword
            )

        spinnerTier =
            findViewById(
                R.id.spinnerUserTier
            )

        tvTitle =
            findViewById(
                R.id.tvEditUserTitle
            )

        btnSave =
            findViewById(
                R.id.btnSaveUser
            )
    }

    private fun setupTierSpinner() {

        val tiers =
            arrayOf(
                "VIP",
                "REGULAR",
                "NEW",
                "INACTIVE"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                tiers
            )

        spinnerTier.setAdapter(
            adapter
        )
    }

    private fun loadUser(id: String) {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication required",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        lifecycleScope.launch {

            try {

                val users =
                    adminUserRepository.getAdminUsers(
                        token
                    )

                val user =
                    users.firstOrNull {
                        it.id.toString() == id
                    }

                if (user == null) {

                    Toast.makeText(
                        this@AdminEditUserActivity,
                        "User not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                    return@launch
                }

                etName.setText(
                    user.name
                )

                etEmail.setText(
                    user.email
                )

                etPhone.setText(
                    user.phone
                )

                spinnerTier.setText(
                    if (user.isActive) {
                        "REGULAR"
                    } else {
                        "INACTIVE"
                    },
                    false
                )

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@AdminEditUserActivity,
                    "Failed to load user",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveUser() {

        val name =
            etName.text
                .toString()
                .trim()

        val email =
            etEmail.text
                .toString()
                .trim()

        val phone =
            etPhone.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()

        val tier =
            spinnerTier.text
                .toString()
                .trim()

        if (
            name.isEmpty() ||
            email.isEmpty() ||
            phone.isEmpty() ||
            tier.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please fill in all fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication required",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (isEditMode) {

            updateExistingUser(
                name = name,
                email = email,
                phone = phone,
                token = token
            )

        } else {

            if (password.isBlank()) {

                Toast.makeText(
                    this,
                    "Please enter a password",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            if (password.length < 6) {

                Toast.makeText(
                    this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            createNewUser(
                name = name,
                email = email,
                phone = phone,
                password = password,
                token = token
            )
        }
    }

    private fun createNewUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        token: String
    ) {

        lifecycleScope.launch {

            try {

                btnSave.isEnabled = false

                adminUserRepository.createAdminUser(

                    request =
                        CreateAdminUserRequestDto(
                            name = name,
                            phone = phone,
                            email = email,
                            password = password,
                            role = "CUSTOMER"
                        ),

                    token = token
                )

                Toast.makeText(
                    this@AdminEditUserActivity,
                    "User created successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@AdminEditUserActivity,
                    "Failed to create user",
                    Toast.LENGTH_SHORT
                ).show()

                btnSave.isEnabled = true
            }
        }
    }

    private fun updateExistingUser(
        name: String,
        email: String,
        phone: String,
        token: String
    ) {

        val id =
            userId
                ?.toLongOrNull()

        if (id == null) {

            Toast.makeText(
                this,
                "Invalid user ID",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                btnSave.isEnabled = false

                adminUserRepository.updateAdminUser(

                    id = id,

                    request =
                        UpdateAdminUserRequestDto(
                            name = name,
                            phone = phone,
                            email = email
                        ),

                    token = token
                )

                Toast.makeText(
                    this@AdminEditUserActivity,
                    "User updated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@AdminEditUserActivity,
                    "Failed to update user",
                    Toast.LENGTH_SHORT
                ).show()

                btnSave.isEnabled = true
            }
        }
    }
}