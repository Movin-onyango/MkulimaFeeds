package com.mkulimafeeds

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderStatus
import com.mkulimafeeds.presentation.dealer.notifications.DealerNotificationHelper
import com.mkulimafeeds.presentation.dealer.orders.AssignedOrderItemAdapter
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModel
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModelFactory
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssignedOrdersActivity : AppCompatActivity() {

    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvNeededDate: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderTitle: TextView

    private lateinit var btnCallCustomer: View
    private lateinit var btnUpdateStatus: MaterialButton

    private lateinit var rvOrderItems: RecyclerView
    private lateinit var orderItemAdapter: AssignedOrderItemAdapter

    private lateinit var tokenManager: TokenManager

    private val dealerOrderViewModel: DealerOrderViewModel by viewModels {
        DealerOrderViewModelFactory(
            DealerOrderRepository(
                NetworkModule.apiService
            )
        )
    }

    private var currentOrderId: Long? = null
    private var currentOrder: DealerOrder? = null

    private val moneyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_assigned_orders)

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                1001
            )
        }

        initializeViews()

        currentOrderId =
            intent.getLongExtra("ORDER_ID", -1L)
                .takeIf { it > 0L }
                ?: intent.getStringExtra("ORDER_ID")
                    ?.toLongOrNull()

        if (currentOrderId == null) {
            Toast.makeText(
                this,
                "Invalid order ID",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        tokenManager = TokenManager(this)

        setupWindowInsets()
        setupBackButton()
        setupOrderItems()
        setupCallCustomer()
        setupStatusButton()
        observeViewModel()

        loadOrder()
    }

    private fun initializeViews() {
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress)
        tvNeededDate = findViewById(R.id.tvNeededDate)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvOrderTitle = findViewById(R.id.tvOrderTitle)

        btnCallCustomer = findViewById(R.id.btnCallCustomer)
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus)

        rvOrderItems = findViewById(R.id.rvOrderItems)
    }

    private fun setupOrderItems() {
        orderItemAdapter = AssignedOrderItemAdapter()

        rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@AssignedOrdersActivity)
            adapter = orderItemAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupWindowInsets() {
        val rootView = findViewById<View>(R.id.assigned_root)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )

            insets
        }
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupCallCustomer() {
        btnCallCustomer.setOnClickListener {

            val phone =
                currentOrder?.telephone
                    ?.trim()
                    .orEmpty()

            if (phone.isBlank()) {
                Toast.makeText(
                    this,
                    "Customer phone number is unavailable",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent = Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:$phone")
            )

            startActivity(intent)
        }
    }

    private fun setupStatusButton() {
        btnUpdateStatus.setOnClickListener {

            val order = currentOrder
                ?: return@setOnClickListener

            val nextStatus =
                getNextStatus(order.status)

            if (nextStatus == null) {
                Toast.makeText(
                    this,
                    "This order is already completed",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val token = tokenManager.getToken()

            if (token.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Authentication token not found",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            btnUpdateStatus.isEnabled = false
            btnUpdateStatus.text = "Updating..."

            dealerOrderViewModel.updateOrderStatus(
                id = order.id,
                status = nextStatus.name,
                token = token
            )
        }
    }

    private fun observeViewModel() {

        lifecycleScope.launch {
            dealerOrderViewModel.statusUpdateSuccess
                .collect { order ->

                    DealerNotificationHelper
                        .showOrderStatusUpdated(
                            context = this@AssignedOrdersActivity,
                            order = order
                        )
                }
        }

        lifecycleScope.launch {
            dealerOrderViewModel.uiState
                .collect { state ->

                    state.selectedOrder?.let { order ->

                        currentOrder = order

                        displayOrder(order)
                    }

                    if (!state.isLoading) {

                        if (state.statusUpdateLoading) {

                            btnUpdateStatus.isEnabled = false
                            btnUpdateStatus.text = "Updating..."

                        } else {

                            btnUpdateStatus.isEnabled =
                                currentOrder
                                    ?.let {
                                        getNextStatus(it.status) != null
                                    }
                                    ?: false

                            updateStatusButtonText(
                                currentOrder
                            )
                        }
                    }

                    state.error?.let { error ->

                        Toast.makeText(
                            this@AssignedOrdersActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()

                        dealerOrderViewModel.clearError()

                        btnUpdateStatus.isEnabled = true

                        updateStatusButtonText(
                            currentOrder
                        )
                    }
                }
        }
    }

    private fun loadOrder() {

        val orderId =
            currentOrderId
                ?: return

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

        dealerOrderViewModel.loadAssignedOrder(
            id = orderId,
            token = token
        )
    }

    private fun displayOrder(
        order: DealerOrder
    ) {

        tvOrderTitle.text =
            "Order #${order.id}"

        tvCustomerName.text =
            order.customerName

        tvCustomerPhone.text =
            order.telephone

        tvDeliveryAddress.text =
            order.location

        tvNeededDate.text =
            formatDate(order.neededDate)

        tvTotalAmount.text =
            "KES ${moneyFormat.format(order.totalAmount)}"

        tvOrderStatus.text =
            formatStatus(order.status)

        // IMPORTANT:
        // This is what puts the order items into the RecyclerView.
        orderItemAdapter.updateItems(
            order.items
        )

        updateStatusButtonText(order)

        btnUpdateStatus.isEnabled =
            getNextStatus(order.status) != null

        btnCallCustomer.isEnabled =
            order.telephone.isNotBlank()
    }

    private fun updateStatusButtonText(
        order: DealerOrder?
    ) {

        if (order == null) {
            btnUpdateStatus.text =
                "Update Status"
            return
        }

        val nextStatus =
            getNextStatus(order.status)

        btnUpdateStatus.text =
            if (nextStatus == null) {
                "Order Completed"
            } else {
                "Mark as ${formatStatus(nextStatus)}"
            }
    }

    private fun getNextStatus(
        status: DealerOrderStatus
    ): DealerOrderStatus? {

        return when (status) {

            DealerOrderStatus.CONFIRMED ->
                DealerOrderStatus.PROCESSING

            DealerOrderStatus.PROCESSING ->
                DealerOrderStatus.READY

            DealerOrderStatus.READY ->
                DealerOrderStatus.COMPLETED

            DealerOrderStatus.PENDING,
            DealerOrderStatus.COMPLETED,
            DealerOrderStatus.CANCELLED ->
                null
        }
    }

    private fun formatStatus(
        status: DealerOrderStatus
    ): String {

        return status.name
            .lowercase(Locale.getDefault())
            .replaceFirstChar {
                it.uppercase()
            }
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        return try {

            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (e: Exception) {

            "Date unavailable"
        }
    }
}