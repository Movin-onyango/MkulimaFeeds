package com.mkulimafeeds

data class Dealer(
    val id: String,
    val name: String,
    val location: String,
    val phone: String,
    val rating: Float,
    val status: String // ACTIVE, INACTIVE
)
