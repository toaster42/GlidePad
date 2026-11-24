package com.example.layout

import androidx.compose.ui.input.key.Key
import com.example.AppScreen
import com.example.ui.layout.ShortcutAction
import com.example.ui.layout.resolveShortcut
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [resolveShortcut] covering screen selection, Float toggle, and Escape precedence.
 */
class KeyboardShortcutsTest {

    @Test
    fun numberKeysOneThroughFive_mapToExpectedScreens() {
        val expected = listOf(
            Key.One to AppScreen.TOUCHPAD,
            Key.Two to AppScreen.KEYBOARD,
            Key.Three to AppScreen.MACROS,
            Key.Four to AppScreen.VIRTUAL_SCREEN,
            Key.Five to AppScreen.SETTINGS
        )

        for ((key, screen) in expected) {
            assertEquals(
                ShortcutAction.Select(screen),
                resolveShortcut(
                    key = key,
                    isFloat = false,
                    isDialogOpen = false,
                    currentScreen = AppScreen.TOUCHPAD
                )
            )
            assertEquals(
                ShortcutAction.Select(screen),
                resolveShortcut(
                    key = key,
                    isFloat = true,
                    isDialogOpen = true,
                    currentScreen = AppScreen.SETTINGS
                )
            )
        }
    }

    @Test
    fun keyF_mapsToToggleFloat() {
        assertEquals(
            ShortcutAction.ToggleFloat,
            resolveShortcut(
                key = Key.F,
                isFloat = false,
                isDialogOpen = false,
                currentScreen = AppScreen.TOUCHPAD
            )
        )
        assertEquals(
            ShortcutAction.ToggleFloat,
            resolveShortcut(
                key = Key.F,
                isFloat = true,
                isDialogOpen = false,
                currentScreen = AppScreen.MACROS
            )
        )
    }

    @Test
    fun escape_followsDialogThenFloatThenTouchpadPrecedence() {
        assertEquals(
            ShortcutAction.CloseDialog,
            resolveShortcut(
                key = Key.Escape,
                isFloat = true,
                isDialogOpen = true,
                currentScreen = AppScreen.SETTINGS
            )
        )

        assertEquals(
            ShortcutAction.ExitFloat,
            resolveShortcut(
                key = Key.Escape,
                isFloat = true,
                isDialogOpen = false,
                currentScreen = AppScreen.SETTINGS
            )
        )

        assertEquals(
            ShortcutAction.GoToTouchpad,
            resolveShortcut(
                key = Key.Escape,
                isFloat = false,
                isDialogOpen = false,
                currentScreen = AppScreen.KEYBOARD
            )
        )

        assertEquals(
            ShortcutAction.None,
            resolveShortcut(
                key = Key.Escape,
                isFloat = false,
                isDialogOpen = false,
                currentScreen = AppScreen.TOUCHPAD
            )
        )
    }

    @Test
    fun keySixAndUnmappedKeys_returnNone() {
        val unmapped = listOf(Key.Six, Key.Zero, Key.A, Key.Enter, Key.Spacebar)
        for (key in unmapped) {
            assertEquals(
                ShortcutAction.None,
                resolveShortcut(
                    key = key,
                    isFloat = false,
                    isDialogOpen = false,
                    currentScreen = AppScreen.TOUCHPAD
                )
            )
        }
    }
}
