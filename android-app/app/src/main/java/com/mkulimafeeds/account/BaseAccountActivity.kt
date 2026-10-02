package com.mkulimafeeds.account

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.AboutActivity
import com.mkulimafeeds.AdminDashboardActivity
import com.mkulimafeeds.AdminInsightsActivity
import com.mkulimafeeds.AdminOrderActivity
import com.mkulimafeeds.AdminProductActivity
import com.mkulimafeeds.CartActivity
import com.mkulimafeeds.CatalogActivity
import com.mkulimafeeds.ChangePasswordActivity
import com.mkulimafeeds.DealerCatalogActivity
import com.mkulimafeeds.DealerDashboardActivity
import com.mkulimafeeds.DealerInsightsActivity
import com.mkulimafeeds.DealerOrdersActivity
import com.mkulimafeeds.DeleteAccountActivity
import com.mkulimafeeds.HelpSupportActivity
import com.mkulimafeeds.HomeActivity
import com.mkulimafeeds.LoginActivity
import com.mkulimafeeds.MyOrdersActivity
import com.mkulimafeeds.PrivacyPolicyActivity
import com.mkulimafeeds.R
import com.mkulimafeeds.TermsActivity
import com.mkulimafeeds.ThemeSelectionActivity
import com.mkulimafeeds.data.local.TokenManager
import kotlinx.coroutines.launch

/**
 * Base class for all role-specific account screens.
 *
 * Subclasses provide:
 *   - fetchProfile()             → user data
 *   - buildHeroCard()            → role-specific hero view
 *   - buildStats()               → 3 KPI cards
 *   - buildActivityMenu()        → role-specific menu
 *   - buildExtraSection()        → optional role-specific extra section
 *   - getNavLayoutId()           → which bottom nav to inflate
 *
 * The base renders:
 *   - Hero (from subclass)
 *   - Stats (from subclass)
 *   - Activity section (from subclass)
 *   - Extra section (from subclass, if any)
 *   - Security section (shared)
 *   - Preferences section (shared)
 *   - Support section (shared)
 *   - Danger section (shared)
 *
 * The base also wires the universal bottom-nav destination map —
 * role-specific destinations are resolved via `currentRole()`.
 *
 * Edge-to-edge insets:
 *   - Root takes top + left + right padding (status bar, cutouts)
 *   - The bottom nav's INNER ROW consumes the bottom system-bar inset
 *     so the card's white background stays full-bleed to the physical
 *     bottom edge while the icons sit above gesture / 3-button nav.
 *
 * Bottom-nav resolution:
 *   The nav layout is inflated into `@id/accountBottomNavContainer`.
 *   After inflation, the container holds exactly one child — the nav's
 *   root card — and that card holds exactly one child — the icon row.
 *   We resolve both by position, not by ID, so this base class never
 *   needs to know what any role's nav root ID is called. Adding a new
 *   role means adding a new `bottom_nav_*.xml` and pointing
 *   `getNavLayoutId()` at it — no changes needed here.
 */
abstract class BaseAccountActivity : AppCompatActivity() {

    // =========================================================
    // SUBCLASS CONTRACT
    // =========================================================

    /**
     * Fetch the current user's profile.
     * Called once on create; subclass returns null on error.
     */
    protected abstract suspend fun fetchProfile(): AccountProfile?

    /**
     * Build the role-specific hero card (avatar, name, role badge, etc.)
     */
    protected abstract fun buildHeroCard(profile: AccountProfile): View

    /**
     * Build the 3-stat strip for this role.
     */
    protected abstract fun buildStats(profile: AccountProfile): List<AccountStat>

    /**
     * Build the role-specific "MY ACTIVITY" menu items.
     */
    protected abstract fun buildActivityMenu(): List<AccountMenuItem>

    /**
     * Optional role-specific extra section between activity and security.
     * Return null to skip.
     */
    protected open fun buildExtraSection(): List<AccountMenuItem>? = null

    /**
     * Which bottom nav layout to inflate.
     */
    protected abstract fun getNavLayoutId(): Int

    // =========================================================
    // BASE VIEWS
    // =========================================================

    private lateinit var heroSlot: ViewGroup
    private lateinit var statsSlot: LinearLayout
    private lateinit var activitySlot: ViewGroup
    private lateinit var extraSlot: ViewGroup
    private lateinit var securitySlot: ViewGroup
    private lateinit var preferencesSlot: ViewGroup
    private lateinit var supportSlot: ViewGroup
    private lateinit var dangerSlot: ViewGroup
    private lateinit var bottomNavContainer: ViewGroup
    private lateinit var loadingOverlay: View
    private lateinit var bottomNavRoot: View

    protected lateinit var tokenManager: TokenManager
    protected lateinit var preferencesManager: AccountPreferencesManager

    /**
     * Cached role — populated when fetchProfile() succeeds.
     * Default is CUSTOMER so nav clicks before load still land somewhere sane.
     */
    private var cachedRole: String = "CUSTOMER"

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_account_base)

        tokenManager = TokenManager(applicationContext)
        preferencesManager = AccountPreferencesManager(applicationContext)

        bindViews()
        applyWindowInsets()

        inflateBottomNav()

        loadAndRender()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the account every time we come back to it
        // (role changes, profile updates, etc.)
        if (::tokenManager.isInitialized) {
            loadAndRender()
        }
    }

    private fun bindViews() {
        heroSlot = findViewById(R.id.accountHeroSlot)
        statsSlot = findViewById(R.id.accountStatsSlot)
        activitySlot = findViewById(R.id.accountActivitySlot)
        extraSlot = findViewById(R.id.accountExtraSlot)
        securitySlot = findViewById(R.id.accountSecuritySlot)
        preferencesSlot = findViewById(R.id.accountPreferencesSlot)
        supportSlot = findViewById(R.id.accountSupportSlot)
        dangerSlot = findViewById(R.id.accountDangerSlot)
        bottomNavContainer = findViewById(R.id.accountBottomNavContainer)
        loadingOverlay = findViewById(R.id.accountLoadingOverlay)
    }

    private fun applyWindowInsets() {
        val rootView = findViewById<View>(R.id.accountBaseRoot)

        // Root: top + sides only. Bottom inset is consumed by the nav bar.
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0   // NO bottom padding on root
            )
            insets
        }
    }

    private fun inflateBottomNav() {
        LayoutInflater.from(this)
            .inflate(getNavLayoutId(), bottomNavContainer, true)

        // The container now holds exactly one child — the nav's root card.
        // Resolving by position instead of by ID keeps this base class
        // decoupled from any role-specific resource name.
        bottomNavRoot =
            if (bottomNavContainer.childCount > 0) {
                bottomNavContainer.getChildAt(0)
            } else {
                bottomNavContainer
            }

        // The nav card holds exactly one child — the icon row.
        // We pad the row, NOT the card, so the card's white background
        // extends to the physical bottom edge of the screen while the
        // icons + labels lift above gesture / 3-button navigation.
        val innerRow: View =
            if (bottomNavRoot is ViewGroup && (bottomNavRoot as ViewGroup).childCount > 0) {
                (bottomNavRoot as ViewGroup).getChildAt(0)
            } else {
                bottomNavRoot
            }

        ViewCompat.setOnApplyWindowInsetsListener(innerRow) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                v.paddingBottom + systemBars.bottom
            )
            insets
        }

        setupNavListeners()
    }

    // =========================================================
    // LOAD & RENDER
    // =========================================================

    private fun loadAndRender() {
        showLoading()

        lifecycleScope.launch {
            try {
                val profile = fetchProfile()
                if (profile == null) {
                    toast("Failed to load profile")
                    hideLoading()
                    return@launch
                }

                // Cache role for nav dispatch
                cachedRole = profile.role.uppercase()

                renderHero(profile)
                renderStats(profile)
                renderActivitySection()
                renderExtraSection()
                renderSharedSections()
                hideLoading()

            } catch (e: Exception) {
                hideLoading()
                toast(e.message ?: "Something went wrong")
            }
        }
    }

    // =========================================================
    // RENDER HERO
    // =========================================================

    private fun renderHero(profile: AccountProfile) {
        heroSlot.removeAllViews()
        heroSlot.addView(buildHeroCard(profile))
    }

    // =========================================================
    // RENDER STATS
    // =========================================================

    private fun renderStats(profile: AccountProfile) {
        statsSlot.removeAllViews()
        val stats = buildStats(profile)

        stats.forEach { stat ->
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_account_stat_card, statsSlot, false)

            val tvValue = card.findViewById<TextView>(R.id.tvValue)
            val tvLabel = card.findViewById<TextView>(R.id.tvLabel)

            tvValue.text = stat.value
            tvLabel.text = stat.label
            stat.valueColor?.let { tvValue.setTextColor(it) }

            // Give each card equal weight
            val params = card.layoutParams as LinearLayout.LayoutParams
            params.width = 0
            params.weight = 1f
            card.layoutParams = params

            statsSlot.addView(card)
        }
    }

    // =========================================================
    // RENDER ACTIVITY SECTION
    // =========================================================

    private fun renderActivitySection() {
        val items = buildActivityMenu()
        if (items.isEmpty()) return

        val section = AccountMenuSection("MY ACTIVITY", items)
        activitySlot.addView(buildMenuSectionView(section))
    }

    // =========================================================
    // RENDER EXTRA SECTION (role-specific)
    // =========================================================

    private fun renderExtraSection() {
        val items = buildExtraSection() ?: return
        if (items.isEmpty()) return

        extraSlot.removeAllViews()
        extraSlot.addView(buildMenuSectionView(AccountMenuSection("", items)))
    }

    // =========================================================
    // RENDER SHARED SECTIONS
    // =========================================================

    private fun renderSharedSections() {
        // SECURITY
        securitySlot.removeAllViews()
        securitySlot.addView(buildMenuSectionView(buildSecuritySection()))

        // PREFERENCES
        preferencesSlot.removeAllViews()
        preferencesSlot.addView(buildMenuSectionView(buildPreferencesSection()))

        // SUPPORT
        supportSlot.removeAllViews()
        supportSlot.addView(buildMenuSectionView(buildSupportSection()))

        // DANGER
        dangerSlot.removeAllViews()
        dangerSlot.addView(buildMenuSectionView(buildDangerSection()))
    }

    private fun buildMenuSectionView(section: AccountMenuSection): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Section header (if title non-empty)
        if (section.title.isNotBlank()) {
            val header = LayoutInflater.from(this)
                .inflate(R.layout.item_account_section_header, container, false)
            header.findViewById<TextView>(R.id.tvSectionTitle).text = section.title
            container.addView(header)
        }

        // The menu rows as a single RecyclerView
        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@BaseAccountActivity)
            adapter = AccountMenuAdapter(section.items)
            isNestedScrollingEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(rv)

        return container
    }

    // =========================================================
    // SHARED SECTIONS
    // =========================================================

    protected fun buildSecuritySection(): AccountMenuSection =
        AccountMenuSection(
            title = "SECURITY",
            items = listOf(
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_lock_lock,
                    label = "Change Password",
                    subtitle = "Update your account password",
                    onTap = {
                        startActivity(Intent(this, ChangePasswordActivity::class.java))
                    }
                )
            )
        )

    protected fun buildPreferencesSection(): AccountMenuSection =
        AccountMenuSection(
            title = "PREFERENCES",
            items = listOf(
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_menu_manage,
                    label = "Theme",
                    subtitle = humanReadableTheme(preferencesManager.getTheme()),
                    onTap = {
                        startActivity(Intent(this, ThemeSelectionActivity::class.java))
                    }
                ),
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_menu_edit,
                    label = "Language",
                    subtitle = humanReadableLanguage(preferencesManager.getLanguage()),
                    onTap = {
                        toast("Language selection coming soon")
                    }
                ),
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_popup_reminder,
                    label = "Notifications",
                    subtitle = if (preferencesManager.areNotificationsEnabled())
                        "Enabled" else "Disabled",
                    onTap = {
                        toast("Notification preferences coming soon")
                    }
                )
            )
        )

    protected fun buildSupportSection(): AccountMenuSection =
        AccountMenuSection(
            title = "SUPPORT",
            items = listOf(
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_menu_help,
                    label = "Help & Support",
                    onTap = {
                        startActivity(Intent(this, HelpSupportActivity::class.java))
                    }
                ),
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_dialog_info,
                    label = "About",
                    onTap = {
                        startActivity(Intent(this, AboutActivity::class.java))
                    }
                ),
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_menu_info_details,
                    label = "Terms of Service",
                    onTap = {
                        startActivity(Intent(this, TermsActivity::class.java))
                    }
                ),
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_lock_lock,
                    label = "Privacy Policy",
                    onTap = {
                        startActivity(Intent(this, PrivacyPolicyActivity::class.java))
                    }
                )
            )
        )

    protected fun buildDangerSection(): AccountMenuSection =
        AccountMenuSection(
            title = "",
            items = listOf(
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_menu_delete,
                    label = "Delete Account",
                    tintColor = Color.parseColor("#B91C1C"),
                    onTap = {
                        startActivity(Intent(this, DeleteAccountActivity::class.java))
                    }
                ),
                AccountMenuItem(
                    iconRes = android.R.drawable.ic_menu_close_clear_cancel,
                    label = "Log Out",
                    tintColor = Color.parseColor("#B91C1C"),
                    onTap = {
                        confirmLogout()
                    }
                )
            )
        )

    // =========================================================
    // NAV — UNIVERSAL DESTINATION MAP
    // =========================================================

    /**
     * Wires whatever nav IDs exist in the inflated layout. Role-specific
     * destinations are resolved lazily at click time via currentRole(),
     * so the same base class serves customer / dealer / admin / staff.
     */
    private fun setupNavListeners() {
        // Every role has the same conceptual tabs; the role-specific layouts
        // use consistent IDs (navDashboard, navProducts, navCatalog, etc.).
        // We wire the ones that exist in the inflated layout.

        // Home / Dashboard
        findViewById<View?>(R.id.navHome)?.setOnClickListener {
            navigateTo(HomeActivity::class.java)
        }
        findViewById<View?>(R.id.navDashboard)?.setOnClickListener {
            navigateTo(roleDashboard())
        }

        // Catalog / Products
        findViewById<View?>(R.id.navCatalog)?.setOnClickListener {
            navigateTo(roleCatalog())
        }
        findViewById<View?>(R.id.navProducts)?.setOnClickListener {
            navigateTo(AdminProductActivity::class.java)
        }

        // Cart (customer only)
        findViewById<View?>(R.id.navCart)?.setOnClickListener {
            navigateTo(CartActivity::class.java)
        }

        // Orders
        findViewById<View?>(R.id.navOrders)?.setOnClickListener {
            navigateTo(roleOrders())
        }

        // Insights (dealer/admin/staff)
        findViewById<View?>(R.id.navInsights)?.setOnClickListener {
            navigateTo(roleInsights())
        }

        // Account — already on it, no-op
        findViewById<View?>(R.id.navAccount)?.setOnClickListener { /* no-op */ }
    }

    private fun navigateTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish()
    }

    private fun roleDashboard(): Class<*> = when (currentRole()) {
        "ADMIN", "STAFF" -> AdminDashboardActivity::class.java
        "DEALER" -> DealerDashboardActivity::class.java
        else -> HomeActivity::class.java
    }

    private fun roleCatalog(): Class<*> = when (currentRole()) {
        "DEALER" -> DealerCatalogActivity::class.java
        else -> CatalogActivity::class.java
    }

    private fun roleOrders(): Class<*> = when (currentRole()) {
        "ADMIN", "STAFF" -> AdminOrderActivity::class.java
        "DEALER" -> DealerOrdersActivity::class.java
        else -> MyOrdersActivity::class.java
    }

    private fun roleInsights(): Class<*> = when (currentRole()) {
        "DEALER" -> DealerInsightsActivity::class.java
        else -> AdminInsightsActivity::class.java
    }

    private fun currentRole(): String {
        // Read from JWT or cache the role when fetchProfile() succeeds
        return cachedRole
    }

    // =========================================================
    // HELPERS
    // =========================================================

    protected fun humanReadableTheme(theme: String): String = when (theme) {
        AccountPreferencesManager.THEME_LIGHT -> "Light"
        AccountPreferencesManager.THEME_DARK -> "Dark"
        AccountPreferencesManager.THEME_SYSTEM -> "System default"
        else -> theme
    }

    protected fun humanReadableLanguage(lang: String): String = when (lang) {
        AccountPreferencesManager.LANGUAGE_EN -> "English"
        AccountPreferencesManager.LANGUAGE_SW -> "Kiswahili"
        else -> lang
    }

    private fun confirmLogout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Log out?")
            .setMessage("You'll need to log in again to access your account.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log Out") { _, _ ->
                tokenManager.clearToken()
                preferencesManager.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }

    protected fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
    }

    protected fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Base data returned by fetchProfile(). Role-specific subclasses
 * wrap this and add their own fields if needed.
 */
data class AccountProfile(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val status: String,
    val memberSince: Long
)