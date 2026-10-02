package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID =
            "EXTRA_PRODUCT_ID"
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var ivProductImage: ImageView
    private lateinit var tvStockBadge: TextView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvProductDescription: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var btnMinus: TextView
    private lateinit var btnPlus: TextView
    private lateinit var btnAddToCart: MaterialButton

    // =========================================================
    // STATE
    // =========================================================

    private lateinit var productRepository: ProductRepository

    private var loadedProduct: Product? = null
    private var quantity: Int = 1

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_product_detail
        )

        val rootView =
            findViewById<View>(
                R.id.product_detail_root
            )

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )

            insets
        }

        productRepository =
            ProductRepository(
                NetworkModule.apiService
            )

        initializeViews()

        setupQuantitySelector()

        setupBackButton()

        setupAddToCartButton()

        val productId =
            intent.getStringExtra(
                EXTRA_PRODUCT_ID
            )

        if (productId.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Invalid product",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        loadProduct(productId)
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private fun initializeViews() {

        ivProductImage =
            findViewById(R.id.ivProductDetailImage)

        tvStockBadge =
            findViewById(R.id.tvProductDetailStock)

        tvProductName =
            findViewById(R.id.tvProductDetailName)

        tvProductPrice =
            findViewById(R.id.tvProductDetailPrice)

        tvProductDescription =
            findViewById(R.id.tvProductDetailDescription)

        tvQuantity =
            findViewById(R.id.tvQuantity)

        btnMinus =
            findViewById(R.id.btnMinus)

        btnPlus =
            findViewById(R.id.btnPlus)

        btnAddToCart =
            findViewById(R.id.btnAddToCart)
    }

    // =========================================================
    // LOAD PRODUCT FROM BACKEND
    // =========================================================

    private fun loadProduct(
        productId: String
    ) {

        val numericId =
            productId.toLongOrNull()

        if (numericId == null) {

            Toast.makeText(
                this,
                "Invalid product ID",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        btnAddToCart.isEnabled =
            false

        btnAddToCart.text =
            "Loading..."

        lifecycleScope.launch {

            try {

                val product =
                    productRepository.getProduct(
                        numericId
                    )

                loadedProduct =
                    product

                bindProduct(product)

                btnAddToCart.isEnabled =
                    product.stock > 0

            } catch (e: Exception) {

                Toast.makeText(
                    this@ProductDetailActivity,
                    "Failed to load product: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    // =========================================================
    // BIND PRODUCT DATA TO VIEWS
    // =========================================================

    private fun bindProduct(
        product: Product
    ) {
        btnAddToCart.text =
            if (product.stock > 0) {
                "ADD TO CART"
            } else {
                "OUT OF STOCK"
            }


        tvProductName.text =
            product.name

        tvProductPrice.text =
            String.format(
                Locale.getDefault(),
                "KES %,.2f",
                product.price
            )

        tvProductDescription.text =
            product.description.ifBlank {
                "No description available."
            }

        // -----------------------------------------------------
        // STOCK BADGE
        // -----------------------------------------------------

        if (product.stock > 0) {

            tvStockBadge.text =
                "IN STOCK"

            tvStockBadge.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.brand_dark_green
                )
            )

        } else {

            tvStockBadge.text =
                "OUT OF STOCK"

            tvStockBadge.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.brand_blue
                )
            )
        }

        // -----------------------------------------------------
        // IMAGE
        // -----------------------------------------------------

        ProductImageLoader.load(
            ivProductImage,
            product.imageUrl
        )
    }

    // =========================================================
    // QUANTITY
    // =========================================================

    private fun setupQuantitySelector() {

        btnPlus.setOnClickListener {

            quantity++

            tvQuantity.text =
                quantity.toString()
        }

        btnMinus.setOnClickListener {

            if (quantity > 1) {

                quantity--

                tvQuantity.text =
                    quantity.toString()
            }
        }
    }

    // =========================================================
    // BACK
    // =========================================================

    private fun setupBackButton() {

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }
    }

    // =========================================================
    // ADD TO CART
    // =========================================================

    private fun setupAddToCartButton() {

        btnAddToCart.setOnClickListener {

            val product =
                loadedProduct
                    ?: return@setOnClickListener

            if (product.stock <= 0) {

                Toast.makeText(
                    this,
                    "This product is out of stock",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val numericId =
                product.id.toIntOrNull()

            if (numericId == null) {

                Toast.makeText(
                    this,
                    "Invalid product ID",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Add the item `quantity` times to the cart.
            repeat(quantity) {

                CartManager.addItem(
                    CartItem(
                        id = numericId,
                        name = product.name,
                        price = product.price,
                        sku = product.sku.ifBlank {
                            "PRODUCT-${product.id}"
                        },
                        imageRes =
                            product.imageResId
                                ?: R.drawable.ic_launcher_background
                    )
                )
            }

            Toast.makeText(
                this,
                "${product.name} × $quantity added to cart!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}