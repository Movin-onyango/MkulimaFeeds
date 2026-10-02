package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.BulkOrderRequest
import com.mkulimafeeds.data.remote.BulkOrderValidationResponse
import com.mkulimafeeds.data.remote.DealerApplicationResponse
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.remote.OrderResponseDto
import com.mkulimafeeds.data.remote.RoleAuditResponse
import com.mkulimafeeds.data.remote.RoleMatrixResponse

class RoleRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    // =========================================================
    // ROLE MATRIX
    // =========================================================

    suspend fun getRoleMatrix(token: String): RoleMatrixResponse =
        apiService.getRoleMatrix(token)

    // =========================================================
    // USER ACTIONS
    // =========================================================

    suspend fun promoteUser(
        userId: Long,
        reason: String?,
        token: String
    ) = apiService.promoteUserToDealer(userId, reason, token)

    suspend fun demoteUser(
        userId: Long,
        reason: String?,
        token: String
    ) = apiService.demoteUserToCustomer(userId, reason, token)

    suspend fun suspendUser(
        userId: Long,
        reason: String,
        token: String
    ) = apiService.suspendUser(userId, reason, token)

    suspend fun reactivateUser(
        userId: Long,
        reason: String?,
        token: String
    ) = apiService.reactivateUser(userId, reason, token)

    // =========================================================
    // DEALER APPLICATIONS
    // =========================================================

    suspend fun getDealerApplications(
        token: String
    ): List<DealerApplicationResponse> =
        apiService.getDealerApplications(token)

    suspend fun approveDealerApplication(
        userId: Long,
        reason: String?,
        token: String
    ) = apiService.approveDealerApplication(userId, reason, token)

    suspend fun rejectDealerApplication(
        userId: Long,
        reason: String?,
        token: String
    ) = apiService.rejectDealerApplication(userId, reason, token)

    // =========================================================
    // AUDIT LOG
    // =========================================================

    suspend fun getAuditLog(
        token: String,
        limit: Int = 100
    ): List<RoleAuditResponse> =
        apiService.getAuditLog(token, limit)

    // =========================================================
    // BULK ORDERS (dealer only)
    // =========================================================

    suspend fun createBulkOrder(
        request: BulkOrderRequest,
        token: String
    ): OrderResponseDto = apiService.createBulkOrder(request, token)

    suspend fun validateBulkOrder(
        request: BulkOrderRequest,
        token: String
    ): BulkOrderValidationResponse =
        apiService.validateBulkOrder(request, token)

    suspend fun getMyBulkOrders(token: String): List<OrderResponseDto> =
        apiService.getMyBulkOrders(token)
}