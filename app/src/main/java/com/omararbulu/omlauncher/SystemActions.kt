package com.omararbulu.omlauncher

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.provider.Settings

object SystemActions {
    /** Baja la cortina de notificaciones. */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar")
            val cls = Class.forName("android.app.StatusBarManager")
            cls.getMethod("expandNotificationsPanel").invoke(service)
        } catch (e: Throwable) {
            // Algunos teléfonos no lo permiten; no pasa nada.
        }
    }

    const val CLAUDE_PACKAGE = "com.anthropic.claude"
    const val NOTE_PREFIX = "Nota de voz: "

    /** Abre la nota en el app de Claude; si no está instalado, muestra el menú de compartir. */
    fun sendToClaude(context: Context, text: String) {
        val base = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, NOTE_PREFIX + text)
        val claude = Intent(base).setPackage(CLAUDE_PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(claude)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent.createChooser(base, "Enviar nota").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun open(context: Context, action: String) {
        runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openUsageAccess(context: Context) = open(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
    fun openHomeSettings(context: Context) = open(context, Settings.ACTION_HOME_SETTINGS)

    fun pickWallpaper(context: Context) {
        runCatching {
            context.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Elegir fondo")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openCalendar(context: Context) {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
