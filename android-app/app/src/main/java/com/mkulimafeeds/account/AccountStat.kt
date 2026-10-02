package com.mkulimafeeds.account

/**
 * A KPI tile shown in the 3-column stats strip.
 */
data class AccountStat(
    val value: String,
    val label: String,
    val valueColor: Int? = null
)