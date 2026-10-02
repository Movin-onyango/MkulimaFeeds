package com.mkulimafeeds.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mkulimafeeds.DealerAssignedOrdersActivity
import com.mkulimafeeds.DealerCustomersActivity
import com.mkulimafeeds.DealerReportsActivity
import com.mkulimafeeds.DealerSettingsActivity
import com.mkulimafeeds.R
import com.mkulimafeeds.data.remote.DealerInfo
import com.mkulimafeeds.data.remote.ProfileStats
import com.mkulimafeeds.data.repository.UserProfileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DealerAccountActivity : BaseAccountActivity() {

    private val profileRepository by lazy { UserProfileRepository() }

    private var cachedStats: ProfileStats? = null
    private var cachedDealer: DealerInfo? = null

    // =========================================================
    // DATA
    // =========================================================

    override suspend fun fetchProfile(): AccountProfile? {
        val token = tokenManager.getToken() ?: return null

        return try {
            val response = profileRepository.getMyProfile(token)

            cachedStats = response.stats
            cachedDealer = response.dealer

            AccountProfile(
                id = response.user.id,
                name = response.user.name,
                email = response.user.email,
                phone = response.user.phone,
                role = response.user.role,
                status = if (response.user.isActive) "ACTIVE" else "INACTIVE",
                memberSince = response.user.memberSince
            )
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================
    // HERO
    // =========================================================

    override fun buildHeroCard(profile: AccountProfile): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.activity_account_dealer_hero, null, false)

        val dealer = cachedDealer

        // Avatar
        view.findViewById<TextView>(R.id.tvAvatarInitials).text =
            computeInitials(profile.name)

        // Name + email
        view.findViewById<TextView>(R.id.tvName).text =
            profile.name.ifBlank { "—" }

        view.findViewById<TextView>(R.id.tvEmail).text =
            profile.email.ifBlank { "No email" }

        // Business name (fallback to a placeholder if not set)
        view.findViewById<TextView>(R.id.tvBusinessName).text =
            dealer?.businessName?.ifBlank { "Business not set" }
                ?: "Business not set"

        // Region
        view.findViewById<TextView>(R.id.tvRegion).text =
            dealer?.businessRegion?.ifBlank { "Region not set" }
                ?: "Region not set"

        // Member since
        view.findViewById<TextView>(R.id.tvMemberSince).text =
            "Member since ${formatMonthYear(profile.memberSince)}"

        // Tier badge
        view.findViewById<TextView>(R.id.tvTier).text =
            dealer?.currentTier?.uppercase() ?: "—"

        // Tier progress
        val tierContainer = view.findViewById<View>(R.id.tierProgressContainer)
        val tierProgressLabel = view.findViewById<TextView>(R.id.tvTierProgressLabel)
        val tierProgressValue = view.findViewById<TextView>(R.id.tvTierProgressValue)
        val tierProgress = view.findViewById<LinearProgressIndicator>(R.id.tierProgress)

        if (dealer?.nextTier == null) {
            // Already at max tier
            tierProgressLabel.text = "Maximum tier reached"
            tierProgressValue.text = dealer?.lifetimeBulkOrders?.toString() ?: "0"
            tierProgress.progress = 100
        } else {
            tierProgressLabel.text = "Progress to ${dealer.nextTier}"
            tierProgressValue.text = "${dealer.lifetimeBulkOrders} orders · ${dealer.ordersToNextTier} to go"
            tierProgress.progress = dealer.tierProgressPercent
        }

        // Account manager
        val managerContainer = view.findViewById<View>(R.id.managerContainer)
        val managerName = dealer?.accountManagerName
        val managerEmail = dealer?.accountManagerEmail

        if (managerName.isNullOrBlank()) {
            managerContainer.visibility = View.GONE
        } else {
            view.findViewById<TextView>(R.id.tvManagerName).text = managerName
            view.findViewById<TextView>(R.id.tvManagerEmail).text =
                managerEmail ?: "—"
        }

        // Credit card
        val creditCard = view.findViewById<View>(R.id.creditCard)
        if (dealer == null || dealer.creditLimit.toDoubleOrNull() == 0.0) {
            creditCard.visibility = View.GONE
        } else {
            val available = dealer.creditAvailable.toDoubleOrNull() ?: 0.0
            val used = dealer.creditUsed.toDoubleOrNull() ?: 0.0
            val limit = dealer.creditLimit.toDoubleOrNull() ?: 0.0
            val percentUsed = if (limit > 0) ((used / limit) * 100).toInt() else 0

            view.findViewById<TextView>(R.id.tvPaymentTerms).text =
                formatPaymentTerms(dealer.paymentTerms)

            view.findViewById<TextView>(R.id.tvCreditAvailable).text =
                "${formatCurrency(available)} available"

            view.findViewById<LinearProgressIndicator>(R.id.creditProgress).progress =
                (100 - percentUsed).coerceIn(0, 100)

            view.findViewById<TextView>(R.id.tvCreditDetail).text =
                "${formatCurrency(available)} of ${formatCurrency(limit)} available · " +
                        "Used ${formatCurrency(used)}"
        }

        return view
    }

    // =========================================================
    // STATS
    // =========================================================

    override fun buildStats(profile: AccountProfile): List<AccountStat> {
        val stats = cachedStats ?: return emptyList()

        return listOf(
            AccountStat(
                value = stats.totalOrders.toString(),
                label = "Assigned"
            ),
            AccountStat(
                value = stats.completedOrders.toString(),
                label = "Completed"
            ),
            AccountStat(
                value = formatCompactCurrency(
                    stats.totalSpent.toDoubleOrNull() ?: 0.0
                ),
                label = "Earned"
            )
        )
    }

    // =========================================================
    // ACTIVITY MENU
    // =========================================================

    override fun buildActivityMenu(): List<AccountMenuItem> {
        return listOf(
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_agenda,
                label = "Assigned Orders",
                subtitle = "Orders you're fulfilling",
                onTap = {
                    startActivity(Intent(this, DealerAssignedOrdersActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_gallery,
                label = "My Bulk Orders",
                subtitle = "Wholesale purchases from the main store",
                onTap = {
                    toast("Bulk orders coming in a later pass")
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_myplaces,
                label = "My Customers",
                subtitle = "Customers assigned to you",
                onTap = {
                    startActivity(Intent(this, DealerCustomersActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_edit,
                label = "Request Quote",
                subtitle = "Request a custom price for large orders",
                onTap = {
                    toast("Quote requests coming in a later pass")
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_sort_by_size,
                label = "Performance Reports",
                subtitle = "Your delivery & earnings history",
                onTap = {
                    startActivity(Intent(this, DealerReportsActivity::class.java))
                }
            )
        )
    }

    // =========================================================
    // EXTRA SECTION — dealer-specific settings
    // =========================================================

    override fun buildExtraSection(): List<AccountMenuItem>? {
        return listOf(
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_preferences,
                label = "Dealer Settings",
                subtitle = "Notifications and business preferences",
                onTap = {
                    startActivity(Intent(this, DealerSettingsActivity::class.java))
                }
            )
        )
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    override fun getNavLayoutId(): Int =
        R.layout.bottom_nav_dealer

    // =========================================================
    // HELPERS
    // =========================================================

    private fun computeInitials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "--"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts[0].first().toString() + parts[1].first().toString()).uppercase()
        }
    }

    private fun formatMonthYear(epochMs: Long): String = try {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(epochMs))
    } catch (_: Exception) {
        "—"
    }

    private fun formatCurrency(value: Double): String =
        String.format(Locale.US, "KES %,.0f", value)

    private fun formatCompactCurrency(value: Double): String = when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "%.0fK", value / 1_000)
        else -> String.format(Locale.US, "%.0f", value)
    }

    private fun formatPaymentTerms(terms: String): String = when (terms.uppercase()) {
        "PREPAID" -> "Prepaid"
        "NET_30" -> "Net-30"
        "NET_60" -> "Net-60"
        else -> terms
    }
}