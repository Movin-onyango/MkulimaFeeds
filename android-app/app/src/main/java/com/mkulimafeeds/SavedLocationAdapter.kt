package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.data.remote.SavedLocationResponse

class SavedLocationAdapter(
    private var locations: List<SavedLocationResponse>,
    private val onEditClick: (SavedLocationResponse) -> Unit,
    private val onDeleteClick: (SavedLocationResponse) -> Unit
) : RecyclerView.Adapter<SavedLocationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvDefaultBadge: TextView = view.findViewById(R.id.tvDefaultBadge)
        val btnEdit: TextView = view.findViewById(R.id.btnEdit)
        val btnDelete: TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = locations[position]

        holder.tvLabel.text = location.label
        holder.tvAddress.text = location.address

        holder.tvDefaultBadge.visibility =
            if (location.isDefault) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener { onEditClick(location) }
        holder.btnDelete.setOnClickListener { onDeleteClick(location) }
    }

    override fun getItemCount(): Int = locations.size

    fun submitList(newLocations: List<SavedLocationResponse>) {
        locations = newLocations
        notifyDataSetChanged()
    }
}