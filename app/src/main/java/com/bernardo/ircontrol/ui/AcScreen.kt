package com.bernardo.ircontrol.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernardo.ircontrol.AcFan
import com.bernardo.ircontrol.AcMode
import com.bernardo.ircontrol.AppState
import com.bernardo.ircontrol.Screen

private fun AcMode.icon() = when (this) {
    AcMode.AUTO -> Icons.Rounded.Autorenew
    AcMode.COOL -> Icons.Rounded.AcUnit
    AcMode.DRY -> Icons.Rounded.WaterDrop
    AcMode.FAN -> Icons.Rounded.Air
    AcMode.HEAT -> Icons.Rounded.WbSunny
}

@Composable
fun AcScreen(state: AppState) {
    if (state.acBrand == null || state.acModel == null) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.AcUnit, null, tint = Palette.Accent, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Controle do ar-condicionado", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Escolha a marca do seu ar-condicionado para começar.",
                color = Palette.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )
            RemoteButton({ state.screen = if (state.acBrand == null) Screen.AC_BRANDS else Screen.AC_MODELS },
                Modifier.fillMaxWidth().height(56.dp), corner = 20.dp, background = Palette.okGradient) {
                Text("Escolher marca", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val ac = state.ac
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cabeçalho
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(Modifier.clickable { state.screen = Screen.AC_MODELS }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.acBrand!!.name, color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(state.acModelLabel, color = Palette.TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Trocar modelo", tint = Palette.TextDim)
            }
            Text(
                state.sendingLabel ?: state.status ?: " ",
                color = if (state.sendingLabel != null) Palette.Accent else Palette.Orange,
                fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Mostrador de temperatura
        Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 10.dp.toPx()
                val r = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                val tl = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
                drawArc(Palette.Button, 135f, 270f, false, tl, r, style = Stroke(w, cap = StrokeCap.Round))
                if (ac.power) {
                    val range = state.acTempRange
                    val frac = (ac.temp - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1)
                    drawArc(Palette.ringGradient, 135f, 270f * (0.08f + 0.92f * frac), false, tl, r, style = Stroke(w, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (ac.power) "${ac.temp}°" else "--",
                    color = if (ac.power) Palette.Text else Palette.TextDim,
                    fontSize = 68.sp, fontWeight = FontWeight.Light,
                )
                Text(
                    if (ac.power) "${ac.mode.label} · Vent. ${ac.fan.label}" else "Desligado",
                    color = Palette.TextDim, fontSize = 14.sp,
                )
            }
        }

        // − Power +
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            RemoteButton({ state.updateAc { it.copy(temp = it.temp - 1) } }, Modifier.size(68.dp), corner = 34.dp) {
                KeyLabel("Diminuir", Icons.Rounded.Remove, size = 22)
            }
            RemoteButton(
                { state.updateAc { it.copy(power = !it.power) } }, Modifier.size(84.dp), corner = 42.dp,
                background = if (ac.power) Palette.okGradient else SolidColor(Palette.Button), elevation = 10.dp,
            ) { KeyLabel("Ligar/desligar", Icons.Rounded.PowerSettingsNew, size = 26, color = if (ac.power) Color.White else Palette.Text) }
            RemoteButton({ state.updateAc { it.copy(temp = it.temp + 1) } }, Modifier.size(68.dp), corner = 34.dp) {
                KeyLabel("Aumentar", Icons.Rounded.Add, size = 22)
            }
        }

        SectionTitle("Modo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcMode.entries.forEach { m ->
                Chip(m.label, m == ac.mode, Modifier.width(64.dp)) { state.updateAc { it.copy(mode = m) } }
            }
        }

        SectionTitle("Ventilação")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcFan.entries.forEach { f ->
                Chip(f.label, f == ac.fan, Modifier.width(80.dp), icon = false) { state.updateAc { it.copy(fan = f) } }
            }
        }

        Spacer(Modifier.height(18.dp))
        RemoteButton({ state.acSwing() }, Modifier.width(200.dp).height(48.dp), corner = 18.dp, outlined = !ac.swing,
            background = SolidColor(Palette.Accent.copy(alpha = 0.35f))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeyLabel(null, Icons.Rounded.SwapVert, size = 14)
                Text("  Vaivém ${if (ac.swing) "ligado" else "desligado"}", color = Palette.Text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, color = Palette.TextDim, fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier, icon: Boolean = true, onClick: () -> Unit) {
    RemoteButton(
        onClick, modifier.height(if (icon) 66.dp else 44.dp), corner = 18.dp, outlined = !selected,
        background = SolidColor(Palette.Accent.copy(alpha = 0.35f)), elevation = 2.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon) {
                val mode = AcMode.entries.firstOrNull { it.label == label }
                if (mode != null) Icon(mode.icon(), null, tint = if (selected) Color.White else Palette.TextDim, modifier = Modifier.size(22.dp))
            }
            Text(label, color = if (selected) Color.White else Palette.Text, fontSize = 12.sp)
        }
    }
}

@Composable
fun AcBrandPicker(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Marca do ar-condicionado", { state.screen = Screen.REMOTE })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.bernardo.ircontrol.AcBrands.all, key = { it.name }) { b ->
                ListCard(onClick = { state.selectAcBrand(b) }, selected = b == state.acBrand) {
                    Text(b.name, color = Palette.Text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text(b.protocols.joinToString(" / ") { it.title.substringBefore(" (") }, color = Palette.TextDim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AcModelPicker(state: AppState) {
    val brand = state.acBrand ?: return
    Column(Modifier.fillMaxSize()) {
        TopBar(brand.name, { state.screen = Screen.AC_BRANDS })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                RemoteButton({ state.selectAcModel(AppState.ALL) }, Modifier.fillMaxWidth().height(76.dp), corner = 22.dp, background = Palette.okGradient) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HelpOutline, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text("Não sei o modelo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Envia cada comando em todos os protocolos da ${brand.name} (${brand.protocols.size})",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Text(
                    "Ou escolha o protocolo. Toque em ⏻ para testar: o ar deve ligar.",
                    color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(brand.protocols, key = { it.name }) { p ->
                ListCard(onClick = { state.selectAcModel(p.name) }, selected = state.acModel == p.name) {
                    Column(Modifier.weight(1f)) {
                        Text(p.title, color = Palette.Text, fontSize = 16.sp)
                        Text("Temperatura ${p.tempRange.first}–${p.tempRange.last} °C", color = Palette.TextDim, fontSize = 12.sp)
                    }
                    IconButton(onClick = { state.testAc(p) }) {
                        Icon(Icons.Rounded.PowerSettingsNew, "Testar", tint = Palette.Orange)
                    }
                }
            }
        }
    }
}
