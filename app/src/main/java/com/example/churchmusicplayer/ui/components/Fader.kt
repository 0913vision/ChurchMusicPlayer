package com.example.churchmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun Fader(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var faderHeight by remember { mutableStateOf(0) }
    var thumbHeight by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .width(60.dp)
            .height(300.dp)
            .background(Color.Black)
            .onSizeChanged { size ->
                faderHeight = size.height
            }
    ) {
        // Fader track
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .background(Color.DarkGray)
        )

        // Fader thumb
        val thumbOffset = if (isDragging) {
            dragOffset
        } else {
            (faderHeight - thumbHeight) * (100 - volume) / 100f
        }

        Card(
            modifier = Modifier
                .width(50.dp)
                .height(40.dp)
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, thumbOffset.roundToInt()) }
                .onSizeChanged { size ->
                    thumbHeight = size.height
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        dragOffset = (dragOffset + delta).coerceIn(0f, (faderHeight - thumbHeight).toFloat())
                        val newVolume = (100 - (dragOffset / (faderHeight - thumbHeight) * 100)).roundToInt().coerceIn(0, 100)
                        onVolumeChange(newVolume)
                    },
                    onDragStarted = { isDragging = true },
                    onDragStopped = { isDragging = false }
                ),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {}
    }

    LaunchedEffect(volume) {
        if (!isDragging) {
            dragOffset = (faderHeight - thumbHeight) * (100 - volume) / 100f
        }
    }
}