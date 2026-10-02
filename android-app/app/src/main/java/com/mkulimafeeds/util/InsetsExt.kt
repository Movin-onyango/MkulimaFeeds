package com.mkulimafeeds.util
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Apply system-bar insets for an edge-to-edge screen with a bottom nav.
 *
 * - Root gets top + left + right padding (status bar, cutouts).
 * - The bottom nav's INNER ROW gets the bottom inset as padding, so the
 *   nav card's background stays full-bleed to the physical bottom edge,
 *   while the icons lift above gesture / 3-button navigation.
 *
 * @param root       the screen's root layout view
 * @param bottomNav  the MaterialCardView that wraps the nav's icon row
 */
fun applyBottomNavInsets(root: View, bottomNav: View) {

    // Root: top + sides only. Bottom inset handled by the nav.
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            0
        )
        insets
    }

    // Resolve the inner row inside the nav card.
    val innerRow: View =
        if (bottomNav is ViewGroup && bottomNav.childCount > 0) {
            bottomNav.getChildAt(0)
        } else {
            bottomNav
        }

    // Inner row: consume the bottom system bar inset.
    ViewCompat.setOnApplyWindowInsetsListener(innerRow) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            v.paddingLeft,
            v.paddingTop,
            v.paddingRight,
            v.paddingBottom + systemBars.bottom
        )
        insets
    }
}