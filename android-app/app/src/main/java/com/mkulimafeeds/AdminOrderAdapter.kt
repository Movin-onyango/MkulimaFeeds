package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AdminOrderAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvOrderId: TextView =
            view.findViewById(R.id.tvOrderId)

        val tvStatusBadge: TextView =
            view.findViewById(R.id.tvStatusBadge)

        val tvPrice: TextView =
            view.findViewById(R.id.tvPrice)

        val tvCustomerName: TextView =
            view.findViewById(R.id.tvCustomerName)

        val tvOrderDate: TextView =
            view.findViewById(R.id.tvOrderDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_admin_order_card,
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
            "#ORD-${order.id}"

        holder.tvCustomerName.text =
            order.customerName

        holder.tvOrderDate.text =
            "${order.date} • ${order.items.size} items"

        holder.tvPrice.text =
            String.format(
                Locale.getDefault(),
                "KSh %,.0f",
                order.totalAmount
            )

        holder.tvStatusBadge.text =
            order.status

        applyStatusStyle(
            holder,
            order.status
        )

        holder.itemView.setOnClickListener {
            onOrderClick(order)
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

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_pending
                    )

                holder.tvStatusBadge
                    .setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_grey
                        )
                    )
            }

            "CONFIRMED" -> {

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_badge_light_blue
                    )

                holder.tvStatusBadge
                    .setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.brand_blue
                        )
                    )
            }

            "PROCESSING" -> {

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_badge_light_blue
                    )

                holder.tvStatusBadge
                    .setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.brand_blue
                        )
                    )
            }

            "READY" -> {

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_in_stock
                    )

                holder.tvStatusBadge
                    .setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.brand_dark_green
                        )
                    )
            }

            "COMPLETED" -> {

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_in_stock
                    )

                holder.tvStatusBadge
                    .setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.brand_dark_green
                        )
                    )
            }

            "CANCELLED" -> {

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_pending
                    )

                holder.tvStatusBadge
                    .setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_grey
                        )
                    )
            }

            else -> {

                holder.tvStatusBadge
                    .setBackgroundResource(
                        R.drawable.bg_status_pending
                    )

                holder.tvStatusBadge
                    .setTextColor(
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
        newList: List<Order>
    ) {

        orders = newList
        notifyDataSetChanged()
    }
}