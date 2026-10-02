package com.mkulimafeeds.account

/**
 * A logical grouping of menu items in the account screen.
 * Example: "SECURITY" section contains Change Password, Sessions, etc.
 */
data class AccountMenuSection(
    val title: String,
    val items: List<AccountMenuItem>
)