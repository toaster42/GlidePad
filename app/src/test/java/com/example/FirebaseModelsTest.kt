package com.example

import com.example.model.CloudMacro
import com.example.model.ErrorReport
import com.example.model.SavedPcProfile
import com.example.model.UserFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseModelsTest {

    @Test
    fun testCloudMacroToMap() {
        val macro = CloudMacro(
            id = "macro_1",
            label = "Copy Text",
            description = "Ctrl+C",
            shortcutKeys = listOf("Ctrl", "C"),
            actionType = "KEYBOARD",
            sortOrder = 1
        )
        val map = macro.toMap()
        assertEquals("Copy Text", map["label"])
        assertEquals("Ctrl+C", map["description"])
        assertEquals("KEYBOARD", map["actionType"])
        assertEquals(listOf("Ctrl", "C"), map["shortcutKeys"])
        assertEquals(1, map["sortOrder"])
    }

    @Test
    fun testSavedPcProfileToMap() {
        val profile = SavedPcProfile(
            id = "profile_1",
            name = "Office PC",
            ipAddress = "192.168.1.150",
            port = 8889,
            pointerSensitivity = 1.5,
            accelerationMultiplier = 1.3,
            accelerationProfile = "DYNAMIC"
        )
        val map = profile.toMap()
        assertEquals("Office PC", map["name"])
        assertEquals("192.168.1.150", map["ipAddress"])
        assertEquals(8889, map["port"])
        assertEquals(1.5, map["pointerSensitivity"])
        assertEquals("DYNAMIC", map["accelerationProfile"])
    }

    @Test
    fun testUserFeedbackToMap() {
        val feedback = UserFeedback(
            userId = "user_123",
            title = "Pinch to zoom enhancement",
            description = "Would love custom sensitivity for pinch zoom gestures.",
            category = "GESTURE",
            userEmail = "user@example.com"
        )
        val map = feedback.toMap()
        assertEquals("user_123", map["userId"])
        assertEquals("Pinch to zoom enhancement", map["title"])
        assertEquals("GESTURE", map["category"])
        assertEquals("user@example.com", map["userEmail"])
    }

    @Test
    fun testErrorReportToMap() {
        val report = ErrorReport(
            userId = "user_123",
            errorMessage = "UDP ping timeout",
            connectionMode = "LOCAL_WIFI",
            targetIp = "192.168.1.150",
            userComments = "Receiver script crashed",
            recentLogs = listOf("PING 192.168.1.150", "TIMEOUT")
        )
        val map = report.toMap()
        assertEquals("user_123", map["userId"])
        assertEquals("UDP ping timeout", map["errorMessage"])
        assertEquals("LOCAL_WIFI", map["connectionMode"])
        assertEquals(2, (map["recentLogs"] as List<*>).size)
    }
}
