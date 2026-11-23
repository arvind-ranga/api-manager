package com.example.data.sync

import android.util.Base64

/**
 * Splits a backup JSON document into QR-friendly chunks for fully offline,
 * camera-to-camera transfer. No network and no cloud account required.
 */
object QrBackupCodec {
    private const val PREFIX = "APIMGR1"
    private const val CHUNK_SIZE = 700

    fun encodeChunks(json: String): List<String> {
        val encoded = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val chunks = encoded.chunked(CHUNK_SIZE)
        return chunks.mapIndexed { index, chunk ->
            "$PREFIX|${index + 1}|${chunks.size}|$chunk"
        }
    }

    data class Part(val index: Int, val total: Int, val data: String)

    fun decodePart(value: String): Part? {
        val split = value.split("|", limit = 4)
        if (split.size != 4 || split[0] != PREFIX) return null
        val index = split[1].toIntOrNull() ?: return null
        val total = split[2].toIntOrNull() ?: return null
        if (index < 1 || total < 1 || index > total || split[3].isBlank()) return null
        return Part(index, total, split[3])
    }

    fun assemble(parts: List<Part>): Result<String> = runCatching {
        val total = parts.firstOrNull()?.total ?: throw IllegalArgumentException("No QR data")
        require(parts.size == total) { "Missing QR chunks (${parts.size}/$total)" }
        require(parts.map { it.index }.toSet() == (1..total).toSet()) {
            "Duplicate or missing QR chunk numbers"
        }
        val encoded = parts.sortedBy { it.index }.joinToString("") { it.data }
        String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
    }
}
