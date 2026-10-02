package com.mkulimafeeds

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.SavedLocationResponse
import com.mkulimafeeds.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

class SavedLocationsActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { UserProfileRepository() }

    private lateinit var rvLocations: RecyclerView
    private lateinit var adapter: SavedLocationAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_locations)

        val rootView = findViewById<View>(R.id.savedLocationsRoot)
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

        initializeViews()
        setupRecyclerView()
        setupToolbar()
        setupFab()
        loadLocations()
    }

    private fun initializeViews() {
        rvLocations = findViewById(R.id.rvLocations)
        emptyState = findViewById(R.id.emptyState)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    private fun setupRecyclerView() {
        adapter = SavedLocationAdapter(
            locations = emptyList(),
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
        rvLocations.layoutManager = LinearLayoutManager(this)
        rvLocations.adapter = adapter
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fabAddLocation).setOnClickListener {
            showAddDialog()
        }
    }

    // =========================================================
    // LOADING
    // =========================================================

    private fun loadLocations() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        showLoading()

        lifecycleScope.launch {
            try {
                val locations = repository.getSavedLocations(token)
                adapter.submitList(locations)
                toggleEmptyState(locations.isEmpty())
            } catch (e: Exception) {
                toast(e.message ?: "Failed to load locations")
            } finally {
                hideLoading()
            }
        }
    }

    // =========================================================
    // ADD / EDIT DIALOG
    // =========================================================

    private fun showAddDialog() {
        showFormDialog(existing = null)
    }

    private fun showEditDialog(location: SavedLocationResponse) {
        showFormDialog(existing = location)
    }

    private fun showFormDialog(existing: SavedLocationResponse?) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val etLabel = EditText(this).apply {
            hint = "Label (e.g., Home, Farm, Office)"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(existing?.label ?: "")
            maxLines = 1
        }

        val etAddress = EditText(this).apply {
            hint = "Address (e.g., Ngong Road, Nairobi)"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(existing?.address ?: "")
            minLines = 2
        }

        val cbDefault = CheckBox(this).apply {
            text = "Set as default delivery address"
            isChecked = existing?.isDefault ?: false
        }

        container.addView(etLabel)
        container.addView(etAddress)
        container.addView(cbDefault)

        val title = if (existing == null) "Add Location" else "Edit Location"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val label = etLabel.text.toString().trim()
                val address = etAddress.text.toString().trim()
                val isDefault = cbDefault.isChecked

                if (label.isBlank() || address.isBlank()) {
                    toast("Label and address are required")
                    return@setPositiveButton
                }

                if (existing == null) {
                    createLocation(label, address, isDefault)
                } else {
                    updateLocation(existing.id, label, address, isDefault)
                }
            }
            .show()
    }

    private fun createLocation(
        label: String,
        address: String,
        isDefault: Boolean
    ) {
        val token = tokenManager.getToken() ?: return

        showLoading()

        lifecycleScope.launch {
            try {
                repository.createSavedLocation(
                    label = label,
                    address = address,
                    isDefault = isDefault,
                    token = token
                )
                toast("Location added")
                loadLocations()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to add location")
                hideLoading()
            }
        }
    }

    private fun updateLocation(
        id: Long,
        label: String,
        address: String,
        isDefault: Boolean
    ) {
        val token = tokenManager.getToken() ?: return

        showLoading()

        lifecycleScope.launch {
            try {
                repository.updateSavedLocation(
                    id = id,
                    label = label,
                    address = address,
                    isDefault = isDefault,
                    token = token
                )
                toast("Location updated")
                loadLocations()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to update location")
                hideLoading()
            }
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    private fun confirmDelete(location: SavedLocationResponse) {
        AlertDialog.Builder(this)
            .setTitle("Delete Location")
            .setMessage("Delete \"${location.label}\"? This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteLocation(location.id)
            }
            .show()
    }

    private fun deleteLocation(id: Long) {
        val token = tokenManager.getToken() ?: return

        showLoading()

        lifecycleScope.launch {
            try {
                repository.deleteSavedLocation(id, token)
                toast("Location deleted")
                loadLocations()
            } catch (e: Exception) {
                toast(e.message ?: "Failed to delete location")
                hideLoading()
            }
        }
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyState.visibility = View.VISIBLE
            rvLocations.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvLocations.visibility = View.VISIBLE
        }
    }

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