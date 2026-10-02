package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.remote.DealerApplicationResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DealerApplicationAdapter(
    private var applications: List<DealerApplicationResponse>,
    private val onApprove: (DealerApplicationResponse) -> Unit,
    private val onReject: (DealerApplicationResponse) -> Unit
) : RecyclerView.Adapter<DealerApplicationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvApplicantName)
        val tvEmail: TextView = view.findViewById(R.id.tvApplicantEmail)
        val tvPhone: TextView = view.findViewById(R.id.tvApplicantPhone)
        val tvAppliedOn: TextView = view.findViewById(R.id.tvAppliedOn)
        val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dealer_application, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val application = applications[position]

        holder.tvName.text = application.name
        holder.tvEmail.text = application.email.ifBlank { "No email" }
        holder.tvPhone.text = application.phone.ifBlank { "No phone" }
        holder.tvAppliedOn.text = "Applied: ${formatDate(application.createdAt)}"

        holder.btnApprove.setOnClickListener { onApprove(application) }
        holder.btnReject.setOnClickListener { onReject(application) }
    }

    override fun getItemCount(): Int = applications.size

    fun submitList(newApplications: List<DealerApplicationResponse>) {
        applications = newApplications
        notifyDataSetChanged()
    }

    private fun formatDate(epochMs: Long): String =
        try {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(epochMs))
        } catch (_: Exception) {
            "—"
        }
}