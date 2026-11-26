package com.lagradost.cloudstream3.skia

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentResultBinding
import com.lagradost.cloudstream3.databinding.FragmentResultSwipeBinding
import com.lagradost.cloudstream3.ui.WatchType
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
        if (swipe == null || result == null) return
        extendUnderStatusBar(swipe)
        fadeInTopBar(swipe, result)
        bookmarkButton(fragment, viewModel, swipe)
        moreMenu(viewModel, swipe)
    }

    private fun color(view: View, attr: Int): Int {
        val value = TypedValue()
        view.context.theme.resolveAttribute(attr, value, true)
        return value.data
    }

    /**
     * Upstream pads the whole page by the status bar. Like Nuvio, let the artwork run under
     * the status bar instead: pull the content up by that padding and give it to the top bar,
     * whose background then also covers the status bar once it fades in. Re-applied on every
     * layout, since upstream sets the padding again on configuration changes.
     */
    private fun extendUnderStatusBar(swipe: FragmentResultSwipeBinding) {
        val root = swipe.root
        val bar = swipe.resultTopBar
        val barPadding = bar.paddingTop
        var applied = -1
        // The root would clip the pulled-up content to its padding.
        (root as? ViewGroup)?.clipToPadding = false

        fun apply() {
            val top = root.paddingTop
            if (top == applied) return
            applied = top
            swipe.resultMainHolder.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = -top
            }
            bar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = -top }
            bar.updatePadding(top = barPadding + top)
            // Side panels (sync, recommendations) keep their content below the status bar.
            swipe.startPanel.updatePadding(top = top)
            swipe.endPanel.updatePadding(top = top)
        }

        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (root.paddingTop != applied) root.post { apply() }
        }
        apply()
    }

    /**
     * The top bar floats over the artwork without a background. Once the artwork scrolls
     * away, fade in the page color behind it, and the page title with it.
     */
    private fun fadeInTopBar(swipe: FragmentResultSwipeBinding, result: FragmentResultBinding) {
        val bar = swipe.resultTopBar
        val title = swipe.root.findViewById<TextView>(R.id.skia_top_title)
        val scroll = result.resultScroll
        val artwork = result.resultTopHolder
        val background = ColorDrawable(color(bar, R.attr.primaryBlackBackground)).apply {
            alpha = 0
        }
        bar.background = background

        fun update() {
            val barHeight = bar.height.takeIf { it > 0 } ?: return
            val start = artwork.height - 2 * barHeight
            val fraction = ((scroll.scrollY - start).toFloat() / barHeight).coerceIn(0f, 1f)
            background.alpha = (fraction * 255).toInt()
            if (title != null) {
                val name = result.resultTitle.text
                if (title.text != name) title.text = name
                title.alpha = fraction
            }
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

    /**
     * Upstream's bookmark FAB (hidden in the Skia layout) becomes a round top bar button:
     * "+" while not in the library, an accent bookmark once it is. Tapping it runs the
     * FAB's own click handler (the watch status picker).
     */
    private fun bookmarkButton(
        fragment: Fragment,
        viewModel: ResultViewModel2,
        swipe: FragmentResultSwipeBinding,
    ) {
        val button = swipe.root.findViewById<ImageView>(R.id.skia_bookmark_button) ?: return
        val fab = swipe.resultBookmarkFab
        button.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            fab.performClick()
        }
        viewModel.watchStatus.observe(fragment.viewLifecycleOwner) { type ->
            val saved = type != WatchType.NONE
            button.setImageResource(
                if (saved) R.drawable.ic_baseline_bookmark_24 else R.drawable.ic_baseline_add_24
            )
            button.imageTintList = ColorStateList.valueOf(
                color(button, if (saved) R.attr.colorPrimary else R.attr.white)
            )
            button.contentDescription = button.context.getString(
                if (saved) type.stringRes else R.string.action_add_to_bookmarks
            )
            button.isVisible = true
        }
    }

    /** Folds upstream's row of page actions into one "more" menu. */
    private fun moreMenu(viewModel: ResultViewModel2, swipe: FragmentResultSwipeBinding) {
        val more = swipe.root.findViewById<View>(R.id.skia_more_button) ?: return
        more.setOnClickListener { anchor ->
            val ctx = anchor.context
            fun item(button: ImageView, label: Int) =
                if (!button.isVisible) null else SkiaPopupMenu.Item(
                    button.drawable?.constantState?.newDrawable()?.mutate(),
                    ctx.getString(label)
                ) { button.performClick() }

            SkiaPopupMenu.show(anchor, listOfNotNull(
                item(
                    swipe.resultFavorite,
                    if (viewModel.favoriteStatus.value == true) R.string.action_remove_from_favorites
                    else R.string.action_add_to_favorites
                ),
                item(
                    swipe.resultSubscribe,
                    if (viewModel.subscribeStatus.value == true) R.string.action_unsubscribe
                    else R.string.action_subscribe
                ),
                item(swipe.resultShare, R.string.result_share),
                item(swipe.resultOpenInBrowser, R.string.result_open_in_browser),
                item(swipe.resultSearch, R.string.result_search_tooltip),
                item(swipe.resultRecommendationsBtt, R.string.recommendations_tooltip),
            ))
        }
    }
}
