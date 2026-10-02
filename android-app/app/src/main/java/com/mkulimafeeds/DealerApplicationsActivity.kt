package com.mkulimafeeds

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
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
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.DealerApplicationResponse
import com.mkulimafeeds.data.repository.RoleRepository
import kotlinx.coroutines.launch

class DealerApplicationsActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { RoleRepository() }

    private lateinit var rvApplications: RecyclerView
    private lateinit var adapter: DealerApplicationAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dealer_applications)

        val rootView = findViewById<View>(R.id.dealerAppsRoot)
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

        rvApplications = findViewById(R.id.rvApplications)
        emptyState = findViewById(R.id.emptyState)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        adapter = DealerApplicationAdapter(
            applications = emptyList(),
            onApprove = { confirmApprove(it) },
            onReject = { confirmReject(it) }
        )
        rvApplications.layoutManager = LinearLayoutManager(this)
        rvApplications.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadApplications()
    }

    private fun loadApplications() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val applications = repository.getDealerApplications(token)
                adapter.submitList(applications)
                emptyState.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
                rvApplications.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                toast(e.message ?: "Failed to load applications")
            } finally {
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    // =========================================================
    // APPROVE
    // =========================================================

    private fun confirmApprove(application: DealerApplicationResponse) {
        val input = EditText(this).apply {
            hint = "Optional note (e.g., approved for Nairobi region)"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 8)
        }

        AlertDialog.Builder(this)
            .setTitle("Approve ${application.name}?")
            .setMessage("This will grant dealer access: bulk ordering and dealer features.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Approve") { _, _ ->
                performApprove(application.id, input.text.toString().trim().ifBlank { null })
            }
            .show()
    }

    private fun performApprove(userId: Long, reason: String?) {
        val token = tokenManager.getToken() ?: return
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                repository.approveDealerApplication(userId, reason, token)
                toast("Application approved")
                loadApplications()
            } catch (e: Exception) {
                toast(e.message ?: "Approval failed")
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    // =========================================================
    // REJECT
    // =========================================================

    private fun confirmReject(application: DealerApplicationResponse) {
        val input = EditText(this).apply {
            hint = "Reason for rejection (required)"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 8)
        }

        AlertDialog.Builder(this)
            .setTitle("Reject ${application.name}?")
            .setMessage("The applicant will be notified.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isBlank()) {
                    toast("Reason is required")
                    return@setPositiveButton
                }
                performReject(application.id, reason)
            }
            .show()
    }

    private fun performReject(userId: Long, reason: String) {
        val token = tokenManager.getToken() ?: return
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                repository.rejectDealerApplication(userId, reason, token)
                toast("Application rejected")
                loadApplications()
            } catch (e: Exception) {
                toast(e.message ?: "Rejection failed")
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}