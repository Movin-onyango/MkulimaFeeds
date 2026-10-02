package com.mkulimafeeds
import com.mkulimafeeds.util.applyBottomNavInsets
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mkulimafeeds.account.CustomerAccountActivity
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvEmptyCart: TextView
    private lateinit var cardSummary: View
    private lateinit var tvSubtotalValue: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var btnCheckout: MaterialButton
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)

        rvCartItems = findViewById(R.id.rvCartItems)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        cardSummary = findViewById(R.id.cardSummary)
        tvSubtotalValue = findViewById(R.id.tvSubtotalValue)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        btnCheckout = findViewById(R.id.btnCheckout)

        adapter = CartAdapter(CartManager.getItems()) { item ->
            CartManager.removeItem(item)
            updateCartUI()
        }
        rvCartItems.adapter = adapter

        updateCartUI()

        btnCheckout.setOnClickListener {
            if (CartManager.getItems().isNotEmpty()) {
                startActivity(Intent(this, DeliveryActivity::class.java))
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
        applyBottomNavInsets(
            root = findViewById(R.id.nav_cart_root),
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
        setupNavigation()
        highlightActiveTab()
    }

    override fun onResume() {
        super.onResume()
        highlightActiveTab()
        updateCartUI()
    }

    // =========================================================
    // BOTTOM NAVIGATION (inline — no helper)
    // =========================================================

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navCatalog).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
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

        // navCart = current, no-op
    }

    private fun highlightActiveTab() {
        setInactiveTab(R.id.icHome, R.id.tvHome)
        setInactiveTab(R.id.icCatalog, R.id.tvCatalog)
        setActiveTab(R.id.icCart, R.id.tvCart)
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
    // CART UI
    // =========================================================

    private fun updateCartUI() {
        val items = CartManager.getItems()

        if (items.isEmpty()) {
            tvEmptyCart.visibility = View.VISIBLE
            rvCartItems.visibility = View.GONE
            cardSummary.visibility = View.GONE
        } else {
            tvEmptyCart.visibility = View.GONE
            rvCartItems.visibility = View.VISIBLE
            cardSummary.visibility = View.VISIBLE

            val total = CartManager.getTotalPrice()
            val formattedTotal = String.format(Locale.getDefault(), "KES %.2f", total)
            tvSubtotalValue.text = formattedTotal
            tvTotalValue.text = formattedTotal
        }

        adapter.updateItems(items)
        updateCartBadge(items.sumOf { it.quantity })
    }

    private fun updateCartBadge(count: Int) {
        val badge = findViewById<TextView>(R.id.tvCartBadge) ?: return

        if (count > 0) {
            badge.text = if (count > 9) "9+" else count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}