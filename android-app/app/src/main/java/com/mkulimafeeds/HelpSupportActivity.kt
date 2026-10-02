package com.mkulimafeeds

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class HelpSupportActivity : AppCompatActivity() {

    // Update these with your real contact details when ready
    private val supportEmail = "support@mkulimafeeds.co.ke"
    private val supportPhone = "+254700000000"
    private val whatsappNumber = "254700000000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help_support)

        val rootView = findViewById<View>(R.id.helpRoot)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialCardView>(R.id.cardEmail).setOnClickListener {
            openEmail()
        }

        findViewById<MaterialCardView>(R.id.cardPhone).setOnClickListener {
            openPhone()
        }

        findViewById<MaterialCardView>(R.id.cardWhatsApp).setOnClickListener {
            openWhatsApp()
        }
    }

    // =========================================================
    // CONTACT ACTIONS
    // =========================================================

    private fun openEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$supportEmail")
                putExtra(Intent.EXTRA_SUBJECT, "MkulimaFeeds Support")
            }
            startActivity(intent)
        } catch (_: Exception) {
            toast("No email app available")
        }
    }

    private fun openPhone() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$supportPhone")
            }
            startActivity(intent)
        } catch (_: Exception) {
            toast("No dialer available")
        }
    }

    private fun openWhatsApp() {
        try {
            val url = "https://wa.me/$whatsappNumber"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        } catch (_: Exception) {
            toast("No app available to open WhatsApp")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}