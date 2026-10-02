package com.mkulimafeeds.presentation.customer.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.R
import com.mkulimafeeds.domain.model.CustomerOrder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyOrdersAdapter(
    private var orders: List<CustomerOrder>,
    private val onOrderClick: (CustomerOrder) -> Unit
) : RecyclerView.Adapter<MyOrdersAdapter.OrderViewHolder>() {

    private val moneyFormat =
        DecimalFormat("#,##0.00")

    class OrderViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

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

        val view =
            LayoutInflater.from(parent.context)
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

        val order =
            orders[position]

        holder.tvOrderId.text =
            "#MF-${order.id}"

        holder.tvOrderDate.text =
            formatDate(order.createdAt)

        holder.tvItems.text =
            "${order.items.size} " +
                    if (order.items.size == 1) {
                        "item"
                    } else {
                        "items"
                    }

        holder.tvTotal.text =
            "KES ${moneyFormat.format(order.totalAmount)}"

        holder.tvStatus.text =
            "● ${formatStatus(order.status)}"

        applyStatusStyle(
            holder,
            order.status.name
        )

        holder.card.setOnClickListener {
            onOrderClick(order)
        }
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        return try {

            SimpleDateFormat(
                "MMM dd, yyyy",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (e: Exception) {

            "Date unavailable"
        }
    }

    private fun formatStatus(
        status: com.mkulimafeeds.domain.model.CustomerOrderStatus
    ): String {

        return when (status) {

            com.mkulimafeeds.domain.model.CustomerOrderStatus.PENDING ->
                "Pending"

            com.mkulimafeeds.domain.model.CustomerOrderStatus.CONFIRMED ->
                "Confirmed"

            com.mkulimafeeds.domain.model.CustomerOrderStatus.PROCESSING ->
                "Processing"

            com.mkulimafeeds.domain.model.CustomerOrderStatus.READY ->
                "Ready"

            com.mkulimafeeds.domain.model.CustomerOrderStatus.COMPLETED ->
                "Completed"

            com.mkulimafeeds.domain.model.CustomerOrderStatus.CANCELLED ->
                "Cancelled"

            com.mkulimafeeds.domain.model.CustomerOrderStatus.UNKNOWN ->
                "Unavailable"
        }
    }

    private fun applyStatusStyle(
        holder: OrderViewHolder,
        status: String
    ) {

        val context =
            holder.itemView.context

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

            "CANCELLED",
            "UNKNOWN" -> {

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
        newOrders: List<CustomerOrder>
    ) {

        orders = newOrders
        notifyDataSetChanged()
    }
}