package com.mkulimafeeds.data.remote

import com.mkulimafeeds.data.remote.CustomerAnalyticsResponse
import com.mkulimafeeds.data.remote.dto.AdminUserDto
import com.mkulimafeeds.data.remote.dto.CreateAdminUserRequestDto
import com.mkulimafeeds.data.remote.dto.MeResponseDto
import com.mkulimafeeds.data.remote.dto.UpdateAdminUserRequestDto
import com.mkulimafeeds.data.remote.dto.auth.ChangePasswordRequestDto
import com.mkulimafeeds.data.remote.dto.customer.CustomerOrderResponseDto
import com.mkulimafeeds.data.remote.dto.dealer.DealerCustomerDto
import com.mkulimafeeds.data.remote.dto.dealer.DealerOrderResponseDto
import com.mkulimafeeds.data.remote.dto.dealer.DealerUpdateOrderStatusRequest
import com.mkulimafeeds.data.remote.dto.notification.MarkNotificationReadRequest
import com.mkulimafeeds.data.remote.dto.notification.NotificationResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import com.mkulimafeeds.data.remote.CreateSavedLocationRequest
import com.mkulimafeeds.data.remote.DeleteAccountRequest
import com.mkulimafeeds.data.remote.SavedLocationResponse
import com.mkulimafeeds.data.remote.UpdateProfileRequest
import com.mkulimafeeds.data.remote.UpdateSavedLocationRequest
import com.mkulimafeeds.data.remote.UserProfileResponse
import com.mkulimafeeds.data.remote.BulkOrderRequest
import com.mkulimafeeds.data.remote.BulkOrderValidationResponse
import com.mkulimafeeds.data.remote.DealerApplicationResponse
import com.mkulimafeeds.data.remote.PromoteDealerRequest
import com.mkulimafeeds.data.remote.ReviewDealerApplicationRequest
import com.mkulimafeeds.data.remote.RoleAuditResponse
import com.mkulimafeeds.data.remote.RoleMatrixResponse
import com.mkulimafeeds.data.remote.SuspendUserRequest

class ApiService(
    private val client: HttpClient
) {

    // =========================================================
    // AUTHENTICATION
    // =========================================================

    suspend fun login(
        identifier: String,
        password: String
    ): AuthResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    UnifiedLoginRequest(
                        identifier = identifier,
                        password = password
                    )
                )
            }
            .body()
    }
// =========================================================
// PUBLIC BRANDING
// =========================================================

    suspend fun getPublicBranding(): Map<String, String> {
        return client
            .get("${ApiConfig.BASE_URL}api/public/branding")
            .body()
    }
    // =========================================================
    // TWO-STEP REGISTRATION
    // =========================================================

    suspend fun registerInitiate(
        request: RegisterInitiateRequest
    ): RegisterInitiateResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/auth/register/initiate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .body()
    }

    suspend fun registerVerify(
        destination: String,
        code: String
    ): AuthResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/auth/register/verify") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterVerifyRequest(
                        destination = destination,
                        code = code
                    )
                )
            }
            .body()
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    suspend fun forgotPasswordInitiate(
        identifier: String
    ): ForgotPasswordInitiateResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/auth/forgot-password/initiate") {
                contentType(ContentType.Application.Json)
                setBody(
                    ForgotPasswordInitiateRequest(
                        identifier = identifier
                    )
                )
            }
            .body()
    }

    suspend fun forgotPasswordVerify(
        identifier: String,
        emailCode: String?,
        phoneCode: String?
    ): Map<String, String> {
        return client
            .post("${ApiConfig.BASE_URL}api/auth/forgot-password/verify") {
                contentType(ContentType.Application.Json)
                setBody(
                    ForgotPasswordVerifyRequest(
                        identifier = identifier,
                        emailCode = emailCode,
                        phoneCode = phoneCode
                    )
                )
            }
            .body()
    }

    suspend fun forgotPasswordReset(
        identifier: String,
        resetToken: String,
        newPassword: String
    ) {
        client
            .post("${ApiConfig.BASE_URL}api/auth/forgot-password/reset") {
                contentType(ContentType.Application.Json)
                setBody(
                    ForgotPasswordResetRequest(
                        identifier = identifier,
                        resetToken = resetToken,
                        newPassword = newPassword
                    )
                )
            }
    }

    suspend fun register(
        request: RegisterRequest
    ): AuthResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .body()
    }

    suspend fun changePassword(
        request: ChangePasswordRequestDto,
        token: String
    ) {
        client.put(
            "${ApiConfig.BASE_URL}api/auth/change-password"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(request)
        }
    }

    suspend fun getCurrentUser(
        token: String
    ): AuthMeResponse {
        return client
            .get("${ApiConfig.BASE_URL}api/auth/me") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun getUserProfile(
        token: String
    ): MeResponseDto {
        return client
            .get("${ApiConfig.BASE_URL}api/users/me") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    // =========================================================
    // USER PROFILE
    // =========================================================

    suspend fun getMyProfile(
        token: String
    ): UserProfileResponse {
        return client
            .get("${ApiConfig.BASE_URL}api/users/me/profile") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun updateMyProfile(
        request: UpdateProfileRequest,
        token: String
    ): UserProfileResponse {
        return client
            .put("${ApiConfig.BASE_URL}api/users/me") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun deleteMyAccount(
        password: String,
        confirmation: String,
        token: String
    ) {
        client.post("${ApiConfig.BASE_URL}api/users/me/delete") {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(
                DeleteAccountRequest(
                    password = password,
                    confirmation = confirmation
                )
            )
        }
    }

    // =========================================================
    // SAVED LOCATIONS
    // =========================================================

    suspend fun getMySavedLocations(
        token: String
    ): List<SavedLocationResponse> {
        return client
            .get("${ApiConfig.BASE_URL}api/users/me/locations") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun createSavedLocation(
        request: CreateSavedLocationRequest,
        token: String
    ): SavedLocationResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/users/me/locations") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun updateSavedLocation(
        id: Long,
        request: UpdateSavedLocationRequest,
        token: String
    ): SavedLocationResponse {
        return client
            .put("${ApiConfig.BASE_URL}api/users/me/locations/$id") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun deleteSavedLocation(
        id: Long,
        token: String
    ) {
        client.delete("${ApiConfig.BASE_URL}api/users/me/locations/$id") {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }
    }

    // =========================================================
    // BUSINESS RULES / SETTINGS
    // =========================================================

    suspend fun getAllSettings(
        token: String
    ): List<SettingResponse> {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/settings") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun updateSettings(
        changes: List<SettingUpdate>,
        token: String
    ): UpdateSettingsResponse {
        return client
            .put("${ApiConfig.BASE_URL}api/admin/settings") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(UpdateSettingsRequest(changes = changes))
            }
            .body()
    }

    // =========================================================
    // HEALTH
    // =========================================================

    suspend fun healthCheck(): String {
        return client
            .get("${ApiConfig.BASE_URL}api/health")
            .body()
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    suspend fun getProducts(): List<ProductResponseDto> {
        return client
            .get("${ApiConfig.BASE_URL}api/products")
            .body()
    }

    suspend fun getProduct(
        id: Long
    ): ProductResponseDto {
        return client
            .get("${ApiConfig.BASE_URL}api/products/$id")
            .body()
    }

    suspend fun createProduct(
        request: CreateProductRequestDto,
        token: String
    ): ProductResponseDto {
        return client
            .post("${ApiConfig.BASE_URL}api/products") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    // =========================================================
    // ADMIN USERS
    // =========================================================

    suspend fun getAdminUsers(
        token: String
    ): List<AdminUserDto> {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/users") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun createAdminUser(
        request: CreateAdminUserRequestDto,
        token: String
    ): AdminUserDto {
        return client
            .post("${ApiConfig.BASE_URL}api/admin/users") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun updateAdminUser(
        id: Long,
        request: UpdateAdminUserRequestDto,
        token: String
    ): AdminUserDto {
        return client
            .put("${ApiConfig.BASE_URL}api/admin/users/$id") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun deactivateAdminUser(
        id: Long,
        token: String
    ) {
        client.put(
            "${ApiConfig.BASE_URL}api/admin/users/$id/deactivate"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }
    }

    // =========================================================
    // ADMIN ORDERS
    // =========================================================

    suspend fun getAdminOrders(
        token: String
    ): List<OrderResponseDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/admin/orders"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun getAdminOrder(
        id: Long,
        token: String
    ): OrderResponseDto =
        client.get(
            "${ApiConfig.BASE_URL}api/admin/orders/$id"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun getAdminAnalytics(
        token: String
    ): AnalyticsOverviewResponse {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/analytics") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    // =========================================================
    // CUSTOMER ANALYTICS
    // =========================================================

    suspend fun getCustomerAnalytics(
        token: String
    ): CustomerAnalyticsResponse {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/customers/analytics") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun updateOrderStatus(
        id: Long,
        status: String,
        token: String
    ): OrderResponseDto =
        client.put(
            "${ApiConfig.BASE_URL}api/admin/orders/$id/status"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(UpdateOrderStatusRequest(status = status))
        }.body()

    suspend fun assignDealer(
        id: Long,
        dealerId: Long?,
        token: String
    ): OrderResponseDto =
        client.put(
            "${ApiConfig.BASE_URL}api/admin/orders/$id/dealer"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(mapOf("dealerId" to dealerId))
        }.body()

    suspend fun getAdminDealers(
        token: String
    ): List<DealerResponseDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/admin/dealers"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    // =========================================================
    // DEALER ORDERS
    // =========================================================

    suspend fun getDealerOrders(
        token: String
    ): List<DealerOrderResponseDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/dealer/orders"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun getDealerOrder(
        id: Long,
        token: String
    ): DealerOrderResponseDto =
        client.get(
            "${ApiConfig.BASE_URL}api/dealer/orders/$id"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun updateDealerOrderStatus(
        id: Long,
        status: String,
        token: String
    ): DealerOrderResponseDto =
        client.put(
            "${ApiConfig.BASE_URL}api/dealer/orders/$id/status"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(
                DealerUpdateOrderStatusRequest(status = status)
            )
        }.body()

    // =========================================================
    // CUSTOMER ORDERS
    // =========================================================

    suspend fun createOrder(
        request: CreateOrderRequest,
        token: String
    ): OrderResponseDto =
        client.post(
            "${ApiConfig.BASE_URL}api/orders"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(request)
        }.body()

    suspend fun getMyOrders(
        token: String
    ): List<OrderResponseDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/orders"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun getMyOrder(
        id: Long,
        token: String
    ): OrderResponseDto =
        client.get(
            "${ApiConfig.BASE_URL}api/orders/$id"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun cancelMyOrder(
        id: Long,
        token: String
    ): OrderResponseDto =
        client.put(
            "${ApiConfig.BASE_URL}api/orders/$id/cancel"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun getCustomerOrders(
        token: String
    ): List<CustomerOrderResponseDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/orders"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun getCustomerOrder(
        id: Long,
        token: String
    ): CustomerOrderResponseDto =
        client.get(
            "${ApiConfig.BASE_URL}api/orders/$id"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun cancelCustomerOrder(
        id: Long,
        token: String
    ): CustomerOrderResponseDto =
        client.put(
            "${ApiConfig.BASE_URL}api/orders/$id/cancel"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    // =========================================================
    // DEALER CUSTOMERS
    // =========================================================

    suspend fun getDealerCustomers(
        token: String
    ): List<DealerCustomerDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/dealer/customers"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    suspend fun getNotifications(
        token: String
    ): List<NotificationResponseDto> =
        client.get(
            "${ApiConfig.BASE_URL}api/notifications"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }.body()

    suspend fun markNotificationAsRead(
        id: Long,
        token: String
    ): NotificationResponseDto =
        client.put(
            "${ApiConfig.BASE_URL}api/notifications/$id/read"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(MarkNotificationReadRequest(isRead = true))
        }.body()

    suspend fun markAllNotificationsAsRead(
        token: String
    ) {
        client.put(
            "${ApiConfig.BASE_URL}api/notifications/read-all"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }
    }

    // =========================================================
    // PRODUCT MANAGEMENT
    // =========================================================

    suspend fun updateProduct(
        id: Long,
        request: UpdateProductRequestDto,
        token: String
    ): ProductResponseDto {
        return client
            .put("${ApiConfig.BASE_URL}api/products/$id") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    // =========================================================
    // PRODUCT IMAGE UPLOAD
    // =========================================================

    suspend fun uploadProductImage(
        id: Long,
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String,
        token: String
    ) {
        val response = client.put(
            "${ApiConfig.BASE_URL}api/products/$id/image"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")

            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = ChannelProvider {
                                ByteReadChannel(imageBytes)
                            },
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"file\"; filename=\"$fileName\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    mimeType
                                )
                            }
                        )
                    }
                )
            )
        }

        if (response.status.value !in 200..299) {
            val errorBody = try {
                response.bodyAsText()
            } catch (_: Exception) {
                "No error body"
            }
            throw IllegalStateException(
                "Image upload failed: HTTP ${response.status.value} - $errorBody"
            )
        }
    }

    // =========================================================
    // PRODUCT IMAGE DOWNLOAD
    // =========================================================

    suspend fun getProductImage(
        id: Long
    ): ByteArray {
        return client
            .get("${ApiConfig.BASE_URL}api/products/$id/image")
            .body()
    }

    // =========================================================
    // DELETE / DEACTIVATE PRODUCT
    // =========================================================

    suspend fun deleteProduct(
        id: Long,
        token: String
    ) {
        client.delete(
            "${ApiConfig.BASE_URL}api/products/$id"
        ) {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }
    }

    // =========================================================
    // ROLES & RBAC
    // =========================================================

    suspend fun getRoleMatrix(
        token: String
    ): RoleMatrixResponse {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/roles") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun promoteUserToDealer(
        userId: Long,
        reason: String?,
        token: String
    ) {
        client.post(
            "${ApiConfig.BASE_URL}api/admin/users/$userId/promote"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(PromoteDealerRequest(reason = reason))
        }
    }

    suspend fun demoteUserToCustomer(
        userId: Long,
        reason: String?,
        token: String
    ) {
        client.post(
            "${ApiConfig.BASE_URL}api/admin/users/$userId/demote"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(PromoteDealerRequest(reason = reason))
        }
    }

    suspend fun suspendUser(
        userId: Long,
        reason: String,
        token: String
    ) {
        client.post(
            "${ApiConfig.BASE_URL}api/admin/users/$userId/suspend"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(SuspendUserRequest(reason = reason))
        }
    }

    suspend fun reactivateUser(
        userId: Long,
        reason: String?,
        token: String
    ) {
        client.post(
            "${ApiConfig.BASE_URL}api/admin/users/$userId/reactivate"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(
                ReviewDealerApplicationRequest(reason = reason)
            )
        }
    }

    suspend fun getDealerApplications(
        token: String
    ): List<DealerApplicationResponse> {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/dealer-applications") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    suspend fun approveDealerApplication(
        userId: Long,
        reason: String?,
        token: String
    ) {
        client.post(
            "${ApiConfig.BASE_URL}api/admin/dealer-applications/$userId/approve"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(
                ReviewDealerApplicationRequest(reason = reason)
            )
        }
    }

    suspend fun rejectDealerApplication(
        userId: Long,
        reason: String?,
        token: String
    ) {
        client.post(
            "${ApiConfig.BASE_URL}api/admin/dealer-applications/$userId/reject"
        ) {
            contentType(ContentType.Application.Json)
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
            setBody(
                ReviewDealerApplicationRequest(reason = reason)
            )
        }
    }

    suspend fun getAuditLog(
        token: String,
        limit: Int = 100
    ): List<RoleAuditResponse> {
        return client
            .get("${ApiConfig.BASE_URL}api/admin/audit-log?limit=$limit") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }

    // =========================================================
// RESET SETTINGS TO DEFAULTS
// =========================================================

    suspend fun resetSettingsToDefaults(
        token: String
    ) {
        client.post("${ApiConfig.BASE_URL}api/admin/settings/reset") {
            header(ApiConfig.AUTH_HEADER, "Bearer $token")
        }
    }
    // =========================================================
    // DEALER BULK ORDERS
    // =========================================================

    suspend fun createBulkOrder(
        request: BulkOrderRequest,
        token: String
    ): OrderResponseDto {
        return client
            .post("${ApiConfig.BASE_URL}api/dealer/orders/bulk") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun validateBulkOrder(
        request: BulkOrderRequest,
        token: String
    ): BulkOrderValidationResponse {
        return client
            .post("${ApiConfig.BASE_URL}api/dealer/orders/bulk/validate") {
                contentType(ContentType.Application.Json)
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
                setBody(request)
            }
            .body()
    }

    suspend fun getMyBulkOrders(
        token: String
    ): List<OrderResponseDto> {
        return client
            .get("${ApiConfig.BASE_URL}api/dealer/orders/bulk") {
                header(ApiConfig.AUTH_HEADER, "Bearer $token")
            }
            .body()
    }
}