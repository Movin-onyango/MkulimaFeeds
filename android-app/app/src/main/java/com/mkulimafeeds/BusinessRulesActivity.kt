package com.mkulimafeeds

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.SettingResponse
import com.mkulimafeeds.data.remote.SettingUpdate
import com.mkulimafeeds.data.repository.BusinessRulesRepository
import kotlinx.coroutines.launch

class BusinessRulesActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { BusinessRulesRepository() }

    /**
     * Track original and current values for dirty detection.
     */
    private val originalValues = mutableMapOf<String, String>()
    private val currentInputs = mutableMapOf<String, () -> String>()

    private lateinit var settingsContainer: LinearLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var loadingOverlay: View
    private lateinit var tvSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_business_rules)

        val rootView = findViewById<View>(R.id.businessRulesRoot)
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

        settingsContainer = findViewById(R.id.settingsContainer)
        btnSave = findViewById(R.id.btnSaveRules)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvSubtitle = findViewById(R.id.tvSubtitle)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        btnSave.setOnClickListener { saveChanges() }

        // NEW — reset to defaults
        findViewById<MaterialButton>(R.id.btnResetDefaults)
            .setOnClickListener {
                confirmResetToDefaults()
            }

        loadSettings()
    }

    // =========================================================
    // LOAD
    // =========================================================

    private fun loadSettings() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        showLoading()

        lifecycleScope.launch {
            try {
                val settings = repository.getAllSettings(token)
                renderSettings(settings)
            } catch (e: Exception) {
                toast(e.message ?: "Failed to load settings")
            } finally {
                hideLoading()
            }
        }
    }

    // =========================================================
    // RENDER
    // =========================================================

    private fun renderSettings(settings: List<SettingResponse>) {

        settingsContainer.removeAllViews()
        originalValues.clear()
        currentInputs.clear()

        // Group by category
        val grouped = settings.groupBy { it.category }

        // Category display order
        val categoryOrder = listOf("BULK_ORDER", "DEALER", "GENERAL")
        val categoryLabels = mapOf(
            "BULK_ORDER" to "BULK ORDER RULES",
            "DEALER" to "DEALER RULES",
            "GENERAL" to "GENERAL"
        )

        for (category in categoryOrder) {
            val items = grouped[category] ?: continue
            if (items.isEmpty()) continue

            // Category header
            val header = TextView(this).apply {
                text = categoryLabels[category] ?: category
                setTextColor(0xFF757575.toInt())
                textSize = 11f
                letterSpacing = 0.1f
                setPadding(0, 24, 0, 12)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            settingsContainer.addView(header)

            // Settings in this category
            items.forEach { setting ->
                addSettingRow(setting)
            }
        }
    }

    private fun addSettingRow(setting: SettingResponse) {

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 0f
            strokeWidth = (1 * resources.displayMetrics.density).toInt()
            strokeColor = 0xFFF0F0F0.toInt()
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // Description label
        val label = TextView(this).apply {
            text = setting.description
            setTextColor(0xFF1A1A1A.toInt())
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        inner.addView(label)

        // Key as subtitle
        val keyLabel = TextView(this).apply {
            text = setting.key
            setTextColor(0xFF9CA3AF.toInt())
            textSize = 10f
            setPadding(0, 6, 0, 12)
        }
        inner.addView(keyLabel)

        // Input widget based on valueType
        when (setting.valueType) {
            "BOOLEAN" -> addBooleanInput(inner, setting)
            "ENUM" -> addEnumInput(inner, setting)
            "INT" -> addNumericInput(inner, setting, decimal = false)
            "DECIMAL" -> addNumericInput(inner, setting, decimal = true)
            else -> addTextInput(inner, setting)
        }

        card.addView(inner)
        settingsContainer.addView(card)
    }

    private fun addBooleanInput(
        parent: LinearLayout,
        setting: SettingResponse
    ) {
        val switch = MaterialSwitch(this).apply {
            text = if (setting.value.toBoolean()) "Enabled" else "Disabled"
            isChecked = setting.value.toBooleanStrictOrNull() ?: false
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.text = if (isChecked) "Enabled" else "Disabled"
        }

        parent.addView(switch)

        originalValues[setting.key] = setting.value
        currentInputs[setting.key] = { switch.isChecked.toString() }
    }

    private fun addEnumInput(
        parent: LinearLayout,
        setting: SettingResponse
    ) {
        val options = setting.enumOptions ?: emptyList()
        if (options.isEmpty()) return

        val currentIndex = options.indexOf(setting.value).coerceAtLeast(0)

        val selectedText = TextView(this).apply {
            text = setting.value
            setTextColor(0xFF04261B.toInt())
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4, 0, 4)
            isClickable = true
            isFocusable = true
        }

        selectedText.setOnClickListener {
            val currentOptions = options.toTypedArray()
            val currentSelected = options.indexOf(selectedText.text.toString())
                .coerceAtLeast(0)

            android.app.AlertDialog.Builder(this)
                .setTitle(setting.description)
                .setSingleChoiceItems(currentOptions, currentSelected) { dialog, which ->
                    selectedText.text = options[which]
                    dialog.dismiss()
                }
                .show()
        }

        parent.addView(selectedText)

        originalValues[setting.key] = setting.value
        currentInputs[setting.key] = { selectedText.text.toString() }
    }

    private fun addNumericInput(
        parent: LinearLayout,
        setting: SettingResponse,
        decimal: Boolean
    ) {
        val input = EditText(this).apply {
            setText(setting.value)
            inputType = if (decimal) {
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_NUMBER
            }
            setTextColor(0xFF1A1A1A.toInt())
            textSize = 15f
            setPadding(0, 8, 0, 8)
        }
        parent.addView(input)

        originalValues[setting.key] = setting.value
        currentInputs[setting.key] = { input.text.toString().trim() }
    }

    private fun addTextInput(
        parent: LinearLayout,
        setting: SettingResponse
    ) {
        val input = EditText(this).apply {
            setText(setting.value)
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(0xFF1A1A1A.toInt())
            textSize = 15f
            setPadding(0, 8, 0, 8)
        }
        parent.addView(input)

        originalValues[setting.key] = setting.value
        currentInputs[setting.key] = { input.text.toString().trim() }
    }

    // =========================================================
    // SAVE
    // =========================================================

    private fun saveChanges() {

        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            return
        }

        // Collect only changed values
        val changes = mutableListOf<SettingUpdate>()
        for ((key, getter) in currentInputs) {
            val original = originalValues[key] ?: continue
            val current = getter()
            if (current != original) {
                changes.add(SettingUpdate(key = key, value = current))
            }
        }

        if (changes.isEmpty()) {
            toast("No changes to save")
            return
        }

        showLoading()
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.updateSettings(changes, token)
                toast("${changes.size} rule(s) updated")
                loadSettings()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to save changes")
            } finally {
                hideLoading()
                btnSave.isEnabled = true
            }
        }
    }

    // =========================================================
    // RESET TO DEFAULTS
    // =========================================================

    private fun confirmResetToDefaults() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset to defaults?")
            .setMessage(
                "This will restore all business rules to their factory " +
                        "default values. Any custom values you've set will be " +
                        "overwritten. This cannot be undone."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset") { _, _ ->
                performReset()
            }
            .show()
    }

    private fun performReset() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        showLoading()
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.resetToDefaults(token)
                toast("Settings restored to defaults")
                loadSettings()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to reset settings")
            } finally {
                hideLoading()
                btnSave.isEnabled = true
            }
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}