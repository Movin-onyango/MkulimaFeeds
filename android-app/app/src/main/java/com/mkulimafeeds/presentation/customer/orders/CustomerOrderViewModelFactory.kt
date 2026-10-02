package com.mkulimafeeds.presentation.customer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mkulimafeeds.data.repository.customer.CustomerOrderRepository

class CustomerOrderViewModelFactory(
    private val repository: CustomerOrderRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                CustomerOrderViewModel::class.java
            )
        ) {
            return CustomerOrderViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}