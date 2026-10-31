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
}
