package com.omararbulu.omlauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
)

/** Lista de apps instaladas y cómo abrirlas. */
class AppsRepo(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val collator = Collator.getInstance(Locale("es", "PE")).apply { strength = Collator.PRIMARY }

    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    init {
        refresh()
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun refresh() {
        val user = Process.myUserHandle()
        apps = try {
            launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != context.packageName }
                .map {
                    AppEntry(
                        key = it.componentName.flattenToString(),
                        label = it.label?.toString() ?: it.componentName.packageName,
                        component = it.componentName,
                        user = user,
                    )
                }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun byKey(key: String): AppEntry? = apps.firstOrNull { it.key == key }

    fun byPackage(pkg: String?): AppEntry? =
        if (pkg.isNullOrEmpty()) null else apps.firstOrNull { it.component.packageName == pkg }

    fun launch(entry: AppEntry) {
        try {
            launcherApps.startMainActivity(entry.component, entry.user, null, null)
        } catch (e: Exception) {
            // La app pudo desinstalarse justo ahora.
            refresh()
        }
    }

    fun openAppInfo(entry: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + entry.component.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    private fun resolvePackage(intent: Intent): String? {
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = info?.activityInfo?.packageName
        return if (pkg == null || pkg == "android") null else pkg
    }

    /** Favoritas y fila de abajo por defecto, la primera vez que se abre el launcher. */
    fun defaults(): Pair<List<String>, List<String>> {
        val favoritePackages = listOf(
            listOf("com.google.android.calendar", "com.samsung.android.calendar"),
            listOf("com.google.android.apps.tasks"),
            listOf("com.aspiro.tidal"),
            listOf("com.anthropic.claude"),
            listOf("com.whatsapp.w4b", "com.whatsapp"),
        )
        val favorites = favoritePackages.mapNotNull { options ->
            options.firstNotNullOfOrNull { byPackage(it) }?.key
        }
        val dial = resolvePackage(Intent(Intent.ACTION_DIAL))
        val sms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
        val camera = resolvePackage(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        val browser = byPackage("com.android.chrome")?.component?.packageName
            ?: resolvePackage(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
        val dock = listOf(dial, sms, camera, browser).map { byPackage(it)?.key ?: "" }
        return favorites to dock
    }

    fun dispose() {
        runCatching { launcherApps.unregisterCallback(callback) }
    }

    companion object {
        fun normalize(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
                .lowercase(Locale.ROOT)
    }
}
