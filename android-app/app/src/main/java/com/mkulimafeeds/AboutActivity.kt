package com.mkulimafeeds

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mkulimafeeds.data.repository.BrandingRepository
import kotlinx.coroutines.launch

class AboutActivity : AppCompatActivity() {

    private val brandingRepository by lazy { BrandingRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        val rootView = findViewById<View>(R.id.aboutRoot)
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

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadBranding()
    }

    private fun loadBranding() {
        lifecycleScope.launch {
            val branding = brandingRepository.getBranding()

            findViewById<TextView>(R.id.tvAppName).text = branding.appName
            findViewById<TextView>(R.id.tvVersion).text = "Version ${branding.versionLabel}"
            findViewById<TextView>(R.id.tvAboutText).text = branding.aboutText
            findViewById<TextView>(R.id.tvSupportEmail).text = branding.supportEmail
            findViewById<TextView>(R.id.tvSupportPhone).text = branding.supportPhone
            findViewById<TextView>(R.id.tvCompanyName).text = "© ${branding.companyName}"
        }
    }
}