package com.bernardo.ircontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernardo.ircontrol.AppState
import com.bernardo.ircontrol.RemoteKey

@Composable
fun RemoteHeader(state: AppState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(
            Modifier.clickable {
                state.screen = if (state.brand == null) com.bernardo.ircontrol.Screen.BRANDS else com.bernardo.ircontrol.Screen.MODELS
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.brand ?: "Escolher marca da TV", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(state.modelLabel, color = Palette.TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ExpandMore, contentDescription = "Trocar modelo", tint = Palette.TextDim)
        }
        val msg = state.sendingLabel ?: state.status
        Text(
            msg ?: " ",
            color = if (state.sendingLabel != null) Palette.Accent else Palette.Orange,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun RemoteScreen(state: AppState) {
    val p = state::press
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteHeader(state)

        // Linha superior: Power, GUIDE, HDMI, Home
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val m = Modifier.width(74.dp).height(46.dp)
            RemoteButton({ p(RemoteKey.POWER) }, m, corner = 16.dp, outlined = true) { KeyLabel("Power", Icons.Rounded.PowerSettingsNew, size = 16) }
            RemoteButton({ p(RemoteKey.GUIDE) }, m, corner = 16.dp, outlined = true) { KeyLabel("GUIDE") }
            RemoteButton({ p(RemoteKey.HDMI) }, m, corner = 16.dp, outlined = true) { KeyLabel("HDMI") }
            RemoteButton({ p(RemoteKey.HOME) }, m, corner = 16.dp, outlined = true) { KeyLabel("Home", Icons.Rounded.Home, size = 16) }
        }

        Spacer(Modifier.height(22.dp))
        DPad(state)
        Spacer(Modifier.height(26.dp))

        // VOL, MENU / Mudo / Entrada, CH
        Row(verticalAlignment = Alignment.Top) {
            Rocker("VOL", { p(RemoteKey.VOL_UP) }, { p(RemoteKey.VOL_DOWN) }, Modifier.width(72.dp).height(216.dp))
            Column(
                Modifier.padding(horizontal = 46.dp).offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val m = Modifier.size(70.dp)
                RemoteButton({ p(RemoteKey.MENU) }, m, corner = 30.dp) { KeyLabel("MENU", size = 14) }
                RemoteButton({ p(RemoteKey.MUTE) }, m, corner = 30.dp) { KeyLabel("Mudo", Icons.Filled.VolumeOff, size = 14) }
                RemoteButton({ p(RemoteKey.INPUT) }, m, corner = 30.dp) { KeyLabel("Entrada", Icons.Filled.Input, size = 14) }
            }
            Rocker("CH", { p(RemoteKey.CH_UP) }, { p(RemoteKey.CH_DOWN) }, Modifier.width(72.dp).height(216.dp))
        }
    }
}

@Composable
private fun DPad(state: AppState) {
    val p = state::press
    Box(Modifier.width(320.dp).height(300.dp), contentAlignment = Alignment.Center) {
        // Contorno em cruz com gradiente
        Canvas(Modifier.size(262.dp)) {
            val armHalf = 56.dp.toPx()
            val r = CornerRadius(44.dp.toPx())
            val c = size.width / 2
            val v = Path().apply { addRoundRect(RoundRect(c - armHalf, 0f, c + armHalf, size.height, r)) }
            val h = Path().apply { addRoundRect(RoundRect(0f, c - armHalf, size.width, c + armHalf, r)) }
            val cross = Path().apply { op(v, h, PathOperation.Union) }
            drawPath(cross, Palette.ringGradient, style = Stroke(width = 2.5.dp.toPx()))
        }
        val arrow = Modifier.size(70.dp)
        RemoteButton({ p(RemoteKey.UP) }, arrow.offset(y = (-88).dp), corner = 28.dp) { KeyLabel("Cima", Icons.Rounded.PlayArrow, -90f, 18) }
        RemoteButton({ p(RemoteKey.DOWN) }, arrow.offset(y = 88.dp), corner = 28.dp) { KeyLabel("Baixo", Icons.Rounded.PlayArrow, 90f, 18) }
        RemoteButton({ p(RemoteKey.LEFT) }, arrow.offset(x = (-90).dp), corner = 28.dp) { KeyLabel("Esquerda", Icons.Rounded.PlayArrow, 180f, 18) }
        RemoteButton({ p(RemoteKey.RIGHT) }, arrow.offset(x = 90.dp), corner = 28.dp) { KeyLabel("Direita", Icons.Rounded.PlayArrow, 0f, 18) }
        RemoteButton({ p(RemoteKey.OK) }, Modifier.size(80.dp), corner = 34.dp, background = Palette.okGradient, elevation = 10.dp) {
            KeyLabel("OK", size = 22, color = androidx.compose.ui.graphics.Color.White)
        }

        val corner = Modifier.size(68.dp)
        RemoteButton({ p(RemoteKey.TOOLS) }, corner.offset((-124).dp, (-112).dp), corner = 32.dp) { KeyLabel("TOOLS", size = 13) }
        RemoteButton({ p(RemoteKey.INFO) }, corner.offset(124.dp, (-112).dp), corner = 32.dp) { KeyLabel("INFO", size = 13) }
        RemoteButton({ p(RemoteKey.BACK) }, corner.offset((-124).dp, 112.dp), corner = 32.dp) { KeyLabel("Voltar", Icons.Filled.Undo, size = 14) }
        RemoteButton({ p(RemoteKey.EXIT) }, corner.offset(124.dp, 112.dp), corner = 32.dp) { KeyLabel("EXIT", size = 13) }
    }
}

/** Aba "Control": teclado numérico, mídia e teclas coloridas. */
@Composable
fun ControlScreen(state: AppState) {
    val p = state::press
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteHeader(state)
        val k = Modifier.width(88.dp).height(62.dp)
        RemoteKey.digits.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                if (row.size == 1) Spacer(Modifier.width(88.dp))
                row.forEach { key -> RemoteButton({ p(key) }, k, corner = 24.dp) { KeyLabel(key.aliases[0], size = 24) } }
                if (row.size == 1) Spacer(Modifier.width(88.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val m = Modifier.size(54.dp)
            RemoteButton({ p(RemoteKey.REWIND) }, m, corner = 20.dp) { KeyLabel("Voltar", Icons.Rounded.FastRewind) }
            RemoteButton({ p(RemoteKey.PLAY) }, m, corner = 20.dp) { KeyLabel("Play", Icons.Rounded.PlayArrow) }
            RemoteButton({ p(RemoteKey.PAUSE) }, m, corner = 20.dp) { KeyLabel("Pause", Icons.Rounded.Pause) }
            RemoteButton({ p(RemoteKey.STOP) }, m, corner = 20.dp) { KeyLabel("Stop", Icons.Rounded.Stop) }
            RemoteButton({ p(RemoteKey.FORWARD) }, m, corner = 20.dp) { KeyLabel("Avançar", Icons.Rounded.FastForward) }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                RemoteKey.RED to Palette.Red, RemoteKey.GREEN to androidx.compose.ui.graphics.Color(0xFF3DBE5B),
                RemoteKey.YELLOW to androidx.compose.ui.graphics.Color(0xFFF2C230), RemoteKey.BLUE to Palette.Blue,
            ).forEach { (key, color) ->
                RemoteButton({ p(key) }, Modifier.width(66.dp).height(30.dp), corner = 15.dp,
                    background = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.85f))) {}
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val m = Modifier.width(96.dp).height(46.dp)
            RemoteButton({ p(RemoteKey.RECORD) }, m, corner = 16.dp, outlined = true) { KeyLabel("REC", size = 13) }
            RemoteButton({ p(RemoteKey.SUBTITLE) }, m, corner = 16.dp, outlined = true) { KeyLabel("LEGENDA", size = 13) }
            RemoteButton({ p(RemoteKey.SLEEP) }, m, corner = 16.dp, outlined = true) { KeyLabel("SLEEP", size = 13) }
        }
    }
}
