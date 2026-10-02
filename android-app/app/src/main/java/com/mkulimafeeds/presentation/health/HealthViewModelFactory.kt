package com.mkulimafeeds.presentation.health
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mkulimafeeds.data.repository.HealthRepository
import com.mkulimafeeds.data.remote.NetworkModule

class HealthViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            return HealthViewModel(
                repository = HealthRepository(
                    apiService = NetworkModule.apiService
                )
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}