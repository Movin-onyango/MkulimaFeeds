package com.movofeeds.service

import com.movofeeds.repository.DealerCustomerRecord
import com.movofeeds.repository.DealerCustomerRepository

class DealerCustomerService(
    private val dealerCustomerRepository:
    DealerCustomerRepository =
        DealerCustomerRepository()
) {

    fun getDealerCustomers(
        dealerId: Long
    ): List<DealerCustomerRecord> {

        require(dealerId > 0) {
            "Invalid dealer ID"
        }

        return dealerCustomerRepository
            .findByDealerId(dealerId)
    }
}