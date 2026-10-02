package com.mkulimafeeds
import com.mkulimafeeds.util.applyBottomNavInsets
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.account.CustomerAccountActivity
import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var productRepository: ProductRepository

    private lateinit var cardFeatured: MaterialCardView
    private lateinit var ivFeatured: ImageView
    private lateinit var tvFeaturedName: TextView
    private lateinit var tvFeaturedPrice: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Root: top + sides only. Bottom inset consumed by nav bar.
        applyBottomNavInsets(
            root = findViewById(R.id.home_root),
            bottomNav = findViewById(R.id.bottomNavCustomer)
        )
/*
        // Bottom nav: consume bottom system bar inset.
        val bottomNav = findViewById<View>(R.id.bottomNavCustomer)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemBars.bottom
            )
            insets
        }

 */

        productRepository = ProductRepository(NetworkModule.apiService)

        initializeViews()
        setupSearchIcon()
        setupNavigation()
        highlightActiveTab()

        loadFeaturedProduct()
    }

    override fun onResume() {
        super.onResume()
        highlightActiveTab()
        updateCartBadge()
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private fun initializeViews() {
        cardFeatured = findViewById(R.id.cardFeaturedProduct)
        ivFeatured = findViewById(R.id.ivFeaturedProductImage)
        tvFeaturedName = findViewById(R.id.tvFeaturedProductName)
        tvFeaturedPrice = findViewById(R.id.tvFeaturedProductPrice)
    }

    // =========================================================
    // SEARCH ICON (replaces Search tab)
    // =========================================================

    private fun setupSearchIcon() {
        findViewById<View>(R.id.btnSearch).setOnClickListener {
            showSearchDialog()
        }
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Search feeds, supplements..."
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Search Catalog")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    val intent = Intent(this, CatalogActivity::class.java).apply {
                        putExtra("SEARCH_QUERY", query)
                    }
                    startActivity(intent)
                }
            }
            .show()
    }

    // =========================================================
    // FEATURED PRODUCT
    // =========================================================

    private fun loadFeaturedProduct() {
        lifecycleScope.launch {
            try {
                val products = productRepository.getProducts()
                if (products.isEmpty()) {
                    showNoFeaturedProduct()
                    return@launch
                }
                val featured = products.firstOrNull { it.stock > 0 }
                    ?: products.first()
                bindFeaturedProduct(featured)
            } catch (_: Exception) {
                showNoFeaturedProduct()
            }
        }
    }

    private fun bindFeaturedProduct(product: Product) {
        tvFeaturedName.text = product.name
        tvFeaturedPrice.text = String.format(
            Locale.getDefault(),
            "KES %,.2f",
            product.price
        )
        ProductImageLoader.load(ivFeatured, product.imageUrl)

        cardFeatured.setOnClickListener {
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
            startActivity(intent)
        }
    }

    private fun showNoFeaturedProduct() {
        tvFeaturedName.text = "No products available"
        tvFeaturedPrice.text = "Check back soon"
        cardFeatured.setOnClickListener {
            Toast.makeText(this, "No featured product yet", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================
    // BOTTOM NAVIGATION (inline — no helper)
    // =========================================================

    private fun setupNavigation() {

        findViewById<View>(R.id.navCatalog).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, CustomerAccountActivity::class.java))
            finish()
        }

        // navHome = current, no-op
    }

    // =========================================================
    // ACTIVE TAB HIGHLIGHT
    // =========================================================

    private fun highlightActiveTab() {
        setActiveTab(R.id.icHome, R.id.tvHome)
        setInactiveTab(R.id.icCatalog, R.id.tvCatalog)
        setInactiveTab(R.id.icCart, R.id.tvCart)
        setInactiveTab(R.id.icOrders, R.id.tvOrders)
        setInactiveTab(R.id.icAccount, R.id.tvAccount)
    }

    private fun setActiveTab(ic: Int, tv: Int) {
        findViewById<ImageView>(ic).setColorFilter(
            ContextCompat.getColor(this, R.color.brand_dark_green)
        )
        findViewById<TextView>(tv).setTextColor(
            ContextCompat.getColor(this, R.color.brand_dark_green)
        )
        findViewById<TextView>(tv).setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun setInactiveTab(ic: Int, tv: Int) {
        findViewById<ImageView>(ic).setColorFilter(
            ContextCompat.getColor(this, R.color.text_grey)
        )
        findViewById<TextView>(tv).setTextColor(
            ContextCompat.getColor(this, R.color.text_grey)
        )
        findViewById<TextView>(tv).setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    // =========================================================
    // CART BADGE
    // =========================================================

    private fun updateCartBadge() {
        val count = CartManager.getItems().sumOf { it.quantity }
        val badge = findViewById<TextView>(R.id.tvCartBadge)
            ?: return

        if (count > 0) {
            badge.text = if (count > 9) "9+" else count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}