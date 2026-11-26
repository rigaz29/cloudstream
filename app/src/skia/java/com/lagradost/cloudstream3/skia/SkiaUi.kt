package com.lagradost.cloudstream3.skia

import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.PlayerCustomLayoutBinding
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.UIHelper.getStatusBarHeight

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

    /**
     * Puts the home source picker (an extended FAB in upstream) at the top of Home, level
     * with the search and profile buttons of the header, below the status bar.
     */
    /**
     * Player overlay header (phone): title first, left aligned, with the source/resolution
     * line below it as a small pill. Done at runtime on upstream's own views instead of
     * overriding the 1000+ line player layout; ids, order of animations and texts are upstream's.
     */
    fun stylePlayerTop(binding: PlayerCustomLayoutBinding) {
        if (!isLayout(PHONE)) return
        val res = binding.root.resources
        fun dp(value: Int) = (value * res.displayMetrics.density).toInt()

        val column = binding.playerVideoTitleRez.parent as? LinearLayout ?: return
        column.setPadding(column.paddingLeft, dp(18), column.paddingRight, column.paddingBottom)

        // Title above the source line.
        column.removeView(binding.playerVideoTitleHolder)
        column.addView(binding.playerVideoTitleHolder, 0)
        binding.playerVideoTitleHolder.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        binding.playerVideoTitle.apply {
            gravity = Gravity.START
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        }

        binding.playerVideoTitleRez.apply {
            gravity = Gravity.START
            updateLayoutParams<LinearLayout.LayoutParams> {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                topMargin = dp(6)
            }
            setBackgroundResource(R.drawable.skia_player_pill)
            setPadding(dp(10), dp(3), dp(10), dp(4))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        binding.playerVideoInfo.gravity = Gravity.START
    }

    fun placeHomeSourcePill(pill: View) {
        // Window insets do not reach this view (a parent consumes them), so use the
        // status bar height directly, like upstream does for its own headers.
        val offset = pill.resources.getDimensionPixelSize(R.dimen.skia_home_pill_offset)
        pill.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = pill.context.getStatusBarHeight() + offset
        }
    }
}
