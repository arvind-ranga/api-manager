package com.example.data.sync

import org.json.JSONObject

/**
 * A QR/LAN transfer carries the backup and its one-time encryption key together.
 * The sender adds this wrapper so the receiver never has to type a separate
 * transfer code. Locking the app itself still requires the usual PIN or biometrics.
 */
object TransferPayload {
    const val FORMAT = "api-manager-transfer"
    const val VERSION = 2

    data class Package(
        val transferCode: String,
        val backupJson: String
    )

    fun wrap(backupJson: String, transferCode: String): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("transferCode", transferCode)
        put("backup", JSONObject(backupJson))
    }.toString()

    fun unwrap(json: String): Result<Package> = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) {
            "This is not an API Manager transfer"
        }
        require(root.optInt("version", -1) == VERSION) {
            "This transfer was created by an unsupported app version"
        }
        val code = root.optString("transferCode").trim()
        require(code.isNotBlank()) {
            "This transfer is missing its key. Update both devices and try again."
        }
        val backup = root.optJSONObject("backup")
            ?: throw IllegalArgumentException("Transfer is missing its backup")
        Package(transferCode = code, backupJson = backup.toString())
    }
}