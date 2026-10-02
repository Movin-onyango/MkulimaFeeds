package com.mkulimafeeds

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.customer.CustomerOrderRepository
import com.mkulimafeeds.domain.model.CustomerOrder
import com.mkulimafeeds.domain.model.CustomerOrderStatus
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderItemAdapter
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderViewModel
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderViewModelFactory
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private val orderViewModel: CustomerOrderViewModel by viewModels {
        CustomerOrderViewModelFactory(
            CustomerOrderRepository(
                NetworkModule.apiService
            )
        )
    }

    private lateinit var tokenManager: TokenManager

    private lateinit var tvOrderTitle: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvNeededDate: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvTelephone: TextView
    private lateinit var tvOrderTotal: TextView

    private lateinit var btnTrackOrder: MaterialButton
    private lateinit var btnCancelOrder: MaterialButton

    private lateinit var rvOrderItems: RecyclerView
    private lateinit var orderItemAdapter: CustomerOrderItemAdapter

    private var orderId: Long = -1L

    private val moneyFormat =
        DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_order_detail
        )

        initializeViews()
        setupWindowInsets()

        tokenManager =
            TokenManager(this)

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

        setupOrderItems()
        setupNavigation()
        observeOrderState()
        loadOrder()
    }

    private fun initializeViews() {

        tvOrderTitle =
            findViewById(R.id.tvOrderTitle)

        tvOrderStatus =
            findViewById(R.id.tvOrderStatus)

        tvNeededDate =
            findViewById(R.id.tvNeededDate)

        tvDeliveryAddress =
            findViewById(R.id.tvDeliveryAddress)

        tvTelephone =
            findViewById(R.id.tvTelephone)

        tvOrderTotal =
            findViewById(R.id.tvOrderTotal)

        btnTrackOrder =
            findViewById(R.id.btnTrackOrder)

        btnCancelOrder =
            findViewById(R.id.btnCancelOrder)

        rvOrderItems =
            findViewById(R.id.rvOrderItems)
    }

    private fun setupOrderItems() {

        orderItemAdapter =
            CustomerOrderItemAdapter()

        rvOrderItems.apply {

            layoutManager =
                LinearLayoutManager(
                    this@OrderDetailActivity
                )

            adapter =
                orderItemAdapter

            isNestedScrollingEnabled =
                false
        }
    }

    private fun setupWindowInsets() {

        val rootView =
            findViewById<View>(
                R.id.detail_root
            )

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
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
                        this@OrderDetailActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()

                    orderViewModel.clearError()
                }

                state.selectedOrder?.let { order ->
                    displayOrder(order)
                }

                btnCancelOrder.isEnabled =
                    !state.isCancelling

                if (state.isCancelling) {
                    btnCancelOrder.text =
                        "Cancelling..."
                }
            }
        }
    }

    private fun displayOrder(
        order: CustomerOrder
    ) {

        tvOrderTitle.text =
            "Order #MF-${order.id}"

        tvOrderStatus.text =
            "● ${formatStatus(order.status)}"

        tvNeededDate.text =
            formatDate(order.neededDate)

        tvDeliveryAddress.text =
            order.location.ifBlank {
                "Delivery address unavailable"
            }

        tvTelephone.text =
            order.telephone.ifBlank {
                "Telephone unavailable"
            }

        tvOrderTotal.text =
            "KES ${moneyFormat.format(order.totalAmount)}"

        orderItemAdapter.updateItems(
            order.items
        )

        updateProgress(
            order.status
        )

        updateCancelButton(
            order.status
        )
    }

    private fun updateCancelButton(
        status: CustomerOrderStatus
    ) {

        val canCancel =
            status ==
                    CustomerOrderStatus.PENDING ||
                    status ==
                    CustomerOrderStatus.CONFIRMED

        btnCancelOrder.visibility =
            if (canCancel) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (canCancel) {
            btnCancelOrder.text =
                "Cancel Order"
        }
    }

    private fun updateProgress(
        status: CustomerOrderStatus
    ) {

        val progress1 =
            findViewById<View>(R.id.progress1)

        val progress2 =
            findViewById<View>(R.id.progress2)

        val progress3 =
            findViewById<View>(R.id.progress3)

        val progress4 =
            findViewById<View>(R.id.progress4)

        val completed =
            getColor(
                R.color.brand_dark_green
            )

        val inactive =
            getColor(
                R.color.light_grey
            )

        val level =
            when (status) {

                CustomerOrderStatus.PENDING -> 1
                CustomerOrderStatus.CONFIRMED -> 2
                CustomerOrderStatus.PROCESSING -> 3

                CustomerOrderStatus.READY,
                CustomerOrderStatus.COMPLETED -> 4

                CustomerOrderStatus.CANCELLED,
                CustomerOrderStatus.UNKNOWN -> 0
            }

        progress1.setBackgroundColor(
            if (level >= 1)
                completed
            else
                inactive
        )

        progress2.setBackgroundColor(
            if (level >= 2)
                completed
            else
                inactive
        )

        progress3.setBackgroundColor(
            if (level >= 3)
                completed
            else
                inactive
        )

        progress4.setBackgroundColor(
            if (level >= 4)
                completed
            else
                inactive
        )
    }

    private fun cancelOrder() {

        if (
            orderViewModel.uiState.value.isCancelling
        ) {
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

        btnCancelOrder.isEnabled = false
        btnCancelOrder.text =
            "Cancelling..."

        orderViewModel.cancelOrder(
            id = orderId,
            token = token
        )
    }

    private fun setupNavigation() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        btnTrackOrder.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TrackOrderActivity::class.java
                ).apply {

                    putExtra(
                        "ORDER_ID",
                        orderId.toString()
                    )
                }
            )
        }

        btnCancelOrder.setOnClickListener {
            cancelOrder()
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
}