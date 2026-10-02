package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AdminUserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserActivity : AppCompatActivity() {

    private lateinit var adapter: AdminUserAdapter
    private lateinit var rvUsers: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var userChipGroup: ChipGroup

    private val tokenManager by lazy {
        TokenManager(applicationContext)
    }

    private val adminUserRepository by lazy {
        AdminUserRepository(
            NetworkModule.apiService
        )
    }

    private var allUsers: List<User> = emptyList()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_admin_user
        )

        val rootView =
            findViewById<View>(
                R.id.adminUserRoot
            )

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.adminBottomNav
            )

        rvUsers =
            findViewById(
                R.id.rvAdminUsers
            )

        etSearch =
            findViewById(
                R.id.etSearchUsers
            )

        userChipGroup =
            findViewById(
                R.id.userChipGroup
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

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupNavigation(bottomNav)

        findViewById<View>(
            R.id.fabAddUser
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    AdminEditUserActivity::class.java
                )

            intent.putExtra(
                "IS_EDIT",
                false
            )

            startActivity(intent)
        }
    }

    // =========================================================
    // STEP 1: REFRESH ON RESUME
    // =========================================================
    //
    // onResume fires every time the screen comes back to the
    // foreground — including when we return from
    // UserRoleManagementActivity after promoting/demoting a
    // user. This is the mechanism that picks up role changes
    // made on other screens.
    // =========================================================

    override fun onResume() {
        super.onResume()

        refreshUsers()
    }

    private fun setupRecyclerView() {

        adapter = AdminUserAdapter(
            users = emptyList(),

            // Normal tap → open role management
            onUserClick = { user ->

                val numericId =
                    user.id.toLongOrNull()

                if (numericId == null) {

                    Toast.makeText(
                        this,
                        "Invalid user ID",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@AdminUserAdapter
                }

                startActivity(
                    UserRoleManagementActivity.newIntent(
                        context = this,
                        userId = numericId,
                        userName = user.name,
                        userEmail = user.email,
                        userRole = user.role,
                        userStatus = user.status
                    )
                )
            },

            // Long press → deactivate user (unchanged)
            onDeactivateUser = { user ->
                confirmDeactivateUser(user)
            }
        )

        rvUsers.layoutManager =
            LinearLayoutManager(this)

        rvUsers.adapter =
            adapter
    }

    private fun setupSearch() {

        etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterUsers()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupFilters() {

        userChipGroup
            .setOnCheckedStateChangeListener {
                    _, _ ->
                filterUsers()
            }
    }

    private fun filterUsers() {

        val query =
            etSearch.text
                .toString()
                .trim()

        val checkedChipId =
            userChipGroup.checkedChipId

        val tier =
            when (checkedChipId) {

                R.id.chipVIP ->
                    "VIP"

                R.id.chipNew ->
                    "NEW"

                R.id.chipInactive ->
                    "INACTIVE"

                else ->
                    "All Customers"
            }

        var filteredList =
            when (tier) {

                "All Customers" ->
                    allUsers

                else ->
                    allUsers.filter { user ->
                        user.membershipTier.equals(
                            tier,
                            ignoreCase = true
                        )
                    }
            }

        if (query.isNotEmpty()) {

            filteredList =
                filteredList.filter { user ->

                    user.name.contains(
                        query,
                        ignoreCase = true
                    ) ||

                            user.email.contains(
                                query,
                                ignoreCase = true
                            ) ||

                            user.phone.contains(
                                query,
                                ignoreCase = true
                            )
                }
        }

        adapter.updateList(
            filteredList
        )
    }

    // =========================================================
    // STEP 2 + STEP 3: REFRESH FROM BACKEND
    // =========================================================
    //
    // Always fetches a fresh list from the API — never reads
    // from the cached allUsers. The mapper now copies `role`
    // and `status` from the DTO so the adapter renders the
    // correct badges.
    // =========================================================

    private fun refreshUsers() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            allUsers =
                emptyList()

            adapter.updateList(
                emptyList()
            )

            return
        }

        lifecycleScope.launch {

            try {

                val dtoUsers =
                    adminUserRepository
                        .getAdminUsers(
                            token
                        )

                allUsers =
                    dtoUsers.map { dto ->

                        User(
                            id = dto.id.toString(),

                            name = dto.name,

                            email = dto.email,

                            phone = dto.phone,

                            // -------------------------------------
                            // STEP 3: copy role and status from DTO
                            // -------------------------------------
                            role = dto.role,

                            status = dto.status,

                            dealerStatus = dto.dealerStatus,

                            joinedDate = formatDate(dto.createdAt),

                            totalOrders = dto.totalOrders,

                            membershipTier =
                                if (dto.isActive) {
                                    "ACTIVE"
                                } else {
                                    "INACTIVE"
                                }
                        )
                    }

                filterUsers()

            } catch (e: Exception) {

                e.printStackTrace()

                allUsers =
                    emptyList()

                adapter.updateList(
                    emptyList()
                )

                Toast.makeText(
                    this@AdminUserActivity,
                    "Failed to load users",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeactivateUser(
        user: User
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Deactivate User"
            )
            .setMessage(
                "Are you sure you want to deactivate ${user.name}?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Deactivate"
            ) { _, _ ->

                deactivateUser(
                    user
                )
            }
            .show()
    }

    private fun deactivateUser(
        user: User
    ) {

        val id =
            user.id.toLongOrNull()

        if (id == null) {

            Toast.makeText(
                this,
                "Invalid user ID",
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

        lifecycleScope.launch {

            try {

                adminUserRepository
                    .deactivateAdminUser(
                        id = id,
                        token = token
                    )

                Toast.makeText(
                    this@AdminUserActivity,
                    "${user.name} deactivated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                refreshUsers()

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@AdminUserActivity,
                    "Failed to deactivate user",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            SimpleDateFormat(
                "MMM dd, yyyy",
                Locale.getDefault()
            ).format(Date(timestamp))
        } catch (_: Exception) {
            "—"
        }
    }

    private fun setupNavigation(
        bottomNav: BottomNavigationView
    ) {

        bottomNav.selectedItemId =
            R.id.admin_users

        bottomNav.setOnItemSelectedListener {
                item ->

            when (item.itemId) {

                R.id.admin_home -> {

                    startActivity(
                        Intent(
                            this,
                            AdminDashboardActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                R.id.admin_products -> {

                    startActivity(
                        Intent(
                            this,
                            AdminProductActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                R.id.admin_orders -> {

                    startActivity(
                        Intent(
                            this,
                            AdminOrderActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                R.id.admin_users -> {
                    true
                }

                else -> {
                    false
                }
            }
        }
    }
}