package com.mkulimafeeds.presentation.dealer.orders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.R
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DealerAssignedOrdersAdapter(
    private var orders: List<DealerOrder>,
    private val onOrderClick: (DealerOrder) -> Unit
) : RecyclerView.Adapter<DealerAssignedOrdersAdapter.OrderViewHolder>() {

    fun submitList(newOrders: List<DealerOrder>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_dealer_assigned_order,
                parent,
                false
            )

        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int
    ) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int =
        orders.size

    inner class OrderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderId =
            itemView.findViewById<TextView>(R.id.tvOrderId)

        private val tvCustomerName =
            itemView.findViewById<TextView>(R.id.tvCustomerName)

        private val tvStatus =
            itemView.findViewById<TextView>(R.id.tvStatus)

        private val tvLocation =
            itemView.findViewById<TextView>(R.id.tvLocation)

        private val tvNeededDate =
            itemView.findViewById<TextView>(R.id.tvNeededDate)

        private val tvTotalAmount =
            itemView.findViewById<TextView>(R.id.tvTotalAmount)

        fun bind(order: DealerOrder) {

            tvOrderId.text =
                "#${order.id}"

            tvCustomerName.text =
                order.customerName

            tvStatus.text =
                formatStatus(order.status)

            tvLocation.text =
                "📍 ${order.location}"

            tvNeededDate.text =
                "Needed: ${formatDate(order.neededDate)}"

            tvTotalAmount.text =
                "KES ${order.totalAmount.toPlainString()}"

            applyStatusStyle(order.status)

            itemView.setOnClickListener {
                onOrderClick(order)
            }
        }

        private fun applyStatusStyle(
            status: DealerOrderStatus
        ) {
            when (status) {

                DealerOrderStatus.CONFIRMED -> {
                    tvStatus.setBackgroundColor(
                        Color.parseColor("#DBEAFE")
                    )
                    tvStatus.setTextColor(
                        Color.parseColor("#1D4ED8")
                    )
                }

                DealerOrderStatus.PROCESSING -> {
                    tvStatus.setBackgroundColor(
                        Color.parseColor("#FEF3C7")
                    )
                    tvStatus.setTextColor(
                        Color.parseColor("#92400E")
                    )
                }

                DealerOrderStatus.READY -> {
                    tvStatus.setBackgroundColor(
                        Color.parseColor("#D1FAE5")
                    )
                    tvStatus.setTextColor(
                        Color.parseColor("#047857")
                    )
                }

                DealerOrderStatus.COMPLETED -> {
                    tvStatus.setBackgroundColor(
                        Color.parseColor("#DCFCE7")
                    )
                    tvStatus.setTextColor(
                        Color.parseColor("#166534")
                    )
                }

                DealerOrderStatus.PENDING -> {
                    tvStatus.setBackgroundColor(
                        Color.parseColor("#F3F4F6")
                    )
                    tvStatus.setTextColor(
                        Color.parseColor("#4B5563")
                    )
                }

                DealerOrderStatus.CANCELLED -> {
                    tvStatus.setBackgroundColor(
                        Color.parseColor("#FEE2E2")
                    )
                    tvStatus.setTextColor(
                        Color.parseColor("#B91C1C")
                    )
                }
            }
        }

        private fun formatStatus(
            status: DealerOrderStatus
        ): String =
            status.name
                .lowercase(Locale.getDefault())
                .replaceFirstChar {
                    it.uppercase()
                }

        private fun formatDate(
            timestamp: Long
        ): String =
            try {
                val formatter =
                    SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                    )

                formatter.format(
                    Date(timestamp)
                )
            } catch (e: Exception) {
                "Date unavailable"
            }
    }
}