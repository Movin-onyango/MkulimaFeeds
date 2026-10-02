package com.movofeeds
import com.movofeeds.routes.customerAnalyticsRoutes
import com.movofeeds.service.CustomerAnalyticsService
import com.movofeeds.routes.dealerOrderRoutes
import com.movofeeds.routes.dealerCustomerRoutes
import com.movofeeds.service.DealerCustomerService
import com.movofeeds.config.AppDependencies
import com.movofeeds.routes.authRoutes
import com.movofeeds.routes.healthRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import com.movofeeds.routes.userRoutes
import com.movofeeds.routes.productRoutes
import com.movofeeds.service.ProductService
import com.movofeeds.routes.orderRoutes
import com.movofeeds.service.OrderService
import com.movofeeds.routes.notificationRoutes
import com.movofeeds.service.NotificationService
import com.movofeeds.routes.analyticsRoutes
import com.movofeeds.service.AnalyticsService
import io.ktor.http.HttpStatusCode
import com.movofeeds.routes.settingsRoutes
import com.movofeeds.service.SettingsService
import com.movofeeds.routes.roleRoutes
import com.movofeeds.service.RoleService
import com.movofeeds.routes.dealerBulkOrderRoutes
import com.movofeeds.service.BulkOrderService
fun Application.configureRouting() {

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
            }
        )
    }

    val dependencies = AppDependencies()

    routing {

        get("/") {
            call.respondText("Hello, World!")
        }
        // TEMPORARY — remove after testing Pass A
        get("/api/_test/otp") {
            val svc = com.movofeeds.service.VerificationService()
            val result = svc.sendVerificationCode(
                userId = null,
                purpose = com.movofeeds.service.VerificationService.PURPOSE_REGISTER,
                channel = com.movofeeds.service.VerificationService.CHANNEL_EMAIL,
                destination = "test@example.com"
            )
            if (result.isSuccess) {
                call.respondText("Code sent (check backend console)")
            } else {
                call.respondText(
                    "Failed: ${result.exceptionOrNull()?.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        healthRoutes(
            dependencies.healthService
        )

        authRoutes(
            dependencies.authService
        )
        userRoutes()
        productRoutes(
            productService = ProductService()
        )
        orderRoutes(
            orderService = OrderService()
        )
        dealerOrderRoutes(
            orderService = OrderService()
        )
        notificationRoutes(
            notificationService =
                NotificationService()
        )
        dealerCustomerRoutes(
            dealerCustomerService =
                DealerCustomerService()
        )
        analyticsRoutes(
            analyticsService = AnalyticsService()
        )
        customerAnalyticsRoutes(
            customerAnalyticsService =
                CustomerAnalyticsService()
        )
        settingsRoutes(
            settingsService = SettingsService()
        )
        roleRoutes(
            roleService = RoleService()
        )
        dealerBulkOrderRoutes(
            bulkOrderService = BulkOrderService(),
            orderService = OrderService()
        )
    }
}