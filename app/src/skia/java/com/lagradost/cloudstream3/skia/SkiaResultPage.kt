package com.lagradost.cloudstream3.skia

import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentResultBinding
import com.lagradost.cloudstream3.databinding.FragmentResultSwipeBinding
import com.lagradost.cloudstream3.ui.result.ResultViewModel2

/**
 * Skia additions to the phone details page, attached from ResultFragmentPhone with a
 * one-line hook once upstream has set up its own observers.
 */
object SkiaResultPage {
    fun attach(
        fragment: Fragment,
        viewModel: ResultViewModel2,
        swipe: FragmentResultSwipeBinding?,
        result: FragmentResultBinding?,
    ) {
        SkiaSeasonPills.attach(fragment, viewModel, result)
        if (swipe != null && result != null) fadeInTopBar(swipe.resultTopBar, result)
    }

    /**
     * The top bar floats over the artwork without a background. Once the artwork scrolls
     * away, fade in the page color behind it so content does not run under the buttons.
     */
    private fun fadeInTopBar(bar: View, result: FragmentResultBinding) {
        val scroll = result.resultScroll
        val artwork = result.resultTopHolder
        val ctx = bar.context
        val value = TypedValue()
        ctx.theme.resolveAttribute(R.attr.primaryBlackBackground, value, true)
        val background = ColorDrawable(value.data).apply { alpha = 0 }
        bar.background = background

        fun update() {
            val barHeight = bar.height.takeIf { it > 0 } ?: return
            val start = artwork.height - 2 * barHeight
            val fraction = ((scroll.scrollY - start).toFloat() / barHeight).coerceIn(0f, 1f)
            background.alpha = (fraction * 255).toInt()
        }

        val listener = ViewTreeObserver.OnScrollChangedListener { update() }
        scroll.viewTreeObserver.addOnScrollChangedListener(listener)
        scroll.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                v.viewTreeObserver.removeOnScrollChangedListener(listener)
                v.removeOnAttachStateChangeListener(this)
            }
        })
        bar.post { update() }
    }
}
