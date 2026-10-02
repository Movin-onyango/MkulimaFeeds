package com.mkulimafeeds

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.util.Calendar
import java.util.Locale

class DeliveryActivity : AppCompatActivity() {

    private lateinit var etDeliveryDate: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvItemsTotal: TextView

    private var selectedDeliveryDateMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_delivery)

        val rootView = findViewById<View>(R.id.delivery_root)

        etDeliveryDate = findViewById(R.id.etDeliveryDate)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee)
        tvItemsTotal = findViewById(R.id.tvItemsTotal)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)

        tvDeliveryFee.text = "KES 0.00"

        updateTotals()

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        etDeliveryDate.setOnClickListener {
            showDatePicker()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnContinue).setOnClickListener {

            if (!validateInputs()) {
                return@setOnClickListener
            }

            if (CartManager.getItems().isEmpty()) {
                Toast.makeText(
                    this,
                    "Your cart is empty",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val selectedDate =
                selectedDeliveryDateMillis

            if (selectedDate == null) {
                Toast.makeText(
                    this,
                    "Please select a delivery date",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val intent =
                Intent(
                    this,
                    CheckoutReviewActivity::class.java
                )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_DATE_DISPLAY,
                etDeliveryDate.text.toString()
            )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_NEEDED_DATE,
                selectedDate
            )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_PHONE,
                etPhone.text.toString().trim()
            )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_ADDRESS,
                etAddress.text.toString().trim()
            )

            intent.putExtra(
                CheckoutReviewActivity.EXTRA_TOTAL,
                tvGrandTotal.text.toString()
            )

            startActivity(intent)
        }

        findViewById<View>(R.id.btnCurrentLocation).setOnClickListener {
            etAddress.setText(
                "Greenhouse Office Park, Ngong Road, Nairobi"
            )

            Toast.makeText(
                this,
                "Location detected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDatePicker() {

        val calendar =
            Calendar.getInstance()

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH)

        val day =
            calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val selectedCalendar =
                        Calendar.getInstance().apply {
                            set(
                                Calendar.YEAR,
                                selectedYear
                            )
                            set(
                                Calendar.MONTH,
                                selectedMonth
                            )
                            set(
                                Calendar.DAY_OF_MONTH,
                                selectedDay
                            )
                            set(
                                Calendar.HOUR_OF_DAY,
                                0
                            )
                            set(
                                Calendar.MINUTE,
                                0
                            )
                            set(
                                Calendar.SECOND,
                                0
                            )
                            set(
                                Calendar.MILLISECOND,
                                0
                            )
                        }

                    selectedDeliveryDateMillis =
                        selectedCalendar.timeInMillis

                    val formattedDate =
                        String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                        )

                    etDeliveryDate.setText(
                        formattedDate
                    )
                },
                year,
                month,
                day
            )

        datePickerDialog.datePicker.minDate =
            System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun updateTotals() {

        val itemsTotal =
            CartManager.getTotalPrice()

        tvItemsTotal.text =
            String.format(
                Locale.getDefault(),
                "KES %.2f",
                itemsTotal
            )

        val deliveryFee =
            0.0

        val grandTotal =
            itemsTotal + deliveryFee

        tvGrandTotal.text =
            String.format(
                Locale.getDefault(),
                "KES %.2f",
                grandTotal
            )
    }

    private fun validateInputs(): Boolean {

        if (
            selectedDeliveryDateMillis == null ||
            etDeliveryDate.text
                .toString()
                .trim()
                .isEmpty()
        ) {
            Toast.makeText(
                this,
                "Please select a delivery date",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        val phone =
            etPhone.text
                .toString()
                .trim()

        if (phone.isEmpty()) {
            etPhone.error =
                "Phone number is required"
            return false
        }

        if (phone.length < 9) {
            etPhone.error =
                "Enter a valid phone number"
            return false
        }

        val address =
            etAddress.text
                .toString()
                .trim()

        if (address.isEmpty()) {
            etAddress.error =
                "Delivery address is required"
            return false
        }

        return true
    }
}