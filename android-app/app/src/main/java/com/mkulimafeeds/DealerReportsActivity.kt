package com.mkulimafeeds

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderStatus
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModel
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModelFactory
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class DealerReportsActivity : AppCompatActivity() {


    private lateinit var tokenManager: TokenManager

    private lateinit var tvTotalOrders: TextView
    private lateinit var tvActiveOrders: TextView
    private lateinit var tvCompletedOrders: TextView
    private lateinit var tvCancelledOrders: TextView
    private lateinit var tvTotalEarnings: TextView
    private lateinit var tvAverageOrder: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var tvPerformanceMessage: TextView

    private val dealerOrderViewModel: DealerOrderViewModel by viewModels {
        DealerOrderViewModelFactory(
            DealerOrderRepository(
                NetworkModule.apiService
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dealer_reports)

        tokenManager = TokenManager(this)

        setupViews()
        setupToolbar()
        observeOrders()

        loadOrders()
    }

    private fun setupViews() {
        tvTotalOrders =
            findViewById(R.id.tvTotalOrders)

        tvActiveOrders =
            findViewById(R.id.tvActiveOrders)

        tvCompletedOrders =
            findViewById(R.id.tvCompletedOrders)

        tvCancelledOrders =
            findViewById(R.id.tvCancelledOrders)

        tvTotalEarnings =
            findViewById(R.id.tvTotalEarnings)

        tvAverageOrder =
            findViewById(R.id.tvAverageOrder)

        tvCompletionRate =
            findViewById(R.id.tvCompletionRate)

        tvPerformanceMessage =
            findViewById(R.id.tvPerformanceMessage)
    }

    private fun setupToolbar() {
        findViewById<MaterialToolbar>(
            R.id.toolbarDealerReports
        ).setNavigationOnClickListener {
            finish()
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

            return
        }

        dealerOrderViewModel
            .loadAssignedOrders(
                token = token
            )
    }

    private fun observeOrders() {

        lifecycleScope.launch {

            dealerOrderViewModel.uiState
                .collect { state ->

                    if (state.isLoading) {
                        return@collect
                    }

                    state.error?.let { error ->

                        Toast.makeText(
                            this@DealerReportsActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()

                        dealerOrderViewModel
                            .clearError()

                        return@collect
                    }

                    updateReport(
                        state.orders
                    )
                }
        }
    }

    private fun updateReport(
        orders: List<DealerOrder>
    ) {

        val totalOrders =
            orders.size

        val activeOrders =
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

        val completedOrders =
            orders.count { order ->

                order.status ==
                        DealerOrderStatus.COMPLETED
            }

        val cancelledOrders =
            orders.count { order ->

                order.status ==
                        DealerOrderStatus.CANCELLED
            }

        val completedOrderValues =
            orders
                .filter { order ->
                    order.status ==
                            DealerOrderStatus.COMPLETED
                }
                .map { order ->
                    order.totalAmount
                }

        val totalEarnings =
            completedOrderValues.fold(
                BigDecimal.ZERO
            ) { total, amount ->

                total.add(amount)
            }

        val averageOrder =

            if (completedOrderValues.isNotEmpty()) {

                totalEarnings.divide(
                    BigDecimal(
                        completedOrderValues.size
                    ),
                    2,
                    RoundingMode.HALF_UP
                )

            } else {

                BigDecimal.ZERO
            }

        val measurableOrders =
            completedOrders + cancelledOrders

        val completionRate =

            if (measurableOrders > 0) {

                (
                        completedOrders.toDouble() /
                                measurableOrders.toDouble()
                        ) * 100.0

            } else {

                0.0
            }

        tvTotalOrders.text =
            totalOrders.toString()

        tvActiveOrders.text =
            activeOrders.toString()

        tvCompletedOrders.text =
            completedOrders.toString()

        tvCancelledOrders.text =
            cancelledOrders.toString()

        tvTotalEarnings.text =
            formatAmount(totalEarnings)

        tvAverageOrder.text =
            formatAmount(averageOrder)

        tvCompletionRate.text =
            String.format(
                Locale.US,
                "%.1f%%",
                completionRate
            )

        tvPerformanceMessage.text =
            when {

                totalOrders == 0 ->

                    "No assigned orders yet."

                completionRate >= 90 ->

                    "Excellent performance. Keep maintaining this delivery level."

                completionRate >= 75 ->

                    "Good performance. Keep improving delivery completion."

                completionRate >= 50 ->

                    "Your performance is improving. Focus on completing active orders."

                else ->

                    "Focus on completing assigned orders and reducing cancellations."
            }
    }

    private fun formatAmount(
        amount: BigDecimal
    ): String {

        return "KES ${
            amount.setScale(
                2,
                RoundingMode.HALF_UP
            ).toPlainString()
        }"
    }


}
