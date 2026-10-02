/*package com.mkulimafeeds.presentation.health
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mkulimafeeds.R

class HealthFragment : Fragment(R.layout.fragment_health) {

    private lateinit var viewModel: HealthViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusText = view.findViewById<TextView>(R.id.statusText)
        val checkButton = view.findViewById<Button>(R.id.checkButton)

        viewModel = ViewModelProvider(
            this,
            HealthViewModelFactory()
        )[HealthViewModel::class.java]

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            statusText.text = when {
                state.isLoading -> "Checking server..."
                state.message != null -> state.message
                state.error != null -> "Error: ${state.error}"
                else -> "Ready"
            }
        }

        checkButton.setOnClickListener {
            viewModel.checkHealth()
        }
    }
}
*/