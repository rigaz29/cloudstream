package com.lagradost.cloudstream3.skia

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.lagradost.cloudstream3.R

/**
 * CloudStream Skia UI helpers that upstream code calls through one-line hooks
 * (search the code base for "Skia hook").
 */
object SkiaUi {
    /** Collapsed description length on the details page (upstream: 10). */
    const val DESCRIPTION_LINES = 4

    /**
     * Turns the bottom navigation into a floating pill.
     *
     * Upstream pads the bar with the system bar insets and grows it by the same amount,
     * which would put the gesture area inside the pill. Here the insets become margins
     * instead, so the pill keeps its height and floats above the system navigation.
     * Replaces the insets listener set by UIHelper.fixSystemBarsPadding.
     */
    fun floatBottomNav(nav: View) {
        // Below Android 11 upstream does not draw edge to edge; the XML margins are enough.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val res = nav.resources
        val margin = res.getDimensionPixelSize(R.dimen.skia_nav_margin)
        val height = res.getDimensionPixelSize(R.dimen.nav_view_height)

        ViewCompat.setOnApplyWindowInsetsListener(nav) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = 0, top = 0, right = 0, bottom = 0)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.height = height
                leftMargin = margin + insets.left
                rightMargin = margin + insets.right
                bottomMargin = margin + insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(nav)
    }
}
