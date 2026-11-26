package com.lagradost.cloudstream3.skia

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.lagradost.cloudstream3.R
import kotlin.math.ceil

/**
 * Continue watching progress on a home poster (Skia home_result_grid_expanded): a thin line
 * at the bottom instead of upstream's ring. Upstream only sets max, progress and visibility
 * (in seconds), so this view also shows the poster scrim and the "12 min left" label that
 * sit next to it in the card. Other cards hide the bar, and with it the extras.
 */
class SkiaResumeProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal,
) : ProgressBar(context, attrs, defStyleAttr) {

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        syncExtras()
    }

    override fun setMax(max: Int) {
        super.setMax(max)
        syncExtras()
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        syncExtras()
    }

    // Called from the ProgressBar constructor too, before the view has a parent.
    private fun syncExtras() {
        val card = parent as? View ?: return
        val shown = visibility == VISIBLE
        card.findViewById<View>(R.id.skia_resume_scrim)?.visibility =
            if (shown) VISIBLE else GONE

        val label = card.findViewById<TextView>(R.id.skia_resume_left) ?: return
        val leftSeconds = max - progress
        if (!shown || max <= 0 || leftSeconds <= 0) {
            label.visibility = GONE
            return
        }
        val minutes = ceil(leftSeconds / 60.0).toInt()
        label.text = if (minutes >= 60) {
            resources.getString(R.string.skia_time_left_hours, minutes / 60, minutes % 60)
        } else {
            resources.getString(R.string.skia_time_left_minutes, minutes)
        }
        label.visibility = VISIBLE
    }
}
