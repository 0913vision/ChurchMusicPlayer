package com.example.churchmusicplayer

import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import com.example.churchmusicplayer.data.ConnectionStatus
import com.example.churchmusicplayer.ui.components.Fader
import com.example.churchmusicplayer.ui.components.SimpleFader
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val volume by viewModel.volume.collectAsState()
    val state by viewModel.state.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ConnectionStatusBar(connectionStatus, onReconnectClick = { viewModel.reconnect() })

        // Record visualization (placeholder)
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Record Visualization Placeholder")
        }

        // Song selection
        Column {
            Button(
                onClick = { viewModel.changeSong("slow") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentSong == "slow") "✓ 잔잔한 음악" else "잔잔한 음악")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.changeSong("fast") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentSong == "fast") "✓ 통성기도 음악" else "통성기도 음악")
            }
        }

        // Fader for volume control
//        Fader(
//            currentVolume = volume.toFloat(),
//            onVolumeChange = { newVolume ->
//                val roundedVolume = newVolume.roundToInt().coerceIn(0, 100)
//                if (roundedVolume != volume) {
//                    viewModel.changeVolume(roundedVolume)
//                }
//            },
//            modifier = Modifier.align(Alignment.CenterHorizontally)
//        )
        Fader(
            volume = volume,
            onVolumeChange = { newVolume -> viewModel.changeVolume(newVolume) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
//        SimpleFader(
//            volume = volume,
//            onVolumeChange = { newVolume ->
//                viewModel.changeVolume(newVolume.toInt())
//            },
//            modifier = Modifier.align(Alignment.CenterHorizontally)
//        )

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                viewModel.reconnect()
            }) {
                Text("Refresh")
            }
//            Text("Volume: $volume")
            Button(onClick = { viewModel.changeState() }) {
                Text(if (state == 1) "Pause" else "Play")
            }
        }
    }
}

@Composable
fun ConnectionStatusBar(status: ConnectionStatus, onReconnectClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when (status) {
                    is ConnectionStatus.Connected -> Color(0xFF4CAF50)  // Green
                    is ConnectionStatus.GracePeriod -> Color(0xFF4CAF50)  // Green
                    is ConnectionStatus.Connecting -> Color(0xFFFFC107)  // Yellow
                    is ConnectionStatus.Disconnected -> Color(0xFFF44336)  // Red
                    is ConnectionStatus.Error -> Color(0xFFF44336)  // Red
                    else -> Color.Gray
                }
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (status) {
                is ConnectionStatus.Connected -> "연결 상태: 매우 좋음"
                is ConnectionStatus.GracePeriod -> "연결 상태: 보통"
                is ConnectionStatus.Connecting -> "연결 중..."
                is ConnectionStatus.Disconnected -> "연결 끊김"
                is ConnectionStatus.Error -> "오류: ${status.message}"
                else -> "알 수 없는 상태"
            },
            color = Color.White
        )
        if (status !is ConnectionStatus.Connected && status !is ConnectionStatus.Connecting && status !is ConnectionStatus.GracePeriod) {
            Button(onClick = onReconnectClick) {
                Text("재연결")
            }
        }
    }
}