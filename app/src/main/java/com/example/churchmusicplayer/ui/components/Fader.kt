package com.example.churchmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun Fader(
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var faderHeight by remember { mutableStateOf(0f) }
    var thumbHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .width(60.dp)
            .height(200.dp)
            .background(Color.LightGray)
            .onGloballyPositioned { coordinates ->
                faderHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newPosition = (faderHeight - thumbHeight) * (1 - currentVolume / 100) + dragAmount.y
                    val newVolume = (1 - newPosition / (faderHeight - thumbHeight)).coerceIn(0f, 1f) * 100
                    onVolumeChange(newVolume)
                }
            }
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .offset(
                    y = with(density) {
                        -((currentVolume / 100) * (faderHeight - thumbHeight)).toDp()
                    }
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .onGloballyPositioned { coordinates ->
                    thumbHeight = coordinates.size.height.toFloat()
                }
        )
    }
}