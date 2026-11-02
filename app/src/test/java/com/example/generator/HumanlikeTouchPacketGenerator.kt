package com.example.generator

import org.json.JSONObject
import java.util.Random
import kotlin.math.*

/**
 * Data representation of a simulated realistic touch/network packet.
 */
data class HumanlikeTouchPacket(
    val type: String,
    val dx: Int,
    val dy: Int,
    val btn: Int,
    val wheel: Int,
    val timestampMs: Long,
    val description: String = ""
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("type", type)
            put("dx", dx)
            put("dy", dy)
            put("btn", btn)
            put("wheel", wheel)
            put("time", timestampMs)
        }.toString()
    }
}

/**
 * Generates realistic, randomized, humanlike touchpad and gesture packet data.
 *
 * Models physical human neuromuscular motor control principles:
 *  1. Flash & Hogan (1985) Minimum-Jerk trajectory model (bell-shaped velocity profiles for swipes).
 *  2. Wrist/Elbow biomechanical arc curvature (human hands pivot in natural arcs, not straight lines).
 *  3. 8-12 Hz physiological tremor and signal-dependent motor noise (Schmidt et al., 1979).
 *  4. Exponential deceleration decay for ballistic flicks.
 *  5. Dwell time and sub-pixel drift for tap-to-click gestures.
 *  6. Smooth inertial decay for two-finger scrolling.
 */
class HumanlikeTouchPacketGenerator(private val seed: Long = 42L) {

    private val random = Random(seed)

    /**
     * Generates a minimum-jerk human swipe trajectory between two coordinates.
     *
     * @param startX Starting X coordinate on virtual trackpad.
     * @param startY Starting Y coordinate on virtual trackpad.
     * @param targetX Destination X coordinate.
     * @param targetY Destination Y coordinate.
     * @param durationMs Duration of swipe in milliseconds (typically 200ms - 600ms).
     * @param sampleIntervalMs Polling interval (typically 8ms - 16ms for 60-120Hz touch sampling).
     * @param arcCurvature Maximum perpendicular displacement caused by wrist pivot (pixels).
     * @param tremorMagnitude Intensity of physiological tremor (pixels, typically 0.5 - 1.5).
     * @param startTimestamp Base timestamp in ms.
     */
    fun generateSwipeArc(
        startX: Double = 0.0,
        startY: Double = 0.0,
        targetX: Double,
        targetY: Double,
        durationMs: Long = 320L,
        sampleIntervalMs: Long = 12L,
        arcCurvature: Double = 18.0,
        tremorMagnitude: Double = 1.0,
        startTimestamp: Long = System.currentTimeMillis()
    ): List<HumanlikeTouchPacket> {
        val packets = mutableListOf<HumanlikeTouchPacket>()
        val totalSteps = max(4, (durationMs / sampleIntervalMs).toInt())

        val totalDistX = targetX - startX
        val totalDistY = targetY - startY
        val straightDist = hypot(totalDistX, totalDistY)

        // Perpendicular unit vector for wrist pivot arc curvature
        val perpX = if (straightDist > 0.001) -totalDistY / straightDist else 0.0
        val perpY = if (straightDist > 0.001) totalDistX / straightDist else 0.0

        // Randomize arc side (wrist curvature left or right)
        val arcSign = if (random.nextBoolean()) 1.0 else -1.0
        val actualArc = arcCurvature * arcSign

        var lastRecordedX = startX
        var lastRecordedY = startY

        // Random tremor phase offsets (simulating independent motor unit firing)
        val tremorPhaseX = random.nextDouble() * 2 * PI
        val tremorPhaseY = random.nextDouble() * 2 * PI
        val tremorFreqHz = 9.0 + random.nextDouble() * 3.0 // 9-12 Hz tremor

        for (i in 1..totalSteps) {
            val t = i.toDouble() / totalSteps.toDouble() // Normalized time tau in [0, 1]
            val currentTimeMs = startTimestamp + (t * durationMs).toLong()

            // Minimum-jerk polynomial trajectory S(tau) = 10*tau^3 - 15*tau^4 + 6*tau^5
            val s = 10.0 * t.pow(3) - 15.0 * t.pow(4) + 6.0 * t.pow(5)

            // Linear minimum-jerk position
            val linearX = startX + totalDistX * s
            val linearY = startY + totalDistY * s

            // Natural arc offset: peaks at mid-trajectory (sin(pi * tau))
            val arcOffset = actualArc * sin(PI * t)

            // Physiological tremor & motor noise (stronger at higher velocity)
            val velocityFactor = sin(PI * t) // Velocity peaks at tau = 0.5
            val tremorNoiseX = tremorMagnitude * velocityFactor * sin(2 * PI * tremorFreqHz * (t * durationMs / 1000.0) + tremorPhaseX) +
                    (random.nextGaussian() * 0.3 * tremorMagnitude)
            val tremorNoiseY = tremorMagnitude * velocityFactor * sin(2 * PI * tremorFreqHz * (t * durationMs / 1000.0) + tremorPhaseY) +
                    (random.nextGaussian() * 0.3 * tremorMagnitude)

            val currentRealX = linearX + perpX * arcOffset + tremorNoiseX
            val currentRealY = linearY + perpY * arcOffset + tremorNoiseY

            // Quantize to integer pixel deltas with residual tracking
            val deltaX = (currentRealX - lastRecordedX).roundToInt()
            val deltaY = (currentRealY - lastRecordedY).roundToInt()

            lastRecordedX += deltaX
            lastRecordedY += deltaY

            if (deltaX != 0 || deltaY != 0 || i == totalSteps) {
                packets.add(
                    HumanlikeTouchPacket(
                        type = "mouse",
                        dx = deltaX,
                        dy = deltaY,
                        btn = 0,
                        wheel = 0,
                        timestampMs = currentTimeMs,
                        description = "Swipe arc step $i/$totalSteps (tau=${"%.2f".format(t)})"
                    )
                )
            }
        }

        return packets
    }

    /**
     * Generates a rapid ballistic flick where the finger accelerates quickly and releases,
     * leaving cursor inertia decaying exponentially.
     */
    fun generateFlick(
        angleRadians: Double = random.nextDouble() * 2 * PI,
        initialSpeedPxPerFrame: Double = 35.0 + random.nextDouble() * 25.0,
        decayFactor: Double = 0.72,
        startTimestamp: Long = System.currentTimeMillis()
    ): List<HumanlikeTouchPacket> {
        val packets = mutableListOf<HumanlikeTouchPacket>()
        var currentSpeed = initialSpeedPxPerFrame
        var step = 0
        var currentTime = startTimestamp

        while (currentSpeed > 0.8 && step < 20) {
            step++
            currentTime += 12L + random.nextInt(4)

            // Subtle angle drift during flick
            val driftAngle = angleRadians + (random.nextGaussian() * 0.05)
            val rawDx = currentSpeed * cos(driftAngle)
            val rawDy = currentSpeed * sin(driftAngle)

            val dx = rawDx.roundToInt()
            val dy = rawDy.roundToInt()

            packets.add(
                HumanlikeTouchPacket(
                    type = "mouse",
                    dx = dx,
                    dy = dy,
                    btn = 0,
                    wheel = 0,
                    timestampMs = currentTime,
                    description = "Flick step $step (speed=${"%.1f".format(currentSpeed)})"
                )
            )

            currentSpeed *= decayFactor
        }

        return packets
    }

    /**
     * Generates precision micro-adjustments as a human acquires a small button or UI target.
     * Characterized by tiny 1-3 pixel corrective steps and small pauses.
     */
    fun generatePrecisionHover(
        steps: Int = 6,
        targetDx: Int = 8,
        targetDy: Int = -5,
        startTimestamp: Long = System.currentTimeMillis()
    ): List<HumanlikeTouchPacket> {
        val packets = mutableListOf<HumanlikeTouchPacket>()
        var remX = targetDx
        var remY = targetDy
        var currentTime = startTimestamp

        for (i in 1..steps) {
            currentTime += 18L + random.nextInt(12) // Slightly slower pacing during precision targeting
            val dx = if (remX != 0) (remX / (steps - i + 1)).coerceIn(-2, 2) else (random.nextInt(3) - 1)
            val dy = if (remY != 0) (remY / (steps - i + 1)).coerceIn(-2, 2) else (random.nextInt(3) - 1)

            remX -= dx
            remY -= dy

            packets.add(
                HumanlikeTouchPacket(
                    type = "mouse",
                    dx = dx,
                    dy = dy,
                    btn = 0,
                    wheel = 0,
                    timestampMs = currentTime,
                    description = "Precision hover micro-step $i/$steps"
                )
            )
        }

        return packets
    }

    /**
     * Generates a realistic tap-to-click gesture.
     * Touch contacts surface -> micro settling drift (<=1px) -> Button press -> Dwell hold (50-110ms) -> Button release.
     */
    fun generateTapToClick(
        buttonCode: Int = 1, // 1 = Left Click, 2 = Right Click
        dwellMs: Long = 65L + random.nextInt(40),
        startTimestamp: Long = System.currentTimeMillis()
    ): List<HumanlikeTouchPacket> {
        val packets = mutableListOf<HumanlikeTouchPacket>()

        // 1. Contact / settling packet
        val settleTime = startTimestamp + 8L
        val settleDx = if (random.nextDouble() < 0.25) (random.nextInt(3) - 1) else 0
        val settleDy = if (random.nextDouble() < 0.25) (random.nextInt(3) - 1) else 0
        packets.add(
            HumanlikeTouchPacket(
                type = "mouse",
                dx = settleDx,
                dy = settleDy,
                btn = 0,
                wheel = 0,
                timestampMs = settleTime,
                description = "Tap contact settlement"
            )
        )

        // 2. Button Down event
        val downTime = settleTime + 12L
        packets.add(
            HumanlikeTouchPacket(
                type = "mouse",
                dx = 0,
                dy = 0,
                btn = buttonCode,
                wheel = 0,
                timestampMs = downTime,
                description = "Tap button $buttonCode PRESS"
            )
        )

        // 3. Button Up / Release event after dwell
        val upTime = downTime + dwellMs
        packets.add(
            HumanlikeTouchPacket(
                type = "mouse",
                dx = 0,
                dy = 0,
                btn = 0,
                wheel = 0,
                timestampMs = upTime,
                description = "Tap button $buttonCode RELEASE"
            )
        )

        return packets
    }

    /**
     * Generates a realistic two-finger scroll gesture with smooth acceleration and inertial roll-off.
     */
    fun generateTwoFingerScroll(
        totalUnits: Int = -12, // Negative = scroll down (natural scrolling), positive = scroll up
        durationMs: Long = 280L,
        startTimestamp: Long = System.currentTimeMillis()
    ): List<HumanlikeTouchPacket> {
        val packets = mutableListOf<HumanlikeTouchPacket>()
        val steps = max(4, (durationMs / 24L).toInt())
        var remainingUnits = totalUnits
        var currentTime = startTimestamp

        for (i in 1..steps) {
            currentTime += 24L + random.nextInt(6)
            val t = i.toDouble() / steps.toDouble()
            // Inertial decay curve
            val weight = sin(PI * t).pow(1.5)
            val allocated = if (i == steps) remainingUnits else (totalUnits * weight / (steps * 0.6)).roundToInt()
            val chunk = allocated.coerceIn(-4, 4)
            remainingUnits -= chunk

            packets.add(
                HumanlikeTouchPacket(
                    type = "mouse",
                    dx = 0,
                    dy = 0,
                    btn = 0,
                    wheel = chunk,
                    timestampMs = currentTime,
                    description = "Scroll step $i/$steps (wheel=$chunk)"
                )
            )
        }

        return packets
    }

    /**
     * Generates a comprehensive, realistic multi-gesture user session:
     *  - Swipe to a window/button
     *  - Micro-adjust hover
     *  - Tap to click
     *  - Short idle pause
     *  - Two-finger scroll down
     *  - Ballistic flick to return
     *  - Idle gap (allowing heartbeat keepalives)
     */
    fun generateRealisticSession(
        baseTimestamp: Long = System.currentTimeMillis()
    ): List<HumanlikeTouchPacket> {
        val sessionPackets = mutableListOf<HumanlikeTouchPacket>()
        var clock = baseTimestamp

        // 1. Initial Swipe Arc across screen
        val swipe1 = generateSwipeArc(
            startX = 100.0,
            startY = 150.0,
            targetX = 420.0,
            targetY = 280.0,
            durationMs = 260L,
            startTimestamp = clock
        )
        sessionPackets.addAll(swipe1)
        clock = swipe1.last().timestampMs + 25L

        // 2. Micro-adjust targeting
        val micro = generatePrecisionHover(
            steps = 5,
            targetDx = 6,
            targetDy = -4,
            startTimestamp = clock
        )
        sessionPackets.addAll(micro)
        clock = micro.last().timestampMs + 35L

        // 3. Tap Click
        val tap = generateTapToClick(
            buttonCode = 1,
            dwellMs = 75L,
            startTimestamp = clock
        )
        sessionPackets.addAll(tap)
        clock = tap.last().timestampMs + 80L

        // 4. Two-finger scroll
        val scroll = generateTwoFingerScroll(
            totalUnits = -15,
            durationMs = 240L,
            startTimestamp = clock
        )
        sessionPackets.addAll(scroll)
        clock = scroll.last().timestampMs + 40L

        // 5. Ballistic flick
        val flick = generateFlick(
            angleRadians = -PI / 3.0,
            initialSpeedPxPerFrame = 40.0,
            startTimestamp = clock
        )
        sessionPackets.addAll(flick)

        return sessionPackets
    }
}
