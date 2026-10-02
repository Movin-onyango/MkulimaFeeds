package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AdminOrderItemAdapter(
    private var items: List<OrderItem>
) : RecyclerView.Adapter<AdminOrderItemAdapter.OrderItemViewHolder>() {

    class OrderItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvItemName: TextView =
            view.findViewById(R.id.tvItemName)

        val tvItemQty: TextView =
            view.findViewById(R.id.tvItemQty)

        val tvItemPrice: TextView =
            view.findViewById(R.id.tvItemPrice)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderItemViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_admin_order_item,
                parent,
                false
            )

        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderItemViewHolder,
        position: Int
    ) {
        val item = items[position]

        holder.tvItemName.text = item.name

        holder.tvItemQty.text = "Qty: ${item.qty}"

        holder.tvItemPrice.text = String.format(
            Locale.getDefault(),
            "KES %,.2f",
            item.subtotal
        )
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<OrderItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}