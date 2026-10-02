package com.mkulimafeeds

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.dealer.DealerCustomerRepository
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerAdapter
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerViewModel
import com.mkulimafeeds.presentation.dealer.customers.DealerCustomerViewModelFactory
import kotlinx.coroutines.launch

class DealerCustomersActivity :
    AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    private lateinit var recyclerView:
            RecyclerView

    private lateinit var progressBar:
            ProgressBar

    private lateinit var tvEmpty:
            TextView

    private lateinit var adapter:
            DealerCustomerAdapter

    private val viewModel:
            DealerCustomerViewModel by viewModels {

        DealerCustomerViewModelFactory(
            DealerCustomerRepository(
                NetworkModule.apiService
            )
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_dealer_customers
        )

        tokenManager =
            TokenManager(this)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        observeUi()
        loadCustomers()
    }

    private fun setupViews() {

        recyclerView =
            findViewById(
                R.id.recyclerCustomers
            )

        progressBar =
            findViewById(
                R.id.progressCustomers
            )

        tvEmpty =
            findViewById(
                R.id.tvEmptyCustomers
            )
    }

    private fun setupToolbar() {

        findViewById<MaterialToolbar>(
            R.id.toolbarCustomers
        ).setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {

        adapter =
            DealerCustomerAdapter(
                customers = emptyList()
            ) { customer ->

                val intent = android.content.Intent(
                    this,
                    DealerCustomerDetailsActivity::class.java
                ).apply {
                    putExtra(
                        "CUSTOMER_ID",
                        customer.id
                    )
                    putExtra(
                        "CUSTOMER_NAME",
                        customer.name
                    )
                    putExtra(
                        "CUSTOMER_PHONE",
                        customer.phone
                    )
                    putExtra(
                        "CUSTOMER_EMAIL",
                        customer.email
                    )
                }

                startActivity(intent)
            }


        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter =
            adapter
    }

    private fun observeUi() {

        lifecycleScope.launch {

            viewModel.uiState.collect { state ->

                progressBar.visibility =
                    if (state.isLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                adapter.updateList(
                    state.customers
                )

                tvEmpty.visibility =
                    if (
                        !state.isLoading &&
                        state.customers.isEmpty()
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                state.error?.let { error ->

                    Toast.makeText(
                        this@DealerCustomersActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()

                    viewModel.clearError()
                }
            }
        }
    }

    private fun loadCustomers() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        viewModel.loadCustomers(
            token
        )
    }
}