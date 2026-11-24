package com.example.ui.layout

/**
 * Responsive shell layout modes for GlidePad across handheld, tablet/desktop, and floating windows.
 */
enum class LayoutMode {
    /** Handheld / narrow portrait layout using a bottom navigation bar. */
    COMPACT,

    /** Tablet / ChromeOS desktop layout using a left-aligned navigation rail. */
    EXPANDED,

    /** Minimal full-window touchpad mode for small or user-floated windows. */
    FLOAT
}

/**
 * Pure window-size policy determining the active [LayoutMode] from dp dimensions.
 */
object WindowLayoutPolicy {
    /** Maximum width (exclusive) under which a sub-480dp-tall window automatically enters Float mode. */
    const val FLOAT_MAX_WIDTH_DP = 600f

    /** Maximum height (exclusive) under which a sub-600dp-wide window automatically enters Float mode. */
    const val FLOAT_MAX_HEIGHT_DP = 480f

    /** Minimum side threshold (exclusive) below which any window automatically enters Float mode. */
    const val FLOAT_MIN_SIDE_DP = 320f

    /** Minimum width (inclusive) for the expanded navigation-rail layout outside Float mode. */
    const val EXPANDED_MIN_WIDTH_DP = 600f

    /**
     * Resolves the automatic [LayoutMode] for a window of [widthDp] by [heightDp].
     *
     * Returns [LayoutMode.FLOAT] when `(heightDp < 480 && widthDp < 600) || min(widthDp, heightDp) < 320`,
     * otherwise [LayoutMode.EXPANDED] when `widthDp >= 600`, otherwise [LayoutMode.COMPACT].
     */
    fun resolve(widthDp: Float, heightDp: Float): LayoutMode {
        val isSmallFloatWindow =
            (heightDp < FLOAT_MAX_HEIGHT_DP && widthDp < FLOAT_MAX_WIDTH_DP) ||
                minOf(widthDp, heightDp) < FLOAT_MIN_SIDE_DP
        return when {
            isSmallFloatWindow -> LayoutMode.FLOAT
            widthDp >= EXPANDED_MIN_WIDTH_DP -> LayoutMode.EXPANDED
            else -> LayoutMode.COMPACT
        }
    }

    /**
     * Resolves the non-Float layout mode ([LayoutMode.EXPANDED] or [LayoutMode.COMPACT]) from [widthDp].
     *
     * Used when Float mode is disabled (including manual override out of Float on a small window) so the
     * host layout does not fall back into [LayoutMode.FLOAT].
     */
    fun resolveNonFloat(widthDp: Float): LayoutMode =
        if (widthDp >= EXPANDED_MIN_WIDTH_DP) LayoutMode.EXPANDED else LayoutMode.COMPACT
}
