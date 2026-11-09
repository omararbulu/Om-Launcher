package com.omararbulu.omlauncher

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun AgendaScreen(
    paper: Paper,
    now: LocalDateTime,
    is24: Boolean,
    events: List<CalEvent>,
    hasCalendar: Boolean,
    onRequestCalendar: () -> Unit,
    onOpenCalendar: (LocalDate) -> Unit,
    widgets: WidgetHub,
    widgetIds: List<Int>,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val addWidget by rememberUpdatedState(onAddWidget)
    val today = now.toLocalDate()
    val tomorrow = today.plusDays(1)
    val ink = paper.ink

    var editingWidgets by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var monthEvents by remember { mutableStateOf<List<CalEvent>>(emptyList()) }

    // Los eventos del mes que se está mirando; se recargan cuando cambian los del inicio.
    LaunchedEffect(month, hasCalendar, events) {
        monthEvents = withContext(Dispatchers.IO) {
            CalendarSource.load(context, month.atDay(1), month.atEndOfMonth().plusDays(1))
        }
    }
    val marked = remember(monthEvents) { monthEvents.map { it.day }.toSet() }

    Box(Modifier.fillMaxSize().background(paper.overlay)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Mantener presionado en cualquier espacio libre agrega un widget.
                    detectTapGestures(onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        addWidget()
                    })
                }
                .statusBarsPadding()
                .navigationBarsPadding(),
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
                listOf(today, tomorrow).forEach { day ->
                    item(key = "h$day") { SectionTitle(Fmt.dayHeader(day, today), paper) }
                    val list = events.filter { it.day == day }
                    if (list.isEmpty()) {
                        item(key = "e$day") {
                            Text(
                                "Sin actividades", fontSize = 16.sp, color = paper.muted,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    } else {
                        items(list) { e -> EventRow(e, now, is24, paper) { onOpenCalendar(e.day) } }
                    }
                    item(key = "s$day") { Spacer(Modifier.height(22.dp)) }
                }

                item(key = "month") {
                    MonthCalendar(
                        month = month,
                        today = today,
                        selected = selected,
                        marked = marked,
                        paper = paper,
                        onPrev = { month = month.minusMonths(1); selected = null },
                        onNext = { month = month.plusMonths(1); selected = null },
                        onToday = { month = YearMonth.from(today); selected = null },
                        onSelect = { d -> selected = if (d == selected) null else d },
                    )
                }

                val pick = selected
                if (pick != null && pick != today && pick != tomorrow) {
                    item(key = "sel") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionTitle(
                                "${Fmt.dayLong(pick)} ${pick.dayOfMonth} de ${Fmt.monthName(pick.monthValue).lowercase()}",
                                paper, Modifier.weight(1f),
                            )
                            Text(
                                "Abrir", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = paper.accent,
                                modifier = Modifier.clickable { onOpenCalendar(pick) }.padding(6.dp),
                            )
                        }
                    }
                    val list = monthEvents.filter { it.day == pick }
                    if (list.isEmpty()) {
                        item(key = "sel-empty") {
                            Text(
                                "Sin actividades", fontSize = 16.sp, color = paper.muted,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    } else {
                        items(list) { e -> EventRow(e, now, is24, paper) { onOpenCalendar(e.day) } }
                    }
                }
            }

            if (widgetIds.isNotEmpty()) {
                item(key = "wtitle") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 26.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionTitle("Widgets", paper, Modifier.weight(1f))
                        Text(
                            if (editingWidgets) "Listo" else "Editar",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = paper.accent,
                            modifier = Modifier.clickable { editingWidgets = !editingWidgets }.padding(6.dp),
                        )
                    }
                }
                items(widgetIds, key = { "w$it" }) { id ->
                    WidgetCard(widgets, id, editingWidgets) { onRemoveWidget(id) }
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
                    color = if (ink.light) Charcoal else Color.White,
                )
            }
            Box(Modifier.weight(1f).height(1.dp).background(paper.line))
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    marked: Set<LocalDate>,
    paper: Paper,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val ink = paper.ink
    val isCurrent = month == YearMonth.from(today)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ink.main.copy(alpha = if (ink.light) 0.06f else 0.05f))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${Fmt.monthName(month.monthValue)} ${month.year}",
                fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = ink.main,
                modifier = Modifier.weight(1f),
            )
            if (!isCurrent) {
                Text(
                    "Hoy", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = paper.accent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onToday() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
            ArrowButton("‹", paper, onPrev)
            ArrowButton("›", paper, onNext)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                Text(
                    it, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = paper.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val length = month.lengthOfMonth()
        val rows = (offset + length + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val n = r * 7 + c - offset + 1
                    if (n in 1..length) {
                        val day = month.atDay(n)
                        val isToday = day == today
                        val isSelected = day == selected
                        val past = day.isBefore(today)
                        Column(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(day) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            isToday -> Modifier.background(ink.main)
                                            isSelected -> Modifier.border(1.5.dp, ink.main, CircleShape)
                                            else -> Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$n",
                                    fontSize = 15.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isToday -> if (ink.light) Charcoal else Color.White
                                        past -> paper.muted
                                        else -> ink.main
                                    },
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.size(5.dp).clip(CircleShape)
                                    .background(if (day in marked) Clay else Color.Transparent),
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f).height(46.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowButton(symbol: String, paper: Paper, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 26.sp, color = paper.ink.main)
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
private fun WidgetCard(widgets: WidgetHub, id: Int, editing: Boolean, onRemove: () -> Unit) {
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

/** Milisegundos del inicio de un día, para abrir el calendario en esa fecha. */
fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
