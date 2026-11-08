package com.omararbulu.omlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActions(
    val close: () -> Unit,
    val pickDockSlot: (Int) -> Unit,
    val requestCalendar: () -> Unit,
    val openUsageAccess: () -> Unit,
    val requestMic: () -> Unit,
    val pickWallpaper: () -> Unit,
    val openHomeSettings: () -> Unit,
)

data class PermissionState(val calendar: Boolean, val usage: Boolean, val mic: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    prefs: Prefs,
    apps: AppsRepo,
    permissions: PermissionState,
    actions: SettingsActions,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun label(key: String) = apps.byKey(key)?.let { prefs.renames[key] ?: it.label }

    ModalBottomSheet(
        onDismissRequest = actions.close,
        sheetState = sheetState,
        containerColor = SheetBg,
        contentColor = Charcoal,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Ajustes", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Charcoal)

            Group("Saludo") {
                Segmented(
                    listOf("day" to "Día corto", "hello" to "Saludo", "time" to "Hora"),
                    prefs.greeting, prefs::updateGreeting,
                )
            }
            Group("Color del texto") {
                Segmented(
                    listOf("auto" to "Según el fondo", "dark" to "Oscuro", "light" to "Claro"),
                    prefs.textTone, prefs::updateTextTone,
                )
            }

            Column {
                ToggleRow("Mostrar tiempo en pantalla", prefs.showScreenTime, prefs::updateShowScreenTime)
                ToggleRow("Mostrar próxima actividad", prefs.showNextEvent, prefs::updateShowNextEvent)
                ToggleRow("Paisaje según la hora del día", prefs.landscape, prefs::updateLandscape)
                NavRow("Usar mi propio fondo", if (prefs.landscape) "Apaga el paisaje" else "Elegir", actions.pickWallpaper)
            }

            Group("Favoritas") {
                if (prefs.favorites.isEmpty()) {
                    Hint("Todavía no hay favoritas.")
                }
                prefs.favorites.forEach { key ->
                    Row(
                        Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label(key) ?: "App no instalada", fontSize = 17.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        SmallAction("Subir") { prefs.moveFavorite(key, -1) }
                        SmallAction("Bajar") { prefs.moveFavorite(key, 1) }
                        SmallAction("Quitar") { prefs.toggleFavorite(key) }
                    }
                }
                Hint("Para agregar una app, mantenla presionada en la lista de todas las apps.")
            }

            Group("Fila de abajo") {
                prefs.dock.forEachIndexed { i, key ->
                    NavRow("Posición ${i + 1}", label(key) ?: "Vacía") { actions.pickDockSlot(i) }
                }
            }

            if (prefs.hidden.isNotEmpty()) {
                Group("Apps ocultas") {
                    prefs.hidden.forEach { key ->
                        val name = label(key) ?: key.substringBefore('/')
                        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            SmallAction("Mostrar") { prefs.setHidden(key, false) }
                        }
                    }
                }
            }

            Group("Permisos") {
                NavRow("Calendario", if (permissions.calendar) "Activo" else "Permitir", actions.requestCalendar)
                NavRow("Tiempo en pantalla", if (permissions.usage) "Activo" else "Abrir ajustes", actions.openUsageAccess)
                NavRow("Micrófono", if (permissions.mic) "Activo" else "Permitir", actions.requestMic)
            }

            NavRow("Launcher predeterminado", "Cambiar", actions.openHomeSettings)
            Hint("Om Launcher ${BuildConfigInfo.version}")
        }
    }
}

object BuildConfigInfo {
    var version: String = ""
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp, color = Color(0xFF5B6370),
        )
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 14.sp, color = Muted)
}

@Composable
private fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SheetChip).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (value, text) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Charcoal else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    color = if (on) Color.White else Charcoal, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Charcoal,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = SheetChip,
                    uncheckedBorderColor = Color(0xFFC9C5BF),
                ),
            )
        }
        HorizontalDivider(color = SheetLine)
    }
}

@Composable
private fun NavRow(text: String, value: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text("$value  ›", fontSize = 15.sp, color = Muted)
        }
        HorizontalDivider(color = SheetLine)
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Text(
        text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Clay,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
    )
    Spacer(Modifier.height(0.dp))
}
