package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying the active navigation destinations in [AppScreen].
 */
class AppScreenTest {

    @Test
    fun `AC-9 AppScreen entries exclude RECEIVER and preserve canonical order`() {
        assertEquals(
            listOf(
                AppScreen.TOUCHPAD,
                AppScreen.KEYBOARD,
                AppScreen.MACROS,
                AppScreen.VIRTUAL_SCREEN,
                AppScreen.SETTINGS
            ),
            AppScreen.entries
        )
    }
}
