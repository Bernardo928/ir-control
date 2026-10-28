package com.bernardo.ircontrol.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernardo.ircontrol.AppState
import com.bernardo.ircontrol.Screen

/** Marcas que aparecem no topo da lista. */
private val POPULAR = listOf(
    "Samsung", "LG", "Philips", "Sony", "TCL", "Panasonic", "Toshiba", "Sharp", "Philco", "AOC",
    "Hisense", "Semp", "Sanyo", "JVC", "Hitachi", "Mitsubishi", "Pioneer", "Vizio", "Grundig", "Thomson",
)

@Composable
internal fun TopBar(title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Voltar", tint = Palette.Text) }
        else Spacer(Modifier.size(12.dp))
        Text(title, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BrandPicker(state: AppState) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("Escolha a marca da TV", if (state.brand != null) ({ state.screen = Screen.REMOTE }) else null)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Buscar marca…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Palette.Outline, focusedBorderColor = Palette.Accent),
        )
        val q = query.trim()
        val list = remember(state.brands, q) {
            val all = state.brands.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            val pop = all.filter { b -> POPULAR.any { it.equals(b.name, true) } }
                .sortedBy { b -> POPULAR.indexOfFirst { it.equals(b.name, true) } }
            pop + (all - pop.toSet())
        }
        if (state.brand == null) {
            ListCard(onClick = { state.tab = 2; state.screen = Screen.REMOTE }, selected = false,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text("❄  Só quero controlar o ar-condicionado", color = Palette.Text, fontSize = 15.sp)
            }
        }
        if (state.brands.isEmpty()) Loading()
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.name }) { b ->
                ListCard(onClick = { state.selectBrand(b.name) }, selected = b.name == state.brand) {
                    Text(b.name, color = Palette.Text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text("${b.models} controles", color = Palette.TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ModelPicker(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar(state.brand ?: "", { state.screen = Screen.BRANDS })
        if (state.loading) { Loading(); return@Column }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                RemoteButton({ state.selectModel(AppState.ALL) }, Modifier.fillMaxWidth().height(76.dp), corner = 22.dp, background = Palette.okGradient) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HelpOutline, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text("Não sei o modelo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Envia todos os códigos de TV da ${state.brand} (${state.tvModels.size} controles)", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Text(
                    "Ou escolha o modelo do controle. Toque em ⏻ para testar se a TV responde.",
                    color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.models, key = { it.id }) { m ->
                ListCard(onClick = { state.selectModel(m.id) }, selected = m.id == state.modelId) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title, color = Palette.Text, fontSize = 16.sp)
                        Text(m.subtitle + if (m.looksLikeTv) "" else " · talvez não seja TV", color = Palette.TextDim, fontSize = 12.sp)
                    }
                    if (m.code(com.bernardo.ircontrol.RemoteKey.POWER) != null)
                        IconButton(onClick = { state.testPower(m) }) {
                            Icon(Icons.Rounded.PowerSettingsNew, "Testar", tint = Palette.Orange)
                        }
                }
            }
        }
    }
}

@Composable
internal fun ListCard(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.ButtonPressed else Palette.Button, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun Loading() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
