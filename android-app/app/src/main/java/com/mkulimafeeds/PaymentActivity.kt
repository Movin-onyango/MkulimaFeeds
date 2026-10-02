package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class PaymentActivity : AppCompatActivity() {

    private lateinit var radioButtons:
            List<RadioButton>

    private lateinit var tvTotalAmount:
            TextView

    private lateinit var orderRequestJson:
            String

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_payment
        )

        val rootView =
            findViewById<android.view.View>(
                R.id.payment_root
            )

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
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

        tvTotalAmount =
            findViewById(
                R.id.tvTotalAmount
            )

        val totalAmount =
            intent.getStringExtra(
                CheckoutReviewActivity.EXTRA_TOTAL
            ) ?: "KES 0.00"

        orderRequestJson =
            intent.getStringExtra(
                CheckoutReviewActivity.EXTRA_ORDER_REQUEST
            ).orEmpty()

        if (orderRequestJson.isBlank()) {

            Toast.makeText(
                this,
                "Checkout information is missing",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        tvTotalAmount.text =
            totalAmount

        val rMpesa =
            findViewById<RadioButton>(
                R.id.radioMpesa
            )

        val rAirtel =
            findViewById<RadioButton>(
                R.id.radioAirtel
            )

        val rTelkom =
            findViewById<RadioButton>(
                R.id.radioTelkom
            )

        val rBank =
            findViewById<RadioButton>(
                R.id.radioBank
            )

        radioButtons =
            listOf(
                rMpesa,
                rAirtel,
                rTelkom,
                rBank
            )

        radioButtons.forEach { button ->

            button.setOnClickListener {
                selectMethod(button)
            }
        }

        findViewById<MaterialButton>(
            R.id.btnPayNow
        ).setOnClickListener {

            val selected =
                radioButtons.firstOrNull {
                    it.isChecked
                }

            if (selected == null) {

                Toast.makeText(
                    this,
                    "Please select a payment method",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            showPrompt(selected)
        }
    }

    private fun selectMethod(
        selected: RadioButton
    ) {

        radioButtons.forEach {
            it.isChecked =
                it == selected
        }
    }

    private fun showPrompt(
        selected: RadioButton
    ) {

        val title =
            when (selected.id) {

                R.id.radioMpesa,
                R.id.radioAirtel,
                R.id.radioTelkom ->
                    "Enter Phone Number"

                else ->
                    "Enter Card/Account Number"
            }

        val hint =
            when (selected.id) {

                R.id.radioMpesa,
                R.id.radioAirtel,
                R.id.radioTelkom ->
                    "07xx xxx xxx"

                else ->
                    "xxxx xxxx xxxx xxxx"
            }

        val builder =
            AlertDialog.Builder(this)

        builder.setTitle(title)

        val input =
            EditText(this)

        input.hint =
            hint

        input.setPadding(
            50,
            40,
            50,
            40
        )

        builder.setView(input)

        builder.setPositiveButton(
            "Confirm"
        ) { _, _ ->

            val detail =
                input.text
                    .toString()
                    .trim()

            if (detail.isBlank()) {

                Toast.makeText(
                    this,
                    "Please enter details",
                    Toast.LENGTH_SHORT
                ).show()

                return@setPositiveButton
            }

            val intent =
                Intent(
                    this,
                    ProcessingPaymentActivity::class.java
                )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_TOTAL,
                tvTotalAmount.text.toString()
            )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_ORDER_REQUEST,
                orderRequestJson
            )

            intent.putExtra(
                "PAYMENT_METHOD",
                selected.text.toString()
            )

            startActivity(intent)
            finish()
        }

        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}