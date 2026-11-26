package com.lagradost.cloudstream3.skia

import android.graphics.Rect
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.lagradost.cloudstream3.R
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Home hero page dots and auto paging (Nuvio-like). Phone only: the dots row exists in the Skia
 * fragment_home_head override, so on TV [attach] finds nothing and returns.
 * Hooked from HomeParentItemAdapterPreview.
 */
object SkiaHomeHero {
    private const val ADVANCE_MS = 8_000L

    /** Auto paging cycles through the first pages only, so it never pages in more items. */
    private const val ADVANCE_LIMIT = 10
    private const val MAX_DOTS = 7

    fun attach(root: View, pager: ViewPager2) {
        val dots = root.findViewById<LinearLayout>(R.id.skia_hero_dots) ?: return
        root.findViewById<View>(R.id.home_padding)?.let(::alignTopRow)
        val visible = Rect()

        val advance = object : Runnable {
            override fun run() {
                val count = pager.adapter?.itemCount ?: 0
                val onScreen = pager.isShown &&
                        pager.windowVisibility == View.VISIBLE &&
                        pager.getGlobalVisibleRect(visible) &&
                        visible.height() > pager.height / 2
                if (count > 1 && onScreen && pager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                    val limit = min(count, ADVANCE_LIMIT)
                    val next = if (pager.currentItem + 1 >= limit) 0 else pager.currentItem + 1
                    pager.setCurrentItem(next, true)
                }
                pager.removeCallbacks(this)
                pager.postDelayed(this, ADVANCE_MS)
            }
        }

        fun restartTimer() {
            pager.removeCallbacks(advance)
            if (pager.isAttachedToWindow) pager.postDelayed(advance, ADVANCE_MS)
        }

        fun updateDots(animate: Boolean) {
            val count = pager.adapter?.itemCount ?: 0
            dots.visibility = if (count > 1) View.VISIBLE else View.GONE
            if (count <= 1) return
            val shown = min(count, MAX_DOTS)
            val current = pager.currentItem.coerceIn(0, count - 1)
            val first = (current - shown / 2).coerceIn(0, count - shown)
            val density = dots.resources.displayMetrics.density
            if (animate && dots.childCount == shown) {
                TransitionManager.beginDelayedTransition(dots, ChangeBounds().setDuration(220))
            }
            while (dots.childCount < shown) {
                dots.addView(View(dots.context).apply {
                    setBackgroundResource(R.drawable.skia_hero_dot)
                })
            }
            while (dots.childCount > shown) dots.removeViewAt(dots.childCount - 1)
            for (i in 0 until shown) {
                val dot = dots.getChildAt(i)
                val active = first + i == current
                val params = (dot.layoutParams as? LinearLayout.LayoutParams)
                    ?: LinearLayout.LayoutParams(0, 0)
                params.width = ((if (active) 22 else 6) * density).roundToInt()
                params.height = (6 * density).roundToInt()
                params.marginStart = if (i == 0) 0 else (5 * density).roundToInt()
                dot.layoutParams = params
                dot.alpha = if (active) 0.92f else 0.35f
            }
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(animate = true)
                restartTimer()
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) restartTimer()
            }
        })

        pager.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = updateDots(animate = false)
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) =
                updateDots(animate = false)

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) =
                updateDots(animate = false)
        })

        pager.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = restartTimer()
            override fun onViewDetachedFromWindow(v: View) {
                pager.removeCallbacks(advance)
            }
        })
        if (pager.isAttachedToWindow) restartTimer()
        updateDots(animate = false)
    }

    /**
     * Upstream places the search/profile row with the status bar height resource, which is
     * smaller than the real inset on many phones. Use the real inset so the row lines up with
     * the source pill (SkiaUi.placeHomeSourcePill): both are centered 26dp below the status bar.
     */
    private fun alignTopRow(row: View) {
        row.doOnAttach {
            val top = ViewCompat.getRootWindowInsets(row)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: return@doOnAttach
            val params = row.layoutParams as? ViewGroup.MarginLayoutParams ?: return@doOnAttach
            params.topMargin = top + row.resources.displayMetrics.density.roundToInt()
            row.layoutParams = params
        }
    }
}
