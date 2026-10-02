package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class PaymentFailedActivity : AppCompatActivity() {

    private var orderRequestJson: String = ""
    private var totalAmount: String = "KES 0.00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_failed)

        orderRequestJson =
            intent.getStringExtra(
                CheckoutReviewActivity.EXTRA_ORDER_REQUEST
            ).orEmpty()

        totalAmount =
            intent.getStringExtra(
                CheckoutReviewActivity.EXTRA_TOTAL
            ) ?: "KES 0.00"

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.payment_failed_root)
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

        findViewById<MaterialButton>(
            R.id.btnTryAgain
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    ProcessingPaymentActivity::class.java
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

        findViewById<MaterialButton>(
            R.id.btnChangeMethod
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    PaymentActivity::class.java
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
}