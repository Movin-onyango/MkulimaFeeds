package com.mkulimafeeds.presentation.dealer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository

class DealerOrderViewModelFactory(
    private val repository: DealerOrderRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                DealerOrderViewModel::class.java
            )
        ) {
            return DealerOrderViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }


}
