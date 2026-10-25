package com.bernardo.ircontrol.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernardo.ircontrol.AppState
import com.bernardo.ircontrol.Screen

@Composable
fun App(state: AppState) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.Background, Palette.BackgroundDeep)))
            .statusBarsPadding().navigationBarsPadding()
    ) {
        when (state.screen) {
            Screen.BRANDS -> {
                BackHandler(enabled = state.brand != null && state.modelId != null) { state.screen = Screen.REMOTE }
                BrandPicker(state)
            }
            Screen.MODELS -> {
                BackHandler { state.screen = if (state.modelId != null) Screen.REMOTE else Screen.BRANDS }
                ModelPicker(state)
            }
            Screen.REMOTE -> Column(Modifier.fillMaxSize()) {
                BackHandler(enabled = state.tab != 0) { state.tab = 0 }
                Box(Modifier.weight(1f)) {
                    when (state.tab) {
                        0 -> RemoteScreen(state)
                        1 -> ControlScreen(state)
                        else -> SettingsScreen(state)
                    }
                }
                BottomBar(state)
            }
        }
    }
}

@Composable
private fun BottomBar(state: AppState) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavItem(Icons.Rounded.SettingsRemote, "Remote", state.tab == 0) { state.tab = 0 }
        NavItem(Icons.Rounded.GridView, "Control", state.tab == 1) { state.tab = 1 }
        NavItem(Icons.Rounded.Settings, "Settings", state.tab == 2) { state.tab = 2 }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = if (selected) Palette.Accent else Palette.TextDim
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = c, modifier = Modifier.size(26.dp))
        Text(label, color = c, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(state: AppState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Configurações", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        SettingCard("Marca", state.brand ?: "—") { state.screen = Screen.BRANDS }
        SettingCard("Modelo", state.modelLabel) { state.screen = Screen.MODELS }
        SettingCard(
            "Emissor infravermelho",
            if (state.transmitter.available) "Disponível ✓" else "Não encontrado neste aparelho",
            null,
        )
        SettingCard(
            "Banco de códigos",
            "IR-DB (github.com/probonopd/irdb) — ${state.brands.size} marcas, embutido no app, funciona offline.",
            null,
        )
        SettingCard(
            "Dica",
            "No modo \"Não sei o modelo\" cada tecla envia em sequência os códigos de todos os controles de TV da marca. " +
                "Se descobrir o que funciona, escolha o modelo para respostas mais rápidas.",
            null,
        )
    }
}

@Composable
private fun SettingCard(title: String, value: String, onClick: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Button)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp)
    ) {
        Text(title, color = Palette.TextDim, fontSize = 12.sp)
        Text(value, color = Palette.Text, fontSize = 16.sp)
    }
}
