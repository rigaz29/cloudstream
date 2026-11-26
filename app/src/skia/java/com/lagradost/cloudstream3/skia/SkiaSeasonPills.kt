package com.lagradost.cloudstream3.skia

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentResultBinding
import com.lagradost.cloudstream3.ui.result.ResultViewModel2

/**
 * Season pills on the phone details page: a horizontal row with one pill per season,
 * replacing upstream's "Season" button + dialog when a title has more than one season.
 *
 * Reads the same view model data the TV layout uses (seasonSelections and
 * selectedSeasonIndex) and switches seasons through ResultViewModel2.changeSeason.
 * Attached from ResultFragmentPhone with a one-line hook, after upstream's observers,
 * so it runs after them for the same update.
 */
object SkiaSeasonPills {
    fun attach(fragment: Fragment, viewModel: ResultViewModel2, binding: FragmentResultBinding?) {
        val list = binding?.skiaSeasonPills ?: return
        val seasonButton = binding.resultSeasonButton
        val adapter = Adapter { season -> viewModel.changeSeason(season) }
        list.adapter = adapter
        val owner = fragment.viewLifecycleOwner

        viewModel.seasonSelections.observe(owner) { seasons ->
            val ctx = list.context
            // Keep every entry so positions match selectedSeasonIndex.
            adapter.submit(seasons.map { (text, season) -> (text?.asStringNull(ctx) ?: "") to season })
            val show = seasons.size > 1
            list.isVisible = show
            if (show) seasonButton.isGone = true
        }
        viewModel.selectedSeasonIndex.observe(owner) { index ->
            adapter.select(index)
            if (index >= 0) list.scrollToPosition(index)
        }
        // Upstream shows the season button whenever a season is selected.
        viewModel.selectedSeason.observe(owner) {
            if (list.isVisible) seasonButton.isGone = true
        }
    }

    private class Adapter(val onPick: (Int) -> Unit) : RecyclerView.Adapter<Adapter.Holder>() {
        private var items: List<Pair<String, Int>> = emptyList()
        private var selected = -1

        class Holder(val text: TextView) : RecyclerView.ViewHolder(text)

        @SuppressLint("NotifyDataSetChanged")
        fun submit(newItems: List<Pair<String, Int>>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun select(index: Int) {
            val old = selected
            selected = index
            if (old in items.indices) notifyItemChanged(old)
            if (index in items.indices) notifyItemChanged(index)
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.skia_season_pill, parent, false) as TextView
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (name, season) = items[position]
            holder.text.text = name
            holder.text.isSelected = position == selected
            holder.text.visibility = if (name.isBlank()) View.GONE else View.VISIBLE
            holder.text.setOnClickListener { if (position != selected) onPick(season) }
        }
    }
}
