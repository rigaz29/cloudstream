package com.lagradost.cloudstream3.skia

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.WeakHashMap

/**
 * How far a multi-source search is: how many sources were asked, how many answered
 * (successfully or not) and how many returned results.
 *
 * Upstream's SearchViewModel only keeps successful results, so it reports progress here
 * through one-line hooks (start / answered / clear). State is kept per view model
 * instance, so the main search and quick search do not mix.
 */
object SkiaSearchProgress {
    data class Progress(val total: Int, val answered: Int, val withResults: Int) {
        val done: Boolean get() = answered >= total
    }

    private class State {
        val live = MutableLiveData<Progress?>(null)
        var token = -1
        var total = 0
        var answered = 0
        var withResults = 0
    }

    private val states = WeakHashMap<Any, State>()

    private fun state(owner: Any): State = states.getOrPut(owner) { State() }

    @Synchronized
    fun of(owner: Any): LiveData<Progress?> = state(owner).live

    /** A search with [token] starts on [total] sources. */
    @Synchronized
    fun start(owner: Any, token: Int, total: Int) {
        val s = state(owner)
        s.token = token
        s.total = total
        s.answered = 0
        s.withResults = 0
        s.live.postValue(Progress(total, 0, 0))
    }

    /** One source of search [token] answered; answers of older searches are ignored. */
    @Synchronized
    fun answered(owner: Any, token: Int, hasResults: Boolean) {
        val s = state(owner)
        if (s.token != token) return
        s.answered++
        if (hasResults) s.withResults++
        s.live.postValue(Progress(s.total, s.answered, s.withResults))
    }

    @Synchronized
    fun clear(owner: Any) {
        val s = state(owner)
        s.token = -1
        s.live.postValue(null)
    }
}
