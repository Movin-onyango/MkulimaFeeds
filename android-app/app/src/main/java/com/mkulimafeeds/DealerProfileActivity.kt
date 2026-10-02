package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import kotlinx.coroutines.launch

class DealerProfileActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    private lateinit var tvDealerName: TextView
    private lateinit var tvDealerId: TextView
    private lateinit var tvDealerRole: TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_dealer_profile
        )

        tokenManager =
            TokenManager(this)

        setupViews()
        setupProfile()
        setupToolbar()
        setupSettings()
        setupChangePassword()
        setupLogout()
    }

    private fun setupViews() {

        tvDealerName =
            findViewById(
                R.id.tvDealerName
            )

        tvDealerId =
            findViewById(
                R.id.tvDealerId
            )

        tvDealerRole =
            findViewById(
                R.id.tvDealerRole
            )
    }

    private fun setupProfile() {

        tvDealerName.text =
            "Loading..."

        tvDealerId.text =
            "User ID: Loading..."

        tvDealerRole.text =
            "DEALER"

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            tvDealerName.text =
                "Dealer"

            tvDealerId.text =
                "User ID: N/A"

            return
        }

        lifecycleScope.launch {

            try {

                val response =
                    NetworkModule.apiService
                        .getUserProfile(
                            token
                        )

                val user =
                    response.user

                tvDealerName.text =
                    user.name.ifBlank {
                        "Dealer"
                    }

                tvDealerId.text =
                    "User ID: ${user.id}"

                tvDealerRole.text =
                    user.role
                        .trim()
                        .uppercase()

            } catch (e: Exception) {

                e.printStackTrace()

                tvDealerName.text =
                    "Dealer"

                tvDealerId.text =
                    "User ID: N/A"

                tvDealerRole.text =
                    "DEALER"

                Toast.makeText(
                    this@DealerProfileActivity,
                    "Failed to load profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupToolbar() {

        findViewById<MaterialToolbar>(
            R.id.toolbarDealerProfile
        ).setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSettings() {

        findViewById<MaterialButton>(
            R.id.btnDealerSettings
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    DealerSettingsActivity::class.java
                )
            )
        }
    }

    private fun setupChangePassword() {

        findViewById<MaterialButton>(
            R.id.btnChangePassword
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ChangePasswordActivity::class.java
                )
            )
        }
    }

    private fun setupLogout() {

        findViewById<MaterialButton>(
            R.id.btnDealerLogout
        ).setOnClickListener {

            tokenManager.clearToken()

            val intent =
                Intent(
                    this,
                    WelcomeActivity::class.java
                ).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

            startActivity(intent)

            finish()
        }
    }
}