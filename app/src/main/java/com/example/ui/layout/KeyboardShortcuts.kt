package com.example.ui.layout

import androidx.compose.ui.input.key.Key
import com.example.AppScreen

/**
 * Discrete keyboard-shortcut actions supported by the GlidePad root shell.
 */
sealed interface ShortcutAction {
    /** Switch the active destination to [screen]. */
    data class Select(val screen: AppScreen) : ShortcutAction

    /** Toggle Float window mode on or off. */
    data object ToggleFloat : ShortcutAction

    /** Exit Float window mode back to the normal shell. */
    data object ExitFloat : ShortcutAction

    /** Dismiss the currently open modal dialog. */
    data object CloseDialog : ShortcutAction

    /** Navigate back to the primary Touchpad screen. */
    data object GoToTouchpad : ShortcutAction

    /** No action mapped for the key press in the current state. */
    data object None : ShortcutAction
}

/**
 * Maps a hardware keyboard [key] press to a [ShortcutAction] given the current UI state.
 *
 * - `1`..`5` select `TOUCHPAD`, `KEYBOARD`, `MACROS`, `VIRTUAL_SCREEN`, and `SETTINGS`.
 * - `F` toggles Float mode.
 * - `Esc` closes an open dialog first, otherwise exits Float mode, otherwise returns to `TOUCHPAD`
 *   if on another screen, otherwise returns [ShortcutAction.None].
 * - All other keys (including `6`) return [ShortcutAction.None].
 */
fun resolveShortcut(
    key: Key,
    isFloat: Boolean,
    isDialogOpen: Boolean,
    currentScreen: AppScreen
): ShortcutAction = when (key) {
    Key.One -> ShortcutAction.Select(AppScreen.TOUCHPAD)
    Key.Two -> ShortcutAction.Select(AppScreen.KEYBOARD)
    Key.Three -> ShortcutAction.Select(AppScreen.MACROS)
    Key.Four -> ShortcutAction.Select(AppScreen.VIRTUAL_SCREEN)
    Key.Five -> ShortcutAction.Select(AppScreen.SETTINGS)
    Key.F -> ShortcutAction.ToggleFloat
    Key.Escape -> when {
        isDialogOpen -> ShortcutAction.CloseDialog
        isFloat -> ShortcutAction.ExitFloat
        currentScreen != AppScreen.TOUCHPAD -> ShortcutAction.GoToTouchpad
        else -> ShortcutAction.None
    }
    else -> ShortcutAction.None
}
