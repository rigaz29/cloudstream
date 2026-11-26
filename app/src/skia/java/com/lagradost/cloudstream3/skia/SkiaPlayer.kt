package com.lagradost.cloudstream3.skia

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import kotlin.math.min
import kotlin.math.roundToInt

/** Skia changes to player dialogs, hooked from GeneratorPlayer. */
object SkiaPlayer {
    /**
     * The source and subtitle picker opens as a panel sliding in from the right (Nuvio-like)
     * instead of covering the whole screen, so the paused video stays in view. Only on wide
     * (landscape) phone screens; elsewhere upstream's full-screen dialog stays.
     */
    fun sidePanel(dialog: Dialog, content: View) {
        if (!isLayout(PHONE)) return
        val metrics = content.resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        if (widthDp < 600) return
        val window = dialog.window ?: return
        fun dp(value: Int) = (value * metrics.density).roundToInt()

        val ground = TypedValue().also {
            content.context.theme.resolveAttribute(R.attr.primaryBlackBackground, it, true)
        }.data
        val radius = dp(16).toFloat()
        window.setBackgroundDrawable(GradientDrawable().apply {
            setColor(ground)
            cornerRadii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
        })
        window.setLayout(
            min((metrics.widthPixels * 0.62f).roundToInt(), dp(600)),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        window.setGravity(Gravity.END)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.34f)
        window.setWindowAnimations(R.style.SkiaSidePanelAnimation)
        content.clipToOutline = true
        // Upstream relied on the screen edge for the left gap; the panel needs its own.
        // (The root's padding is reset by upstream's system bar insets, so pad the column row.)
        (content as? ViewGroup)?.getChildAt(0)?.let {
            it.setPaddingRelative(dp(16), it.paddingTop, it.paddingEnd, it.paddingBottom)
        }
    }
}
