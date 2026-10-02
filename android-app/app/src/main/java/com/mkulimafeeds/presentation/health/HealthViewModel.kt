package com.mkulimafeeds.presentation.health
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HealthUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class HealthViewModel(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.value = HealthUiState(isLoading = true)

            repository.checkHealth()
                .onSuccess { response ->
                    _uiState.value = HealthUiState(
                        isLoading = false,
                        message = response
                    )
                }
                .onFailure { exception ->
                    _uiState.value = HealthUiState(
                        isLoading = false,
                        error = exception.message ?: "Unable to connect to server"
                    )
                }
        }
    }
}