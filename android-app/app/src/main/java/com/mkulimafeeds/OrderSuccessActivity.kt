package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class OrderSuccessActivity :
    AppCompatActivity() {

    companion object {

        const val EXTRA_ORDER_ID =
            "EXTRA_ORDER_ID"

        const val EXTRA_PAYMENT_STATUS =
            "PAYMENT_STATUS"
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_order_success
        )

        val orderId =
            intent.getLongExtra(
                EXTRA_ORDER_ID,
                -1L
            )

        val paymentStatus =
            intent.getStringExtra(
                EXTRA_PAYMENT_STATUS
            )

        val tvStatus =
            findViewById<TextView>(
                R.id.tvOrderStatus
            )

        tvStatus.text =
            when {

                paymentStatus == "PAID" &&
                        orderId > 0L ->
                    "Payment Successful!\nOrder #MF-$orderId Confirmed"

                orderId > 0L ->
                    "Order #MF-$orderId Placed\nPayment Pending (Pay Later)"

                paymentStatus == "PAID" ->
                    "Payment Successful!\nOrder Confirmed"

                else ->
                    "Order Placed\nPayment Pending (Pay Later)"
            }

        val rootView =
            findViewById<View>(
                R.id.success_root
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

        findViewById<MaterialButton>(
            R.id.btnBackHome
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    HomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }

        findViewById<MaterialButton>(
            R.id.btnViewOrders
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MyOrdersActivity::class.java
                )
            )

            finish()
        }
    }
}