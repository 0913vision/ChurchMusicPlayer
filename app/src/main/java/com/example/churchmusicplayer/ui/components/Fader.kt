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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.example.churchmusicplayer.ui.Layout
import com.example.churchmusicplayer.ui.LocalUiScale
import kotlin.math.roundToInt

@Composable
fun Fader(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    processing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var faderHeight by remember { mutableStateOf(0) }
    var thumbHeight by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    // Note(yoochan.kim): the fader grows with the app zoom; the drag maths are
    // safe because they use the measured pixel sizes
    val uiScale = LocalUiScale.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // Note(yoochan.kim): the travel needs air at both ends — a thumb
            // pinned to the very edge reads as broken rather than as loudest
            .padding(vertical = Layout.faderPaddingV)
//            .background(Color.Black)
            .onSizeChanged { size ->
                faderHeight = size.height
            }
    ) {
        // Fader track
        Box(
            modifier = Modifier
                .width(Layout.faderTrackWidth * uiScale)
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
                .width(Layout.faderThumbWidth * uiScale)
                .height(Layout.faderThumbHeight * uiScale)
//                .shadow(1.dp)
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, thumbOffset.roundToInt()) }
                .onSizeChanged { size ->
                    thumbHeight = size.height
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(10.dp),
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                // Note(yoochan.kim): swapping this modifier mid-drag kills the drag
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (processing) return@rememberDraggableState
                        dragOffset = (dragOffset + delta).coerceIn(0f, (faderHeight - thumbHeight).toFloat())
                        val newVolume = (100 - (dragOffset / (faderHeight - thumbHeight) * 100)).roundToInt().coerceIn(0, 100)
                        onVolumeChange(newVolume)
                    },
                    onDragStarted = { isDragging = true },
                    onDragStopped = { isDragging = false }
                ),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                // processing이 true면 회색으로 변경하고 투명도 추가
                containerColor = if (!processing) Color.White else Color.Gray.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (!processing) 4.dp else 1.dp // processing이 true면 elevation 감소
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(Layout.faderThumbWidth * uiScale / 2)
                        .height(5.dp)
                        .background(Color(0xFFB9B4B2), RoundedCornerShape(2.dp))
                )
            }
        }
    }

    LaunchedEffect(volume) {
        if (!isDragging) {
            dragOffset = (faderHeight - thumbHeight) * (100 - volume) / 100f
        }
    }
}