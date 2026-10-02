package com.mkulimafeeds.presentation.dealer.customers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.R
import com.mkulimafeeds.domain.model.dealer.DealerCustomer

class DealerCustomerAdapter(
    private var customers: List<DealerCustomer>,
    private val onCustomerClicked:
        (DealerCustomer) -> Unit
) : RecyclerView.Adapter<DealerCustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val tvCustomerName: TextView =
            view.findViewById(R.id.tvCustomerName)

        val tvCustomerPhone: TextView =
            view.findViewById(R.id.tvCustomerPhone)

        val tvCustomerEmail: TextView =
            view.findViewById(R.id.tvCustomerEmail)

        val tvTotalOrders: TextView =
            view.findViewById(R.id.tvTotalOrders)

        val tvActiveOrders: TextView =
            view.findViewById(R.id.tvActiveOrders)

        val tvCompletedOrders: TextView =
            view.findViewById(R.id.tvCompletedOrders)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomerViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_dealer_customer,
                parent,
                false
            )

        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CustomerViewHolder,
        position: Int
    ) {

        val customer = customers[position]

        holder.tvCustomerName.text =
            customer.name

        holder.tvCustomerPhone.text =
            customer.phone

        holder.tvCustomerEmail.text =
            customer.email
                ?: "No email provided"

        holder.tvTotalOrders.text =
            "${customer.totalOrders}\nOrders"

        holder.tvActiveOrders.text =
            "${customer.activeOrders}\nActive"

        holder.tvCompletedOrders.text =
            "${customer.completedOrders}\nCompleted"

        holder.itemView.setOnClickListener {
            onCustomerClicked(customer)
        }
    }

    override fun getItemCount(): Int =
        customers.size

    fun updateList(
        newCustomers: List<DealerCustomer>
    ) {
        customers = newCustomers
        notifyDataSetChanged()
    }
}