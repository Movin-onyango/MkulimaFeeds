package com.mkulimafeeds.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.mkulimafeeds.AdminOrderActivity
import com.mkulimafeeds.AdminProductActivity
import com.mkulimafeeds.AdminUserActivity
import com.mkulimafeeds.AnalyticsDashboardActivity
import com.mkulimafeeds.MyOrdersActivity
import com.mkulimafeeds.NotificationsActivity
import com.mkulimafeeds.R
import com.mkulimafeeds.SavedLocationsActivity
import com.mkulimafeeds.data.remote.ProfileStats
import com.mkulimafeeds.data.repository.AdminStatsRepository
import com.mkulimafeeds.data.repository.UserProfileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Staff account screen.
 *
 * Similar to admin but with permission-gated menu items.
 * Staff members only see operations they're authorized for.
 *
 * The gating logic reads the user's role and (in a future pass)
 * their effective permissions. For now, all staff see the same
 * core operations (products, orders, analytics, users view-only).
 */
class StaffAccountActivity : BaseAccountActivity() {

    private val profileRepository by lazy { UserProfileRepository() }
    private val adminStatsRepository by lazy { AdminStatsRepository() }

    private var cachedStats: ProfileStats? = null

    // =========================================================
    // DATA
    // =========================================================

    override suspend fun fetchProfile(): AccountProfile? {
        val token = tokenManager.getToken() ?: return null

        return try {
            val response = profileRepository.getMyProfile(token)
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
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================
    // HERO
    // =========================================================

    override fun buildHeroCard(profile: AccountProfile): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.activity_account_staff_hero, null, false)

        view.findViewById<TextView>(R.id.tvAvatarInitials).text =
            computeInitials(profile.name)

        view.findViewById<TextView>(R.id.tvName).text =
            profile.name.ifBlank { "—" }

        view.findViewById<TextView>(R.id.tvEmail).text =
            profile.email.ifBlank { "No email" }

        view.findViewById<TextView>(R.id.tvMemberSince).text =
            "Member since ${formatMonthYear(profile.memberSince)}"

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
                label = "Orders"
            ),
            AccountStat(
                value = stats.completedOrders.toString(),
                label = "Completed"
            ),
            AccountStat(
                value = formatCompactCurrency(
                    stats.totalSpent.toDoubleOrNull() ?: 0.0
                ),
                label = "Handled"
            )
        )
    }

    // =========================================================
    // ACTIVITY MENU — Staff Operations
    // =========================================================

    override fun buildActivityMenu(): List<AccountMenuItem> {
        return listOf(
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_gallery,
                label = "Product Management",
                subtitle = "Add, edit, and manage products",
                onTap = {
                    startActivity(Intent(this, AdminProductActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_agenda,
                label = "Order Management",
                subtitle = "Process and fulfil orders",
                onTap = {
                    startActivity(Intent(this, AdminOrderActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_sort_by_size,
                label = "Analytics",
                subtitle = "Business intelligence dashboard",
                onTap = {
                    startActivity(Intent(this, AnalyticsDashboardActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_myplaces,
                label = "Users (View Only)",
                subtitle = "Browse user accounts",
                onTap = {
                    startActivity(Intent(this, AdminUserActivity::class.java))
                }
            )
        )
    }

    // =========================================================
    // EXTRA SECTION — staff personal activity
    // =========================================================

    override fun buildExtraSection(): List<AccountMenuItem>? {
        return listOf(
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_agenda,
                label = "My Orders",
                subtitle = "Your personal order history",
                onTap = {
                    startActivity(Intent(this, MyOrdersActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_myplaces,
                label = "My Saved Locations",
                subtitle = "Personal delivery addresses",
                onTap = {
                    startActivity(Intent(this, SavedLocationsActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_popup_reminder,
                label = "Notifications",
                subtitle = "System alerts and updates",
                onTap = {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                }
            )
        )
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    override fun getNavLayoutId(): Int =
        R.layout.bottom_nav_admin
    // Staff uses the same bottom nav as admin (Dashboard · Products · Orders · Insights · Account)

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