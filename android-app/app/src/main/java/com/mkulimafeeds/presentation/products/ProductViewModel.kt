package com.mkulimafeeds.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class ProductViewModel : ViewModel() {

    private val repository =
        ProductRepository(NetworkModule.apiService)

    private val _uiState =
        MutableStateFlow(ProductUiState())

    val uiState: StateFlow<ProductUiState> =
        _uiState.asStateFlow()

    fun loadProducts() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val products =
                    repository.getProducts()

                _uiState.value =
                    _uiState.value.copy(
                        products = products,
                        isLoading = false,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                            ?: "Failed to load products"
                    )
            }
        }
    }

    fun searchProducts(query: String) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    searchQuery = query,
                    isLoading = true,
                    error = null
                )

            try {

                val products =
                    if (query.isBlank()) {
                        repository.getProducts()
                    } else {
                        repository.searchProducts(query)
                    }

                _uiState.value =
                    _uiState.value.copy(
                        searchQuery = query,
                        products = products,
                        isLoading = false,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                            ?: "Failed to search products"
                    )
            }
        }
    }
}