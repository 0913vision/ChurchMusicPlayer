package com.example.churchmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SimpleFader(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text(text = "Volume: $volume")
//        Spacer(modifier = Modifier.height(8.dp))
//        Box(
//            modifier = Modifier
//                .width(60.dp)
//                .height(200.dp)
//                .background(Color.LightGray)
//        ) {
//
//        }
        Slider(
            value = volume.toFloat(),
            onValueChange = { newVolume ->
                onVolumeChange(newVolume.toInt())
            },
            valueRange = 0f..100f,
            steps = 100,
            modifier = Modifier
                .width(200.dp)
//                .rotate(-90f)
        )
    }
}