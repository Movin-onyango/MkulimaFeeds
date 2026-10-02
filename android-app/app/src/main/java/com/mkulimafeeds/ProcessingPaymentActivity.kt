package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.mkulimafeeds.data.remote.CreateOrderRequest
import com.mkulimafeeds.presentation.orders.OrderViewModel
import kotlinx.serialization.json.Json
import com.mkulimafeeds.data.local.TokenManager
class ProcessingPaymentActivity : AppCompatActivity() {

    private val orderViewModel =
        OrderViewModel()

    private var orderRequestJson: String = ""
    private var totalAmount: String = "KES 0.00"

    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_processing_payment)

        orderRequestJson =
            intent.getStringExtra(
                CheckoutReviewActivity.EXTRA_ORDER_REQUEST
            ).orEmpty()

        totalAmount =
            intent.getStringExtra(
                CheckoutReviewActivity.EXTRA_TOTAL
            ) ?: "KES 0.00"

        if (orderRequestJson.isBlank()) {
            openPaymentFailure(
                "Checkout information is missing"
            )
            return
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                simulatePayment()
            },
            3000
        )
    }

    private fun simulatePayment() {

        if (completed) {
            return
        }

        completed = true

        val isSuccess =
            (0..99).random() < 85

        if (!isSuccess) {
            openPaymentFailure(
                "Payment could not be completed"
            )
            return
        }

        createBackendOrder()
    }

    private fun createBackendOrder() {

        val token =
            TokenManager(this).getToken()

        if (token.isNullOrBlank()) {
            openPaymentFailure(
                "Authentication token not found"
            )
            return
        }

        val request =
            try {
                Json.decodeFromString<CreateOrderRequest>(
                    orderRequestJson
                )
            } catch (e: Exception) {
                openPaymentFailure(
                    "Invalid checkout information"
                )
                return
            }

        orderViewModel.createOrder(
            request = request,
            token = token,
            onSuccess = { order ->

                runOnUiThread {

                    CartManager.clearCart()

                    val intent =
                        Intent(
                            this,
                            OrderSuccessActivity::class.java
                        )

                    intent.putExtra(
                        OrderSuccessActivity.EXTRA_ORDER_ID,
                        order.id
                    )

                    intent.putExtra(
                        OrderSuccessActivity.EXTRA_PAYMENT_STATUS,
                        "PAID"
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()
                }
            },
            onError = { error ->

                runOnUiThread {
                    openPaymentFailure(error)
                }
            }
        )
    }

    private fun openPaymentFailure(
        message: String
    ) {

        val intent =
            Intent(
                this,
                PaymentFailedActivity::class.java
            )

        intent.putExtra(
            "ERROR_MESSAGE",
            message
        )

        intent.putExtra(
            CheckoutReviewActivity.EXTRA_TOTAL,
            totalAmount
        )

        intent.putExtra(
            CheckoutReviewActivity.EXTRA_ORDER_REQUEST,
            orderRequestJson
        )

        startActivity(intent)
        finish()
    }
}