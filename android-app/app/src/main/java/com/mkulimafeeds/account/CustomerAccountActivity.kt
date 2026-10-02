package com.mkulimafeeds.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.mkulimafeeds.MyOrdersActivity
import com.mkulimafeeds.R
import com.mkulimafeeds.SavedLocationsActivity
import com.mkulimafeeds.TrackOrderActivity
import com.mkulimafeeds.data.remote.ProfileStats
import com.mkulimafeeds.data.repository.UserProfileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerAccountActivity : BaseAccountActivity() {

    private val profileRepository by lazy { UserProfileRepository() }

    /** Cached between fetchProfile() and buildStats(). */
    private var cachedStats: ProfileStats? = null

    // =========================================================
    // DATA
    // =========================================================

    override suspend fun fetchProfile(): AccountProfile? {
        val token = tokenManager.getToken() ?: return null

        return try {
            val response = profileRepository.getMyProfile(token)

            // Cache stats for buildStats()
            cachedStats = response.stats

            AccountProfile(
                id = response.user.id,
                name = response.user.name,
                email = response.user.email,
                phone = response.user.phone,
                role = response.user.role,
                status = if (response.user.isActive) "ACTIVE" else "INACTIVE",
                memberSince = response.user.memberSince
            )
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================
    // HERO
    // =========================================================

    override fun buildHeroCard(profile: AccountProfile): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.activity_account_customer_hero, null, false)

        view.findViewById<TextView>(R.id.tvName).text =
            profile.name.ifBlank { "—" }

        view.findViewById<TextView>(R.id.tvEmail).text =
            profile.email.ifBlank { "No email" }

        view.findViewById<TextView>(R.id.tvAvatarInitials).text =
            computeInitials(profile.name)

        view.findViewById<TextView>(R.id.tvMemberSince).text =
            "Member since ${formatMonthYear(profile.memberSince)}"

        view.findViewById<TextView>(R.id.tvTier).text =
            (cachedStats?.tier ?: "NEW").uppercase()

        return view
    }

    // =========================================================
    // STATS
    // =========================================================

    override fun buildStats(profile: AccountProfile): List<AccountStat> {
        val stats = cachedStats ?: return emptyList()
        val spent = stats.totalSpent.toDoubleOrNull() ?: 0.0

        return listOf(
            AccountStat(
                value = stats.totalOrders.toString(),
                label = "Orders"
            ),
            AccountStat(
                value = stats.completedOrders.toString(),
                label = "Completed"
            ),
            AccountStat(
                value = formatCompactCurrency(spent),
                label = "Total Spent"
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
                label = "Order History",
                subtitle = "All your past orders",
                onTap = {
                    startActivity(Intent(this, MyOrdersActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_myplaces,
                label = "Saved Locations",
                subtitle = "Manage delivery addresses",
                onTap = {
                    startActivity(Intent(this, SavedLocationsActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_directions,
                label = "Track Active Order",
                subtitle = "Follow your current order",
                onTap = {
                    startActivity(Intent(this, TrackOrderActivity::class.java))
                }
            )
        )
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    override fun getNavLayoutId(): Int =
        R.layout.bottom_nav_customer

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

    private fun formatCompactCurrency(value: Double): String = when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "%.0fK", value / 1_000)
        else -> String.format(Locale.US, "%.0f", value)
    }
}