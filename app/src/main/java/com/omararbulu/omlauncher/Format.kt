package com.omararbulu.omlauncher

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

object Fmt {
    private val daysShort = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    private val daysLong = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")
    private val monthsLong = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre",
    )

    fun dayShort(d: LocalDate) = daysShort[d.dayOfWeek.value - 1]
    fun dayLong(d: LocalDate) = daysLong[d.dayOfWeek.value - 1]

    fun time(t: LocalDateTime, is24: Boolean): String =
        if (is24) "%02d:%02d".format(t.hour, t.minute)
        else "%d:%02d".format(if (t.hour % 12 == 0) 12 else t.hour % 12, t.minute)

    fun greeting(mode: String, now: LocalDateTime, is24: Boolean): String = when (mode) {
        "hello" -> when (now.hour) {
            in 5..11 -> "Buenos días,"
            in 12..18 -> "Buenas tardes,"
            else -> "Buenas noches,"
        }
        "time" -> time(now, is24)
        else -> dayShort(now.toLocalDate()).replaceFirstChar { it.uppercase() } + "."
    }

    fun subline(mode: String, now: LocalDateTime, battery: Int, is24: Boolean): String {
        val d = now.toLocalDate()
        val bat = if (battery >= 0) " · $battery%" else ""
        return if (mode == "time") {
            "${dayLong(d)} ${d.dayOfMonth} de ${monthsLong[d.monthValue - 1]}$bat"
        } else {
            "${time(now, is24)} · ${dayShort(d)} ${d.dayOfMonth} ${months[d.monthValue - 1]}$bat"
        }
    }

    fun dayHeader(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana · ${dayShort(d)} ${d.dayOfMonth}"
        else -> "${dayShort(d).replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}"
    }

    /** "en 1 h 55 min", "ahora", "mañana 09:30". */
    fun relative(e: CalEvent, now: LocalDateTime, is24: Boolean): String {
        if (!e.begin.isAfter(now)) return "ahora"
        val today = now.toLocalDate()
        if (e.day != today) {
            val prefix = if (e.day == today.plusDays(1)) "mañana" else "${dayShort(e.day)} ${e.day.dayOfMonth}"
            return prefix
        }
        val mins = Duration.between(now, e.begin).toMinutes() + 1
        val h = mins / 60
        val m = mins % 60
        return when {
            h == 0L -> "en $m min"
            m == 0L -> "en $h h"
            else -> "en $h h $m min"
        }
    }

    fun mmss(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
}
