package com.example.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL

/** Downloads a backup from another device over the local network. */
object LocalSyncClient {
    private const val MAX_BYTES = 50 * 1024 * 1024

    suspend fun download(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = URI(url)
            require(uri.scheme.equals("http", ignoreCase = true)) {
                "Only local HTTP transfer is supported"
            }
            require(uri.host != null) { "Invalid sync address" }
            require(uri.path == "/sync") { "Invalid sync address" }
            require(uri.query?.startsWith("token=") == true) {
                "Missing transfer token"
            }

            // Never allow this client to be used as a general HTTP client.
            val address = InetAddress.getByName(uri.host)
            require(
                address.isSiteLocalAddress ||
                    address.isLoopbackAddress ||
                    address.isLinkLocalAddress
            ) {
                "Sync is only allowed on a local network"
            }

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Device refused the transfer (${connection.responseCode})")
                }
                connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_BYTES) { "Backup is too large" }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
