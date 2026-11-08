package com.omararbulu.omlauncher

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun AgendaScreen(
    paper: Paper,
    now: LocalDateTime,
    is24: Boolean,
    events: List<CalEvent>,
    hasCalendar: Boolean,
    onRequestCalendar: () -> Unit,
    onOpenCalendar: () -> Unit,
    widgets: WidgetHub,
    widgetIds: List<Int>,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    val today = now.toLocalDate()
    var editingWidgets by remember { mutableStateOf(false) }
    val ink = paper.ink

    Box(Modifier.fillMaxSize().background(paper.overlay)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 36.dp, bottom = 56.dp),
        ) {
            item { WeekStrip(today, paper) }
            item { Spacer(Modifier.height(26.dp)) }

            if (!hasCalendar) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(paper.line.copy(alpha = 0.15f)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Tu calendario", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ink.main)
                        Text(
                            "Para ver tus actividades aquí y en el inicio, el launcher necesita leer tu calendario.",
                            fontSize = 15.sp, color = paper.muted,
                        )
                        Text(
                            "Permitir acceso", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = paper.accent,
                            modifier = Modifier.clickable { onRequestCalendar() }.padding(vertical = 6.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(26.dp)) }
            } else {
                val byDay = events.groupBy { it.day }
                val days = (listOf(today) + byDay.keys).distinct().sorted()
                days.forEach { day ->
                    item(key = "h$day") { SectionTitle(Fmt.dayHeader(day, today), paper) }
                    val list = byDay[day].orEmpty()
                    if (list.isEmpty()) {
                        item(key = "e$day") {
                            Text(
                                "Sin actividades", fontSize = 16.sp, color = paper.muted,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    } else {
                        items(list) { e -> EventRow(e, now, is24, paper, onOpenCalendar) }
                    }
                    item(key = "s$day") { Spacer(Modifier.height(22.dp)) }
                }
            }

            item(key = "wtitle") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Widgets", paper, Modifier.weight(1f))
                    if (widgetIds.isNotEmpty()) {
                        Text(
                            if (editingWidgets) "Listo" else "Editar",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = paper.accent,
                            modifier = Modifier.clickable { editingWidgets = !editingWidgets }.padding(6.dp),
                        )
                    }
                }
            }
            items(widgetIds, key = { "w$it" }) { id ->
                WidgetCard(widgets, id, editingWidgets, paper) { onRemoveWidget(id) }
            }
            item(key = "wadd") {
                Column(
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, paper.line, RoundedCornerShape(18.dp))
                        .clickable { onAddWidget() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("+ Agregar widget", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink.main)
                    Text("Clima, Google Calendar o el que quieras", fontSize = 13.sp, color = paper.muted)
                }
            }
        }
        PageDots(
            active = 1, color = ink.main,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun WeekStrip(today: LocalDate, paper: Paper) {
    val ink = paper.ink
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            for (offset in -3..3) {
                val d = today.plusDays(offset.toLong())
                if (offset == 0) {
                    Text(
                        Fmt.dayLong(d).replaceFirstChar { it.uppercase() },
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = ink.main,
                    )
                } else {
                    Text(Fmt.dayShort(d), fontSize = 14.sp, color = paper.muted)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(1.dp).background(paper.line))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(ink.main),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%02d".format(today.dayOfMonth), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (ink.light) Charcoal else androidx.compose.ui.graphics.Color.White,
                )
            }
            Box(Modifier.weight(1f).height(1.dp).background(paper.line))
        }
    }
}

@Composable
private fun SectionTitle(text: String, paper: Paper, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        color = paper.muted, modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun EventRow(e: CalEvent, now: LocalDateTime, is24: Boolean, paper: Paper, onClick: () -> Unit) {
    val past = !e.allDay && e.end.isBefore(now)
    val color = if (past) paper.muted else paper.ink.main
    Row(
        Modifier.fillMaxWidth().heightIn(min = 38.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (e.allDay) "Día" else Fmt.time(e.begin, is24),
            fontSize = 15.sp, fontWeight = if (past) FontWeight.Medium else FontWeight.SemiBold,
            color = color, modifier = Modifier.width(60.dp),
        )
        Text(
            e.title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (past) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun WidgetCard(widgets: WidgetHub, id: Int, editing: Boolean, paper: Paper, onRemove: () -> Unit) {
    val density = LocalDensity.current
    val info = remember(id) { widgets.info(id) }
    val height = info?.let { with(density) { it.minHeight.toDp() } }?.coerceIn(110.dp, 360.dp) ?: 160.dp
    Column(Modifier.padding(bottom = 14.dp)) {
        AndroidView(
            factory = { ctx ->
                widgets.createView(ctx, id) ?: TextView(ctx).apply { text = "Este widget ya no está disponible" }
            },
            modifier = Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(18.dp)),
        )
        if (editing) {
            Text(
                "Quitar widget", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Clay,
                modifier = Modifier.clickable { onRemove() }.padding(vertical = 8.dp),
            )
        }
    }
}
