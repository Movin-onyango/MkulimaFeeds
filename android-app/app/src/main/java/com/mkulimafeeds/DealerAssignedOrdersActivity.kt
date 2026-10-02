package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.presentation.dealer.orders.DealerAssignedOrdersAdapter
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModel
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModelFactory
import kotlinx.coroutines.launch

class DealerAssignedOrdersActivity :
    AppCompatActivity() {

    private lateinit var recyclerAssignedOrders: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView
    private lateinit var tvSummary: TextView

    private lateinit var tokenManager: TokenManager

    private val dealerOrderViewModel:
            DealerOrderViewModel by viewModels {
        DealerOrderViewModelFactory(
            DealerOrderRepository(
                NetworkModule.apiService
            )
        )
    }

    private lateinit var adapter:
            DealerAssignedOrdersAdapter

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_dealer_assigned_orders
        )

        tokenManager =
            TokenManager(this)

        setupWindowInsets()
        setupViews()
        setupRecyclerView()
        setupBackButton()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun setupWindowInsets() {

        val rootView =
            findViewById<View>(
                R.id.dealer_assigned_orders_root
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

    private fun setupViews() {

        recyclerAssignedOrders =
            findViewById(
                R.id.recyclerAssignedOrders
            )

        progressBar =
            findViewById(
                R.id.progressBar
            )

        tvEmpty =
            findViewById(
                R.id.tvEmpty
            )

        tvError =
            findViewById(
                R.id.tvError
            )

        tvSummary =
            findViewById(
                R.id.tvSummary
            )
    }

    private fun setupRecyclerView() {

        adapter =
            DealerAssignedOrdersAdapter(
                orders = emptyList()
            ) { order ->

                val intent =
                    Intent(
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

        recyclerAssignedOrders.layoutManager =
            LinearLayoutManager(this)

        recyclerAssignedOrders.adapter =
            adapter
    }

    private fun setupBackButton() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }
    }

    private fun observeViewModel() {

        lifecycleScope.launch {

            dealerOrderViewModel.uiState.collect { state ->

                progressBar.visibility =
                    if (state.isLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                if (state.isLoading) {

                    tvEmpty.visibility =
                        View.GONE

                    tvError.visibility =
                        View.GONE
                }

                adapter.submitList(
                    state.orders
                )

                tvSummary.text =
                    when (state.orders.size) {
                        0 ->
                            "Orders assigned to you"
                        1 ->
                            "1 assigned order"
                        else ->
                            "${state.orders.size} assigned orders"
                    }

                tvEmpty.visibility =
                    if (
                        !state.isLoading &&
                        state.orders.isEmpty() &&
                        state.error == null
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                state.error?.let { error ->

                    tvError.text =
                        error

                    tvError.visibility =
                        View.VISIBLE

                    tvEmpty.visibility =
                        View.GONE

                    dealerOrderViewModel
                        .clearError()
                }
            }
        }
    }

    private fun loadOrders() {

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

        dealerOrderViewModel
            .loadAssignedOrders(
                token = token
            )
    }
}