package com.example.nextclass.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Glance keeps a widget's composition alive after it is first drawn, and a later update does not
 * run the widget's data loading again. Each widget watches this counter and reloads its data
 * whenever it changes, so a tap or a database change shows up straight away.
 */
object WidgetTicker {
    private val _tick = MutableStateFlow(0L)
    val tick: StateFlow<Long> = _tick.asStateFlow()

    fun bump() {
        _tick.update { it + 1 }
    }
}
