package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DealerSelectionAdapter(
    private val dealers: List<Dealer>,
    private val onDealerSelected: (Dealer) -> Unit
) : RecyclerView.Adapter<DealerSelectionAdapter.DealerViewHolder>() {

    class DealerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDealerName)
        val tvLocation: TextView = view.findViewById(R.id.tvDealerLocation)
        val tvRating: TextView = view.findViewById(R.id.tvDealerRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DealerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dealer_selection, parent, false)
        return DealerViewHolder(view)
    }

    override fun onBindViewHolder(holder: DealerViewHolder, position: Int) {
        val dealer = dealers[position]
        holder.tvName.text = dealer.name
        holder.tvLocation.text = dealer.location
        holder.tvRating.text = "${dealer.rating} ★"

        holder.itemView.setOnClickListener {
            onDealerSelected(dealer)
        }
    }

    override fun getItemCount() = dealers.size
}
