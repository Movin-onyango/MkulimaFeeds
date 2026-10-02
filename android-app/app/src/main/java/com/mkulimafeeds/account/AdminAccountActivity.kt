package com.mkulimafeeds.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.mkulimafeeds.AdminUserActivity
import com.mkulimafeeds.AuditLogActivity
import com.mkulimafeeds.BusinessRulesActivity
import com.mkulimafeeds.DealerApplicationsActivity
import com.mkulimafeeds.HelpSupportActivity
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
 * Admin account screen.
 *
 * Extends BaseAccountActivity with:
 *   - Admin hero card (System Administrator badge)
 *   - Admin operations menu (Users, Dealer Applications, Business Rules, Audit Log)
 *   - Admin stats (Users, Orders, Revenue)
 */
class AdminAccountActivity : BaseAccountActivity() {

    private val profileRepository by lazy { UserProfileRepository() }
    private val adminStatsRepository by lazy { AdminStatsRepository() }

    private var cachedStats: ProfileStats? = null
    private var pendingApplications: Int = 0

    // =========================================================
    // DATA
    // =========================================================

    override suspend fun fetchProfile(): AccountProfile? {
        val token = tokenManager.getToken() ?: return null

        return try {
            val response = profileRepository.getMyProfile(token)
            cachedStats = response.stats

            // Fetch pending dealer applications count for badge
            try {
                val apps = adminStatsRepository.getPendingApplicationsCount(token)
                pendingApplications = apps
            } catch (_: Exception) {
                pendingApplications = 0
            }

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
            .inflate(R.layout.activity_account_admin_hero, null, false)

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
                value = "—",     // Placeholder; populated in a later pass
                label = "Users"
            ),
            AccountStat(
                value = stats.totalOrders.toString(),
                label = "Orders"
            ),
            AccountStat(
                value = formatCompactCurrency(
                    stats.totalSpent.toDoubleOrNull() ?: 0.0
                ),
                label = "Revenue"
            )
        )
    }

    // =========================================================
    // ACTIVITY MENU — Admin Operations
    // =========================================================

    override fun buildActivityMenu(): List<AccountMenuItem> {
        return listOf(
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_myplaces,
                label = "User Management",
                subtitle = "View and manage all users",
                onTap = {
                    startActivity(Intent(this, AdminUserActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_agenda,
                label = "Dealer Applications",
                subtitle = "Review and approve dealer requests",
                badgeText = if (pendingApplications > 0)
                    pendingApplications.toString() else null,
                badgeColor = 0xFFFEF3C7.toInt(),
                onTap = {
                    startActivity(Intent(this, DealerApplicationsActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_manage,
                label = "Business Rules",
                subtitle = "Configure dealer tiers and bulk order rules",
                onTap = {
                    startActivity(Intent(this, BusinessRulesActivity::class.java))
                }
            ),
            AccountMenuItem(
                iconRes = android.R.drawable.ic_menu_recent_history,
                label = "Role Audit Log",
                subtitle = "Every role change, promotion, suspension",
                onTap = {
                    startActivity(Intent(this, AuditLogActivity::class.java))
                }
            )
        )
    }

    // =========================================================
    // EXTRA SECTION — Admin personal activity (they're also users)
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