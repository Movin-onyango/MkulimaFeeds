package com.mkulimafeeds

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.data.remote.RoleAuditResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditLogAdapter(
    private var entries: List<RoleAuditResponse>
) : RecyclerView.Adapter<AuditLogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val tvOldRole: TextView = view.findViewById(R.id.tvOldRole)
        val tvNewRole: TextView = view.findViewById(R.id.tvNewRole)
        val tvChangedBy: TextView = view.findViewById(R.id.tvChangedBy)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audit_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.tvUserName.text = entry.userName
        holder.tvTimestamp.text = formatTimestamp(entry.createdAt)
        holder.tvOldRole.text = entry.oldRole.uppercase()
        holder.tvNewRole.text = entry.newRole.uppercase()

        // Style new role based on direction
        val (bgColor, textColor) = styleForRole(entry.newRole)
        holder.tvNewRole.setBackgroundColor(bgColor)
        holder.tvNewRole.setTextColor(textColor)

        holder.tvChangedBy.text = "By ${entry.changedByName}"

        if (entry.reason.isNullOrBlank()) {
            holder.tvReason.visibility = View.GONE
        } else {
            holder.tvReason.visibility = View.VISIBLE
            holder.tvReason.text = entry.reason
        }
    }

    override fun getItemCount(): Int = entries.size

    fun submitList(newEntries: List<RoleAuditResponse>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    private fun styleForRole(role: String): Pair<Int, Int> = when (role.uppercase()) {
        "ADMIN" -> Color.parseColor("#0052FF") to Color.WHITE
        "STAFF" -> Color.parseColor("#EBF2FF") to Color.parseColor("#0052FF")
        "DEALER" -> Color.parseColor("#E8F5E9") to Color.parseColor("#1E8E3E")
        "CUSTOMER" -> Color.parseColor("#F3F4F6") to Color.parseColor("#757575")
        else -> Color.parseColor("#F3F4F6") to Color.parseColor("#757575")
    }

    private fun formatTimestamp(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMs
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(epochMs))
        }
    }
}