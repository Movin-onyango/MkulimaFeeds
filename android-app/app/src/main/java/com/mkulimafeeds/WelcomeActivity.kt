package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable full-screen mode
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        // 2. Handle System Bar Padding (prevents content from overlapping with status/navigation bars)
        val rootView = findViewById<android.view.View>(R.id.welcome_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // We only apply bottom padding for the navigation bar area
            // so the green header stays at the very top.
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // 3. Navigation Logic: Get Started -> Login Screen
        val btnGetStarted = findViewById<MaterialButton>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Note: We don't call finish() here so the user can go back
            // from Login to the Welcome screen if they want.
        }

        // Optional: Logic for "Check Product" button if you have an ID for it
        // val btnCheckProduct = findViewById<MaterialButton>(R.id.btnCheckProduct)
        // btnCheckProduct.setOnClickListener {
        //    // Handle check product navigation
        // }
    }
}