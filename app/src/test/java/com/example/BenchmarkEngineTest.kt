package com.example

import com.example.engine.BenchmarkConfig
import com.example.engine.BenchmarkEngine
import com.example.engine.BenchmarkReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkEngineTest {

    @Test
    fun testEvaluationGradeAPlus() {
        val (grade, recommendation) = BenchmarkEngine.evaluatePerformance(
            avgLat = 3.2,
            p95Lat = 5L,
            lossPct = 0.0f,
            jitter = 1.1
        )
        assertTrue(grade.startsWith("A+"))
        assertTrue(recommendation.contains("wired physical trackpad"))
    }

    @Test
    fun testEvaluationGradeA() {
        val (grade, recommendation) = BenchmarkEngine.evaluatePerformance(
            avgLat = 8.5,
            p95Lat = 11L,
            lossPct = 1.0f,
            jitter = 3.2
        )
        assertTrue(grade.startsWith("A"))
        assertTrue(recommendation.contains("Very good Wi-Fi link"))
    }

    @Test
    fun testEvaluationGradeCForHighLoss() {
        val (grade, recommendation) = BenchmarkEngine.evaluatePerformance(
            avgLat = 35.0,
            p95Lat = 50L,
            lossPct = 12.0f,
            jitter = 8.0
        )
        assertTrue(grade.startsWith("C"))
        assertTrue(recommendation.contains("Noticeable lag detected"))
    }

    @Test
    fun testBenchmarkReportCreation() {
        val report = BenchmarkReport(
            targetIp = "192.168.1.120",
            targetPort = 8089,
            pingsSent = 50,
            pongsReceived = 49,
            pingLossPct = 2.0f,
            minLatencyMs = 2L,
            avgLatencyMs = 4.5,
            medianLatencyMs = 4L,
            p95LatencyMs = 7L,
            maxLatencyMs = 12L,
            jitterMs = 1.4,
            streamPacketsSent = 360,
            streamPacketsAcked = 358,
            streamMeasuredHz = 119.8,
            grade = "A+ (Competitive Esports Grade)",
            recommendation = "Low latency verified."
        )

        assertEquals("192.168.1.120", report.targetIp)
        assertEquals(50, report.pingsSent)
        assertEquals(49, report.pongsReceived)
        assertEquals(2.0f, report.pingLossPct, 0.01f)
        assertEquals(119.8, report.streamMeasuredHz, 0.1)
    }

    @Test
    fun testBenchmarkConfigDefaults() {
        val config = BenchmarkConfig()
        assertEquals(40, config.pingCount)
        assertEquals(3, config.streamDurationSeconds)
        assertEquals(120, config.streamTargetHz)
    }

    @Test
    fun testCalculatePercentile() {
        val latencies = listOf(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 50L) // 10 items
        assertEquals(2L, BenchmarkEngine.calculatePercentile(latencies, 0.0))
        assertEquals(50L, BenchmarkEngine.calculatePercentile(latencies, 1.0))
        assertEquals(6L, BenchmarkEngine.calculatePercentile(latencies, 0.50))
        assertEquals(10L, BenchmarkEngine.calculatePercentile(latencies, 0.90))
        assertEquals(50L, BenchmarkEngine.calculatePercentile(latencies, 0.95))
        assertEquals(50L, BenchmarkEngine.calculatePercentile(latencies, 0.99))

        // Empty list guard
        assertEquals(0L, BenchmarkEngine.calculatePercentile(emptyList(), 0.5))

        // Single element
        assertEquals(5L, BenchmarkEngine.calculatePercentile(listOf(5L), 0.95))
    }

    @Test
    fun testRfc3550JitterConvergence() {
        var jitter = 0.0
        // Repeated diff of 16.0 should converge smoothly towards 16.0
        for (i in 1..40) {
            jitter = BenchmarkEngine.updateRfc3550Jitter(jitter, 16.0)
        }
        assertTrue("Jitter should converge close to 16.0 after 40 iterations", jitter > 14.5 && jitter <= 16.0)

        // Negative difference should take absolute value
        val jitterWithNeg = BenchmarkEngine.updateRfc3550Jitter(0.0, -8.0)
        assertEquals(0.5, jitterWithNeg, 0.001)
    }

    @Test
    fun testCalculateStdDev() {
        val values = listOf(10L, 10L, 10L, 10L)
        val stdDevZero = BenchmarkEngine.calculateStdDev(values, 10.0)
        assertEquals(0.0, stdDevZero, 0.0001)

        val variable = listOf(2L, 4L, 4L, 4L, 5L, 5L, 7L, 9L)
        val mean = variable.average()
        val stdDev = BenchmarkEngine.calculateStdDev(variable, mean)
        assertTrue(stdDev > 1.5 && stdDev < 3.0)

        // Single item guard
        assertEquals(0.0, BenchmarkEngine.calculateStdDev(listOf(5L), 5.0), 0.0001)
    }

    @Test
    fun testEvaluationGradeEdgeCases() {
        // Zero latency clamped
        val (gradeZero, _) = BenchmarkEngine.evaluatePerformance(
            avgLat = 0.0,
            p95Lat = 0L,
            lossPct = 0.0f,
            jitter = 0.0
        )
        assertTrue(gradeZero.startsWith("A+"))

        // 100% packet loss
        val (gradeLoss, recLoss) = BenchmarkEngine.evaluatePerformance(
            avgLat = 0.0,
            p95Lat = 0L,
            lossPct = 100.0f,
            jitter = 0.0
        )
        assertTrue(gradeLoss.startsWith("C"))
        assertTrue(recLoss.contains("lag detected"))

        // Grade B transition (e.g. 18ms latency, 4% loss)
        val (gradeB, _) = BenchmarkEngine.evaluatePerformance(
            avgLat = 18.0,
            p95Lat = 22L,
            lossPct = 4.0f,
            jitter = 4.0
        )
        assertTrue(gradeB.startsWith("B"))
    }

    @Test
    fun testExpandedBenchmarkReportFields() {
        val report = BenchmarkReport(
            targetIp = "192.168.1.150",
            targetPort = 8089,
            pingsSent = 40,
            pongsReceived = 40,
            pingLossPct = 0.0f,
            minLatencyMs = 2L,
            avgLatencyMs = 3.5,
            medianLatencyMs = 3L,
            p90LatencyMs = 4L,
            p95LatencyMs = 5L,
            p99LatencyMs = 6L,
            maxLatencyMs = 6L,
            jitterMs = 0.8,
            jitterStdDevMs = 1.1,
            streamPacketsSent = 360,
            streamPacketsAcked = 360,
            streamMeasuredHz = 120.0,
            grade = "A+ (Competitive Esports Grade)",
            recommendation = "Optimal connection."
        )

        assertEquals(4L, report.p90LatencyMs)
        assertEquals(5L, report.p95LatencyMs)
        assertEquals(6L, report.p99LatencyMs)
        assertEquals(1.1, report.jitterStdDevMs, 0.01)
    }
}
