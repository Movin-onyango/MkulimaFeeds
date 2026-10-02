package com.mkulimafeeds.presentation.dealer.customers

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

class DealerCustomerOrderAdapter(
    private var orders: List<DealerOrder>,
    private val onOrderClicked: (DealerOrder) -> Unit
) : RecyclerView.Adapter<DealerCustomerOrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val tvOrderId: TextView =
            view.findViewById(R.id.tvOrderId)

        val tvOrderDate: TextView =
            view.findViewById(R.id.tvOrderDate)

        val tvOrderStatus: TextView =
            view.findViewById(R.id.tvOrderStatus)

        val tvOrderTotal: TextView =
            view.findViewById(R.id.tvOrderTotal)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_dealer_customer_order,
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
            "Order #${order.id}"

        holder.tvOrderDate.text =
            formatDate(order.createdAt)

        holder.tvOrderStatus.text =
            formatStatus(order.status)

        holder.tvOrderTotal.text =
            "KES ${order.totalAmount.toPlainString()}"

        holder.itemView.setOnClickListener {
            onOrderClicked(order)
        }
    }

    override fun getItemCount(): Int =
        orders.size

    fun updateList(
        newOrders: List<DealerOrder>
    ) {
        orders = newOrders
        notifyDataSetChanged()
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        return try {

            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            ).format(Date(timestamp))

        } catch (e: Exception) {

            "Date unavailable"
        }
    }

    private fun formatStatus(
        status: DealerOrderStatus
    ): String {

        return status.name
            .lowercase(Locale.getDefault())
            .replaceFirstChar {
                it.uppercase()
            }
    }
}