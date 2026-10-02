package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.data.remote.TopCustomer
import java.util.Locale

/**
 * Adapter for the top-customers list.
 *
 * Supports two sort modes:
 *  - BY_ORDERS  → primary value is total orders
 *  - BY_REVENUE → primary value is total revenue
 *
 * The secondary value is always the other metric.
 */
class TopCustomerAdapter(
    private var customers: List<TopCustomer>,
    private val sortMode: SortMode
) : RecyclerView.Adapter<TopCustomerAdapter.ViewHolder>() {

    enum class SortMode {
        BY_ORDERS,
        BY_REVENUE
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvCustomerRank)
        val tvName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvPhone: TextView = view.findViewById(R.id.tvCustomerPhone)
        val tvPrimary: TextView = view.findViewById(R.id.tvCustomerPrimaryValue)
        val tvSecondary: TextView = view.findViewById(R.id.tvCustomerSecondaryValue)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_top_customer,
                parent,
                false
            )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val customer = customers[position]

        // Rank
        holder.tvRank.text = (position + 1).toString()

        // Name + phone
        holder.tvName.text = customer.name
        holder.tvPhone.text = customer.phone

        // Format revenue once
        val revenueFormatted = String.format(
            Locale.getDefault(),
            "KES %,.0f",
            customer.totalRevenue.toDoubleOrNull() ?: 0.0
        )

        when (sortMode) {
            SortMode.BY_ORDERS -> {
                holder.tvPrimary.text =
                    "${customer.totalOrders} orders"
                holder.tvSecondary.text = revenueFormatted
            }
            SortMode.BY_REVENUE -> {
                holder.tvPrimary.text = revenueFormatted
                holder.tvSecondary.text =
                    "${customer.totalOrders} orders"
            }
        }
    }

    override fun getItemCount(): Int = customers.size

    /**
     * Replace the current list with a new one.
     */
    fun submitList(newCustomers: List<TopCustomer>) {
        customers = newCustomers
        notifyDataSetChanged()
    }
}