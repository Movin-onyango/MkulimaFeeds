package com.movofeeds.repository

import com.movofeeds.database.tables.OrdersTable
import com.movofeeds.database.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class DealerCustomerRecord(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String?,
    val totalOrders: Int,
    val activeOrders: Int,
    val completedOrders: Int
)

class DealerCustomerRepository {

    fun findByDealerId(
        dealerId: Long
    ): List<DealerCustomerRecord> =
        transaction {

            val customerIds =
                OrdersTable
                    .select(OrdersTable.customerId)
                    .where {
                        OrdersTable.assignedDealerId eq dealerId
                    }
                    .withDistinct()
                    .map {
                        it[OrdersTable.customerId].value
                    }

            if (customerIds.isEmpty()) {
                return@transaction emptyList()
            }

            UsersTable
                .selectAll()
                .where {
                    UsersTable.id inList customerIds
                }
                .map { userRow ->

                    val customerId =
                        userRow[UsersTable.id].value

                    val customerOrders =
                        OrdersTable
                            .selectAll()
                            .where {
                                (OrdersTable.assignedDealerId eq dealerId) and
                                        (OrdersTable.customerId eq customerId)
                            }

                    val totalOrders =
                        customerOrders.count().toInt()

                    val activeOrders =
                        customerOrders.count { order ->
                            when (
                                order[OrdersTable.status]
                                    .uppercase()
                            ) {
                                "PENDING",
                                "CONFIRMED",
                                "PROCESSING",
                                "READY" -> true

                                else -> false
                            }
                        }

                    val completedOrders =
                        customerOrders.count { order ->
                            order[OrdersTable.status]
                                .equals(
                                    "COMPLETED",
                                    ignoreCase = true
                                )
                        }

                    DealerCustomerRecord(
                        id = customerId,

                        name =
                            userRow[UsersTable.name],

                        phone =
                            userRow[UsersTable.phone].orEmpty(),

                        email =
                            userRow[UsersTable.email].orEmpty(),

                        totalOrders =
                            totalOrders,

                        activeOrders =
                            activeOrders,

                        completedOrders =
                            completedOrders
                    )
                }
                .sortedBy {
                    it.name.lowercase()
                }
        }
}