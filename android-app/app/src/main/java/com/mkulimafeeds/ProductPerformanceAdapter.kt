package com.mkulimafeeds

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ProductPerformanceAdapter(
    private var metrics: List<ProductMetric>
) : RecyclerView.Adapter<ProductPerformanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvName: TextView =
            view.findViewById(R.id.tvProductName)

        val tvOrders: TextView =
            view.findViewById(R.id.tvOrders)

        val tvRevenue: TextView =
            view.findViewById(R.id.tvRevenue)

        val tvGrowth: TextView =
            view.findViewById(R.id.tvGrowth)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_performance_row,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = metrics[position]

        holder.tvName.text =
            item.name

        holder.tvOrders.text =
            item.orders.toString()

        holder.tvRevenue.text =
            String.format(
                Locale.getDefault(),
                "KES %,.0f",
                item.revenue
            )

        val growthText =
            String.format(
                Locale.getDefault(),
                "%+.1f%%",
                item.growth
            )

        holder.tvGrowth.text =
            growthText

        holder.tvGrowth.setTextColor(
            when {
                item.growth > 0 -> Color.rgb(30, 142, 62)
                item.growth < 0 -> Color.rgb(185, 28, 28)
                else -> Color.rgb(117, 117, 117)
            }
        )
    }

    override fun getItemCount(): Int =
        metrics.size

    /**
     * Replace the current list with a new one.
     * Call this instead of re-instantiating the adapter.
     */
    fun submitList(
        newMetrics: List<ProductMetric>
    ) {
        metrics = newMetrics
        notifyDataSetChanged()
    }
}