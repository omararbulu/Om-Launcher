package com.omararbulu.omlauncher

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private lateinit var apps: AppsRepo
    private lateinit var widgets: WidgetHub
    private lateinit var voice: VoiceController

    private val homePresses = mutableIntStateOf(0)
    private val resumes = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = Prefs(this)
        apps = AppsRepo(this)
        widgets = WidgetHub(this, prefs)
        voice = VoiceController(this)
        BuildConfigInfo.version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        }.getOrDefault("")

        if (!prefs.initialized) {
            val (favorites, dock) = apps.defaults()
            prefs.updateFavorites(favorites)
            prefs.updateDock(dock)
            prefs.markInitialized()
        }

        // En el inicio, el botón atrás no hace nada.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        setContent {
            OmApp(
                activity = this,
                prefs = prefs,
                apps = apps,
                widgets = widgets,
                voice = voice,
                homePresses = homePresses.intValue,
                resumes = resumes.intValue,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
    }

    override fun onStart() {
        super.onStart()
        widgets.start()
    }

    override fun onStop() {
        super.onStop()
        widgets.stop()
    }

    override fun onResume() {
        super.onResume()
        resumes.intValue++
    }

    override fun onDestroy() {
        voice.destroy()
        apps.dispose()
        super.onDestroy()
    }
}

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OmApp(
    activity: Activity,
    prefs: Prefs,
    apps: AppsRepo,
    widgets: WidgetHub,
    voice: VoiceController,
    homePresses: Int,
    resumes: Int,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reloj
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(resumes) {
        while (true) {
            now = LocalDateTime.now()
            val ms = System.currentTimeMillis()
            delay(60_000L - ms % 60_000L + 50L)
        }
    }

    // Batería
    var battery by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0 && scale > 0) battery = level * 100 / scale
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val is24 = remember(resumes) { DateFormat.is24HourFormat(context) }

    // Datos que se refrescan cada minuto y al volver al inicio
    var refresh by remember { mutableIntStateOf(0) }
    var screenTime by remember { mutableStateOf<Long?>(null) }
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    var permissions by remember {
        mutableStateOf(PermissionState(false, false, false))
    }
    LaunchedEffect(resumes, now, refresh) {
        permissions = PermissionState(
            calendar = CalendarSource.hasPermission(context),
            usage = ScreenTime.hasPermission(context),
            mic = granted(context, Manifest.permission.RECORD_AUDIO),
        )
        screenTime = withContext(Dispatchers.IO) { ScreenTime.todayMillis(context) }
        events = withContext(Dispatchers.IO) { CalendarSource.load(context) }
    }
    val nextEvent = remember(events, now) { CalendarSource.next(events, now) }

    // Permisos y resultados
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
    }
    val bindWidget = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        widgets.onBindResult(r.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(Unit) {
        if (!prefs.askedCalendar && !CalendarSource.hasPermission(context)) {
            prefs.markAskedCalendar()
            calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    // Escena y colores
    val hour = now.hour + now.minute / 60f
    val scene = remember(now.hour, now.minute) { sceneAt(hour) }
    val ink = when (prefs.textTone) {
        "dark" -> DarkInk
        "light" -> LightInk
        else -> if (prefs.landscape) scene.ink else LightInk
    }
    val paper = if (prefs.landscape) scene.paper else DayPaper
    val accent = if (prefs.landscape) scene.accent else Color(0xFFF2BFA2)

    // Estado de pantallas
    val pagerState = rememberPagerState(pageCount = { 2 })
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var pickDockSlot by remember { mutableIntStateOf(-1) }
    var renaming by remember { mutableStateOf<AppEntry?>(null) }
    var pickingWidget by remember { mutableStateOf(false) }

    LaunchedEffect(homePresses) {
        if (homePresses > 0) {
            drawerOpen = false
            settingsOpen = false
            pickDockSlot = -1
            if (voice.state != VoiceState.Listening) voice.discard()
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = pagerState.currentPage == 1) { scope.launch { pagerState.animateScrollToPage(0) } }
    BackHandler(enabled = drawerOpen) { drawerOpen = false; pickDockSlot = -1 }
    BackHandler(enabled = voice.state == VoiceState.Ready) { voice.discard() }

    // Íconos de la barra de estado según el fondo
    val view = LocalView.current
    val overPaper = drawerOpen || pagerState.currentPage == 1
    val lightIcons = if (overPaper) paper.ink.light else ink.light
    LaunchedEffect(lightIcons, overPaper) {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = overPaper && !paper.ink.light
        }
    }

    MaterialTheme {
        ProvideTextStyle(TextStyle(fontFamily = Outfit)) {
            Box(Modifier.fillMaxSize()) {
                if (prefs.landscape) Landscape(scene, Modifier.fillMaxSize())

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    if (page == 0) {
                        HomeScreen(
                            prefs = prefs,
                            apps = apps,
                            ink = ink,
                            accent = accent,
                            micInk = scene.micInk,
                            textShadow = !prefs.landscape,
                            now = now,
                            battery = battery,
                            is24 = is24,
                            screenTime = screenTime,
                            nextEvent = nextEvent,
                            actions = HomeActions(
                                openApp = { apps.launch(it) },
                                openDrawer = { drawerOpen = true },
                                openSettings = { settingsOpen = true },
                                expandNotifications = { SystemActions.expandNotifications(context) },
                                openAgenda = { scope.launch { pagerState.animateScrollToPage(1) } },
                                screenTimeClick = {
                                    if (permissions.usage) ScreenTime.openWellbeing(context)
                                    else SystemActions.openUsageAccess(context)
                                },
                                micDown = {
                                    if (granted(context, Manifest.permission.RECORD_AUDIO)) {
                                        voice.start()
                                        true
                                    } else {
                                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                        false
                                    }
                                },
                                micUp = { voice.release() },
                            ),
                        )
                    } else {
                        AgendaScreen(
                            paper = paper,
                            now = now,
                            is24 = is24,
                            events = events,
                            hasCalendar = permissions.calendar,
                            onRequestCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                            onOpenCalendar = { day -> SystemActions.openCalendar(context, day.startMillis()) },
                            widgets = widgets,
                            widgetIds = prefs.widgets,
                            onAddWidget = { pickingWidget = true },
                            onRemoveWidget = { widgets.remove(it) },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = slideInVertically { it / 3 } + fadeIn(),
                    exit = slideOutVertically { it / 3 } + fadeOut(),
                ) {
                    AppDrawer(
                        paper = paper,
                        apps = apps.apps,
                        prefs = prefs,
                        pickTitle = if (pickDockSlot >= 0) "Elige la app para la posición ${pickDockSlot + 1} de la fila de abajo" else null,
                        actions = DrawerActions(
                            launch = { apps.launch(it) },
                            pick = {
                                if (pickDockSlot >= 0) prefs.setDockSlot(pickDockSlot, it.key)
                                pickDockSlot = -1
                            },
                            close = { drawerOpen = false; pickDockSlot = -1 },
                            toggleFavorite = { prefs.toggleFavorite(it.key) },
                            rename = { renaming = it },
                            hide = { prefs.setHidden(it.key, true) },
                            appInfo = { apps.openAppInfo(it) },
                        ),
                    )
                }

                VoiceOverlay(voice) { text -> SystemActions.sendToClaude(context, text) }

                if (settingsOpen) {
                    SettingsSheet(
                        prefs = prefs,
                        apps = apps,
                        permissions = permissions,
                        actions = SettingsActions(
                            close = { settingsOpen = false },
                            pickDockSlot = { i ->
                                settingsOpen = false
                                pickDockSlot = i
                                drawerOpen = true
                            },
                            requestCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                            openUsageAccess = { SystemActions.openUsageAccess(context) },
                            requestMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                            pickWallpaper = { SystemActions.pickWallpaper(context) },
                            openHomeSettings = { SystemActions.openHomeSettings(context) },
                        ),
                    )
                }

                renaming?.let { entry ->
                    RenameDialog(
                        current = prefs.renames[entry.key] ?: entry.label,
                        original = entry.label,
                        onDismiss = { renaming = null },
                        onSave = { prefs.rename(entry.key, it); renaming = null },
                    )
                }

                if (pickingWidget) {
                    WidgetPickerDialog(
                        widgets = widgets,
                        onDismiss = { pickingWidget = false },
                        onPick = { option ->
                            pickingWidget = false
                            widgets.startAdd(option.info)?.let { bindWidget.launch(it) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(current: String, original: String, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        title = { Text("Renombrar", fontFamily = Outfit, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Outfit, fontSize = 17.sp, color = Charcoal),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(if (text.trim() == original) null else text) }) {
                Text("Guardar", fontFamily = Outfit, color = Charcoal)
            }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null) }) {
                Text("Nombre original", fontFamily = Outfit, color = Clay)
            }
        },
    )
}

@Composable
private fun WidgetPickerDialog(
    widgets: WidgetHub,
    onDismiss: () -> Unit,
    onPick: (WidgetHub.Option) -> Unit,
) {
    val options = remember { widgets.options() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        title = { Text("Agregar widget", fontFamily = Outfit, fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                items(options) { option ->
                    androidx.compose.foundation.layout.Column(
                        Modifier.fillMaxWidth().clickable { onPick(option) }.padding(vertical = 10.dp),
                    ) {
                        Text(option.app, fontFamily = Outfit, fontSize = 13.sp, color = Muted)
                        Text(option.name, fontFamily = Outfit, fontSize = 17.sp, color = Charcoal)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", fontFamily = Outfit, color = Charcoal) }
        },
    )
}
