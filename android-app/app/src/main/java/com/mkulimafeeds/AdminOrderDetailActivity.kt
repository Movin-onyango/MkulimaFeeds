package com.mkulimafeeds

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.presentation.orders.OrderViewModel
import com.mkulimafeeds.presentation.dealer.DealerViewModel

class AdminOrderDetailActivity : AppCompatActivity() {

    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvNeededDate: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvDealerName: TextView
    private lateinit var btnAssignDealer: MaterialButton
    private lateinit var statusChipGroup: ChipGroup
    private lateinit var tvOrderTitle: TextView

    private lateinit var rvOrderItems: RecyclerView
    private lateinit var orderItemAdapter: AdminOrderItemAdapter

    private lateinit var tokenManager: TokenManager

    private val orderViewModel: OrderViewModel by viewModels()

    private val dealerViewModel: DealerViewModel by viewModels()

    private var currentOrderId: Long? = null
    private var currentOrder: Order? = null
    private var availableDealers: List<Dealer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_order_detail)

        initializeViews()

        val orderIdString =
            intent.getStringExtra("ORDER_ID")

        currentOrderId =
            orderIdString?.toLongOrNull()

        if (currentOrderId == null) {

            Toast.makeText(
                this,
                "Invalid order ID",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        tokenManager =
            TokenManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.toolbarDetail)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                0,
                systemBars.top,
                0,
                0
            )

            insets
        }

        setupBackButton()
        setupDealerButton()

        loadOrder()
    }

    private fun initializeViews() {

        tvCustomerName =
            findViewById(R.id.tvCustomerName)

        tvCustomerPhone =
            findViewById(R.id.tvCustomerPhone)

        tvDeliveryAddress =
            findViewById(R.id.tvDeliveryAddress)

        tvNeededDate =
            findViewById(R.id.tvNeededDate)

        tvTotalAmount =
            findViewById(R.id.tvTotalAmount)

        tvDealerName =
            findViewById(R.id.tvDealerName)

        btnAssignDealer =
            findViewById(R.id.btnAssignDealer)

        statusChipGroup =
            findViewById(R.id.statusChipGroup)

        tvOrderTitle =
            findViewById(R.id.tvOrderTitle)

        rvOrderItems =
            findViewById(R.id.rvOrderItems)

        orderItemAdapter =
            AdminOrderItemAdapter(emptyList())

        rvOrderItems.layoutManager =
            LinearLayoutManager(this)

        rvOrderItems.adapter =
            orderItemAdapter
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

        orderViewModel.loadAdminOrder(
            id = orderId,
            token = token,

            onSuccess = { order ->

                runOnUiThread {

                    currentOrder = order

                    setupStatusListener(order)
                    setupUpdateButton()

                    loadDealers(order)
                }
            },

            onError = { error ->

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Failed to load order: $error",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
            }
        )
    }

    private fun loadDealers(
        order: Order
    ) {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            setupData(order)

            return
        }

        dealerViewModel.loadAdminDealers(
            token = token,

            onSuccess = { dealers ->

                runOnUiThread {

                    availableDealers = dealers

                    setupData(order)
                }
            },

            onError = { error ->

                runOnUiThread {

                    setupData(order)

                    Toast.makeText(
                        this,
                        "Failed to load dealers: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun setupData(order: Order) {

        tvOrderTitle.text =
            "Order ${order.id}"

        tvCustomerName.text =
            order.customerName

        tvCustomerPhone.text =
            order.customerPhone

        tvDeliveryAddress.text =
            order.deliveryAddress

        tvNeededDate.text =
            "Needed by: ${order.neededDate}"

        tvTotalAmount.text =
            String.format(
                "KES %,.2f",
                order.totalAmount
            )

        orderItemAdapter.updateItems(
            order.items
        )

        updateDealerUI(
            order.assignedDealerId
        )

        setSelectedStatus(
            order.status
        )

        updateStatusChipAvailability(
            order.status
        )
    }

    private fun setSelectedStatus(
        status: String
    ) {

        when (status.uppercase()) {

            "PENDING" ->
                findViewById<Chip>(
                    R.id.chipPending
                ).isChecked = true

            "CONFIRMED" ->
                findViewById<Chip>(
                    R.id.chipConfirmed
                ).isChecked = true

            "PROCESSING" ->
                findViewById<Chip>(
                    R.id.chipProcessing
                ).isChecked = true

            "READY" ->
                findViewById<Chip>(
                    R.id.chipReady
                ).isChecked = true

            "COMPLETED" ->
                findViewById<Chip>(
                    R.id.chipCompleted
                ).isChecked = true

            "CANCELLED" ->
                findViewById<Chip>(
                    R.id.chipCancelled
                ).isChecked = true

            else ->
                statusChipGroup.clearCheck()
        }
    }

    private fun updateStatusChipAvailability(
        currentStatus: String
    ) {

        val chips = listOf(
            findViewById<Chip>(
                R.id.chipPending
            ),
            findViewById<Chip>(
                R.id.chipConfirmed
            ),
            findViewById<Chip>(
                R.id.chipProcessing
            ),
            findViewById<Chip>(
                R.id.chipReady
            ),
            findViewById<Chip>(
                R.id.chipCompleted
            ),
            findViewById<Chip>(
                R.id.chipCancelled
            )
        )

        chips.forEach {
            it.isEnabled = false
        }

        when (currentStatus.uppercase()) {

            "PENDING" -> {

                findViewById<Chip>(
                    R.id.chipPending
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipConfirmed
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipCancelled
                ).isEnabled = true
            }

            "CONFIRMED" -> {

                findViewById<Chip>(
                    R.id.chipConfirmed
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipProcessing
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipCancelled
                ).isEnabled = true
            }

            "PROCESSING" -> {

                findViewById<Chip>(
                    R.id.chipProcessing
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipReady
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipCancelled
                ).isEnabled = true
            }

            "READY" -> {

                findViewById<Chip>(
                    R.id.chipReady
                ).isEnabled = true

                findViewById<Chip>(
                    R.id.chipCompleted
                ).isEnabled = true
            }

            "COMPLETED" -> {

                findViewById<Chip>(
                    R.id.chipCompleted
                ).isEnabled = true
            }

            "CANCELLED" -> {

                findViewById<Chip>(
                    R.id.chipCancelled
                ).isEnabled = true
            }
        }
    }

    private fun isValidStatusTransition(
        currentStatus: String,
        newStatus: String
    ): Boolean {

        return when (currentStatus.uppercase()) {

            "PENDING" ->
                newStatus == "CONFIRMED" ||
                        newStatus == "CANCELLED"

            "CONFIRMED" ->
                newStatus == "PROCESSING" ||
                        newStatus == "CANCELLED"

            "PROCESSING" ->
                newStatus == "READY" ||
                        newStatus == "CANCELLED"

            "READY" ->
                newStatus == "COMPLETED"

            "COMPLETED" ->
                false

            "CANCELLED" ->
                false

            else ->
                false
        }
    }

    private fun setupStatusListener(
        order: Order
    ) {

        statusChipGroup.setOnCheckedStateChangeListener {
                _,
                checkedIds ->

            if (checkedIds.isEmpty()) {
                return@setOnCheckedStateChangeListener
            }

            val newStatus =
                when (checkedIds[0]) {

                    R.id.chipPending ->
                        "PENDING"

                    R.id.chipConfirmed ->
                        "CONFIRMED"

                    R.id.chipProcessing ->
                        "PROCESSING"

                    R.id.chipReady ->
                        "READY"

                    R.id.chipCompleted ->
                        "COMPLETED"

                    R.id.chipCancelled ->
                        "CANCELLED"

                    else -> {

                        setSelectedStatus(
                            order.status
                        )

                        return@setOnCheckedStateChangeListener
                    }
                }

            if (newStatus == order.status) {
                return@setOnCheckedStateChangeListener
            }

            if (!isValidStatusTransition(
                    order.status,
                    newStatus
                )
            ) {

                setSelectedStatus(
                    order.status
                )

                Toast.makeText(
                    this,
                    "Invalid status transition: ${order.status} → $newStatus",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnCheckedStateChangeListener
            }

            updateOrderStatus(
                order,
                newStatus
            )
        }
    }

    private fun updateOrderStatus(
        order: Order,
        newStatus: String
    ) {

        val orderId =
            order.id.toLongOrNull()

        if (orderId == null) {

            Toast.makeText(
                this,
                "Invalid order ID",
                Toast.LENGTH_SHORT
            ).show()

            setSelectedStatus(
                order.status
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

            setSelectedStatus(
                order.status
            )

            return
        }

        orderViewModel.updateOrderStatus(
            id = orderId,
            status = newStatus,
            token = token,

            onSuccess = { updatedOrder ->

                runOnUiThread {

                    currentOrder =
                        updatedOrder

                    order.status =
                        updatedOrder.status

                    setSelectedStatus(
                        updatedOrder.status
                    )

                    updateStatusChipAvailability(
                        updatedOrder.status
                    )

                    Toast.makeText(
                        this,
                        "Status updated to ${updatedOrder.status}",
                        Toast.LENGTH_SHORT
                    ).show()


                }
            },

            onError = { error ->

                runOnUiThread {

                    setSelectedStatus(
                        order.status
                    )

                    Toast.makeText(
                        this,
                        "Status update failed: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun setupUpdateButton() {

        findViewById<MaterialButton>(
            R.id.btnUpdateOrder
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Order details updated successfully",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    private fun updateDealerUI(
        dealerId: String?
    ) {

        if (dealerId.isNullOrBlank()) {

            tvDealerName.text =
                "Not Assigned"

            btnAssignDealer.text =
                "Assign"

            return
        }

        val dealer =
            availableDealers.find {
                it.id == dealerId
            }

        tvDealerName.text =
            dealer?.name
                ?: "Dealer ID: $dealerId"

        btnAssignDealer.text =
            "Change"
    }

    private fun setupBackButton() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }
    }

    private fun setupDealerButton() {

        btnAssignDealer.setOnClickListener {

            currentOrder?.let { order ->

                showDealerSelectionSheet(
                    order
                )
            }
        }
    }

    private fun showDealerSelectionSheet(
        order: Order
    ) {

        if (availableDealers.isEmpty()) {

            Toast.makeText(
                this,
                "No active dealers available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val dialog =
            BottomSheetDialog(this)

        val view =
            layoutInflater.inflate(
                R.layout.layout_dealer_selection_sheet,
                null
            )

        val rvDealers =
            view.findViewById<RecyclerView>(
                R.id.rvDealers
            )

        rvDealers.layoutManager =
            LinearLayoutManager(this)

        val adapter =
            DealerSelectionAdapter(
                availableDealers
            ) { selectedDealer: Dealer ->

                assignDealerToBackend(
                    order = order,
                    selectedDealer = selectedDealer,
                    dialog = dialog
                )
            }

        rvDealers.adapter =
            adapter

        dialog.setContentView(view)
        dialog.show()
    }

    private fun assignDealerToBackend(
        order: Order,
        selectedDealer: Dealer,
        dialog: BottomSheetDialog
    ) {

        val orderId =
            order.id.toLongOrNull()

        if (orderId == null) {

            Toast.makeText(
                this,
                "Invalid order ID",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val dealerId =
            selectedDealer.id.toLongOrNull()

        if (dealerId == null) {

            Toast.makeText(
                this,
                "Invalid dealer ID",
                Toast.LENGTH_SHORT
            ).show()

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

        btnAssignDealer.isEnabled = false

        orderViewModel.assignDealer(
            id = orderId,
            dealerId = dealerId,
            token = token,

            onSuccess = { updatedOrder ->

                runOnUiThread {

                    currentOrder =
                        updatedOrder

                    updateDealerUI(
                        updatedOrder.assignedDealerId
                    )

                    Toast.makeText(
                        this,
                        "Order assigned to ${selectedDealer.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    btnAssignDealer.isEnabled =
                        true

                    dialog.dismiss()
                }
            },

            onError = { error ->

                runOnUiThread {

                    btnAssignDealer.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Dealer assignment failed: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}