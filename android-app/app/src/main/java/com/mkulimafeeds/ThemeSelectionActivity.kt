package com.mkulimafeeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mkulimafeeds.account.AccountPreferencesManager

class ThemeSelectionActivity : AppCompatActivity() {

    private lateinit var preferencesManager: AccountPreferencesManager
    private lateinit var container: LinearLayout

    data class ThemeOption(
        val value: String,
        val label: String,
        val description: String
    )

    private val options = listOf(
        ThemeOption(
            AccountPreferencesManager.THEME_LIGHT,
            "Light",
            "Always use light theme"
        ),
        ThemeOption(
            AccountPreferencesManager.THEME_DARK,
            "Dark",
            "Always use dark theme"
        ),
        ThemeOption(
            AccountPreferencesManager.THEME_SYSTEM,
            "System default",
            "Follow your device's setting"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_theme_selection)

        preferencesManager = AccountPreferencesManager(applicationContext)

        val rootView = findViewById<View>(R.id.themeSelectionRoot)
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

        container = findViewById(R.id.themeOptionsContainer)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        renderOptions()
    }

    private fun renderOptions() {
        container.removeAllViews()

        val current = preferencesManager.getTheme()

        options.forEach { option ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_theme_option, container, false)

            view.findViewById<TextView>(R.id.tvThemeLabel).text = option.label
            view.findViewById<TextView>(R.id.tvThemeDescription).text = option.description

            val selected = view.findViewById<ImageView>(R.id.ivSelected)
            selected.visibility =
                if (option.value == current) View.VISIBLE else View.GONE

            view.setOnClickListener {
                preferencesManager.setTheme(option.value)
                Toast.makeText(
                    this,
                    "${option.label} theme saved",
                    Toast.LENGTH_SHORT
                ).show()
                renderOptions()
                applyThemeIfPossible()
            }

            container.addView(view)
        }
    }

    /**
     * Applies the theme to this activity immediately.
     * Full app-wide theme switching comes in a future pass —
     * for now this shows the preference is stored and works.
     */
    private fun applyThemeIfPossible() {
        val theme = preferencesManager.getTheme()
        val mode = when (theme) {
            AccountPreferencesManager.THEME_LIGHT ->
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            AccountPreferencesManager.THEME_DARK ->
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else ->
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
        recreate()
    }
}