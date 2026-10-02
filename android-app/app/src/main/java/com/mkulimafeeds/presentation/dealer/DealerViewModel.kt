package com.mkulimafeeds.presentation.dealer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.Dealer
import com.mkulimafeeds.data.mapper.toDealer
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.DealerRepository
import kotlinx.coroutines.launch

class DealerViewModel : ViewModel() {

    private val repository =
        DealerRepository(NetworkModule.apiService)

    fun loadAdminDealers(
        token: String,
        onSuccess: (List<Dealer>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val dealerDtos =
                    repository.getAdminDealers(token)

                val dealers =
                    dealerDtos.map {
                        it.toDealer()
                    }

                onSuccess(dealers)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to load dealers"
                )
            }
        }
    }
}