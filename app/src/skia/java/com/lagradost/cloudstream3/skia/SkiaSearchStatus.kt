package com.lagradost.cloudstream3.skia

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentSearchBinding
import com.lagradost.cloudstream3.databinding.SkiaSearchStatusBinding
import com.lagradost.cloudstream3.ui.search.SearchViewModel
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout

/**
 * Status line under the search filters: "3 of 12 sources answered" while a search runs,
 * "Results from 5 of 12 sources" when it is done. Attached from SearchFragment with a
 * one-line hook; the numbers come from SkiaSearchProgress.
 *
 * The line is inserted at runtime below the type chips (phone only) instead of living in
 * fragment_search.xml, because upstream binds the TV layout with the same binding class.
 */
object SkiaSearchStatus {
    fun attach(fragment: Fragment, viewModel: SearchViewModel, binding: FragmentSearchBinding?) {
        if (binding == null || !isLayout(PHONE)) return
        val anchor = binding.tvtypesChipsScroll.root
        val parent = anchor.parent as? ViewGroup ?: return
        val status = SkiaSearchStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        parent.addView(status.root, parent.indexOfChild(anchor) + 1)

        SkiaSearchProgress.of(viewModel).observe(fragment.viewLifecycleOwner) { progress ->
            val show = progress != null && progress.total > 0 && !binding.searchHistoryRecycler.isVisible
            status.root.isVisible = show
            if (!show || progress == null) return@observe

            val res = status.root.resources
            status.skiaSearchStatusText.text = if (progress.done) {
                res.getQuantityString(R.plurals.skia_search_done, progress.total, progress.withResults, progress.total)
            } else {
                res.getQuantityString(R.plurals.skia_search_progress, progress.total, progress.answered, progress.total)
            }
            status.skiaSearchStatusDot.alpha = if (progress.done) 0.4f else 1f
        }
    }
}
