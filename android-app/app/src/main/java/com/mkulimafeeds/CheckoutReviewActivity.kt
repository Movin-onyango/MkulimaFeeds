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
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.remote.CreateOrderItemRequest
import com.mkulimafeeds.data.remote.CreateOrderRequest
import com.mkulimafeeds.presentation.orders.OrderViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.mkulimafeeds.data.local.TokenManager
class CheckoutReviewActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_PHONE =
            "EXTRA_PHONE"

        const val EXTRA_ADDRESS =
            "EXTRA_ADDRESS"

        const val EXTRA_DATE_DISPLAY =
            "EXTRA_DATE"

        const val EXTRA_NEEDED_DATE =
            "EXTRA_NEEDED_DATE"

        const val EXTRA_TOTAL =
            "EXTRA_TOTAL"

        const val EXTRA_ORDER_REQUEST =
            "EXTRA_ORDER_REQUEST"
    }

    private val orderViewModel =
        OrderViewModel()

    private lateinit var btnPayNow: MaterialButton
    private lateinit var btnPayLater: MaterialButton

    private var isSubmitting =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_checkout_review
        )

        val rootView =
            findViewById<View>(
                R.id.review_root
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

        btnPayNow =
            findViewById(
                R.id.btnPayNow
            )

        btnPayLater =
            findViewById(
                R.id.btnPayLater
            )

        val tvPhone =
            findViewById<TextView>(
                R.id.tvSummaryPhone
            )

        val tvAddress =
            findViewById<TextView>(
                R.id.tvSummaryAddress
            )

        val tvDate =
            findViewById<TextView>(
                R.id.tvSummaryDate
            )

        val tvTotal =
            findViewById<TextView>(
                R.id.tvGrandTotal
            )

        val phone =
            intent.getStringExtra(
                EXTRA_PHONE
            ).orEmpty()

        val address =
            intent.getStringExtra(
                EXTRA_ADDRESS
            ).orEmpty()

        val date =
            intent.getStringExtra(
                EXTRA_DATE_DISPLAY
            ).orEmpty()

        val total =
            intent.getStringExtra(
                EXTRA_TOTAL
            ) ?: "KES 0.00"

        tvPhone.text =
            "CONTACT NUMBER\n$phone"

        tvAddress.text =
            "DELIVERY ADDRESS\n$address"

        tvDate.text =
            "PREFERRED DELIVERY DATE\n$date"

        tvTotal.text =
            total

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        findViewById<View>(
            R.id.btnEditDetails
        ).setOnClickListener {
            finish()
        }

        btnPayNow.setOnClickListener {

            if (isSubmitting) {
                return@setOnClickListener
            }

            val request =
                buildOrderRequest()

            if (request == null) {
                return@setOnClickListener
            }

            val requestJson =
                Json.encodeToString(request)

            val intent =
                Intent(
                    this,
                    PaymentActivity::class.java
                )

            intent.putExtra(
                EXTRA_TOTAL,
                total
            )

            intent.putExtra(
                EXTRA_ORDER_REQUEST,
                requestJson
            )

            startActivity(intent)
        }

        btnPayLater.setOnClickListener {

            if (isSubmitting) {
                return@setOnClickListener
            }

            val request =
                buildOrderRequest()
                    ?: return@setOnClickListener

            createPayLaterOrder(
                request
            )
        }
    }

    private fun buildOrderRequest():
            CreateOrderRequest? {

        val phone =
            intent.getStringExtra(
                EXTRA_PHONE
            )
                ?.trim()
                .orEmpty()

        val address =
            intent.getStringExtra(
                EXTRA_ADDRESS
            )
                ?.trim()
                .orEmpty()

        val neededDate =
            intent.getLongExtra(
                EXTRA_NEEDED_DATE,
                -1L
            )

        if (phone.isBlank()) {
            Toast.makeText(
                this,
                "Phone number is missing",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        if (address.isBlank()) {
            Toast.makeText(
                this,
                "Delivery address is missing",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        if (neededDate <= 0L) {
            Toast.makeText(
                this,
                "Delivery date is missing",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        val cartItems =
            CartManager.getItems()

        if (cartItems.isEmpty()) {
            Toast.makeText(
                this,
                "Your cart is empty",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        val items =
            cartItems.map { item ->

                CreateOrderItemRequest(
                    productId =
                        item.id.toLong(),
                    quantity =
                        item.quantity.toString()
                )
            }

        return CreateOrderRequest(
            telephone = phone,
            location = address,
            neededDate = neededDate,
            notes = null,
            items = items
        )
    }

    private fun createPayLaterOrder(
        request: CreateOrderRequest
    ) {

        val token =
            TokenManager(this).getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        isSubmitting = true
        setButtonsEnabled(false)

        orderViewModel.createOrder(
            request = request,
            token = token,
            onSuccess = { order ->

                runOnUiThread {

                    CartManager.clearCart()

                    openSuccessScreen(
                        orderId = order.id.toLongOrNull() ?: -1L,
                        paymentStatus = "PENDING"
                    )
                }
            },
            onError = { error ->

                runOnUiThread {

                    isSubmitting = false
                    setButtonsEnabled(true)

                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun setButtonsEnabled(
        enabled: Boolean
    ) {

        btnPayNow.isEnabled =
            enabled

        btnPayLater.isEnabled =
            enabled

        btnPayNow.text =
            if (enabled) {
                "Pay Now"
            } else {
                "Processing..."
            }

        btnPayLater.text =
            if (enabled) {
                "Pay Later"
            } else {
                "Processing..."
            }
    }

    private fun openSuccessScreen(
        orderId: Long,
        paymentStatus: String
    ) {

        val intent =
            Intent(
                this,
                OrderSuccessActivity::class.java
            )

        intent.putExtra(
            OrderSuccessActivity.EXTRA_ORDER_ID,
            orderId
        )

        intent.putExtra(
            OrderSuccessActivity.EXTRA_PAYMENT_STATUS,
            paymentStatus
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}