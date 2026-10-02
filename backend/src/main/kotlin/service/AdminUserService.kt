package com.movofeeds.service

import com.movofeeds.models.AdminUserResponse
import com.movofeeds.models.CreateAdminUserRequest
import com.movofeeds.models.UpdateAdminUserRequest
import com.movofeeds.repository.OrderRepository
import com.movofeeds.repository.UserRecord
import com.movofeeds.repository.UserRepository
import com.movofeeds.security.PasswordHasher

class AdminUserService(
    private val userRepository: UserRepository = UserRepository(),
    private val orderRepository: OrderRepository = OrderRepository()
) {

    private val allowedRoles =
        setOf(
            "CUSTOMER",
            "DEALER",
            "STAFF"
        )

    fun getAllUsers(): List<AdminUserResponse> {
        return userRepository
            .findAll()
            .map { it.toResponse() }
    }

    fun getUser(id: Long): AdminUserResponse {
        require(id > 0) {
            "Invalid user ID"
        }

        val user =
            userRepository.findById(id)
                ?: throw NoSuchElementException("User not found")

        return user.toResponse()
    }

    fun createUser(
        request: CreateAdminUserRequest
    ): AdminUserResponse {

        val name = request.name.trim()
        val phone = request.phone.trim()
        val email = request.email.trim().lowercase()
        val password = request.password
        val role = request.role.trim().uppercase()

        require(name.isNotBlank()) {
            "Name is required"
        }

        require(phone.isNotBlank()) {
            "Phone is required"
        }

        require(email.isNotBlank()) {
            "Email is required"
        }

        require(password.length >= 8) {
            "Password must be at least 8 characters"
        }

        require(role in allowedRoles) {
            "Invalid user role"
        }

        require(!userRepository.existsByEmail(email)) {
            "A user with this email already exists"
        }

        require(!userRepository.existsByPhone(phone)) {
            "A user with this phone number already exists"
        }

        val passwordHash =
            PasswordHasher.hash(password)

        val createdUser =
            userRepository.createUser(
                name = name,
                phone = phone,
                email = email,
                passwordHash = passwordHash,
                role = role
            )

        return createdUser.toResponse()
    }

    fun updateUser(
        id: Long,
        request: UpdateAdminUserRequest
    ): AdminUserResponse {

        require(id > 0) {
            "Invalid user ID"
        }

        val existingUser =
            userRepository.findById(id)
                ?: throw NoSuchElementException("User not found")

        val name = request.name.trim()
        val phone = request.phone.trim()
        val email = request.email.trim().lowercase()

        require(name.isNotBlank()) {
            "Name is required"
        }

        require(phone.isNotBlank()) {
            "Phone is required"
        }

        require(email.isNotBlank()) {
            "Email is required"
        }

        val emailOwner =
            userRepository.findByEmail(email)

        require(
            emailOwner == null ||
                    emailOwner.id == existingUser.id
        ) {
            "A user with this email already exists"
        }

        val phoneOwner =
            userRepository.findByPhone(phone)

        require(
            phoneOwner == null ||
                    phoneOwner.id == existingUser.id
        ) {
            "A user with this phone number already exists"
        }

        val updatedUser =
            userRepository.update(
                id = id,
                name = name,
                phone = phone,
                email = email
            )
                ?: throw NoSuchElementException(
                    "User not found"
                )

        return updatedUser.toResponse()
    }

    fun deactivateUser(id: Long): Boolean {

        require(id > 0) {
            "Invalid user ID"
        }

        val user =
            userRepository.findById(id)
                ?: throw NoSuchElementException(
                    "User not found"
                )

        require(user.isActive) {
            "User is already inactive"
        }

        return userRepository.deactivate(user.id)
    }

    private fun UserRecord.toResponse(): AdminUserResponse {
        return AdminUserResponse(
            id = id,
            name = name,
            phone = phone,
            email = email,
            role = role,
            status = status,
            dealerStatus = dealerStatus,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            totalOrders = orderRepository.countByCustomerId(id)
        )
    }
}