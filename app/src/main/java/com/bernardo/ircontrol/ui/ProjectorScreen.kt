package com.bernardo.ircontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernardo.ircontrol.AppState
import com.bernardo.ircontrol.RemoteKey
import com.bernardo.ircontrol.Screen

/** Aba "Projetor": controle com teclas típicas de projetor (fonte, ocultar, congelar, zoom...). */
@Composable
fun ProjectorScreen(state: AppState) {
    if (state.projBrand == null || state.projModelId == null) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.Videocam, null, tint = Palette.Accent, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Controle do projetor", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Escolha a marca do seu projetor para começar.",
                color = Palette.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )
            RemoteButton({ if (state.projBrand == null) state.openProjBrands() else state.screen = Screen.PROJ_MODELS },
                Modifier.fillMaxWidth().height(56.dp), corner = 20.dp, background = Palette.okGradient) {
                Text("Escolher marca", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val p = state::pressProj
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DeviceHeader(state, state.projBrand!!, state.projModelLabel) { state.screen = Screen.PROJ_MODELS }

        // Ligar, Desligar, Fonte, Menu
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val m = Modifier.width(74.dp).height(46.dp)
            RemoteButton({ p(RemoteKey.POWER) }, m, corner = 16.dp, outlined = true) { KeyLabel("Ligar", Icons.Rounded.PowerSettingsNew, size = 16, color = Palette.Orange) }
            RemoteButton({ p(RemoteKey.POWER_OFF) }, m, corner = 16.dp, outlined = true) { KeyLabel("Desligar", Icons.Rounded.PowerSettingsNew, size = 16) }
            RemoteButton({ p(RemoteKey.INPUT) }, m, corner = 16.dp, outlined = true) { KeyLabel("Fonte", Icons.Filled.Input, size = 16) }
            RemoteButton({ p(RemoteKey.MENU) }, m, corner = 16.dp, outlined = true) { KeyLabel("MENU", size = 13) }
        }

        Spacer(Modifier.height(22.dp))
        DPad(p, RemoteKey.AUTO to "AUTO", RemoteKey.ASPECT to "ASPECT")
        Spacer(Modifier.height(26.dp))

        // VOL, Ocultar / Congelar / Mudo, ZOOM
        Row(verticalAlignment = Alignment.Top) {
            Rocker("VOL", { p(RemoteKey.VOL_UP) }, { p(RemoteKey.VOL_DOWN) }, Modifier.width(72.dp).height(216.dp))
            Column(
                Modifier.padding(horizontal = 46.dp).offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val m = Modifier.size(70.dp)
                RemoteButton({ p(RemoteKey.BLANK) }, m, corner = 30.dp) { KeyLabel("BLANK", size = 13) }
                RemoteButton({ p(RemoteKey.FREEZE) }, m, corner = 30.dp) { KeyLabel("FREEZE", size = 12) }
                RemoteButton({ p(RemoteKey.MUTE) }, m, corner = 30.dp) { KeyLabel("Mudo", Icons.Filled.VolumeOff, size = 14) }
            }
            Rocker("ZOOM", { p(RemoteKey.ZOOM_IN) }, { p(RemoteKey.ZOOM_OUT) }, Modifier.width(72.dp).height(216.dp))
        }
    }
}

@Composable
fun ProjBrandPicker(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Marca do projetor", { state.screen = Screen.REMOTE })
        var query by rememberSaveable { mutableStateOf("") }
        BrandSearchField(query) { query = it }
        if (state.projBrands.isEmpty()) Loading()
        val list = state.projBrands.filter { matchesQuery(it.name, query) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.name }) { b ->
                ListCard(onClick = { state.selectProjBrand(b.name) }, selected = b.name == state.projBrand) {
                    Text(b.name, color = Palette.Text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${b.models} controles", color = Palette.TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ProjModelPicker(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar(state.projBrand ?: "", { state.openProjBrands() })
        if (state.projLoading) { Loading(); return@Column }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                RemoteButton({ state.selectProjModel(AppState.ALL) }, Modifier.fillMaxWidth().height(76.dp), corner = 22.dp, background = Palette.okGradient) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HelpOutline, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text("Não sei o modelo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Envia todos os códigos de projetor da ${state.projBrand} (${state.projModels.size} controles)",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Text(
                    "Ou escolha o modelo do controle. Toque em ⏻ para testar se o projetor responde.",
                    color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.projModels, key = { it.id }) { m ->
                ListCard(onClick = { state.selectProjModel(m.id) }, selected = m.id == state.projModelId) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title, color = Palette.Text, fontSize = 16.sp)
                        Text(m.subtitle, color = Palette.TextDim, fontSize = 12.sp)
                    }
                    if (m.code(RemoteKey.POWER) != null)
                        IconButton(onClick = { state.testPower(m) }) {
                            Icon(Icons.Rounded.PowerSettingsNew, "Testar", tint = Palette.Orange)
                        }
                }
            }
        }
    }
}
