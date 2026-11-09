package com.omararbulu.omlauncher

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

data class CalEvent(
    val title: String,
    val begin: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val day: LocalDate,
)

object CalendarSource {
    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Eventos desde hoy hasta dentro de [days] días, en orden. */
    fun load(context: Context, days: Int = 7): List<CalEvent> =
        load(context, LocalDate.now(), LocalDate.now().plusDays(days.toLong()))

    /** Eventos entre [from] (incluido) y [until] (excluido), en orden. */
    fun load(context: Context, from: LocalDate, until: LocalDate): List<CalEvent> {
        if (!hasPermission(context)) return emptyList()
        val zone = ZoneId.systemDefault()
        val today = from
        val days = java.time.temporal.ChronoUnit.DAYS.between(from, until).coerceAtLeast(1)
        val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = until.atStartOfDay(zone).toInstant().toEpochMilli()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        val result = mutableListOf<CalEvent>()
        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.VISIBLE} = 1",
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(Sin título)"
                    val b = c.getLong(1)
                    val e = c.getLong(2)
                    val allDay = c.getInt(3) == 1
                    if (allDay) {
                        // Los eventos de todo el día vienen en hora UTC.
                        val d0 = Instant.ofEpochMilli(b).atZone(ZoneOffset.UTC).toLocalDate()
                        val d1 = Instant.ofEpochMilli(e).atZone(ZoneOffset.UTC).toLocalDate()
                        var d = if (d0.isBefore(today)) today else d0
                        while (d.isBefore(d1) && d.isBefore(today.plusDays(days))) {
                            result += CalEvent(title, d.atStartOfDay(), d.plusDays(1).atStartOfDay(), true, d)
                            d = d.plusDays(1)
                        }
                    } else {
                        val bl = LocalDateTime.ofInstant(Instant.ofEpochMilli(b), zone)
                        val el = LocalDateTime.ofInstant(Instant.ofEpochMilli(e), zone)
                        result += CalEvent(title, bl, el, false, bl.toLocalDate())
                    }
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        } catch (e: Exception) {
            return result
        }
        return result.sortedWith(compareBy<CalEvent>({ it.day }, { !it.allDay }, { it.begin }))
    }

    /** La próxima actividad con hora (en curso o por empezar). */
    fun next(events: List<CalEvent>, now: LocalDateTime): CalEvent? =
        events.filter { !it.allDay && it.end.isAfter(now) }.minByOrNull { it.begin }
}

object ScreenTime {
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Minutos de uso de hoy, sin contar este launcher. Null si falta el permiso. */
    fun todayMillis(context: Context): Long? {
        if (!hasPermission(context)) return null
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val events = try { usm.queryEvents(start, now) } catch (e: Exception) { return null }
        val event = UsageEvents.Event()
        val open = HashMap<String, Long>()
        var total = 0L
        val own = context.packageName
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg == own) continue
            val key = pkg + "/" + (event.className ?: "")
            when (event.eventType) {
                1 -> if (!open.containsKey(key)) open[key] = event.timeStamp // en primer plano
                2, 23 -> open.remove(key)?.let { total += event.timeStamp - it } // pausada o detenida
            }
        }
        return total
    }

    fun format(ms: Long): String {
        val minutes = ms / 60_000
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun openWellbeing(context: Context) {
        val pm = context.packageManager
        val candidates = listOf("com.samsung.android.forest", "com.google.android.apps.wellbeing")
        for (pkg in candidates) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                return
            }
        }
        runCatching {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
