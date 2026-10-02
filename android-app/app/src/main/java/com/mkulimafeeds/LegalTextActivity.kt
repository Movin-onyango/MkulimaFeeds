package com.mkulimafeeds

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mkulimafeeds.data.repository.BrandingRepository
import kotlinx.coroutines.launch

/**
 * Shared base for Terms of Service and Privacy Policy.
 *
 * Subclasses provide the screen title and the setting key
 * that contains the URL for the legal content.
 *
 * The screen shows:
 *   1. A concise summary text (from branding settings)
 *   2. A "View full document" button that opens the URL externally
 */
abstract class LegalTextActivity : AppCompatActivity() {

    protected abstract fun screenTitle(): String
    protected abstract fun urlSettingKey(): String

    private val brandingRepository by lazy { BrandingRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_legal_text)

        val rootView = findViewById<View>(R.id.legalRoot)
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

        findViewById<TextView>(R.id.tvTitle).text = screenTitle()
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadContent()
    }

    private fun loadContent() {
        val body = findViewById<TextView>(R.id.tvLegalBody)

        lifecycleScope.launch {
            try {
                val branding = brandingRepository.getBranding()

                val url = when (urlSettingKey()) {
                    "branding.terms_url" -> branding.termsUrl
                    "branding.privacy_url" -> branding.privacyUrl
                    else -> branding.websiteUrl
                }

                // Concise summary (URL-based approach — no giant text to maintain)
                body.text = buildString {
                    append("To view the latest version of our ")
                    append(screenTitle().lowercase())
                    append(", please visit:\n\n")
                    append(url)
                    append("\n\n")
                    append("We recommend keeping this document for your records.")
                }

                // Open the URL when tapped
                body.setOnClickListener {
                    try {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@LegalTextActivity,
                            "No browser available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                body.text = "Unable to load content. Please try again later."
            }
        }
    }
}