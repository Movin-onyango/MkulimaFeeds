package com.mkulimafeeds

import android.os.Bundle
import android.view.View
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
import com.mkulimafeeds.data.repository.RoleRepository
import kotlinx.coroutines.launch

class AuditLogActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { RoleRepository() }

    private lateinit var rvAuditLog: RecyclerView
    private lateinit var adapter: AuditLogAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_audit_log)

        val rootView = findViewById<View>(R.id.auditLogRoot)
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

        rvAuditLog = findViewById(R.id.rvAuditLog)
        emptyState = findViewById(R.id.emptyState)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        adapter = AuditLogAdapter(emptyList())
        rvAuditLog.layoutManager = LinearLayoutManager(this)
        rvAuditLog.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadAuditLog()
    }

    private fun loadAuditLog() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) {
            toast("Session expired. Please log in.")
            finish()
            return
        }

        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val entries = repository.getAuditLog(token, limit = 200)
                adapter.submitList(entries)
                emptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
                rvAuditLog.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                toast(e.message ?: "Failed to load audit log")
            } finally {
                loadingOverlay.visibility = View.GONE
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}