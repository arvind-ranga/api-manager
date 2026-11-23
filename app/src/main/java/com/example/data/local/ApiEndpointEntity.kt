package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "api_endpoints")
data class ApiEndpointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    // Local stable record ID (used in backups and device-to-device sync).
    val remoteId: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val encryptedApiKey: String,
    val iv: String,
    val providerType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // null means the key never expires.
    val expiresAtMillis: Long? = null
)
