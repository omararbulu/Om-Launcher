package com.omararbulu.omlauncher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Ajustes del launcher guardados en el teléfono. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("om_prefs", Context.MODE_PRIVATE)

    var initialized by mutableStateOf(sp.getBoolean("initialized", false)); private set
    var greeting by mutableStateOf(sp.getString("greeting", "day") ?: "day"); private set
    var textTone by mutableStateOf(sp.getString("textTone", "auto") ?: "auto"); private set
    var showScreenTime by mutableStateOf(sp.getBoolean("showScreenTime", true)); private set
    var showNextEvent by mutableStateOf(sp.getBoolean("showNextEvent", true)); private set
    var landscape by mutableStateOf(sp.getBoolean("landscape", true)); private set
    var favorites by mutableStateOf(readList("favorites")); private set
    var dock by mutableStateOf(readList("dock").let { (it + List(4) { "" }).take(4) }); private set
    var hidden by mutableStateOf(readList("hidden").toSet()); private set
    var renames by mutableStateOf(readMap("renames")); private set
    var widgets by mutableStateOf(readList("widgets").mapNotNull { it.toIntOrNull() }); private set
    var askedCalendar by mutableStateOf(sp.getBoolean("askedCalendar", false)); private set

    private fun readList(key: String): List<String> =
        (sp.getString(key, "") ?: "").split("\n").filter { it.isNotEmpty() }

    private fun readMap(key: String): Map<String, String> =
        readList(key).mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i > 0) line.substring(0, i) to line.substring(i + 1) else null
        }.toMap()

    private fun putList(key: String, list: List<String>) {
        sp.edit().putString(key, list.joinToString("\n")).apply()
    }

    fun markInitialized() { initialized = true; sp.edit().putBoolean("initialized", true).apply() }
    fun markAskedCalendar() { askedCalendar = true; sp.edit().putBoolean("askedCalendar", true).apply() }
    fun updateGreeting(v: String) { greeting = v; sp.edit().putString("greeting", v).apply() }
    fun updateTextTone(v: String) { textTone = v; sp.edit().putString("textTone", v).apply() }
    fun updateShowScreenTime(v: Boolean) { showScreenTime = v; sp.edit().putBoolean("showScreenTime", v).apply() }
    fun updateShowNextEvent(v: Boolean) { showNextEvent = v; sp.edit().putBoolean("showNextEvent", v).apply() }
    fun updateLandscape(v: Boolean) { landscape = v; sp.edit().putBoolean("landscape", v).apply() }

    fun updateFavorites(list: List<String>) { favorites = list; putList("favorites", list) }
    fun toggleFavorite(key: String) {
        updateFavorites(if (key in favorites) favorites - key else favorites + key)
    }
    fun moveFavorite(key: String, delta: Int) {
        val list = favorites.toMutableList()
        val i = list.indexOf(key)
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return
        list.removeAt(i)
        list.add(j, key)
        updateFavorites(list)
    }

    fun updateDock(list: List<String>) { dock = list; putList("dock", list) }
    fun setDockSlot(index: Int, key: String) {
        updateDock(dock.toMutableList().also { it[index] = key })
    }

    fun setHidden(key: String, hide: Boolean) {
        hidden = if (hide) hidden + key else hidden - key
        putList("hidden", hidden.toList())
        if (hide && key in favorites) updateFavorites(favorites - key)
    }

    fun rename(key: String, name: String?) {
        renames = if (name.isNullOrBlank()) renames - key else renames + (key to name.trim())
        putList("renames", renames.map { "${it.key}\t${it.value}" })
    }

    fun addWidget(id: Int) { widgets = widgets + id; putList("widgets", widgets.map { it.toString() }) }
    fun removeWidget(id: Int) { widgets = widgets - id; putList("widgets", widgets.map { it.toString() }) }
}
