package com.mkulimafeeds
import  com.mkulimafeeds.account.DealerAccountActivity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.data.repository.notification.NotificationRepository
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderStatus
import com.mkulimafeeds.presentation.dealer.notifications.DealerNotificationHelper
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModel
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModelFactory
import com.mkulimafeeds.presentation.notifications.NotificationViewModel
import com.mkulimafeeds.presentation.notifications.NotificationViewModelFactory
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.mkulimafeeds.util.applyBottomNavInsets


class DealerDashboardActivity :
    AppCompatActivity() {

    private lateinit var tvDeliveriesCount: TextView
    private lateinit var tvPendingOrdersCount: TextView
    private lateinit var tvTotalEarnings: TextView

    private lateinit var tvUpcomingOrderId: TextView
    private lateinit var tvUpcomingCustomerName: TextView
    private lateinit var tvUpcomingNeededDate: TextView
    private lateinit var tvUpcomingLocation: TextView
    private lateinit var tvUpcomingStatus: TextView
    private lateinit var tvNoUpcomingDelivery: TextView

    private lateinit var tvPerformanceTitle: TextView
    private lateinit var tvCompletionRate: TextView

    private lateinit var performanceBar1: View
    private lateinit var performanceBar2: View
    private lateinit var performanceBar3: View
    private lateinit var performanceBar4: View
    private lateinit var performanceBar5: View

    private lateinit var upcomingDeliveryProgress: ProgressBar
    private lateinit var btnUpcomingDelivery: MaterialButton

    private lateinit var tokenManager: TokenManager

    private var upcomingOrder: DealerOrder? = null

    private val dealerOrderViewModel:
            DealerOrderViewModel by viewModels {

        DealerOrderViewModelFactory(
            DealerOrderRepository(
                NetworkModule.apiService
            )
        )
    }

    private val notificationViewModel:
            NotificationViewModel by viewModels {

        NotificationViewModelFactory(
            NotificationRepository(
                NetworkModule.apiService
            )
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_dealer_dashboard
        )

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ),
                1001
            )
        }

        tokenManager =
            TokenManager(this)

        setupWindowInsets()
        setupViews()
        setupNavigation()
        observeDealerOrders()
        observeNotifications()

        loadDealerProfile()
    }

    override fun onResume() {
        super.onResume()

        highlightDashboardTab()

        loadDealerOrders()
        loadNotifications()
    }

    private fun setupWindowInsets() {
            applyBottomNavInsets(
                root = findViewById(R.id.dealer_root),
                bottomNav = findViewById(R.id.bottomNavDealer)
            )
    }

    private fun setupViews() {

        tvDeliveriesCount =
            findViewById(
                R.id.tvDeliveriesCount
            )

        tvPendingOrdersCount =
            findViewById(
                R.id.tvPendingOrdersCount
            )

        tvTotalEarnings =
            findViewById(
                R.id.tvTotalEarnings
            )

        tvUpcomingOrderId =
            findViewById(
                R.id.tvUpcomingOrderId
            )

        tvUpcomingCustomerName =
            findViewById(
                R.id.tvUpcomingCustomerName
            )

        tvUpcomingNeededDate =
            findViewById(
                R.id.tvUpcomingNeededDate
            )

        tvUpcomingLocation =
            findViewById(
                R.id.tvUpcomingLocation
            )

        tvUpcomingStatus =
            findViewById(
                R.id.tvUpcomingStatus
            )

        tvNoUpcomingDelivery =
            findViewById(
                R.id.tvNoUpcomingDelivery
            )

        upcomingDeliveryProgress =
            findViewById(
                R.id.upcomingDeliveryProgress
            )

        btnUpcomingDelivery =
            findViewById(
                R.id.btnUpcomingDelivery
            )

        tvPerformanceTitle =
            findViewById(
                R.id.perfTitle
            )

        tvCompletionRate =
            findViewById(
                R.id.tvCompletionRate
            )

        performanceBar1 =
            findViewById(
                R.id.performanceBar1
            )

        performanceBar2 =
            findViewById(
                R.id.performanceBar2
            )

        performanceBar3 =
            findViewById(
                R.id.performanceBar3
            )

        performanceBar4 =
            findViewById(
                R.id.performanceBar4
            )

        performanceBar5 =
            findViewById(
                R.id.performanceBar5
            )
    }

    private fun setupNavigation() {

        findViewById<View>(
            R.id.navInbox
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    NotificationsActivity::class.java
                )
            )
        }

        findViewById<View>(
            R.id.tvViewAllAssignedOrders
        ).setOnClickListener {

            openAssignedOrders()
        }

        btnUpcomingDelivery.setOnClickListener {

            handleUpcomingDeliveryAction()
        }

        findViewById<View>(
            R.id.perfTitle
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    DealerReportsActivity::class.java
                )
            )
        }

        setupDealerNavigation()
        highlightDashboardTab()
    }

    // =========================================================
    // DEALER BOTTOM NAVIGATION
    // =========================================================

    private fun setupDealerNavigation() {

        findViewById<View>(R.id.navCatalog)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        DealerCatalogActivity::class.java
                    )
                )
                finish()
            }

        findViewById<View>(R.id.navOrders)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        DealerOrdersActivity::class.java
                    )
                )
                finish()
            }

        findViewById<View>(R.id.navInsights)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        DealerInsightsActivity::class.java
                    )
                )
                finish()
            }

        findViewById<View>(R.id.navAccount)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        DealerAccountActivity::class.java
                    )
                )
                finish()
            }

        // navDashboard = current, no-op
    }

    private fun highlightDashboardTab() {

        setActiveTab(
            R.id.icDashboard,
            R.id.tvDashboard
        )

        setInactiveTab(
            R.id.icCatalog,
            R.id.tvCatalog
        )

        setInactiveTab(
            R.id.icOrders,
            R.id.tvOrders
        )

        setInactiveTab(
            R.id.icInsights,
            R.id.tvInsights
        )

        setInactiveTab(
            R.id.icAccount,
            R.id.tvAccount
        )
    }

    private fun setActiveTab(
        ic: Int,
        tv: Int
    ) {

        findViewById<ImageView>(ic)
            .setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.brand_dark_green
                )
            )

        findViewById<TextView>(tv).apply {

            setTextColor(
                ContextCompat.getColor(
                    this@DealerDashboardActivity,
                    R.color.brand_dark_green
                )
            )

            setTypeface(
                null,
                Typeface.BOLD
            )
        }
    }

    private fun setInactiveTab(
        ic: Int,
        tv: Int
    ) {

        findViewById<ImageView>(ic)
            .setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.text_grey
                )
            )

        findViewById<TextView>(tv).apply {

            setTextColor(
                ContextCompat.getColor(
                    this@DealerDashboardActivity,
                    R.color.text_grey
                )
            )

            setTypeface(
                null,
                Typeface.NORMAL
            )
        }
    }

    private fun observeDealerOrders() {

        lifecycleScope.launch {

            dealerOrderViewModel.statusUpdateSuccess
                .collect { order ->

                    val preferences =
                        getSharedPreferences(
                            "dealer_settings",
                            MODE_PRIVATE
                        )

                    val notificationsEnabled =
                        preferences.getBoolean(
                            "status_notifications",
                            true
                        )

                    if (notificationsEnabled) {

                        DealerNotificationHelper
                            .showOrderStatusUpdated(
                                context = this@DealerDashboardActivity,
                                order = order
                            )
                    }
                }
        }

        lifecycleScope.launch {

            dealerOrderViewModel.uiState.collect { state ->

                upcomingDeliveryProgress.visibility =
                    if (state.isLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                if (state.isLoading) {

                    btnUpcomingDelivery.isEnabled =
                        false

                    tvNoUpcomingDelivery.visibility =
                        View.GONE
                }

                if (!state.isLoading) {

                    updateStatistics(
                        state.orders
                    )

                    updatePerformance(
                        state.orders
                    )

                    val nextOrder =
                        findUpcomingOrder(
                            state.orders
                        )

                    upcomingOrder =
                        nextOrder

                    if (nextOrder == null) {

                        showEmptyUpcomingDelivery()

                    } else {

                        showUpcomingDelivery(
                            nextOrder
                        )
                    }
                }

                if (state.statusUpdateLoading) {

                    btnUpcomingDelivery.isEnabled =
                        false

                    btnUpcomingDelivery.text =
                        "Updating..."

                } else if (!state.isLoading) {

                    upcomingOrder?.let { order ->

                        btnUpcomingDelivery.isEnabled =
                            getNextStatus(
                                order.status
                            ) != null

                        updateDeliveryButtonText(
                            order
                        )
                    }
                }

                state.error?.let { error ->

                    upcomingOrder = null

                    tvNoUpcomingDelivery.text =
                        error

                    tvNoUpcomingDelivery.visibility =
                        View.VISIBLE

                    btnUpcomingDelivery.visibility =
                        View.GONE

                    dealerOrderViewModel
                        .clearError()

                    Toast.makeText(
                        this@DealerDashboardActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun observeNotifications() {

        lifecycleScope.launch {

            notificationViewModel.uiState.collect { state ->

                updateNotificationBadge(
                    state.unreadCount
                )

                state.error?.let {

                    notificationViewModel
                        .clearError()
                }
            }
        }
    }

    private fun updateStatistics(
        orders: List<DealerOrder>
    ) {

        val completedOrders =
            orders.count { order ->

                order.status ==
                        DealerOrderStatus.COMPLETED
            }

        val pendingOrders =
            orders.count { order ->

                when (order.status) {

                    DealerOrderStatus.CONFIRMED,
                    DealerOrderStatus.PROCESSING,
                    DealerOrderStatus.READY -> true

                    DealerOrderStatus.PENDING,
                    DealerOrderStatus.COMPLETED,
                    DealerOrderStatus.CANCELLED -> false
                }
            }

        val totalEarnings =
            orders
                .filter { order ->

                    order.status ==
                            DealerOrderStatus.COMPLETED
                }
                .fold(
                    BigDecimal.ZERO
                ) { total, order ->

                    total.add(
                        order.totalAmount
                    )
                }

        tvDeliveriesCount.text =
            completedOrders.toString()

        tvPendingOrdersCount.text =
            pendingOrders.toString()

        tvTotalEarnings.text =
            "KES ${
                totalEarnings.setScale(
                    2,
                    java.math.RoundingMode.HALF_UP
                ).toPlainString()
            }"
    }

    private fun updatePerformance(
        orders: List<DealerOrder>
    ) {

        val completed =
            orders.count {

                it.status ==
                        DealerOrderStatus.COMPLETED
            }

        val cancelled =
            orders.count {

                it.status ==
                        DealerOrderStatus.CANCELLED
            }

        val finalized =
            completed + cancelled

        val completionRate =
            if (finalized == 0) {

                0.0

            } else {

                completed.toDouble() /
                        finalized.toDouble() *
                        100.0
            }

        tvPerformanceTitle.text =
            "● Completion Rate"

        tvCompletionRate.text =
            String.format(
                Locale.getDefault(),
                "%.1f%%",
                completionRate
            )

        val statusCounts =
            listOf(

                orders.count {
                    it.status ==
                            DealerOrderStatus.CONFIRMED
                },

                orders.count {
                    it.status ==
                            DealerOrderStatus.PROCESSING
                },

                orders.count {
                    it.status ==
                            DealerOrderStatus.READY
                },

                completed,

                cancelled
            )

        val bars =
            listOf(
                performanceBar1,
                performanceBar2,
                performanceBar3,
                performanceBar4,
                performanceBar5
            )

        updatePerformanceBars(
            bars,
            statusCounts
        )
    }

    private fun updatePerformanceBars(
        bars: List<View>,
        values: List<Int>
    ) {

        val maxValue =
            values.maxOrNull()
                ?: 0

        bars.forEachIndexed { index, bar ->

            val value =
                values.getOrElse(index) {
                    0
                }

            val maxHeightDp =
                60

            val minHeightDp =
                8

            val heightDp =
                if (maxValue == 0) {

                    minHeightDp

                } else {

                    maxOf(
                        minHeightDp,
                        (
                                value.toDouble() /
                                        maxValue.toDouble() *
                                        maxHeightDp
                                ).toInt()
                    )
                }

            val density =
                resources.displayMetrics.density

            val params =
                bar.layoutParams

            params.height =
                (
                        heightDp *
                                density
                        ).toInt()

            bar.layoutParams =
                params
        }
    }

    private fun loadDealerProfile() {

        val token =
            tokenManager.getToken()

        val greeting =
            findViewById<TextView>(
                R.id.tvGreeting
            )

        if (token.isNullOrBlank()) {

            greeting.text =
                "Hello, Dealer 👋"

            return
        }

        greeting.text =
            "Loading..."

        lifecycleScope.launch {

            try {

                val response =
                    NetworkModule.apiService
                        .getUserProfile(
                            token
                        )

                val dealerName =
                    response.user.name
                        .trim()
                        .ifBlank {
                            "Dealer"
                        }

                greeting.text =
                    "Hello, $dealerName 👋"

            } catch (e: Exception) {

                e.printStackTrace()

                greeting.text =
                    "Hello, Dealer 👋"
            }
        }
    }

    private fun loadDealerOrders() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            upcomingOrder = null

            tvDeliveriesCount.text =
                "0"

            tvPendingOrdersCount.text =
                "0"

            tvTotalEarnings.text =
                "KES 0.00"

            tvNoUpcomingDelivery.text =
                "Authentication token not found"

            tvNoUpcomingDelivery.visibility =
                View.VISIBLE

            btnUpcomingDelivery.visibility =
                View.GONE

            return
        }

        dealerOrderViewModel
            .loadAssignedOrders(
                token = token
            )
    }

    private fun loadNotifications() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            updateNotificationBadge(0)

            return
        }

        notificationViewModel
            .loadNotifications(
                token = token
            )
    }

    private fun updateNotificationBadge(
        unreadCount: Int
    ) {

        val badge =
            findViewById<View>(
                R.id.inboxBadge
            )

        badge.visibility =
            if (unreadCount > 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun findUpcomingOrder(
        orders: List<DealerOrder>
    ): DealerOrder? {

        return orders
            .filter { order ->

                when (order.status) {

                    DealerOrderStatus.PENDING,
                    DealerOrderStatus.CONFIRMED,
                    DealerOrderStatus.PROCESSING,
                    DealerOrderStatus.READY -> true

                    DealerOrderStatus.COMPLETED,
                    DealerOrderStatus.CANCELLED -> false
                }
            }
            .minByOrNull { order ->

                order.neededDate
            }
    }

    private fun showUpcomingDelivery(
        order: DealerOrder
    ) {

        tvNoUpcomingDelivery.visibility =
            View.GONE

        btnUpcomingDelivery.visibility =
            View.VISIBLE

        tvUpcomingOrderId.text =
            "#${order.id}"

        tvUpcomingCustomerName.text =
            order.customerName

        tvUpcomingNeededDate.text =
            formatNeededDate(
                order.neededDate
            )

        tvUpcomingLocation.text =
            "📍 ${order.location}"

        tvUpcomingStatus.text =
            formatStatus(
                order.status
            )

        updateDeliveryButtonText(
            order
        )
    }

    private fun showEmptyUpcomingDelivery() {

        tvUpcomingOrderId.text =
            "--"

        tvUpcomingCustomerName.text =
            "No assigned delivery"

        tvUpcomingNeededDate.text =
            "Needed\n--"

        tvUpcomingLocation.text =
            "📍 No delivery location"

        tvUpcomingStatus.text =
            ""

        tvNoUpcomingDelivery.text =
            "No upcoming deliveries assigned."

        tvNoUpcomingDelivery.visibility =
            View.VISIBLE

        btnUpcomingDelivery.visibility =
            View.GONE
    }

    private fun updateDeliveryButtonText(
        order: DealerOrder
    ) {

        val nextStatus =
            getNextStatus(
                order.status
            )

        btnUpcomingDelivery.text =
            when (nextStatus) {

                DealerOrderStatus.CONFIRMED ->
                    "Confirm Order"

                DealerOrderStatus.PROCESSING ->
                    "Start Processing"

                DealerOrderStatus.READY ->
                    "Mark Ready"

                DealerOrderStatus.COMPLETED ->
                    "Complete Delivery"

                null ->
                    "View Delivery"

                else ->
                    "View Delivery"
            }
    }

    private fun handleUpcomingDeliveryAction() {

        val order =
            upcomingOrder

        if (order == null) {

            Toast.makeText(
                this,
                "No upcoming delivery available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nextStatus =
            getNextStatus(
                order.status
            )

        if (nextStatus == null) {

            openOrderDetails(
                order.id
            )

            return
        }

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        btnUpcomingDelivery.isEnabled =
            false

        btnUpcomingDelivery.text =
            "Updating..."

        dealerOrderViewModel
            .updateOrderStatus(
                id = order.id,
                status = nextStatus.name,
                token = token
            )
    }

    private fun getNextStatus(
        status: DealerOrderStatus
    ): DealerOrderStatus? {

        return when (status) {

            DealerOrderStatus.PENDING ->
                DealerOrderStatus.CONFIRMED

            DealerOrderStatus.CONFIRMED ->
                DealerOrderStatus.PROCESSING

            DealerOrderStatus.PROCESSING ->
                DealerOrderStatus.READY

            DealerOrderStatus.READY ->
                DealerOrderStatus.COMPLETED

            DealerOrderStatus.COMPLETED ->
                null

            DealerOrderStatus.CANCELLED ->
                null
        }
    }

    private fun openAssignedOrders() {

        startActivity(
            Intent(
                this,
                DealerAssignedOrdersActivity::class.java
            )
        )
    }

    private fun openOrderDetails(
        orderId: Long
    ) {

        val intent =
            Intent(
                this,
                AssignedOrdersActivity::class.java
            ).apply {

                putExtra(
                    "ORDER_ID",
                    orderId
                )
            }

        startActivity(intent)
    }

    private fun formatStatus(
        status: DealerOrderStatus
    ): String {

        return status.name
            .lowercase(
                Locale.getDefault()
            )
            .replaceFirstChar {
                it.uppercase()
            }
    }

    private fun formatNeededDate(
        timestamp: Long
    ): String {

        return try {

            val formatter =
                SimpleDateFormat(
                    "dd MMM yyyy\nhh:mm a",
                    Locale.getDefault()
                )

            "Needed\n${formatter.format(
                Date(timestamp)
            )}"

        } catch (_: Exception) {

            "Needed\nDate unavailable"
        }
    }
}