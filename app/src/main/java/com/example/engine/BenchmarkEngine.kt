package com.example.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.math.abs
import kotlin.math.max

/**
 * Test configuration parameters for synthetic network benchmark passes.
 *
 * @property pingCount Number of consecutive round-trip ping datagrams transmitted in Stage 1.
 * @property streamDurationSeconds Duration in seconds for continuous throughput stress testing.
 * @property streamTargetHz Target packet dispatch rate per second during streaming phase.
 * @property burstPacketCount Number of back-to-back packets sent during socket burst resilience tests.
 */
data class BenchmarkConfig(
    val pingCount: Int = 40,
    val streamDurationSeconds: Int = 3,
    val streamTargetHz: Int = 120,
    val burstPacketCount: Int = 100
)

/**
 * Diagnostic results report generated upon completion of a full network benchmark run.
 */
data class BenchmarkReport(
    val testTimestamp: Long = System.currentTimeMillis(),
    val targetIp: String,
    val targetPort: Int,
    val pingsSent: Int,
    val pongsReceived: Int,
    val pingLossPct: Float,
    val minLatencyMs: Long,
    val avgLatencyMs: Double,
    val medianLatencyMs: Long,
    val p90LatencyMs: Long = 0L,
    val p95LatencyMs: Long,
    val p99LatencyMs: Long = 0L,
    val maxLatencyMs: Long,
    val jitterMs: Double,
    val jitterStdDevMs: Double = 0.0,
    val streamPacketsSent: Int,
    val streamPacketsAcked: Int,
    val streamMeasuredHz: Double,
    val grade: String,
    val recommendation: String
)

/**
 * Observable UI state model for active and completed network benchmark executions.
 */
sealed class BenchmarkState {
    object Idle : BenchmarkState()
    data class Running(
        val stage: String,
        val progress: Float,
        val currentLatencyMs: Long = 0,
        val currentHz: Double = 0.0
    ) : BenchmarkState()
    data class Completed(val report: BenchmarkReport) : BenchmarkState()
    data class Error(val message: String) : BenchmarkState()
}

/**
 * High-performance network diagnostic benchmark engine for Wi-Fi latency and throughput profiling.
 *
 * Runs multi-stage UDP stress tests against local and remote GlidePad receiver hosts to measure
 * round-trip times (min, avg, p95, max), RFC 3550 jitter, packet loss percentage, and sustained
 * streaming throughput in Hertz (packets per second).
 */
class BenchmarkEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _state = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val state: StateFlow<BenchmarkState> = _state.asStateFlow()

    private var activeJob: Job? = null

    fun reset() {
        activeJob?.cancel()
        _state.value = BenchmarkState.Idle
    }

    fun runFullBenchmark(
        targetIp: String,
        targetPort: Int = 8089,
        config: BenchmarkConfig = BenchmarkConfig()
    ) {
        activeJob?.cancel()
        activeJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    soTimeout = 1500
                }
                val addr = InetAddress.getByName(targetIp)

                // -------------------------------------------------------------
                // Stage 1: RTT Latency & RFC 3550 Jitter Assessment
                // -------------------------------------------------------------
                _state.value = BenchmarkState.Running("Measuring Round-Trip Latency...", 0.05f)

                val latencies = mutableListOf<Long>()
                var previousInterval = 0L
                var calculatedJitter = 0.0

                for (i in 1..config.pingCount) {
                    val clientSeq = i
                    val sendTime = System.currentTimeMillis()

                    val pingJson = JSONObject().apply {
                        put("type", "benchmark_ping")
                        put("seq", clientSeq)
                        put("time", sendTime)
                        put("client_port", socket.localPort)
                    }
                    val sendBytes = pingJson.toString().toByteArray()
                    socket.send(DatagramPacket(sendBytes, sendBytes.size, addr, targetPort))

                    // Wait for echo
                    val buffer = ByteArray(1024)
                    val recvPacket = DatagramPacket(buffer, buffer.size)

                    try {
                        socket.receive(recvPacket)
                        val recvTime = System.currentTimeMillis()
                        val rtt = max(1L, recvTime - sendTime)
                        latencies.add(rtt)

                        if (latencies.size > 1) {
                            val interval = rtt
                            val diff = abs(interval - previousInterval)
                            calculatedJitter = updateRfc3550Jitter(calculatedJitter, diff.toDouble())
                            previousInterval = interval
                        } else {
                            previousInterval = rtt
                        }
                    } catch (e: SocketTimeoutException) {
                        // Packet dropped
                    }

                    val progress = 0.05f + (i.toFloat() / config.pingCount) * 0.45f
                    val latestRtt = if (latencies.isNotEmpty()) latencies.last() else 0L
                    _state.value = BenchmarkState.Running(
                        stage = "Ping Latency: $i/${config.pingCount} (${latestRtt}ms)",
                        progress = progress,
                        currentLatencyMs = latestRtt
                    )

                    delay(25) // 40Hz test pace
                }

                if (latencies.isEmpty()) {
                    _state.value = BenchmarkState.Error(
                        "No replies from $targetIp:$targetPort. Ensure glidepad_benchmark_receiver_v1_1.py is running on your PC. You can copy the script from the benchmark screen."
                    )
                    return@launch
                }

                // -------------------------------------------------------------
                // Stage 2: High-Frequency Stream & Throughput Trial
                // -------------------------------------------------------------
                _state.value = BenchmarkState.Running("Testing 120Hz Stream Throughput...", 0.55f)

                // Start server-side session
                val sessionId = "trial_${System.currentTimeMillis()}"
                val startJson = JSONObject().apply {
                    put("type", "benchmark_start")
                    put("session_id", sessionId)
                }
                val startBytes = startJson.toString().toByteArray()
                socket.send(DatagramPacket(startBytes, startBytes.size, addr, targetPort))

                val streamPacketsToSend = config.streamDurationSeconds * config.streamTargetHz
                val intervalDelayMs = (1000L / config.streamTargetHz).coerceAtLeast(1L)
                val streamStartTime = System.currentTimeMillis()

                for (seq in 1..streamPacketsToSend) {
                    val streamJson = JSONObject().apply {
                        put("type", "benchmark_packet")
                        put("seq", seq)
                        put("time", System.currentTimeMillis())
                    }
                    val data = streamJson.toString().toByteArray()
                    socket.send(DatagramPacket(data, data.size, addr, targetPort))

                    val progress = 0.55f + (seq.toFloat() / streamPacketsToSend) * 0.40f
                    val currentRate = (seq.toDouble() / ((System.currentTimeMillis() - streamStartTime).coerceAtLeast(1) / 1000.0))
                    _state.value = BenchmarkState.Running(
                        stage = "Streaming Packets (${seq}/$streamPacketsToSend) @ ${currentRate.toInt()}Hz",
                        progress = progress,
                        currentHz = currentRate
                    )
                    delay(intervalDelayMs)
                }

                val streamActualDuration = (System.currentTimeMillis() - streamStartTime) / 1000.0
                val measuredHz = streamPacketsToSend / max(0.1, streamActualDuration)

                // Notify finish and request server summary
                val finishJson = JSONObject().apply {
                    put("type", "benchmark_finish")
                    put("session_id", sessionId)
                }
                val finishBytes = finishJson.toString().toByteArray()
                socket.send(DatagramPacket(finishBytes, finishBytes.size, addr, targetPort))

                var serverAckedPackets = streamPacketsToSend
                try {
                    val finishBuffer = ByteArray(2048)
                    val finishPacket = DatagramPacket(finishBuffer, finishBuffer.size)
                    socket.receive(finishPacket)
                    val finishStr = String(finishPacket.data, 0, finishPacket.length)
                    val resObj = JSONObject(finishStr)
                    val summaryObj = resObj.optJSONObject("summary")
                    if (summaryObj != null) {
                        serverAckedPackets = summaryObj.optInt("packets_received", streamPacketsToSend)
                    }
                } catch (e: Exception) {
                    // Use sent count if summary packet timed out
                }

                // -------------------------------------------------------------
                // -------------------------------------------------------------
                // Stage 3: Statistical Synthesis & Grading
                // -------------------------------------------------------------
                val sorted = latencies.sorted()
                val n = sorted.size
                val minLat = sorted.first()
                val maxLat = sorted.last()
                val avgLat = sorted.average()
                val medianLat = calculatePercentile(sorted, 0.50)
                val p90Lat = calculatePercentile(sorted, 0.90)
                val p95Lat = calculatePercentile(sorted, 0.95)
                val p99Lat = calculatePercentile(sorted, 0.99)
                val stdDev = calculateStdDev(sorted, avgLat)

                val pingLossPct = ((config.pingCount - n).toFloat() / config.pingCount) * 100f

                val (grade, recommendation) = evaluatePerformance(avgLat, p95Lat, pingLossPct, calculatedJitter)

                val report = BenchmarkReport(
                    targetIp = targetIp,
                    targetPort = targetPort,
                    pingsSent = config.pingCount,
                    pongsReceived = n,
                    pingLossPct = pingLossPct,
                    minLatencyMs = minLat,
                    avgLatencyMs = avgLat,
                    medianLatencyMs = medianLat,
                    p90LatencyMs = p90Lat,
                    p95LatencyMs = p95Lat,
                    p99LatencyMs = p99Lat,
                    maxLatencyMs = maxLat,
                    jitterMs = calculatedJitter,
                    jitterStdDevMs = stdDev,
                    streamPacketsSent = streamPacketsToSend,
                    streamPacketsAcked = serverAckedPackets,
                    streamMeasuredHz = measuredHz,
                    grade = grade,
                    recommendation = recommendation
                )

                _state.value = BenchmarkState.Completed(report)

            } catch (e: Exception) {
                _state.value = BenchmarkState.Error("Benchmark failed: ${e.localizedMessage}")
            } finally {
                socket?.close()
            }
        }
    }

    companion object {
        /**
         * RFC 3550 jitter calculation: J(i) = J(i-1) + (|D(i-1, i)| - J(i-1)) / 16
         */
        fun updateRfc3550Jitter(currentJitter: Double, diff: Double): Double {
            return currentJitter + (abs(diff) - currentJitter) / 16.0
        }

        /**
         * Calculates percentile from sorted values using clamped indexing.
         */
        fun calculatePercentile(sortedValues: List<Long>, percentile: Double): Long {
            if (sortedValues.isEmpty()) return 0L
            val clampedP = percentile.coerceIn(0.0, 1.0)
            val index = kotlin.math.round((sortedValues.size - 1) * clampedP).toInt().coerceIn(0, sortedValues.size - 1)
            return sortedValues[index]
        }

        /**
         * Calculates sample standard deviation of latency values.
         */
        fun calculateStdDev(values: List<Long>, mean: Double): Double {
            if (values.size <= 1) return 0.0
            val sumSquaredDiffs = values.sumOf { (it - mean) * (it - mean) }
            return kotlin.math.sqrt(sumSquaredDiffs / (values.size - 1))
        }

        /**
         * Grades network performance tier based on average latency, 95th percentile, packet loss, and jitter.
         *
         * @param avgLat Average round-trip ping time in ms.
         * @param p95Lat 95th percentile latency in ms.
         * @param lossPct Percentage of lost UDP datagrams.
         * @param jitter Packet arrival jitter standard deviation in ms.
         * @return Pair containing tier grade title and descriptive user assessment.
         */
        fun evaluatePerformance(
            avgLat: Double,
            p95Lat: Long,
            lossPct: Float,
            jitter: Double
        ): Pair<String, String> {
            return when {
                avgLat <= 6.0 && lossPct <= 0.5f && jitter <= 2.5 -> {
                    "A+ (Competitive Esports Grade)" to
                            "Exceptional connection! Cursor tracking will feel identical to a wired physical trackpad."
                }
                avgLat <= 12.0 && lossPct <= 2.0f && jitter <= 5.0 -> {
                    "A (Smooth Productivity)" to
                            "Very good Wi-Fi link. Gestures and cursor motion will be smooth and responsive."
                }
                avgLat <= 25.0 && lossPct <= 6.0f -> {
                    "B (Acceptable)" to
                            "Usable for browsing, but minor jitter may be noticeable during rapid flicks. Switching to 5GHz Wi-Fi is recommended."
                }
                else -> {
                    "C (High Latency / Packet Loss)" to
                            "Noticeable lag detected. Ensure your PC is connected via Ethernet or 5GHz Wi-Fi, and check firewall settings."
                }
            }
        }
    }
}
