package com.mkulimafeeds.presentation.customer.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.R
import com.mkulimafeeds.domain.model.CustomerOrderItem
import java.math.BigDecimal
import java.text.DecimalFormat

class CustomerOrderItemAdapter :
    RecyclerView.Adapter<CustomerOrderItemAdapter.ItemViewHolder>() {

    private val items =
        mutableListOf<CustomerOrderItem>()

    private val moneyFormat =
        DecimalFormat("#,##0.00")

    private val quantityFormat =
        DecimalFormat("#,##0.##")

    class ItemViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        private val tvProductName: TextView =
            view.findViewById(R.id.tvProductName)

        private val tvProductQuantity: TextView =
            view.findViewById(R.id.tvProductQuantity)

        private val tvProductUnitPrice: TextView =
            view.findViewById(R.id.tvProductUnitPrice)

        private val tvProductSubtotal: TextView =
            view.findViewById(R.id.tvProductSubtotal)

        fun bind(
            item: CustomerOrderItem,
            moneyFormat: DecimalFormat,
            quantityFormat: DecimalFormat
        ) {

            tvProductName.text =
                item.productName

            tvProductQuantity.text =
                quantityFormat.format(
                    item.quantity
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
    ): ItemViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_customer_order_product,
                    parent,
                    false
                )

        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {

        holder.bind(
            item = items[position],
            moneyFormat = moneyFormat,
            quantityFormat = quantityFormat
        )
    }

    override fun getItemCount(): Int =
        items.size

    fun updateItems(
        newItems: List<CustomerOrderItem>
    ) {

        items.clear()
        items.addAll(newItems)

        notifyDataSetChanged()
    }
}