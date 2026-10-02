package com.mkulimafeeds

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderStatus
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModel
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModelFactory
import kotlinx.coroutines.launch
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerOrderAdapter


class DealerCustomerDetailsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvCustomerEmail: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvActiveOrders: TextView
    private lateinit var tvCompletedOrders: TextView
    private lateinit var tvNoOrders: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DealerCustomerOrderAdapter

    private var customerId: Long = -1L

    private val dealerOrderViewModel: DealerOrderViewModel by viewModels {
        DealerOrderViewModelFactory(
            DealerOrderRepository( NetworkModule.apiService )
        )
    }

    override fun onCreate( savedInstanceState: Bundle? ) {
        super.onCreate(savedInstanceState)
        setContentView( R.layout.activity_dealer_customer_details )

        customerId = intent.getLongExtra( "CUSTOMER_ID", -1L )
        if (customerId <= 0L) {
            Toast.makeText(
                this,
                "Invalid customer",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        tokenManager = TokenManager(this)

        setupViews()
        setupCustomerInfo()
        setupToolbar()
        setupRecyclerView()
        observeOrders()
        loadOrders()
    }

    private fun setupViews() {
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail)
        tvTotalOrders = findViewById(R.id.tvTotalOrders)
        tvActiveOrders = findViewById(R.id.tvActiveOrders)
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders)
        tvNoOrders = findViewById(R.id.tvNoOrders)
        progressBar = findViewById(R.id.progressCustomerOrders)
        recyclerView = findViewById(R.id.recyclerCustomerOrders)
    }

    private fun setupCustomerInfo() {
        tvCustomerName.text = intent.getStringExtra( "CUSTOMER_NAME" ) ?: "Customer"
        tvCustomerPhone.text = intent.getStringExtra( "CUSTOMER_PHONE" ) ?: "No phone number"
        tvCustomerEmail.text = intent.getStringExtra( "CUSTOMER_EMAIL" ) ?: "No email provided"
    }

    private fun setupToolbar() {
        findViewById<MaterialToolbar>(
            R.id.toolbarCustomerDetails
        ).setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter =
            DealerCustomerOrderAdapter(
                orders = emptyList()
            ) { order ->

                val intent = android.content.Intent(
                    this,
                    AssignedOrdersActivity::class.java
                ).apply {
                    putExtra(
                        "ORDER_ID",
                        order.id
                    )
                }

                startActivity(intent)
            }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeOrders() {
        lifecycleScope.launch {
            dealerOrderViewModel.uiState
                .collect { state ->
                    progressBar.visibility = if (state.isLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    val customerOrders = state.orders.filter { it.customerId == customerId }
                    adapter.updateList( customerOrders )

                    tvTotalOrders.text = customerOrders.size.toString()
                    tvActiveOrders.text = customerOrders.count {
                        it.status == DealerOrderStatus.CONFIRMED ||
                                it.status == DealerOrderStatus.PROCESSING ||
                                it.status == DealerOrderStatus.READY
                    }.toString()
                    tvCompletedOrders.text = customerOrders.count {
                        it.status == DealerOrderStatus.COMPLETED
                    }.toString()

                    tvNoOrders.visibility = if ( !state.isLoading && customerOrders.isEmpty() ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    state.error?.let { error ->
                        Toast.makeText(
                            this@DealerCustomerDetailsActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()
                        dealerOrderViewModel.clearError()
                    }
                }
        }
    }

    private fun loadOrders() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        dealerOrderViewModel.loadAssignedOrders( token )
    }
}
