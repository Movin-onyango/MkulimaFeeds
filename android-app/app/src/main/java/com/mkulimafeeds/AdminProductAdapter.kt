package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.data.model.Product
import java.util.Locale

class AdminProductAdapter(
    private var products: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgProduct: ImageView =
            view.findViewById(R.id.imgProduct)

        val tvName: TextView =
            view.findViewById(R.id.tvProductName)

        val tvSku: TextView =
            view.findViewById(R.id.tvSku)

        val tvPrice: TextView =
            view.findViewById(R.id.tvPrice)

        val statusBadge: TextView =
            view.findViewById(R.id.statusBadge)

        val btnMore: ImageView =
            view.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_admin_product_card,
                parent,
                false
            )

        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int
    ) {

        val product = products[position]

        holder.tvName.text = product.name

        holder.tvSku.text =
            "SKU: ${product.sku}"

        holder.tvPrice.text =
            String.format(
                Locale.getDefault(),
                "KES %.2f",
                product.price
            )

        product.imageResId?.let {

            holder.imgProduct.setImageResource(it)

        } ?: run {

            holder.imgProduct.setImageResource(
                R.drawable.img_layers_mash
            )
        }

        if (product.stock <= 5) {

            holder.statusBadge.text =
                "Low Stock"

            holder.statusBadge.setBackgroundResource(
                R.drawable.bg_status_pending
            )

            holder.statusBadge.setTextColor(
                android.graphics.Color.parseColor(
                    "#B91C1C"
                )
            )

        } else {

            holder.statusBadge.text =
                "In Stock"

            holder.statusBadge.setBackgroundResource(
                R.drawable.bg_status_in_stock
            )

            holder.statusBadge.setTextColor(
                android.graphics.Color.parseColor(
                    "#2E7D32"
                )
            )
        }

        // Tap product → Edit
        holder.itemView.setOnClickListener {

            onEditClick(product)
        }

        // More button → Delete
        holder.btnMore.setOnClickListener {

            onDeleteClick(product)
        }
    }

    override fun getItemCount(): Int =
        products.size

    fun updateList(
        newList: List<Product>
    ) {

        products = newList

        notifyDataSetChanged()
    }
}