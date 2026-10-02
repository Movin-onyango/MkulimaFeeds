package com.mkulimafeeds.presentation.dealer.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mkulimafeeds.data.repository.dealer.DealerCustomerRepository

class DealerCustomerViewModelFactory(
    private val repository: DealerCustomerRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                DealerCustomerViewModel::class.java
            )
        ) {
            return DealerCustomerViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}