package com.mkulimafeeds.presentation.dealer.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.R
import com.mkulimafeeds.domain.model.dealer.DealerOrderItem
import java.text.DecimalFormat

class AssignedOrderItemAdapter :
    RecyclerView.Adapter<AssignedOrderItemAdapter.OrderItemViewHolder>() {

    private val items = mutableListOf<DealerOrderItem>()

    private val quantityFormat = DecimalFormat("#,##0.##")
    private val moneyFormat = DecimalFormat("#,##0.00")

    inner class OrderItemViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val tvProductName: TextView =
            itemView.findViewById(R.id.tvProductName)

        private val tvProductQuantity: TextView =
            itemView.findViewById(R.id.tvProductQuantity)

        private val tvProductUnitPrice: TextView =
            itemView.findViewById(R.id.tvProductUnitPrice)

        private val tvProductSubtotal: TextView =
            itemView.findViewById(R.id.tvProductSubtotal)

        fun bind(item: DealerOrderItem) {
            tvProductName.text = item.productName

            tvProductQuantity.text =
                quantityFormat.format(
                    item.quantity.stripTrailingZeros().toDouble()
                )

            tvProductUnitPrice.text =
                "KES ${moneyFormat.format(item.unitPrice)}"

            tvProductSubtotal.text =
                "KES ${moneyFormat.format(item.subtotal)}"
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_assigned_order_product,
                parent,
                false
            )

        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderItemViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<DealerOrderItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}