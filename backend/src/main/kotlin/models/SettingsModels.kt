package com.movofeeds.models

import kotlinx.serialization.Serializable

/**
 * A single setting with its metadata.
 * Frontend uses valueType to decide which widget to render.
 */
@Serializable
data class SettingResponse(
    val key: String,
    val value: String,
    val valueType: String,        // INT | DECIMAL | STRING | BOOLEAN | ENUM
    val category: String,          // BULK_ORDER | DEALER | GENERAL
    val description: String,
    val enumOptions: List<String>? = null,  // only for ENUM type
    val updatedAt: Long
)

@Serializable
data class SettingUpdate(
    val key: String,
    val value: String
)

@Serializable
data class UpdateSettingsRequest(
    val changes: List<SettingUpdate>
)

@Serializable
data class UpdateSettingsResponse(
    val status: String,
    val message: String,
    val updated: List<SettingResponse>
)