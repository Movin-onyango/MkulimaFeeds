package com.movofeeds.repository

import com.movofeeds.database.tables.NotificationsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.and

data class NotificationRecord(
    val id: Long,
    val userId: Long,
    val orderId: Long?,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)

class NotificationRepository {

    fun create(
        userId: Long,
        orderId: Long?,
        type: String,
        title: String,
        message: String,
        createdAt: Long
    ): NotificationRecord? {

        return transaction {

            val insertedId =
                NotificationsTable.insert {
                    it[NotificationsTable.userId] =
                        userId

                    it[NotificationsTable.orderId] =
                        orderId

                    it[NotificationsTable.type] =
                        type

                    it[NotificationsTable.title] =
                        title

                    it[NotificationsTable.message] =
                        message

                    it[NotificationsTable.isRead] =
                        false

                    it[NotificationsTable.createdAt] =
                        createdAt
                } get NotificationsTable.id

            findByIdInternal(
                insertedId.value
            )
        }
    }

    fun findById(
        id: Long
    ): NotificationRecord? {

        return transaction {
            findByIdInternal(id)
        }
    }

    fun findByUserId(
        userId: Long
    ): List<NotificationRecord> {

        return transaction {

            NotificationsTable
                .selectAll()
                .where {
                    NotificationsTable.userId eq userId
                }
                .orderBy(
                    NotificationsTable.createdAt,
                    SortOrder.DESC
                )
                .map {
                    it.toNotificationRecord()
                }
        }
    }

    fun markAsRead(
        id: Long,
        userId: Long
    ): NotificationRecord? {

        return transaction {

            NotificationsTable.update(
                where = {
                    (NotificationsTable.id eq id) and
                            (NotificationsTable.userId eq userId)
                }
            ) {
                it[NotificationsTable.isRead] =
                    true
            }

            findByIdInternal(id)
        }
    }

    fun markAllAsRead(
        userId: Long
    ) {

        transaction {

            NotificationsTable.update(
                where = {
                    NotificationsTable.userId eq userId
                }
            ) {
                it[NotificationsTable.isRead] =
                    true
            }
        }
    }

    private fun findByIdInternal(
        id: Long
    ): NotificationRecord? {

        return NotificationsTable
            .selectAll()
            .where {
                NotificationsTable.id eq id
            }
            .singleOrNull()
            ?.toNotificationRecord()
    }

    private fun org.jetbrains.exposed.sql.ResultRow
            .toNotificationRecord(): NotificationRecord {

        return NotificationRecord(
            id =
                this[NotificationsTable.id].value,

            userId =
                this[NotificationsTable.userId],

            orderId =
                this[NotificationsTable.orderId],

            type =
                this[NotificationsTable.type],

            title =
                this[NotificationsTable.title],

            message =
                this[NotificationsTable.message],

            isRead =
                this[NotificationsTable.isRead],

            createdAt =
                this[NotificationsTable.createdAt]
        )
    }
}