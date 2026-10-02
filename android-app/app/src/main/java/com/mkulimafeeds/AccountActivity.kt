package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mkulimafeeds.account.AdminAccountActivity
import com.mkulimafeeds.account.CustomerAccountActivity
import com.mkulimafeeds.account.DealerAccountActivity
import com.mkulimafeeds.account.StaffAccountActivity
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(applicationContext)
        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {
            bounceToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val response = UserProfileRepository(NetworkModule.apiService)
                    .getMyProfile(token)

                val destination = when (response.user.role.trim().uppercase()) {
                    "ADMIN"    -> AdminAccountActivity::class.java
                    "STAFF"    -> StaffAccountActivity::class.java
                    "DEALER"   -> DealerAccountActivity::class.java
                    "CUSTOMER" -> CustomerAccountActivity::class.java
                    else -> {
                        // Unknown role — don't guess. Bounce to login.
                        Log.w("AccountActivity", "Unknown role: ${response.user.role}")
                        bounceToLogin()
                        return@launch
                    }
                }

                startActivity(Intent(this@AccountActivity, destination))
                finish()

            } catch (e: Exception) {
                // Don't assume customer. If we can't determine the role,
                // the token is bad or the network failed — go to login.
                Log.e("AccountActivity", "Failed to resolve role", e)
                bounceToLogin()
            }
        }
    }

    private fun bounceToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}