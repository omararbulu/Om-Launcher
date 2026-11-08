package com.omararbulu.omlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

enum class VoiceState { Idle, Listening, Ready }

/** Dictado mientras se mantiene presionado el micrófono. */
class VoiceController(private val context: Context) {
    var state by mutableStateOf(VoiceState.Idle); private set
    var committed by mutableStateOf(""); private set
    var partial by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null); private set
    var startedAt by mutableStateOf(0L); private set
    var duration by mutableStateOf(0L); private set
    val levels = mutableStateListOf<Float>().apply { repeat(34) { add(0f) } }

    private var recognizer: SpeechRecognizer? = null
    private var holding = false
    private val handler = Handler(Looper.getMainLooper())

    val text: String get() = listOf(committed, partial).filter { it.isNotBlank() }.joinToString(" ").trim()

    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onRmsChanged(rmsdB: Float) {
            val v = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
            if (levels.isNotEmpty()) levels.removeAt(0)
            levels.add(v)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!t.isNullOrBlank()) partial = t
        }

        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            commit(if (t.isNullOrBlank()) partial else t)
            if (holding) listen() else finish()
        }

        override fun onError(code: Int) {
            commit(partial)
            val retry = code == SpeechRecognizer.ERROR_NO_MATCH || code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            if (holding && retry) {
                listen()
                return
            }
            if (holding) {
                holding = false
                error = when (code) {
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Sin conexión para transcribir. Intenta de nuevo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso del micrófono."
                    else -> null
                }
            }
            finish()
        }
    }

    private fun commit(t: String?) {
        if (!t.isNullOrBlank()) committed = (committed + " " + t).trim()
        partial = ""
    }

    private fun listen() {
        try {
            recognizer?.startListening(recognizerIntent())
        } catch (e: Exception) {
            finish()
        }
    }

    fun start() {
        error = null
        committed = ""
        partial = ""
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "Este teléfono no tiene el reconocimiento de voz activo."
            state = VoiceState.Ready
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(listener) }
        }
        holding = true
        startedAt = SystemClock.elapsedRealtime()
        duration = 0L
        state = VoiceState.Listening
        listen()
    }

    fun release() {
        if (state != VoiceState.Listening) return
        holding = false
        duration = SystemClock.elapsedRealtime() - startedAt
        runCatching { recognizer?.stopListening() }
        handler.postDelayed({ if (state == VoiceState.Listening) finish() }, 2500)
    }

    private fun finish() {
        if (state == VoiceState.Idle) return
        if (duration == 0L) duration = SystemClock.elapsedRealtime() - startedAt
        commit(partial)
        if (state == VoiceState.Listening) state = VoiceState.Ready
    }

    fun edit(t: String) { committed = t; partial = "" }

    fun discard() {
        holding = false
        runCatching { recognizer?.cancel() }
        committed = ""
        partial = ""
        error = null
        state = VoiceState.Idle
    }

    fun destroy() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}

@Composable
fun VoiceOverlay(voice: VoiceController, onSend: (String) -> Unit) {
    if (voice.state == VoiceState.Idle) return
    var editing by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(voice.state, voice.startedAt) {
        while (voice.state == VoiceState.Listening) {
            elapsed = SystemClock.elapsedRealtime() - voice.startedAt
            delay(250)
        }
        elapsed = voice.duration
    }
    val listening = voice.state == VoiceState.Listening

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x80141618))
            .clickable(enabled = !listening, indication = null, interactionSource = null) { },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SheetBg)
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (listening) Color(0xFFC0533A) else Color(0xFF4F7E68)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (listening) "ESCUCHANDO" else "NOTA LISTA",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
                    color = Color(0xFF5B6370),
                )
                Spacer(Modifier.weight(1f))
                Text(Fmt.mmss(elapsed), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3A404A))
            }

            if (editing && !listening) {
                BasicTextField(
                    value = voice.text,
                    onValueChange = { voice.edit(it) },
                    textStyle = TextStyle(fontFamily = Outfit, fontSize = 22.sp, color = Charcoal, lineHeight = 30.sp),
                    cursorBrush = SolidColor(Clay),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .border(1.5.dp, Color(0xFFC9C5BF), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                )
            } else {
                val shown = buildAnnotatedString {
                    append(voice.committed)
                    if (voice.partial.isNotBlank()) {
                        if (voice.committed.isNotBlank()) append(" ")
                        withStyle(SpanStyle(color = Color(0xFF7A808A))) { append(voice.partial) }
                    }
                    if (voice.committed.isBlank() && voice.partial.isBlank()) {
                        withStyle(SpanStyle(color = Color(0xFF7A808A))) {
                            append(if (listening) "Habla con calma…" else "No se escuchó nada.")
                        }
                    }
                }
                Text(
                    shown, fontSize = 24.sp, lineHeight = 32.sp, color = Charcoal,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                )
            }

            voice.error?.let { Text(it, fontSize = 14.sp, color = Color(0xFFB0452F)) }

            if (listening) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    voice.levels.forEach { lv ->
                        Box(
                            Modifier.width(4.dp).height((6 + lv * 42).dp)
                                .clip(RoundedCornerShape(2.dp)).background(Clay),
                        )
                    }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Suelta para terminar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                    Text("Mantén presionado el micrófono mientras hablas", fontSize = 13.sp, color = Muted)
                }
            } else {
                Text("Se abrirá en Claude como nota nueva.", fontSize = 14.sp, color = Muted)
                PillButton(
                    "Enviar a Claude", filled = true, enabled = voice.text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    onSend(voice.text)
                    editing = false
                    voice.discard()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(if (editing) "Listo" else "Editar texto", modifier = Modifier.weight(1f)) {
                        editing = !editing
                    }
                    PillButton("Descartar", modifier = Modifier.weight(1f)) {
                        editing = false
                        voice.discard()
                    }
                }
            }
        }
    }
}

@Composable
fun PillButton(
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(if (filled) 54.dp else 48.dp)
            .clip(RoundedCornerShape(27.dp))
            .then(
                if (filled) Modifier.background(if (enabled) Charcoal else Color(0xFF9AA0A8))
                else Modifier.border(1.5.dp, Color(0xFFC9C5BF), RoundedCornerShape(27.dp)),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = if (filled) 17.sp else 16.sp,
            fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium,
            color = if (filled) Color.White else Charcoal,
        )
    }
}
