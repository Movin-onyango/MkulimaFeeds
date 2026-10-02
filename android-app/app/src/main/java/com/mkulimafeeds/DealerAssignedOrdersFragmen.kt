package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.presentation.dealer.orders.DealerAssignedOrdersAdapter
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModel
import com.mkulimafeeds.presentation.dealer.orders.DealerOrderViewModelFactory
import kotlinx.coroutines.launch

class DealerAssignedOrdersFragment : Fragment() {

    private lateinit var rvAssigned: RecyclerView
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var tvError: android.widget.TextView
    private lateinit var tvSummary: android.widget.TextView
    private lateinit var adapter: DealerAssignedOrdersAdapter
    private lateinit var tokenManager: TokenManager

    private val viewModel: DealerOrderViewModel by lazy {
        ViewModelProvider(
            this,
            DealerOrderViewModelFactory(
                DealerOrderRepository(NetworkModule.apiService)
            )
        )[DealerOrderViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_dealer_assigned_orders,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        rvAssigned = view.findViewById(R.id.rvAssignedOrders)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvError = view.findViewById(R.id.tvError)
        tvSummary = view.findViewById(R.id.tvSummary)

        adapter = DealerAssignedOrdersAdapter(
            orders = emptyList()
        ) { order ->
            val intent = Intent(requireContext(), AssignedOrdersActivity::class.java)
            intent.putExtra("ORDER_ID", order.id)
            startActivity(intent)
        }

        rvAssigned.layoutManager = LinearLayoutManager(requireContext())
        rvAssigned.adapter = adapter

        observeViewModel()
        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        val token = tokenManager.getToken() ?: return
        viewModel.loadAssignedOrders(token)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                tvSummary.text = when (state.orders.size) {
                    0 -> "No orders assigned yet"
                    1 -> "1 assigned order"
                    else -> "${state.orders.size} assigned orders"
                }
                adapter.submitList(state.orders)
                tvEmpty.visibility = if (state.orders.isEmpty() && !state.isLoading)
                    View.VISIBLE else View.GONE
                tvError.visibility = if (state.error != null) View.VISIBLE else View.GONE
                state.error?.let { tvError.text = it }
            }
        }
    }
}