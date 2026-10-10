package id.anemona.clicker.ntp

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

/**
 * Minimal SNTP v4 client (RFC 5905). Returns the round-trip time and the offset to apply to
 * [System.currentTimeMillis] to get UTC time as the server sees it.
 *
 * Everything is off the main thread: callers get a callback on [executor].
 */
class NtpClient {

    data class Result(
        /** Add this to System.currentTimeMillis() to obtain corrected UTC. */
        val offsetMs: Double,
        /** Network round-trip observed for the query. */
        val rttMs: Double,
    )

    fun query(server: String, timeoutMs: Int, onDone: (Result?) -> Unit) {
        EXECUTOR.execute {
            val r = runCatching { querySync(server, timeoutMs) }
                .onFailure { Log.w(TAG, "ntp query failed", it) }
                .getOrNull()
            MAIN.post { onDone(r) }
        }
    }

    private fun querySync(server: String, timeoutMs: Int): Result? {
        val socket = DatagramSocket()
        return socket.use {
            it.soTimeout = timeoutMs
            val address = InetAddress.getByName(server)
            val packet = ByteArray(NTP_PACKET_SIZE)
            // LI = 0 (no warning), VN = 4, Mode = 3 (client).
            packet[0] = 0b00_100_011
            val request = DatagramPacket(packet, packet.size, address, NTP_PORT)
            val t0 = System.currentTimeMillis()
            it.send(request)
            val response = DatagramPacket(ByteArray(NTP_PACKET_SIZE), NTP_PACKET_SIZE)
            it.receive(response)
            val t3 = System.currentTimeMillis()

            val data = response.data
            // Transmit timestamp (T2, server send time), bytes 40..47.
            val seconds = u32(data, TRANSMIT_SECONDS_OFFSET)
            val fraction = u32(data, TRANSMIT_FRACTION_OFFSET)
            if (seconds == 0L) return null

            val t2 = (seconds * 1000.0) + (fraction * 1000.0 / TWO_POW_32) - NTP_EPOCH_OFFSET_MS
            // Destination (T3) is captured above in the same clock domain as t0.
            // Round-trip is the full wall time from send to receive; no server receive
            // timestamp (T1) is returned in the reply, so this is an upper bound on RTT.
            val rtt = (t3 - t0).toDouble()
            // Classic SNTP offset: ((T2-T0)+(T2-T3))/2, symmetric when RTT is asymmetric-free.
            val offset = ((t2 - t0) + (t2 - t3)) / 2.0
            Result(offsetMs = offset, rttMs = rtt)
        }
    }

    private fun u32(b: ByteArray, off: Int): Long {
        return ((b[off].toLong() and 0xFF) shl 24) or
            ((b[off + 1].toLong() and 0xFF) shl 16) or
            ((b[off + 2].toLong() and 0xFF) shl 8) or
            (b[off + 3].toLong() and 0xFF)
    }

    companion object {
        private const val TAG = "NtpClient"
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_PORT = 123
        private const val TRANSMIT_SECONDS_OFFSET = 40
        private const val TRANSMIT_FRACTION_OFFSET = 44
        private const val TWO_POW_32 = 4294967296.0
        private const val NTP_EPOCH_OFFSET_MS = 2_208_988_800_000.0
        private val EXECUTOR = Executors.newSingleThreadExecutor { r ->
            Thread(r, "ntp").apply { isDaemon = true }
        }
        private val MAIN = android.os.Handler(android.os.Looper.getMainLooper())
    }
}
