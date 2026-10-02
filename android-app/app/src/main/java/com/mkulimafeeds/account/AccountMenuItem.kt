package com.mkulimafeeds.account

import androidx.annotation.DrawableRes

/**
 * A single row in the account menu.
 *
 * @param iconRes      Icon to show on the left.
 * @param label        Main text.
 * @param subtitle     Optional secondary text.
 * @param badgeText    Optional badge (e.g., "3 pending").
 * @param badgeColor   Color for the badge background.
 * @param tintColor    Icon tint color (default: brand_dark_green).
 * @param onTap        Action to run when tapped.
 */
data class AccountMenuItem(
    @DrawableRes val iconRes: Int,
    val label: String,
    val subtitle: String? = null,
    val badgeText: String? = null,
    val badgeColor: Int? = null,
    val tintColor: Int? = null,
    val onTap: () -> Unit
)