package com.example.churchmusicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.churchmusicplayer.data.ConnectionStatus
import com.example.churchmusicplayer.data.Rejection
import com.example.churchmusicplayer.data.SongChoice
import com.example.churchmusicplayer.ui.Layout
import com.example.churchmusicplayer.ui.components.Fader
import com.example.churchmusicplayer.ui.components.StatusOverlay
import com.example.churchmusicplayer.ui.components.disconnectedNotice
import com.example.churchmusicplayer.ui.components.lockedNotice
import kotlinx.coroutines.delay

private const val REJECTION_VISIBLE_MS = 4_000L
private const val BUTTON_COOLDOWN_MS = 1_000L

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
        // Kiosk is the tablet build's job: it is mounted in one place and must
        // not be navigated away from. The phone build is an ordinary app.
        if (BuildConfig.KIOSK) {
            startLockTask()
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val volume by viewModel.volume.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val songChoices by viewModel.songChoices.collectAsState()
    val songIsLive by viewModel.songIsLive.collectAsState()
    val canPlay by viewModel.canPlay.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val processing by viewModel.processing.collectAsState()
    val adminLocked by viewModel.adminLocked.collectAsState()
    val flow by viewModel.flow.collectAsState()
    val helpline by viewModel.helpline.collectAsState()
    val rejection by viewModel.rejection.collectAsState()
    val micOn by viewModel.micOn.collectAsState()
    val auxOn by viewModel.auxOn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1718))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
        ) {
            ConnectionStatusBar(
                status = connectionStatus,
                onReconnectClick = { viewModel.reconnect() },
                processing = processing,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MainContent(
                volume = volume,
                isPlaying = isPlaying,
                currentSong = currentSong,
                songChoices = songChoices,
                songIsLive = songIsLive,
                canPlay = canPlay,
                onVolumeChange = { viewModel.changeVolume(it) },
                onPlaybackToggle = { viewModel.togglePlayback() },
                onSongChange = { viewModel.changeSong(it) },
                processing = processing,
                micOn = micOn,
                auxOn = auxOn,
                onMicrophone = { viewModel.enableMicrophone() },
                onAux = { viewModel.enableAux() },
            )

            // Two things take the whole screen away, and for the same reason:
            // nothing the operator does here would be accepted. Losing the
            // connection is one; an admin closing the gate is the other, and
            // before v1 the app could not tell that second one apart from
            // everything simply not working.
            val disconnected = connectionStatus !is ConnectionStatus.Connected &&
                connectionStatus !is ConnectionStatus.GracePeriod
            when {
                disconnected -> StatusOverlay(disconnectedNotice(), helpline)
                adminLocked -> StatusOverlay(lockedNotice(flow), helpline)
            }
        }

        Footer()
    }

    RejectionNotice(rejection = rejection, onDismiss = { viewModel.dismissRejection() })
}

/**
 * A refused write, explained and then let go.
 *
 * Before v1 a refusal was indistinguishable from the app doing nothing, so the
 * operator had no way to tell "the device is busy" from "this is broken".
 */
@Composable
fun RejectionNotice(rejection: Rejection?, onDismiss: () -> Unit) {
    if (rejection == null) return

    LaunchedEffect(rejection.at) {
        delay(REJECTION_VISIBLE_MS)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color(0xFF3B0404),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                rejection.reason.message,
                color = Color.White,
                fontSize = Layout.reconnectText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
fun MainContent(
    volume: Int,
    isPlaying: Boolean,
    currentSong: String,
    songChoices: List<SongChoice>,
    songIsLive: Boolean,
    canPlay: Boolean,
    onVolumeChange: (Int) -> Unit,
    onPlaybackToggle: () -> Unit,
    onSongChange: (String) -> Unit,
    processing: Boolean,
    micOn: Boolean,
    auxOn: Boolean,
    onMicrophone: () -> Unit,
    onAux: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = Layout.screenPaddingH,
            vertical = Layout.screenPaddingV,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // The record is given no size of its own: it fills the box it is
            // handed, which is what makes the same layout sit correctly on a
            // phone and on a tablet.
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                RecordVisualization(isPlaying = isPlaying, processing = processing)
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SongSelection(
                    currentSong = currentSong,
                    choices = songChoices,
                    songIsLive = songIsLive,
                    onSongChange = onSongChange,
                    processing = processing,
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Fader(
                    volume = volume,
                    onVolumeChange = onVolumeChange,
                    processing = processing,
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                VolumeAndPlayback(
                    volume = volume,
                    isPlaying = isPlaying,
                    onPlaybackToggle = onPlaybackToggle,
                    processing = processing,
                    canPlay = canPlay,
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ToggleConsoleButton(micOn = micOn, auxOn = auxOn, onMicrophone = onMicrophone, onAux = onAux)
        }
    }
}

@Composable
fun ToggleConsoleButton(micOn: Boolean, auxOn: Boolean, onMicrophone: () -> Unit, onAux: () -> Unit) {
    var resting by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        ConsoleButton(
            label = if (micOn) "마이크 켜져 있음" else "마이크 켜기",
            enabled = !resting && !micOn,
            modifier = Modifier.weight(0.45f),
        ) {
            onMicrophone()
            resting = true
        }
        Spacer(modifier = Modifier.width(Layout.consoleButtonGap))
        ConsoleButton(
            label = if (auxOn) "노래 켜져 있음" else "노래 켜기",
            enabled = !resting && !auxOn,
            modifier = Modifier.weight(0.45f),
        ) {
            onAux()
            resting = true
        }
    }

    // Note(yoochan.kim): the desk's answer takes a poll to arrive, so a short
    // rest covers the gap before 켜져 있음 lands.
    if (resting) {
        LaunchedEffect(Unit) {
            delay(BUTTON_COOLDOWN_MS)
            resting = false
        }
    }
}

@Composable
private fun ConsoleButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.padding(vertical = Layout.consoleButtonPaddingV),
        onClick = { if (enabled) onClick() },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3B0404),
            disabledContainerColor = Color(0xFF302E2F),
            disabledContentColor = Color.DarkGray,
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp),
        enabled = enabled
    ) {
        Text(label, fontSize = Layout.consoleButtonText)
    }
}

@Composable
fun ConnectionStatusBar(status: ConnectionStatus, onReconnectClick: () -> Unit, processing: Boolean) {
    var isButtonEnabled by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Layout.statusBarHeight)
            .background(Color(0xff3B3A3A))
            .padding(horizontal = Layout.statusBarPaddingH),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(Layout.statusDotSize)
                    .background(
                        when (status) {
                            is ConnectionStatus.Connected, is ConnectionStatus.GracePeriod -> Color(0xFF4CAF50)
                            is ConnectionStatus.Connecting -> Color(0xFFFFC107)
                            is ConnectionStatus.Disconnected, is ConnectionStatus.Error -> Color(0xFFF44336)
                        },
                        shape = CircleShape
                    )
            )
            Text(
                text = when (status) {
                    is ConnectionStatus.Connected, is ConnectionStatus.GracePeriod ->
                        if (processing) "연결됨 (작업 처리 중)" else "연결됨"
                    is ConnectionStatus.Connecting -> "연결중"
                    is ConnectionStatus.Disconnected -> "연결 끊김"
                    is ConnectionStatus.Error -> "오류: ${status.message}"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = Layout.statusText,
            )
        }

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
            Text("다시 연결하기", fontSize = Layout.reconnectText)
        }

        if (!isButtonEnabled) {
            LaunchedEffect(Unit) {
                delay(BUTTON_COOLDOWN_MS)
                isButtonEnabled = true
            }
        }
    }
}

@Composable
fun RecordVisualization(isPlaying: Boolean, processing: Boolean) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) rotationAngle %= 360f
    }

    LaunchedEffect(isPlaying, processing) {
        var lastUpdateTime = System.currentTimeMillis()
        while (isPlaying || processing) {
            val currentTime = System.currentTimeMillis()
            val elapsed = (currentTime - lastUpdateTime) / 1000f
            // Turning slowly while the device works is the one sign that a
            // fade is under way rather than the app having frozen.
            val speed = if (isPlaying) 45f else 10f
            rotationAngle += speed * elapsed
            lastUpdateTime = currentTime
            delay(33)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.record),
            contentDescription = "Record",
            modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
        )
    }
}

@Composable
fun SongSelection(
    currentSong: String,
    choices: List<SongChoice>,
    songIsLive: Boolean,
    onSongChange: (String) -> Unit,
    processing: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        choices.forEach { choice ->
            Button(
                onClick = { onSongChange(choice.id) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = Layout.songButtonPaddingV),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF302E2F),
                    disabledContainerColor = Color(0xFF302E2F),
                    disabledContentColor = Color.DarkGray,
                ),
                shape = RoundedCornerShape(10.dp),
                // A song the server does not offer cannot be chosen. The button
                // stays put so the screen does not rearrange itself.
                enabled = choice.available && !processing
            ) {
                // The tick means "this is what you are hearing". While a flow
                // plays its own track that is true of neither, so neither gets
                // one.
                val selected = songIsLive && choice.available && currentSong == choice.id
                Text(
                    if (selected) "✓ ${choice.title}" else choice.title,
                    fontSize = Layout.songButtonText
                )
            }
        }
    }
}

@Composable
fun VolumeAndPlayback(
    volume: Int,
    isPlaying: Boolean,
    onPlaybackToggle: () -> Unit,
    processing: Boolean,
    canPlay: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = volume.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = Layout.volumeText),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(Layout.transportGap))
        Button(
            onClick = onPlaybackToggle,
            modifier = Modifier.size(Layout.playButtonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF302E2F),
                disabledContainerColor = Color(0xFF302E2F),
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            // With no song available there is nothing to start, so the
            // transport goes quiet rather than sending a write that fails.
            enabled = canPlay && !processing
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(Layout.playIconSize),
                tint = if (canPlay && !processing) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun Footer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 2.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.app_name), color = Color.White, fontSize = Layout.footerText)
        // Read from the build rather than typed here, which is how the footer
        // came to read v1.2.0 while the package said 1.0.
        Text("v${BuildConfig.VERSION_NAME}", color = Color.White, fontSize = Layout.footerText)
    }
}
