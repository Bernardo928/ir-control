package com.bernardo.ircontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Conteúdo de uma tecla: texto ou ícone. */
@Composable
fun KeyLabel(text: String? = null, icon: ImageVector? = null, rotation: Float = 0f, size: Int = 15, color: Color = Palette.Text) {
    if (icon != null) Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size((size + 9).dp).rotate(rotation))
    else if (text != null) Text(text, color = color, fontSize = size.sp, letterSpacing = 0.5.sp)
}

/** Tecla genérica com sombra, animação de toque e vibração. */
@Composable
fun RemoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    background: Brush = SolidColor(Palette.Button),
    outlined: Boolean = false,
    elevation: Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .scale(if (pressed) 0.94f else 1f)
            .then(if (outlined) Modifier else Modifier.shadow(if (pressed) 1.dp else elevation, shape))
            .background(if (outlined) SolidColor(if (pressed) Palette.Button else Palette.Background) else background, shape)
            .then(if (outlined) Modifier.border(1.4.dp, Palette.Outline, shape) else Modifier)
            .then(if (pressed && !outlined) Modifier.background(Color.White.copy(alpha = 0.06f), shape) else Modifier)
            .clickable(interactionSource = source, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Tecla dupla vertical (+ / −) com rótulo no meio, como VOL e CH. */
@Composable
fun Rocker(label: String, onUp: () -> Unit, onDown: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.shadow(6.dp, RoundedCornerShape(36.dp)).background(Palette.Button, RoundedCornerShape(36.dp))) {
        Column(Modifier.matchParentSize()) {
            RemoteButton(onUp, Modifier.fillMaxWidth().weight(1f), corner = 36.dp, elevation = 0.dp) {
                KeyLabel("+", size = 30)
            }
            RemoteButton(onDown, Modifier.fillMaxWidth().weight(1f), corner = 36.dp, elevation = 0.dp) {
                KeyLabel("−", size = 30)
            }
        }
        Box(Modifier.align(Alignment.Center)) { KeyLabel(label, size = 24) }
    }
}
