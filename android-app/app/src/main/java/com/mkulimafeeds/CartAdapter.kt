package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class CartAdapter(
    private var items: List<CartItem>,
    private val onRemoveClick: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val tvSku: TextView = view.findViewById(R.id.tvItemSku)
        val tvPrice: TextView = view.findViewById(R.id.tvItemPrice)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val ivRemove: ImageView = view.findViewById(R.id.ivRemove)
        val ivProduct: ImageView = view.findViewById(R.id.ivProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvSku.text = "SKU: ${item.sku}"
        holder.tvPrice.text = String.format(Locale.getDefault(), "KES %.2f", item.price)
        holder.tvQuantity.text = "x${item.quantity}"
        holder.ivProduct.setImageResource(item.imageRes)
        holder.ivRemove.setOnClickListener { onRemoveClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
