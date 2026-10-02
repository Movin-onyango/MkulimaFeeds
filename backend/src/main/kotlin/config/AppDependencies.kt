package com.movofeeds.config

import com.movofeeds.service.AuthService
import com.movofeeds.service.HealthService
import com.movofeeds.service.OrderService

class AppDependencies {

    val healthService = HealthService()

    val authService = AuthService()

    val orderService = OrderService()
}