package com.example.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

/**
 * Tiny local HTTP server used only for device-to-device backup transfer.
 *
 * It binds to all interfaces (0.0.0.0) so phones on the same Wi-Fi can connect
 * regardless of which interface Android reports first, but it only advertises a
 * site-local IPv4 address in the QR code. It never reaches the internet, and it
 * keeps serving until the valid /sync transfer completes or the user cancels.
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

    suspend fun start(port: Int = 0): String = withContext(Dispatchers.IO) {
        // Bind to the wildcard address on an OS-assigned port (0) so we never
        // collide with another app, then advertise the actual bound port.
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), port))
        serverSocket = socket
        running = true
        val actualPort = socket.localPort
        val address = localIpAddress() ?: "127.0.0.1"
        val url = "http://$address:$actualPort/sync?token=$token" +
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

    private fun handle(client: Socket) {
        try {
            client.soTimeout = 15_000
            val reader = client.getInputStream().bufferedReader(Charsets.US_ASCII)
            val requestLine = reader.readLine() ?: return
            val expectedPath = "/sync?token=$token" +
                (transferCode?.let { "&code=$it&nonce=$nonce" } ?: "")
            if (!requestLine.contains(expectedPath)) {
                // Stray or wrong request: reject just this connection and keep serving.
                client.getOutputStream().use {
                    it.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                }
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

    /**
     * Prefers an address on an up, non-VPN Wi-Fi-like interface before falling
     * back to any site-local IPv4. This avoids advertising a virtual/VPN address.
     */
    private fun localIpAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
        val candidates = interfaces.flatMap { it.inetAddresses.toList() }
            .filter { address: InetAddress ->
                !address.isLoopbackAddress &&
                    address is java.net.Inet4Address &&
                    address.isSiteLocalAddress
            }
        if (candidates.isEmpty()) return null

        val preferred = interfaces
            .filter { it.isUp && !it.isLoopback }
            .sortedByDescending { scoreInterface(it) }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is java.net.Inet4Address && it.isSiteLocalAddress }
        return (preferred ?: candidates.first()).hostAddress
    }

    private fun scoreInterface(networkInterface: NetworkInterface): Int {
        val name = networkInterface.name.lowercase()
        return when {
            name.startsWith("wlan") -> 3
            name.startsWith("wifi") -> 3
            name.startsWith("ap") -> 2
            name.startsWith("eth") -> 1
            name.contains("vpn") || name.startsWith("tun") || name.startsWith("ppp") -> -1
            else -> 0
        }
    }

    companion object {
        const val DEFAULT_PORT = 0
    }
}