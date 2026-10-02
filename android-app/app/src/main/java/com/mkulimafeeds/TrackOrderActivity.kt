package com.mkulimafeeds

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.customer.CustomerOrderRepository
import com.mkulimafeeds.domain.model.CustomerOrder
import com.mkulimafeeds.domain.model.CustomerOrderStatus
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderViewModel
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackOrderActivity : AppCompatActivity() {

    private val orderViewModel: CustomerOrderViewModel by viewModels {
        CustomerOrderViewModelFactory(
            CustomerOrderRepository(
                NetworkModule.apiService
            )
        )
    }

    private lateinit var tokenManager: TokenManager

    private var orderId: Long = -1L

    private lateinit var tvTrackingOrderId: TextView
    private lateinit var tvTrackingStatus: TextView
    private lateinit var tvTrackingDate: TextView

    private lateinit var tvStep1Title: TextView
    private lateinit var tvStep1Time: TextView
    private lateinit var tvStep2Title: TextView
    private lateinit var tvStep2Time: TextView
    private lateinit var tvStep3Title: TextView
    private lateinit var tvStep3Time: TextView
    private lateinit var tvStep4Title: TextView
    private lateinit var tvStep4Time: TextView

    private lateinit var dot1: ImageView
    private lateinit var dot2: MaterialCardView
    private lateinit var dot3: ImageView
    private lateinit var dot4: ImageView

    private lateinit var mapStatusCard: MaterialCardView
    private lateinit var tvMapStatus: TextView

    private lateinit var progressLine1: View
    private lateinit var progressLine2: View
    private lateinit var progressLine3: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_track_order)

        initializeViews()
        setupWindowInsets()
        setupNavigation()

        tokenManager = TokenManager(this)

        orderId =
            intent.getStringExtra("ORDER_ID")
                ?.toLongOrNull()
                ?: intent.getLongExtra(
                    "ORDER_ID",
                    -1L
                )

        if (orderId <= 0L) {
            Toast.makeText(
                this,
                "Invalid order",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        observeOrderState()
        loadOrder()
    }

    private fun initializeViews() {

        tvTrackingOrderId =
            findViewById(R.id.tvTrackingOrderId)

        tvTrackingStatus =
            findViewById(R.id.tvTrackingStatus)

        tvTrackingDate =
            findViewById(R.id.tvTrackingDate)

        tvStep1Title =
            findViewById(R.id.tvStep1Title)

        tvStep1Time =
            findViewById(R.id.tvStep1Time)

        tvStep2Title =
            findViewById(R.id.tvStep2Title)

        tvStep2Time =
            findViewById(R.id.tvStep2Time)

        tvStep3Title =
            findViewById(R.id.tvStep3Title)

        tvStep3Time =
            findViewById(R.id.tvStep3Time)

        tvStep4Title =
            findViewById(R.id.tvStep4Title)

        tvStep4Time =
            findViewById(R.id.tvStep4Time)

        dot1 =
            findViewById(R.id.dot1)

        dot2 =
            findViewById(R.id.dot2)

        dot3 =
            findViewById(R.id.dot3)

        dot4 =
            findViewById(R.id.dot4)

        progressLine1 =
            findViewById(R.id.progressLine1)

        progressLine2 =
            findViewById(R.id.progressLine2)

        progressLine3 =
            findViewById(R.id.progressLine3)

        mapStatusCard =
            findViewById(R.id.mapStatusCard)

        tvMapStatus =
            findViewById(R.id.tvMapStatus)
    }

    private fun setupWindowInsets() {

        val rootView =
            findViewById<View>(R.id.track_root)

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
    }

    private fun setupNavigation() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }
    }

    private fun loadOrder() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        orderViewModel.loadOrder(
            id = orderId,
            token = token
        )
    }

    private fun observeOrderState() {

        lifecycleScope.launch {

            orderViewModel.uiState.collect { state ->

                state.error?.let { error ->

                    Toast.makeText(
                        this@TrackOrderActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()

                    orderViewModel.clearError()
                }

                state.selectedOrder?.let { order ->
                    displayOrder(order)
                }
            }
        }
    }

    private fun displayOrder(
        order: CustomerOrder
    ) {

        tvTrackingOrderId.text =
            "#MF-${order.id}"

        tvTrackingStatus.text =
            formatStatus(order.status)

        tvTrackingDate.text =
            "Needed ${formatDate(order.neededDate)}"

        updateTrackingTimeline(order)
    }

    private fun updateTrackingTimeline(
        order: CustomerOrder
    ) {

        val status =
            order.status

        val level =
            when (status) {

                CustomerOrderStatus.PENDING ->
                    1

                CustomerOrderStatus.CONFIRMED ->
                    2

                CustomerOrderStatus.PROCESSING ->
                    3

                CustomerOrderStatus.READY,
                CustomerOrderStatus.COMPLETED ->
                    4

                CustomerOrderStatus.CANCELLED,
                CustomerOrderStatus.UNKNOWN ->
                    0
            }

        if (status == CustomerOrderStatus.CANCELLED) {

            tvMapStatus.text =
                "Order cancelled"

            tvTrackingStatus.setTextColor(
                getColor(R.color.text_grey)
            )

        } else if (status == CustomerOrderStatus.COMPLETED) {

            tvMapStatus.text =
                "Order delivered"

            tvTrackingStatus.setTextColor(
                getColor(R.color.brand_dark_green)
            )

        } else {

            tvMapStatus.text =
                formatStatus(status)

            tvTrackingStatus.setTextColor(
                getColor(R.color.brand_blue)
            )
        }

        setupStep(
            titleView = tvStep1Title,
            timeView = tvStep1Time,
            title = "Order Placed",
            timestamp = order.createdAt,
            active = level >= 1
        )

        setupStep(
            titleView = tvStep2Title,
            timeView = tvStep2Time,
            title = "Order Confirmed",
            timestamp = if (level >= 2) {
                order.updatedAt
            } else {
                null
            },
            active = level >= 2
        )

        setupStep(
            titleView = tvStep3Title,
            timeView = tvStep3Time,
            title = "Processing",
            timestamp = if (level >= 3) {
                order.updatedAt
            } else {
                null
            },
            active = level >= 3
        )

        setupStep(
            titleView = tvStep4Title,
            timeView = tvStep4Time,
            title = when (status) {
                CustomerOrderStatus.READY ->
                    "Ready for Delivery"

                CustomerOrderStatus.COMPLETED ->
                    "Delivered"

                else ->
                    "Ready for Delivery"
            },
            timestamp = if (level >= 4) {
                order.updatedAt
            } else {
                null
            },
            active = level >= 4
        )

        applyDotStyle(dot1, level >= 1)
        applyDotStyle(dot2, level >= 2)
        applyDotStyle(dot3, level >= 3)
        applyDotStyle(dot4, level >= 4)

        applyLineStyle(
            progressLine1,
            level >= 2
        )

        applyLineStyle(
            progressLine2,
            level >= 3
        )

        applyLineStyle(
            progressLine3,
            level >= 4
        )

        if (status == CustomerOrderStatus.CANCELLED) {

            tvStep1Title.text =
                "Order Cancelled"

            tvStep1Time.text =
                formatDate(order.updatedAt)

            tvStep2Title.text =
                "Processing stopped"

            tvStep2Time.text =
                ""

            tvStep3Title.text =
                "Order not proceeding"

            tvStep3Time.text =
                ""

            tvStep4Title.text =
                "Cancelled"

            tvStep4Time.text =
                formatDate(order.updatedAt)
        }
    }

    private fun setupStep(
        titleView: TextView,
        timeView: TextView,
        title: String,
        timestamp: Long?,
        active: Boolean
    ) {

        titleView.text =
            title

        titleView.setTextColor(
            getColor(
                if (active) {
                    R.color.text_black
                } else {
                    R.color.text_grey_light
                }
            )
        )

        timeView.text =
            timestamp?.let {
                formatDateTime(it)
            }.orEmpty()

        timeView.setTextColor(
            getColor(
                if (active) {
                    R.color.text_grey
                } else {
                    R.color.text_grey_light
                }
            )
        )
    }

    private fun applyDotStyle(
        view: View,
        active: Boolean
    ) {

        view.alpha =
            if (active) {
                1f
            } else {
                0.35f
            }
    }

    private fun applyLineStyle(
        view: View,
        active: Boolean
    ) {

        view.setBackgroundColor(
            getColor(
                if (active) {
                    R.color.brand_dark_green
                } else {
                    R.color.light_grey
                }
            )
        )
    }

    private fun formatStatus(
        status: CustomerOrderStatus
    ): String {

        return when (status) {

            CustomerOrderStatus.PENDING ->
                "Pending"

            CustomerOrderStatus.CONFIRMED ->
                "Confirmed"

            CustomerOrderStatus.PROCESSING ->
                "Processing"

            CustomerOrderStatus.READY ->
                "Ready"

            CustomerOrderStatus.COMPLETED ->
                "Completed"

            CustomerOrderStatus.CANCELLED ->
                "Cancelled"

            CustomerOrderStatus.UNKNOWN ->
                "Status unavailable"
        }
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        return try {

            SimpleDateFormat(
                "MMM dd, yyyy",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (e: Exception) {

            "Date unavailable"
        }
    }

    private fun formatDateTime(
        timestamp: Long
    ): String {

        return try {

            SimpleDateFormat(
                "MMM dd, yyyy • hh:mm a",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (e: Exception) {

            "Date unavailable"
        }
    }
}