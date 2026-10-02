package com.mkulimafeeds

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class DealerPerformanceAdapter(
    private var dealers: List<DealerMetric>
) : RecyclerView.Adapter<DealerPerformanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvRank: TextView =
            view.findViewById(R.id.tvRank)

        val tvName: TextView =
            view.findViewById(R.id.tvDealerName)

        val tvLocation: TextView =
            view.findViewById(R.id.tvDealerLocation)

        val tvRevenue: TextView =
            view.findViewById(R.id.tvDealerRevenue)

        val tvGrowth: TextView =
            view.findViewById(R.id.tvDealerGrowth)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_dealer_rank,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val dealer =
            dealers[position]

        holder.tvRank.text =
            (position + 1).toString()

        holder.tvName.text =
            dealer.name

        holder.tvLocation.text =
            dealer.location.uppercase()

        holder.tvRevenue.text =
            String.format(
                Locale.getDefault(),
                "KES %,.0f",
                dealer.revenue
            )

        holder.tvGrowth.text =
            String.format(
                Locale.getDefault(),
                "%+.1f%%",
                dealer.growth
            )

        holder.tvGrowth.setTextColor(
            when {
                dealer.growth > 0 ->
                    Color.rgb(46, 125, 50)

                dealer.growth < 0 ->
                    Color.rgb(185, 28, 28)

                else ->
                    Color.rgb(117, 117, 117)
            }
        )
    }

    override fun getItemCount(): Int =
        dealers.size

    /**
     * Replace the current list with a new one.
     */
    fun submitList(
        newDealers: List<DealerMetric>
    ) {
        dealers = newDealers
        notifyDataSetChanged()
    }
}