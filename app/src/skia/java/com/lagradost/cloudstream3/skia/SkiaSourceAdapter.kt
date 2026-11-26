package com.lagradost.cloudstream3.skia

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.player.DisplayLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities

/**
 * Source list for the player's "sources and subtitles" dialog: a quality badge, the link
 * name and a line with the stream type and where the link comes from.
 *
 * Upstream shows plain "name quality" strings; GeneratorPlayer swaps its adapter for this
 * one through a one-line hook. Rows keep the same order and positions as upstream's list,
 * so its click, long-click and selection handling work unchanged.
 */
class SkiaSourceAdapter(
    context: Context,
    private val links: List<DisplayLink>,
) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = links.size

    override fun getItem(position: Int): DisplayLink = links[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.skia_source_item, parent, false)
        val (link, uri) = links[position].link
        val name = link?.name ?: uri?.name ?: "NULL"

        val quality = Qualities.getStringByInt(link?.quality)
        view.findViewById<TextView>(R.id.skia_source_quality).apply {
            text = quality
            visibility = if (quality.isBlank()) View.GONE else View.VISIBLE
        }
        view.findViewById<TextView>(R.id.skia_source_name).text = name

        val type = when {
            link == null -> view.context.getString(R.string.skia_source_type_file)
            else -> when (link.type) {
                ExtractorLinkType.M3U8 -> "HLS"
                ExtractorLinkType.DASH -> "DASH"
                ExtractorLinkType.VIDEO -> view.context.getString(R.string.skia_source_type_video)
                ExtractorLinkType.TORRENT -> "Torrent"
                ExtractorLinkType.MAGNET -> "Magnet"
            }
        }
        val origin = link?.source?.takeIf { it.isNotBlank() && !name.startsWith(it, ignoreCase = true) }
        view.findViewById<TextView>(R.id.skia_source_meta).text =
            listOfNotNull(type, origin).joinToString(" · ")
        return view
    }
}
