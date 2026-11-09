package com.omararbulu.omlauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class DrawerActions(
    val launch: (AppEntry) -> Unit,
    val pick: (AppEntry) -> Unit,
    val close: () -> Unit,
    val toggleFavorite: (AppEntry) -> Unit,
    val rename: (AppEntry) -> Unit,
    val hide: (AppEntry) -> Unit,
    val appInfo: (AppEntry) -> Unit,
)

private data class Row2(val entry: AppEntry, val label: String, val letter: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    paper: Paper,
    apps: List<AppEntry>,
    prefs: Prefs,
    pickTitle: String?,
    actions: DrawerActions,
) {
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ink = paper.ink

    val rows = remember(apps, prefs.hidden, prefs.renames, query) {
        val q = AppsRepo.normalize(query.trim())
        val visible = apps
            .filter { it.key !in prefs.hidden }
            .map { it to (prefs.renames[it.key] ?: it.label) }
            .filter { q.isEmpty() || AppsRepo.normalize(it.second).contains(q) }
            .sortedBy { AppsRepo.normalize(it.second) }
        var prev = ""
        visible.map { (e, label) ->
            val l = AppsRepo.normalize(label).take(1).uppercase()
            val shown = if (q.isEmpty() && l != prev) l else ""
            prev = l
            Row2(e, label, shown)
        }
    }
    val letters = remember(rows) { rows.filter { it.letter.isNotEmpty() }.map { it.letter } }

    fun closeDrawer() {
        focus.clearFocus()
        query = ""
        actions.close()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.overlay)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Asa para cerrar deslizando hacia abajo
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 60f) closeDrawer() },
                        onVerticalDrag = { c, d -> total += d; c.consume() },
                    )
                }
                .clickable(interactionSource = null, indication = null) { closeDrawer() },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(paper.line))
        }

        Column(Modifier.padding(horizontal = 28.dp)) {
            if (pickTitle != null) {
                Text(pickTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = paper.accent)
                Spacer(Modifier.height(10.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(ink.main.copy(alpha = 0.07f))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchIcon(paper.muted, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Buscar apps", fontSize = 16.sp, color = paper.muted)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = Outfit, fontSize = 16.sp, color = ink.main),
                        cursorBrush = SolidColor(paper.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            rows.firstOrNull()?.let {
                                if (pickTitle != null) actions.pick(it.entry) else actions.launch(it.entry)
                                closeDrawer()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Row(Modifier.weight(1f).padding(start = 28.dp, end = 12.dp)) {
            LazyColumn(Modifier.weight(1f), state = listState) {
                itemsIndexed(rows, key = { _, r -> r.entry.key }) { _, r ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (pickTitle != null) actions.pick(r.entry) else actions.launch(r.entry)
                                        closeDrawer()
                                    },
                                    onLongClick = { if (pickTitle == null) menu = true },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                r.letter, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = paper.accent,
                                modifier = Modifier.width(34.dp),
                            )
                            Text(r.label, fontSize = 20.sp, color = ink.main, maxLines = 1)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = SheetBg) {
                            val fav = r.entry.key in prefs.favorites
                            DropdownMenuItem(
                                text = { Text(if (fav) "Quitar de favoritas" else "Agregar a favoritas", color = Charcoal, fontSize = 16.sp) },
                                onClick = { menu = false; actions.toggleFavorite(r.entry) },
                            )
                            DropdownMenuItem(text = { Text("Renombrar", color = Charcoal, fontSize = 16.sp) }, onClick = { menu = false; actions.rename(r.entry) })
                            DropdownMenuItem(text = { Text("Ocultar", color = Charcoal, fontSize = 16.sp) }, onClick = { menu = false; actions.hide(r.entry) })
                            DropdownMenuItem(
                                text = { Text("Información de la app", color = Charcoal, fontSize = 16.sp) },
                                onClick = { menu = false; actions.appInfo(r.entry) },
                            )
                        }
                    }
                }
            }
            if (query.isEmpty()) {
                Column(
                    Modifier.padding(start = 6.dp, top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    letters.forEach { l ->
                        Text(
                            l, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = paper.muted,
                            modifier = Modifier
                                .clickable(interactionSource = null, indication = null) {
                                    val index = rows.indexOfFirst { it.letter == l }
                                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                                }
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val stroke = w * 0.11f
        drawCircle(
            color, radius = w * 0.3f,
            center = androidx.compose.ui.geometry.Offset(w * 0.45f, w * 0.45f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(w * 0.68f, w * 0.68f),
            androidx.compose.ui.geometry.Offset(w * 0.9f, w * 0.9f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
