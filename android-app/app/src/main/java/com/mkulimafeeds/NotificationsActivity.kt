package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.account.DealerAccountActivity
import com.mkulimafeeds.account.StaffAccountActivity
import com.mkulimafeeds.account.AdminAccountActivity
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.notification.NotificationRepository
import com.mkulimafeeds.domain.model.notification.RemoteNotification
import com.mkulimafeeds.presentation.notifications.NotificationViewModel
import com.mkulimafeeds.presentation.notifications.NotificationViewModelFactory
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var tokenManager: TokenManager
    private lateinit var navContainer: FrameLayout

    private var currentRole: String = ""

    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(
            NotificationRepository(NetworkModule.apiService)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        tokenManager = TokenManager(this)

        applyWindowInsets()
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupNotificationObserver()

        // Role must be resolved before the nav is inflated.
        loadUserRoleThenSetupNav()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    // =========================================================
    // WINDOW INSETS — root takes top/sides, nav consumes bottom
    // =========================================================

    private fun applyWindowInsets() {
        val rootView = findViewById<View>(R.id.notif_root)

        // Root: status bar + cutouts. NO bottom — nav will consume it.
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,     // ← status bar fix
                systemBars.right,
                0                   // ← bottom left to the nav
            )
            insets
        }
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private fun initializeViews() {
        rvNotifications = findViewById(R.id.rvNotifications)
        tvEmpty = findViewById(R.id.tvEmptyNotif)
        navContainer = findViewById(R.id.notificationsNavContainer)
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(emptyList()) { notification ->
            handleNotificationClick(notification)
        }
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    // =========================================================
    // ROLE-AWARE NAV
    // =========================================================

    private fun loadUserRoleThenSetupNav() {
        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Authentication token not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch {
            currentRole = try {
                NetworkModule.apiService.getUserProfile(token)
                    .user.role.trim().uppercase()
            } catch (e: Exception) {
                e.printStackTrace()
                "" // unknown — will fall back to customer nav, but dispatcher is now safe
            }

            inflateRoleNav()
            notificationViewModel.loadNotifications(token)
        }
    }

    private fun inflateRoleNav() {
        val navLayoutId = when (currentRole) {
            "ADMIN", "STAFF" -> R.layout.bottom_nav_admin
            "DEALER"         -> R.layout.bottom_nav_dealer
            else             -> R.layout.bottom_nav_customer
        }

        navContainer.removeAllViews()
        LayoutInflater.from(this).inflate(navLayoutId, navContainer, true)

        // Nav consumes the bottom system-bar inset.
        ViewCompat.setOnApplyWindowInsetsListener(navContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        wireNavListeners()
    }

    private fun wireNavListeners() {
        findViewById<View?>(R.id.navHome)?.setOnClickListener {
            goTo(roleHome())
        }
        findViewById<View?>(R.id.navDashboard)?.setOnClickListener {
            goTo(roleDashboard())
        }
        findViewById<View?>(R.id.navCatalog)?.setOnClickListener {
            goTo(roleCatalog())
        }
        findViewById<View?>(R.id.navProducts)?.setOnClickListener {
            goTo(AdminProductActivity::class.java)
        }
        findViewById<View?>(R.id.navOrders)?.setOnClickListener {
            goTo(roleOrders())
        }
        findViewById<View?>(R.id.navInsights)?.setOnClickListener {
            goTo(roleInsights())
        }
        findViewById<View?>(R.id.navCart)?.setOnClickListener {
            goTo(CartActivity::class.java)
        }
        findViewById<View?>(R.id.navProfile)?.setOnClickListener {
            goTo(AccountActivity::class.java)   // dispatcher (now safe)
        }
        findViewById<View?>(R.id.navAccount)?.setOnClickListener {
            goTo(AccountActivity::class.java)
        }
        // Inbox is current — no-op
        findViewById<View?>(R.id.navInbox)?.setOnClickListener { /* no-op */ }
    }

    private fun goTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        startActivity(intent)
        finish()
    }

    private fun roleHome(): Class<*> = when (currentRole) {
        "ADMIN", "STAFF" -> AdminDashboardActivity::class.java
        "DEALER"         -> DealerDashboardActivity::class.java
        else             -> HomeActivity::class.java
    }

    private fun roleDashboard(): Class<*> = when (currentRole) {
        "ADMIN", "STAFF" -> AdminDashboardActivity::class.java
        "DEALER"         -> DealerDashboardActivity::class.java
        else             -> HomeActivity::class.java
    }

    private fun roleCatalog(): Class<*> = when (currentRole) {
        "DEALER" -> DealerCatalogActivity::class.java
        else     -> CatalogActivity::class.java
    }

    private fun roleOrders(): Class<*> = when (currentRole) {
        "ADMIN", "STAFF" -> AdminOrderActivity::class.java
        "DEALER"         -> DealerOrdersActivity::class.java
        else             -> MyOrdersActivity::class.java
    }

    private fun roleInsights(): Class<*> = when (currentRole) {
        "DEALER" -> DealerInsightsActivity::class.java
        else     -> AdminInsightsActivity::class.java
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private fun setupNotificationObserver() {
        lifecycleScope.launch {
            notificationViewModel.uiState.collect { state ->
                if (state.isLoading) return@collect

                state.error?.let { error ->
                    Toast.makeText(this@NotificationsActivity, error, Toast.LENGTH_LONG).show()
                    notificationViewModel.clearError()
                    return@collect
                }

                val notifications = state.notifications
                if (notifications.isEmpty()) {
                    rvNotifications.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = if (currentRole == "DEALER") {
                        "No new orders assigned yet."
                    } else {
                        "Your inbox is empty"
                    }
                } else {
                    rvNotifications.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.updateList(notifications)
                }
            }
        }
    }

    private fun loadNotifications() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) return
        notificationViewModel.loadNotifications(token)
    }

    private fun handleNotificationClick(notification: RemoteNotification) {
        val token = tokenManager.getToken()
        if (!token.isNullOrBlank()) {
            notificationViewModel.markAsRead(id = notification.id, token = token)
        }

        when (notification.type.trim().uppercase()) {
            "ORDER_ASSIGNED" -> goTo(roleOrders())
            "ORDER_STATUS_UPDATE" -> goTo(roleOrders())
            else -> Toast.makeText(this, notification.title, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBackNotif).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnMarkRead).setOnClickListener {
            markAllNotificationsAsRead()
        }
    }

    private fun markAllNotificationsAsRead() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Authentication token not found", Toast.LENGTH_LONG).show()
            return
        }
        notificationViewModel.markAllAsRead(token)
        Toast.makeText(this, "Inbox marked as read", Toast.LENGTH_SHORT).show()
    }
}