package com.mkulimafeeds

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
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
import com.mkulimafeeds.data.repository.AdminUserRepository
import com.mkulimafeeds.data.repository.RoleRepository
import kotlinx.coroutines.launch

class UserRoleManagementActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_USER_ID = "EXTRA_USER_ID"
        private const val EXTRA_USER_NAME = "EXTRA_USER_NAME"
        private const val EXTRA_USER_EMAIL = "EXTRA_USER_EMAIL"
        private const val EXTRA_USER_ROLE = "EXTRA_USER_ROLE"
        private const val EXTRA_USER_STATUS = "EXTRA_USER_STATUS"

        fun newIntent(
            context: Context,
            userId: Long,
            userName: String,
            userEmail: String,
            userRole: String,
            userStatus: String
        ): Intent {
            return Intent(context, UserRoleManagementActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_USER_EMAIL, userEmail)
                putExtra(EXTRA_USER_ROLE, userRole)
                putExtra(EXTRA_USER_STATUS, userStatus)
            }
        }
    }

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { RoleRepository() }
    private val adminUserRepository by lazy {
        AdminUserRepository(NetworkModule.apiService)
    }

    private var userId: Long = -1L
    private var userRole: String = "CUSTOMER"
    private var userStatus: String = "ACTIVE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_role_management)

        userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
        userRole = intent.getStringExtra(EXTRA_USER_ROLE).orEmpty()
        userStatus = intent.getStringExtra(EXTRA_USER_STATUS).orEmpty()

        if (userId <= 0) {
            toast("Invalid user")
            finish()
            return
        }

        val rootView = findViewById<View>(R.id.roleMgmtRoot)
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

        findViewById<TextView>(R.id.tvUserName).text =
            intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        findViewById<TextView>(R.id.tvUserEmail).text =
            intent.getStringExtra(EXTRA_USER_EMAIL).orEmpty()
        findViewById<TextView>(R.id.tvCurrentRole).text = userRole.uppercase()
        findViewById<TextView>(R.id.tvCurrentStatus).text = userStatus.uppercase()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        configureButtons()

        findViewById<MaterialButton>(R.id.btnPromote).setOnClickListener {
            confirmPromote()
        }

        findViewById<MaterialButton>(R.id.btnDemote).setOnClickListener {
            confirmDemote()
        }

        findViewById<MaterialButton>(R.id.btnSuspend).setOnClickListener {
            confirmSuspend()
        }

        findViewById<MaterialButton>(R.id.btnReactivate).setOnClickListener {
            confirmReactivate()
        }

        // Refresh from backend — overrides possibly-stale Intent extras
        loadUserFromBackend()
    }

    // =========================================================
    // FRESH DATA LOAD
    // =========================================================

    private fun loadUserFromBackend() {

        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            // No session — keep whatever the Intent extras provided.
            return
        }

        lifecycleScope.launch {

            try {

                val users = adminUserRepository.getAdminUsers(token)

                val fresh = users.firstOrNull { it.id == userId }

                if (fresh != null) {

                    userRole = fresh.role
                    userStatus = fresh.status

                    findViewById<TextView>(R.id.tvCurrentRole).text =
                        fresh.role.uppercase()

                    findViewById<TextView>(R.id.tvCurrentStatus).text =
                        fresh.status.uppercase()

                    configureButtons()
                }

            } catch (e: Exception) {

                android.util.Log.w(
                    "UserRoleManagement",
                    "Failed to refresh role/status for user $userId from backend",
                    e
                )

                // Fall back to the Intent extras already bound above.
            }
        }
    }

    private fun configureButtons() {
        val isDealer = userRole.equals("DEALER", ignoreCase = true)
        val isCustomer = userRole.equals("CUSTOMER", ignoreCase = true)
        val isSuspended = userStatus.equals("SUSPENDED", ignoreCase = true)
        val isActive = userStatus.equals("ACTIVE", ignoreCase = true)

        findViewById<MaterialButton>(R.id.btnPromote).visibility =
            if (isCustomer) View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnDemote).visibility =
            if (isDealer) View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnSuspend).visibility =
            if (isActive && !isDealer && !userRole.equals("ADMIN", ignoreCase = true))
                View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnReactivate).visibility =
            if (isSuspended) View.VISIBLE else View.GONE
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    private fun confirmPromote() {
        val input = EditText(this).apply {
            hint = "Optional reason"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 8)
        }

        AlertDialog.Builder(this)
            .setTitle("Promote to Dealer?")
            .setMessage("This grants dealer access: bulk ordering and dealer features.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Promote") { _, _ ->
                val reason = input.text.toString().trim().ifBlank { null }
                executeAction {
                    repository.promoteUser(userId, reason, getTokenOrFail() ?: return@executeAction)
                    toast("User promoted to dealer")
                }
            }
            .show()
    }

    private fun confirmDemote() {
        val input = EditText(this).apply {
            hint = "Reason (recommended)"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 8)
        }

        AlertDialog.Builder(this)
            .setTitle("Demote to Customer?")
            .setMessage("The user will lose dealer access and features.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Demote") { _, _ ->
                val reason = input.text.toString().trim().ifBlank { null }
                executeAction {
                    repository.demoteUser(userId, reason, getTokenOrFail() ?: return@executeAction)
                    toast("User demoted to customer")
                }
            }
            .show()
    }

    private fun confirmSuspend() {
        val input = EditText(this).apply {
            hint = "Reason (required)"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 8)
        }

        AlertDialog.Builder(this)
            .setTitle("Suspend User?")
            .setMessage("The user will not be able to log in until reactivated.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Suspend") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isBlank()) {
                    toast("Reason is required")
                    return@setPositiveButton
                }
                executeAction {
                    repository.suspendUser(userId, reason, getTokenOrFail() ?: return@executeAction)
                    toast("User suspended")
                }
            }
            .show()
    }

    private fun confirmReactivate() {
        executeAction {
            repository.reactivateUser(userId, "Reactivated by admin", getTokenOrFail() ?: return@executeAction)
            toast("User reactivated")
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun executeAction(block: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                block()
                finish()
            } catch (e: Exception) {
                toast(e.message ?: "Action failed")
            }
        }
    }

    private fun getTokenOrFail(): String? {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired")
            finish()
            return null
        }
        return token
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}