package com.example.churchmusicplayer

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.example.churchmusicplayer.data.ConnectionStatus
import com.example.churchmusicplayer.ui.components.Fader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        @Suppress("DEPRECATION")
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        actionBar?.hide()

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

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
        startLockTask()
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val volume by viewModel.volume.collectAsState()
    val state by viewModel.state.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val processing by viewModel.processing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1718))
    ) {
        // Connection Status Bar
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
        ) {
            ConnectionStatusBar(
                status = connectionStatus,
                onReconnectClick = { viewModel.reconnect() }
            )
        }

        // Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
//                .fillMaxHeight()
//                .padding(10.dp)
        ) {
            MainContent(
                volume = volume,
                state = state,
                currentSong = currentSong,
                onVolumeChange = { viewModel.changeVolume(it) },
                onStateChange = { viewModel.changeState() },
                onSongChange = { viewModel.changeSong(it) },
                processing = processing,
            )
            if (connectionStatus !is ConnectionStatus.Connected && connectionStatus !is ConnectionStatus.GracePeriod) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .pointerInput(Unit) { // 터치 이벤트 차단
                            detectTapGestures {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text (
                            "연결이 끊겼습니다.",
                            fontSize = 40.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = -1.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text (
                            "[다시 연결하기]를 눌러서\n재연결을 시도하세요.",
                            fontSize = 25.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = -1.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                        )
                    }

                }
            }
        }

        // Footer
        Footer(
            onRefreshClick = { viewModel.reconnect() }
        )
    }
}

@Composable
fun MainContent(
    volume: Int,
    state: Int,
    currentSong: String,
    onVolumeChange: (Int) -> Unit,
    onStateChange: () -> Unit,
    onSongChange: (String) -> Unit,
    processing: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 50.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxWidth()
//                .border(
//                    width = 0.8.dp,
//                    color = Color.Yellow.copy(alpha = 0.5f),
//                )
        ) {
            // Top Left: Record Visualization
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .padding(12.dp),
//                    .fillMaxSize()
//                    .border(
//                        width = 0.8.dp,
//                        color = Color.White.copy(alpha = 0.5f),
//                    ),
                contentAlignment = Alignment.Center
            ) {
                RecordVisualization(state = state)
            }


            // Top Right: Song Selection
            Box(
                modifier = Modifier
                    .weight(1f),
//                    .aspectRatio(1f)
//                    .fillMaxHeight()
//                    .border(
//                        width = 0.8.dp,
//                        color = Color.White.copy(alpha = 0.5f),
//                    ),
                contentAlignment = Alignment.Center
            ) {
                SongSelection(
                    currentSong = currentSong,
                    onSongChange = onSongChange,
                    processing = processing,
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
//                .border(
//                    width = 0.8.dp,
//                    color = Color.Yellow.copy(alpha = 0.5f),
//                ),
        ) {
            // Bottom Left: Volume Fader
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxSize(),
//                    .aspectRatio(1f)
//                    .fillMaxHeight()
//                    .border(
//                        width = 0.8.dp,
//                        color = Color.White.copy(alpha = 0.5f),
//                    ),
                contentAlignment = Alignment.Center,


                ) {
                Fader(
                    volume = volume,
                    onVolumeChange = onVolumeChange
                )
            }

            // Bottom Right: Volume Display and Play/Pause
            Box(
                modifier = Modifier
                    .weight(1f),
//                    .aspectRatio(1f)
//                    .fillMaxHeight()
//                    .border(
//                        width = 0.8.dp,
//                        color = Color.White.copy(alpha = 0.5f),
//                    ),
                contentAlignment = Alignment.Center
            ) {
                VolumeAndPlayback(
                    volume = volume,
                    state = state,
                    onStateChange = onStateChange,
                    processing = processing,
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusBar(status: ConnectionStatus, onReconnectClick: () -> Unit) {
    var isButtonEnabled by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xff3B3A3A))
            .padding(horizontal = 20.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        when (status) {
                            is ConnectionStatus.Connected -> Color(0xFF4CAF50)  // Green
                            is ConnectionStatus.GracePeriod -> Color(0xFF4CAF50)  // Green
                            is ConnectionStatus.Connecting -> Color(0xFFFFC107)  // Yellow
                            is ConnectionStatus.Disconnected -> Color(0xFFF44336)  // Red
                            is ConnectionStatus.Error -> Color(0xFFF44336)  // Red
                            else -> Color.Gray
                        },
                        shape = CircleShape
                    )
            )
            Text (
                text = when (status) {
                    is ConnectionStatus.Connected -> "연결됨"
                    is ConnectionStatus.GracePeriod -> "연결됨"
                    is ConnectionStatus.Connecting -> "연결중"
                    is ConnectionStatus.Disconnected -> "연결 끊김"
                    is ConnectionStatus.Error -> "오류: ${status.message}"
                    else -> "알 수 없는 상태"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
//        if (status !is ConnectionStatus.Connected && status !is ConnectionStatus.Connecting && status !is ConnectionStatus.GracePeriod) {
        Button(
            onClick = {
                if (isButtonEnabled) {
                    onReconnectClick()
                    isButtonEnabled = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF302E2F)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp),
            enabled = isButtonEnabled
        ) {
            Text(
                "다시 연결하기",
                fontSize = 18.sp,

            )
        }
        if (!isButtonEnabled) {
            LaunchedEffect(Unit) {
                delay(1000)  // 1초 지연
                isButtonEnabled = true
            }
        }
    }
//    }

}

@Composable
fun RecordVisualization(state: Int) {
    var isPlaying by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state) {
        isPlaying = (state == 1)
        if (!isPlaying) {
            rotationAngle %= 360f
        }
    }

    LaunchedEffect(isPlaying) {
        val rotationSpeed = 45f  // 회전 속도 (도/초)
        var lastUpdateTime = System.currentTimeMillis()

        while (isPlaying) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = (currentTime - lastUpdateTime) / 1000f  // 초 단위로 변환
            rotationAngle += rotationSpeed * elapsedTime
            lastUpdateTime = currentTime

            delay(33)  // 약 30 FPS
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.record),
            contentDescription = "Record",
            modifier = Modifier
                .size(250.dp)
                .graphicsLayer {
                    rotationZ = rotationAngle
                }
        )
    }
}

@Composable
fun SongSelection(currentSong: String, onSongChange: (String) -> Unit, processing: Boolean) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /*
        Surface(
            onClick = { onSongChange("slow") },
            modifier = Modifier
                .fillMaxWidth(0.9f),
            color = Color(0xff302e2f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier.padding(vertical = 8.dp), // 필요에 따라 조절
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentSong == "slow") "✓ 잔잔한 음악" else "잔잔한 음악",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
         */
        Button(
            onClick = {
                onSongChange("slow")
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF302E2F),
                disabledContainerColor = Color(0xFF302E2F),
                disabledContentColor = Color.DarkGray,
            ),
            shape = RoundedCornerShape(10.dp),
            enabled = !processing

        ) {
            Text(
                if (currentSong == "slow") "✓ 잔잔한 음악" else "잔잔한 음악",
                fontSize = 18.sp
            )
        }
//        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                onSongChange("fast")
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF302E2F),
                disabledContainerColor = Color(0xFF302E2F),
                disabledContentColor = Color.DarkGray,
            ),
            shape = RoundedCornerShape(10.dp),
            enabled = !processing
        ) {
            Text(
                if (currentSong == "fast") "✓ 통성기도 음악" else "통성기도 음악",
                fontSize = 18.sp
            )
        }
        /*
        Surface(
            onClick = { onSongChange("fast") },
            modifier = Modifier
                .fillMaxWidth(0.9f),
            color = Color(0xff302e2f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier.padding(vertical = 8.dp), // 필요에 따라 조절
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentSong == "fast") "✓ 통성기도 음악" else "통성기도 음악",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
         */
    }
}

@Composable
fun VolumeAndPlayback(volume: Int, state: Int, onStateChange: () -> Unit, processing: Boolean) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = volume.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = {
                onStateChange()
            },
            modifier = Modifier
                .padding(0.dp)
                .size(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF302E2F),
                disabledContainerColor = Color(0xFF302E2F),
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            enabled = !processing
        ) {
            Icon(
                imageVector = if (state == 1) Icons.Default.Pause else Icons.Default.PlayArrow, // TODO
                contentDescription = if (state == 1) "Pause" else "Play",
                modifier = Modifier.size(60.dp),
                tint = if (processing) Color.Gray else Color.White
            )
        }
    }
}

@Composable
fun Footer(onRefreshClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 2.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("사랑의빛교회 기도음악 재생", color = Color.White, fontSize = 15.sp)
//            Text("v0.1.0 | ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}", color = Color.White, fontSize = 15.sp)
        }
//        Column {
//            Text("Last communication: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}", color = Color.White, fontSize = 12.sp)
//        }
        Column {
            Text("v0.1.0", color = Color.White, fontSize = 15.sp)
        }
//        Button(
//            onClick = onRefreshClick,
//            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF302E2F)),
//            shape = RoundedCornerShape(10.dp),
//        ) {
//            Text("새로고침")
//        }
    }
}