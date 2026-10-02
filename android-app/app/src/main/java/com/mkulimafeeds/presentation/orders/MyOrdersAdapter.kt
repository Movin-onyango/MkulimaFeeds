package com.mkulimafeeds.presentation.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.Order
import com.mkulimafeeds.R
import java.util.Locale

class MyOrdersAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<MyOrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val card: MaterialCardView =
            view as MaterialCardView

        val tvOrderId: TextView =
            view.findViewById(R.id.tvOrderId)

        val tvStatus: TextView =
            view.findViewById(R.id.tvStatus)

        val tvOrderDate: TextView =
            view.findViewById(R.id.tvOrderDate)

        val tvItems: TextView =
            view.findViewById(R.id.tvItems)

        val tvTotal: TextView =
            view.findViewById(R.id.tvTotal)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_my_order,
                parent,
                false
            )

        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int
    ) {

        val order = orders[position]

        holder.tvOrderId.text =
            "#MF-${order.id}"

        holder.tvOrderDate.text =
            order.date

        holder.tvItems.text =
            "${order.items.size} item" +
                    if (order.items.size == 1) "" else "s"

        holder.tvTotal.text =
            String.format(
                Locale.getDefault(),
                "KES %,.2f",
                order.totalAmount
            )

        holder.tvStatus.text =
            "● ${formatStatus(order.status)}"

        applyStatusStyle(holder, order.status)

        holder.card.setOnClickListener {
            onOrderClick(order)
        }
    }

    private fun formatStatus(status: String): String {
        return status
            .lowercase(Locale.getDefault())
            .replaceFirstChar {
                it.uppercase()
            }
    }

    private fun applyStatusStyle(
        holder: OrderViewHolder,
        status: String
    ) {

        val context = holder.itemView.context

        when (status.uppercase()) {

            "PENDING" -> {
                holder.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_pending
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.text_grey
                    )
                )
            }

            "CONFIRMED",
            "PROCESSING" -> {
                holder.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_badge_light_blue
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.brand_blue
                    )
                )
            }

            "READY",
            "COMPLETED" -> {
                holder.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_in_stock
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.brand_dark_green
                    )
                )
            }

            "CANCELLED" -> {
                holder.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_pending
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.text_grey
                    )
                )
            }

            else -> {
                holder.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_pending
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.text_grey
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int =
        orders.size

    fun updateList(
        newOrders: List<Order>
    ) {

        orders = newOrders
        notifyDataSetChanged()
    }
}