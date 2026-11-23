package com.example.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.UUID

/**
 * Tiny local HTTP server used only for device-to-device backup transfer.
 * It binds to the device's LAN address and serves one encrypted backup once,
 * protected by a random token embedded in the QR code. It never uses the internet.
 */
class LocalSyncServer(
    private val payload: ByteArray,
    private val scope: CoroutineScope,
    private val transferCode: String? = null
) {
    private val token: String = UUID.randomUUID().toString()
    private val nonce: String = UUID.randomUUID().toString().replace("-", "")
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null

    suspend fun start(port: Int = DEFAULT_PORT): String = withContext(Dispatchers.IO) {
        val socket = ServerSocket(port)
        serverSocket = socket
        running = true
        val address = localIpAddress() ?: "127.0.0.1"
        val url = "http://$address:$port/sync?token=$token" +
            transferCode?.let { "&code=$it&nonce=$nonce" }.orEmpty()

        scope.launch(Dispatchers.IO) {
            while (running) {
                try {
                    val client = socket.accept()
                    launch(Dispatchers.IO) { handle(client) }
                } catch (_: Exception) {
                    // Socket closed by stop().
                    break
                }
            }
        }
        url
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private fun handle(client: java.net.Socket) {
        try {
            client.soTimeout = 5_000
            val reader = client.getInputStream().bufferedReader(Charsets.US_ASCII)
            val requestLine = reader.readLine() ?: return
            val expectedPath = "/sync?token=$token" +
                (transferCode?.let { "&code=$it&nonce=$nonce" } ?: "")
            if (!requestLine.contains(expectedPath)) {
                client.getOutputStream().use { it.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray()) }
                return
            }
            val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: ${payload.size}\r\n" +
                "Connection: close\r\n\r\n"
            client.getOutputStream().use { out ->
                out.write(header.toByteArray(Charsets.US_ASCII))
                out.write(payload)
                out.flush()
            }
        } catch (_: Exception) {
        } finally {
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun localIpAddress(): String? {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { address: InetAddress ->
                !address.isLoopbackAddress && address is java.net.Inet4Address && address.isSiteLocalAddress
            }?.hostAddress
    }

    companion object {
        const val DEFAULT_PORT = 8976
    }
}
