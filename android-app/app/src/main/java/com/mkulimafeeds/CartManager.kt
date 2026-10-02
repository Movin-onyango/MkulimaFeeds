package com.mkulimafeeds

import androidx.annotation.DrawableRes

data class CartItem(
    val id: Int,
    val name: String,
    val price: Double,
    val sku: String,
    @DrawableRes val imageRes: Int,
    var quantity: Int = 1
)

object CartManager {
    private val items = mutableListOf<CartItem>()

    fun addItem(item: CartItem) {
        val existingItem = items.find { it.id == item.id }
        if (existingItem != null) {
            existingItem.quantity += 1
        } else {
            items.add(item)
        }
    }

    fun removeItem(item: CartItem) {
        val existingItem = items.find { it.id == item.id }
        if (existingItem != null) {
            if (existingItem.quantity > 1) {
                existingItem.quantity -= 1
            } else {
                items.remove(existingItem)
            }
        }
    }

    fun getItems(): List<CartItem> = items

    fun getTotalPrice(): Double {
        return items.sumOf { it.price * it.quantity }
    }

    fun clearCart() {
        items.clear()
    }
}
