package com.lagradost.cloudstream3.skia

import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
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
     * Player overlay (phone), Nuvio-like: the title block (title, then the source/resolution
     * line as a small pill) sits bottom-left above the seek bar, and the seek bar is a thin
     * line without a thumb that thickens while dragged. Done at runtime on upstream's own
     * views instead of overriding the 1000+ line player layout (shared with TV).
     */
    fun stylePlayerTop(binding: PlayerCustomLayoutBinding) {
        if (!isLayout(PHONE)) return
        val res = binding.root.resources
        fun dp(value: Int) = (value * res.displayMetrics.density).toInt()

        // Into the seek bar's own ConstraintLayout, right above the time row: that layout
        // also holds the (invisible) scrub preview frame, so the bar keeps its height.
        val column = binding.playerVideoTitleRez.parent as? LinearLayout ?: return
        (column.parent as? ViewGroup)?.removeView(column)
        column.setPadding(0, 0, 0, 0)
        column.gravity = Gravity.START
        binding.playerVideoBar.addView(
            column,
            ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = androidx.media3.ui.R.id.exo_position
                marginStart = dp(24)
                marginEnd = dp(24)
                bottomMargin = dp(6)
            }
        )

        // Title above the source line.
        column.removeView(binding.playerVideoTitleHolder)
        column.addView(binding.playerVideoTitleHolder, 0)
        binding.playerVideoTitleHolder.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        binding.playerVideoTitle.apply {
            gravity = Gravity.START
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
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

        binding.root.findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
            ?.let(::styleTimeBar)
    }

    /**
     * Thin seek line, no thumb at rest; while dragging the line thickens and a small thumb
     * shows. DefaultTimeBar takes these sizes only from XML attributes, so set its fields
     * (minify is off; on failure the upstream look stays).
     */
    private fun styleTimeBar(bar: DefaultTimeBar) {
        val density = bar.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).toInt()
        fun setField(name: String, value: Int) = runCatching {
            DefaultTimeBar::class.java.getDeclaredField(name).apply { isAccessible = true }
                .setInt(bar, value)
        }.isSuccess

        // The bar rectangle is computed in onLayout from barHeight.
        fun setBarHeight(value: Float) {
            if (setField("barHeight", dp(value))) bar.requestLayout()
        }

        if (!setField("barHeight", dp(4f))) return
        bar.requestLayout()
        setField("scrubberEnabledSize", 0)
        setField("scrubberDraggedSize", dp(16f))
        val accent = TypedValue().also {
            bar.context.theme.resolveAttribute(R.attr.colorPrimary, it, true)
        }.data
        bar.setUnplayedColor(0x4DFFFFFF)
        bar.setBufferedColor((accent and 0x00FFFFFF) or 0x59000000)
        bar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) = setBarHeight(7f)

            override fun onScrubMove(timeBar: TimeBar, position: Long) = Unit

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) =
                setBarHeight(4f)
        })
    }

    fun placeHomeSourcePill(pill: View) {
        // Window insets are not dispatched to this view (a parent consumes them), so read the
        // window's own insets once attached. The status_bar_height resource is not reliable:
        // on devices with a cutout the real status bar is taller, and a pill placed from the
        // resource ends up partly under the status bar, where the system takes the touches.
        val offset = pill.resources.getDimensionPixelSize(R.dimen.skia_home_pill_offset)
        fun place() {
            val top = ViewCompat.getRootWindowInsets(pill)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
                ?: pill.context.getStatusBarHeight()
            pill.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = top + offset }
        }
        if (pill.isAttachedToWindow) place() else pill.doOnAttach { place() }
    }
}
