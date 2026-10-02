package com.mkulimafeeds

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String = "CUSTOMER",
    val status: String = "ACTIVE",
    val dealerStatus: String? = null,
    val joinedDate: String,
    val totalOrders: Int,
    val membershipTier: String,
    val avatarResId: Int = R.drawable.ic_user_placeholder
)