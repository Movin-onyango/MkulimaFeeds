package com.mkulimafeeds

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
import com.mkulimafeeds.data.repository.dealer.DealerCustomerRepository
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerAdapter
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerViewModel
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerViewModelFactory
import kotlinx.coroutines.launch

class DealerCustomersFragment : Fragment() {

    private lateinit var rvCustomers: RecyclerView
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var adapter: DealerCustomerAdapter
    private lateinit var tokenManager: TokenManager

    private val viewModel: DealerCustomerViewModel by lazy {
        ViewModelProvider(
            this,
            DealerCustomerViewModelFactory(
                DealerCustomerRepository(NetworkModule.apiService)
            )
        )[DealerCustomerViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_dealer_customers,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        rvCustomers = view.findViewById(R.id.rvCustomers)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = DealerCustomerAdapter(
            customers = emptyList(),
            onCustomerClicked = { customer ->
                val intent = android.content.Intent(
                    requireContext(),
                    DealerCustomerDetailsActivity::class.java
                ).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                    putExtra("CUSTOMER_NAME", customer.name)
                    putExtra("CUSTOMER_PHONE", customer.phone)
                    putExtra("CUSTOMER_EMAIL", customer.email.orEmpty())
                }
                startActivity(intent)
            }
        )

        rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        rvCustomers.adapter = adapter

        observeViewModel()
        loadCustomers()
    }

    override fun onResume() {
        super.onResume()
        loadCustomers()
    }

    private fun loadCustomers() {
        val token = tokenManager.getToken() ?: return
        viewModel.loadCustomers(token)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.updateList(state.customers)
                tvEmpty.visibility =
                    if (state.customers.isEmpty() && !state.isLoading)
                        View.VISIBLE else View.GONE
            }
        }
    }
}