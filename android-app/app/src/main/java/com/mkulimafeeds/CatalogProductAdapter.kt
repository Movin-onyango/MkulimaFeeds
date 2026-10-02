package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.model.Product
import java.util.Locale

class CatalogProductAdapter(
    private var products: List<Product>,
    private val onAddToCart: (Product) -> Unit,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<CatalogProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val image: ImageView =
            view.findViewById(R.id.ivProductImage)

        val name: TextView =
            view.findViewById(R.id.tvProductName)

        val category: TextView =
            view.findViewById(R.id.tvProductCategory)

        val price: TextView =
            view.findViewById(R.id.tvProductPrice)

        val stock: TextView =
            view.findViewById(R.id.tvProductStock)

        val addButton: MaterialButton =
            view.findViewById(R.id.btnAddCart)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_catalog_product,
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

        holder.name.text =
            product.name

        holder.category.text =
            product.category

        holder.price.text =
            String.format(
                Locale.getDefault(),
                "KES %,.2f",
                product.price
            )

        holder.stock.text =
            if (product.stock > 0) {
                "${product.stock} ${product.unit} available"
            } else {
                "Out of stock"
            }

        holder.addButton.isEnabled =
            product.stock > 0

        holder.addButton.text =
            if (product.stock > 0) {
                "Add"
            } else {
                "Unavailable"
            }

        // ADD TO CART
        holder.addButton.setOnClickListener {

            if (product.stock > 0) {
                onAddToCart(product)
            }
        }

        // OPEN PRODUCT DETAIL
        holder.itemView.setOnClickListener {
            onProductClick(product)
        }

        ProductImageLoader.load(
            holder.image,
            product.imageUrl
        )
    }

    override fun getItemCount(): Int =
        products.size

    fun submitList(
        newProducts: List<Product>
    ) {
        products = newProducts
        notifyDataSetChanged()
    }
}