package com.mkulimafeeds

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch

class DealerSettingsActivity : AppCompatActivity() {


    private lateinit var switchOrderNotifications: MaterialSwitch
    private lateinit var switchStatusNotifications: MaterialSwitch

    private val preferences by lazy {
        getSharedPreferences(
            "dealer_settings",
            MODE_PRIVATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_dealer_settings
        )

        setupViews()
        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupViews() {

        switchOrderNotifications =
            findViewById(
                R.id.switchOrderNotifications
            )

        switchStatusNotifications =
            findViewById(
                R.id.switchStatusNotifications
            )
    }

    private fun setupToolbar() {

        findViewById<MaterialToolbar>(
            R.id.toolbarDealerSettings
        ).setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {

        switchOrderNotifications.isChecked =
            preferences.getBoolean(
                KEY_ORDER_NOTIFICATIONS,
                true
            )

        switchStatusNotifications.isChecked =
            preferences.getBoolean(
                KEY_STATUS_NOTIFICATIONS,
                true
            )
    }

    private fun setupListeners() {

        switchOrderNotifications.setOnCheckedChangeListener { _: CompoundButton, enabled ->

            preferences.edit()
                .putBoolean(
                    KEY_ORDER_NOTIFICATIONS,
                    enabled
                )
                .apply()

            showSavedMessage()
        }

        switchStatusNotifications.setOnCheckedChangeListener { _: CompoundButton, enabled ->

            preferences.edit()
                .putBoolean(
                    KEY_STATUS_NOTIFICATIONS,
                    enabled
                )
                .apply()

            showSavedMessage()
        }
    }

    private fun showSavedMessage() {

        Toast.makeText(
            this,
            "Setting saved",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {

        private const val KEY_ORDER_NOTIFICATIONS =
            "order_notifications"

        private const val KEY_STATUS_NOTIFICATIONS =
            "status_notifications"
    }


}
