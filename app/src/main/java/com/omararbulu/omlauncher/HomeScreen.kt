package com.omararbulu.omlauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

class HomeActions(
    val openApp: (AppEntry) -> Unit,
    val openDrawer: () -> Unit,
    val openSettings: () -> Unit,
    val expandNotifications: () -> Unit,
    val openAgenda: () -> Unit,
    val screenTimeClick: () -> Unit,
    val micDown: () -> Boolean,
    val micUp: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    prefs: Prefs,
    apps: AppsRepo,
    ink: Ink,
    accent: Color,
    micInk: Color,
    textShadow: Boolean,
    now: LocalDateTime,
    battery: Int,
    is24: Boolean,
    screenTime: Long?,
    nextEvent: CalEvent?,
    actions: HomeActions,
) {
    val act by rememberUpdatedState(actions)
    val haptic = LocalHapticFeedback.current
    val shadow = if (textShadow) Shadow(Color(0x66000000), blurRadius = 6f) else null
    fun label(e: AppEntry) = prefs.renames[e.key] ?: e.label
    val noRipple = Modifier
    val shadowStyle = LocalTextStyle.current.copy(shadow = shadow)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    act.openSettings()
                })
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -120f) act.openDrawer() else if (total > 120f) act.expandNotifications()
                    },
                    onVerticalDrag = { change, delta ->
                        total += delta
                        change.consume()
                    },
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Tiempo en pantalla, arriba a la derecha
            Box(Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (prefs.showScreenTime) {
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { act.screenTimeClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        if (screenTime != null) {
                            Text(
                                ScreenTime.format(screenTime), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = ink.main, style = shadowStyle,
                            )
                            Text(
                                "en pantalla hoy", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                color = ink.soft, style = shadowStyle,
                            )
                        } else {
                            Text(
                                "Activar tiempo en pantalla", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                color = ink.soft, style = shadowStyle,
                            )
                        }
                    }
                }
            }

            // Saludo, fecha y próxima actividad
            Column(Modifier.padding(horizontal = 32.dp)) {
                val greeting = Fmt.greeting(prefs.greeting, now, is24)
                val big = prefs.greeting != "hello"
                Text(
                    greeting,
                    fontSize = if (big) 76.sp else 44.sp,
                    lineHeight = if (big) 78.sp else 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    color = ink.main,
                    style = shadowStyle,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Fmt.subline(prefs.greeting, now, battery, is24),
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ink.soft,
                    style = shadowStyle,
                )
                if (prefs.showNextEvent && nextEvent != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (ink.light) Color(0x24FFFFFF) else Color(0x73FFFFFF))
                            .clickable { act.openAgenda() }
                            .heightIn(min = 32.dp)
                            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Clay))
                        Text(Fmt.time(nextEvent.begin, is24), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink.main)
                        Text(
                            nextEvent.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink.main,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                        )
                        Text("· " + Fmt.relative(nextEvent, now, is24), fontSize = 14.sp, color = ink.soft, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // Favoritas: solo texto
            Column(Modifier.padding(horizontal = 32.dp)) {
                prefs.favorites.mapNotNull { apps.byKey(it) }.forEach { entry ->
                    Box(
                        Modifier
                            .height(52.dp)
                            .combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    act.openSettings()
                                },
                                onClick = { act.openApp(entry) },
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(label(entry), fontSize = 24.sp, color = ink.main, style = shadowStyle)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Puntos de página y micrófono flotante
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                PageDots(active = 0, color = Color.White, modifier = Modifier.align(Alignment.Center))
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(56.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(accent)
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val started = act.micDown()
                                tryAwaitRelease()
                                if (started) act.micUp()
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    MicIcon(micInk, Modifier.size(24.dp))
                }
            }

            // Fila de abajo
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                prefs.dock.mapNotNull { apps.byKey(it) }.forEach { entry ->
                    Box(
                        noRipple
                            .height(44.dp)
                            .clickable(interactionSource = null, indication = null) { act.openApp(entry) }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label(entry), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = DockText)
                    }
                }
            }
        }
    }
}

@Composable
fun PageDots(active: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) { i ->
            Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(if (i == active) color else color.copy(alpha = 0.45f)),
            )
        }
    }
}

@Composable
fun MicIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = w * 0.085f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Cápsula del micrófono
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.375f, h * 0.125f),
            size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.46f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.125f),
            style = stroke,
        )
        // Arco de soporte
        drawArc(
            color = color,
            startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.21f, h * 0.17f),
            size = androidx.compose.ui.geometry.Size(w * 0.58f, h * 0.58f),
            style = stroke,
        )
        drawLine(
            color, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.75f),
            androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.88f),
            strokeWidth = w * 0.085f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
