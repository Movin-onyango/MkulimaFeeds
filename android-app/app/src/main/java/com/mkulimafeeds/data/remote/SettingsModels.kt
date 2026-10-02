package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SettingResponse(
    val key: String,
    val value: String,
    val valueType: String,
    val category: String,
    val description: String,
    val enumOptions: List<String>? = null,
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