package com.mkulimafeeds.presentation.dealer.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.data.repository.dealer.DealerCustomerRepository
import com.mkulimafeeds.domain.model.dealer.DealerCustomer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DealerCustomerUiState(
    val isLoading: Boolean = false,
    val customers: List<DealerCustomer> = emptyList(),
    val error: String? = null
)

class DealerCustomerViewModel(
    private val repository: DealerCustomerRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DealerCustomerUiState())

    val uiState: StateFlow<DealerCustomerUiState> =
        _uiState.asStateFlow()

    fun loadCustomers(token: String) {
        if (token.isBlank()) {
            _uiState.value = DealerCustomerUiState(
                error = "Authentication token not found"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            repository
                .getCustomers(token)
                .onSuccess { customers ->
                    _uiState.value =
                        DealerCustomerUiState(
                            isLoading = false,
                            customers = customers
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        DealerCustomerUiState(
                            isLoading = false,
                            error =
                                error.message
                                    ?: "Failed to load customers"
                        )
                }
        }
    }

    fun clearError() {
        _uiState.value =
            _uiState.value.copy(error = null)
    }
}