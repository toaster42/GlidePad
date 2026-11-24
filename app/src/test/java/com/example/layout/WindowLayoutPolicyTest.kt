package com.example.layout

import com.example.ui.layout.LayoutMode
import com.example.ui.layout.WindowLayoutPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [WindowLayoutPolicy] threshold resolution and non-Float fallback behavior.
 */
class WindowLayoutPolicyTest {

    private data class PolicyCase(
        val widthDp: Float,
        val heightDp: Float,
        val expected: LayoutMode
    )

    @Test
    fun resolve_matchesAllSpecAndBoundaryCases() {
        val cases = listOf(
            // Spec AC-1 required cases
            PolicyCase(600f, 480f, LayoutMode.EXPANDED),
            PolicyCase(599f, 479f, LayoutMode.FLOAT),
            PolicyCase(411f, 891f, LayoutMode.COMPACT),
            PolicyCase(891f, 411f, LayoutMode.EXPANDED),
            PolicyCase(300f, 700f, LayoutMode.FLOAT),
            PolicyCase(1280f, 800f, LayoutMode.EXPANDED),
            PolicyCase(240f, 180f, LayoutMode.FLOAT),
            PolicyCase(360f, 640f, LayoutMode.COMPACT),
            // Exact boundary cases around 600dp, 480dp, and 320dp
            PolicyCase(600f, 479f, LayoutMode.EXPANDED),
            PolicyCase(599f, 480f, LayoutMode.COMPACT),
            PolicyCase(320f, 600f, LayoutMode.COMPACT),
            PolicyCase(319f, 600f, LayoutMode.FLOAT),
            PolicyCase(600f, 319f, LayoutMode.FLOAT),
            PolicyCase(600f, 320f, LayoutMode.EXPANDED),
            PolicyCase(599f, 320f, LayoutMode.FLOAT),
            PolicyCase(320f, 320f, LayoutMode.FLOAT),
            PolicyCase(320f, 480f, LayoutMode.COMPACT),
            PolicyCase(320f, 479f, LayoutMode.FLOAT)
        )

        for (case in cases) {
            val actual = WindowLayoutPolicy.resolve(case.widthDp, case.heightDp)
            assertEquals(
                "Expected ${case.expected} for ${case.widthDp}x${case.heightDp}dp",
                case.expected,
                actual
            )
        }
    }

    @Test
    fun resolveNonFloat_neverReturnsFloatAndUses600dpBreakpoint() {
        assertEquals(LayoutMode.COMPACT, WindowLayoutPolicy.resolveNonFloat(240f))
        assertEquals(LayoutMode.COMPACT, WindowLayoutPolicy.resolveNonFloat(320f))
        assertEquals(LayoutMode.COMPACT, WindowLayoutPolicy.resolveNonFloat(599f))
        assertEquals(LayoutMode.EXPANDED, WindowLayoutPolicy.resolveNonFloat(600f))
        assertEquals(LayoutMode.EXPANDED, WindowLayoutPolicy.resolveNonFloat(1280f))
    }
}
