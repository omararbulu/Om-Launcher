package com.omararbulu.omlauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.view.View

/** Aloja los widgets de otras apps en la página de Agenda. */
class WidgetHub(private val activity: Activity, private val prefs: Prefs) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    val host = AppWidgetHost(activity, HOST_ID)

    private var pendingId = -1
    private var pendingInfo: AppWidgetProviderInfo? = null

    data class Option(val info: AppWidgetProviderInfo, val app: String, val name: String)

    fun options(): List<Option> {
        val pm = activity.packageManager
        return manager.installedProviders.map { info ->
            val app = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
            }.getOrDefault(info.provider.packageName)
            Option(info, app, info.loadLabel(pm) ?: app)
        }.sortedWith(compareBy<Option>({ it.app.lowercase() }, { it.name.lowercase() }))
    }

    /** Devuelve un Intent si Android tiene que pedir permiso para agregar el widget. */
    fun startAdd(info: AppWidgetProviderInfo): Intent? {
        val id = host.allocateAppWidgetId()
        return if (manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            finishAdd(id, info)
            null
        } else {
            pendingId = id
            pendingInfo = info
            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }
    }

    fun onBindResult(ok: Boolean) {
        val id = pendingId
        val info = pendingInfo
        pendingId = -1
        pendingInfo = null
        if (id == -1) return
        if (ok && info != null) finishAdd(id, info) else host.deleteAppWidgetId(id)
    }

    private fun finishAdd(id: Int, info: AppWidgetProviderInfo) {
        prefs.addWidget(id)
        if (info.configure != null) {
            runCatching { host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null) }
        }
    }

    fun remove(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
        prefs.removeWidget(id)
    }

    fun info(id: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)

    fun createView(context: Context, id: Int): View? {
        val info = info(id) ?: return null
        return runCatching { host.createView(context, id, info) }.getOrNull()
    }

    fun start() = runCatching { host.startListening() }
    fun stop() = runCatching { host.stopListening() }

    companion object {
        const val HOST_ID = 4021
        const val REQUEST_CONFIGURE = 77
    }
}
