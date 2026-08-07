package com.example.churchmusicplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.churchmusicplayer.data.ConnectionStatus
import com.example.churchmusicplayer.data.Rejection
import com.example.churchmusicplayer.data.ServerAddress
import com.example.churchmusicplayer.data.Song
import com.example.churchmusicplayer.ui.Layout
import com.example.churchmusicplayer.ui.LocalUiScale
import com.example.churchmusicplayer.ui.components.Fader
import com.example.churchmusicplayer.ui.components.StatusOverlay
import com.example.churchmusicplayer.ui.components.disconnectedNotice
import com.example.churchmusicplayer.ui.components.lockedNotice
import com.example.churchmusicplayer.ui.components.outdatedNotice
import kotlinx.coroutines.delay

private const val REJECTION_VISIBLE_MS = 4_000L
private const val BUTTON_COOLDOWN_MS = 1_000L

// Note(yoochan.kim): dialogs are read at the same arm's length as the panel, so
// they take a size of their own rather than the framework's default
private val DIALOG_TITLE = 26.sp
private val DIALOG_BODY = 21.sp

private const val UI_PREFS = "ui"
private const val UI_SCALE_KEY = "scale"

/**
 * A press with no ripple.
 *
 * The framework tints its ripple with the theme's accent, which on an
 * unthemed app is Material purple — a colour nothing else on this panel uses,
 * drawn in a rectangle that has nothing to do with the glyph inside it.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.quietClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

/**
 * A press you can feel: the platform's own key tick, the same one a keyboard
 * gives. Compose's haptic types are advisory and some devices ignore them, so
 * this goes through the view, where the constant is honoured.
 */
@Composable
private fun rememberTap(): () -> Unit {
    val view = LocalView.current
    return { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
}

/** Says one thing at a time: a second press replaces the notice, never queues behind it. */
@Composable
private fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    val holder = remember { arrayOfNulls<Toast>(1) }
    return { message ->
        holder[0]?.cancel()
        holder[0] = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
    }
}

/** White where the framework would use its accent: cursor, handles, selection. */
private val PANEL_SELECTION = TextSelectionColors(
    handleColor = Color.White,
    backgroundColor = Color.White.copy(alpha = 0.3f),
)

private fun loadUiScale(context: Context): Float =
    context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE).getFloat(UI_SCALE_KEY, 1f).coerceIn(0.9f, 1.5f)

private fun saveUiScale(context: Context, value: Float) {
    context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE).edit().putFloat(UI_SCALE_KEY, value).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var uiScale by remember { mutableStateOf(loadUiScale(context)) }
            // Note(yoochan.kim): an equipment panel sizes its own type per
            // screen — the system font scale is capped so no setting breaks the
            // layout. The app's own 배율 scales the type only, so spacing and
            // controls hold still and bigger text costs whitespace, not layout
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density.density,
                    fontScale = density.fontScale.coerceAtMost(1f) * uiScale,
                ),
                LocalUiScale provides uiScale,
            ) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            uiScale = uiScale,
                            onUiScale = { scale ->
                                uiScale = scale
                                saveUiScale(context, scale)
                            },
                        )
                    }
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
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    uiScale: Float = 1f,
    onUiScale: (Float) -> Unit = {},
) {
    // Sizes follow the window, not the build variant.
    Layout.forWidth(LocalConfiguration.current.screenWidthDp)

    var showScale by remember { mutableStateOf(false) }
    var showAddress by remember { mutableStateOf(false) }

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
    val consoleInputs by viewModel.consoleInputs.collectAsState()

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
                onOpenScale = { showScale = true },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Wide screens cap the content column; stretching controls across a
            // whole tablet makes them worse, not bigger.
            Box(
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = Layout.contentMaxWidth)
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
                consoleInputs = consoleInputs,
                onEnableConsole = { viewModel.enableConsoleInput(it) },
            )
            }

            // Two things take the whole screen away, and for the same reason:
            // nothing the operator does here would be accepted. Losing the
            // connection is one; an admin closing the gate is the other, and
            // before v1 the app could not tell that second one apart from
            // everything simply not working.
            val context = LocalContext.current
            // Note(yoochan.kim): the media server relays the APK, so the link
            // is just the address this app already knows plus its own flavor
            val openDownload = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(ServerAddress.of(context) + "apk/" + BuildConfig.FLAVOR))
                )
            }
            val disconnected = connectionStatus !is ConnectionStatus.Connected &&
                connectionStatus !is ConnectionStatus.GracePeriod
            when {
                connectionStatus is ConnectionStatus.Outdated -> StatusOverlay(outdatedNotice(openDownload), helpline)
                // Note(yoochan.kim): the address belongs here, not in settings —
                // a wrong one is only ever discovered from this screen
                disconnected -> StatusOverlay(disconnectedNotice { showAddress = true }, helpline)
                adminLocked -> StatusOverlay(lockedNotice(flow, openDownload), helpline)
            }
        }

        Footer()
    }

    RejectionNotice(rejection = rejection, onDismiss = { viewModel.dismissRejection() })

    if (showScale) {
        SettingsDialog(current = uiScale, onPick = onUiScale, onDismiss = { showScale = false })
    }

    if (showAddress) {
        AddressDialog(
            scale = uiScale,
            onDismiss = { showAddress = false },
            onChanged = { viewModel.reconnect() },
        )
    }
}

/** This device's settings: how big the type is, and nothing else. */
@Composable
private fun SettingsDialog(
    current: Float,
    onPick: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    // Note(yoochan.kim): the top step is for eyes that need it, not a nudge —
    // half again as large as normal
    val options = listOf("글자 작게" to 0.9f, "글자 보통" to 1.0f, "글자 크게" to 1.2f, "글자 아주 크게" to 1.5f)

    PanelDialog(scale = current, title = "설정", onDismiss = onDismiss) {
        options.forEachIndexed { index, (label, value) ->
            if (index > 0) SettingsRule()
            val selected = kotlin.math.abs(current - value) < 0.01f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .quietClickable {
                        onPick(value)
                        onDismiss()
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Note(yoochan.kim): the tick leads the label, like the deck's
                // song buttons
                Text(
                    if (selected) "✓" else "",
                    color = Color.White,
                    fontSize = DIALOG_BODY,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                )
                Text(
                    label,
                    color = if (selected) Color.White else Color(0xFF9E9894),
                    fontSize = DIALOG_BODY,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/**
 * A dialog laid out by hand.
 *
 * The framework's own pads its content by a fixed amount and centres its
 * buttons inside a minimum width, which puts every edge on a different line
 * from the panel behind it. Here the margins are the top bar's, so a dialog
 * reads as part of the same screen.
 */
@Composable
private fun PanelDialog(
    scale: Float,
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        ScaledByApp(scale) {
            Surface(color = Color(0xFF2A2829), shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Layout.statusBarPaddingH,
                        vertical = Layout.statusBarPaddingH,
                    ),
                ) {
                    Text(title, color = Color.White, fontSize = DIALOG_TITLE, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(Layout.statusBarPaddingH))
                    content()
                    Spacer(Modifier.height(Layout.statusBarPaddingH))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        buttons()
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .quietClickable { onDismiss() },
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Text("닫기", color = Color.White, fontSize = DIALOG_BODY, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Nothing here but the address: this is not a place to wander into. */
@Composable
private fun AddressDialog(scale: Float, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    var address by remember { mutableStateOf(ServerAddress.of(context)) }
    val valid = ServerAddress.normalize(address) != null

    PanelDialog(
        scale = scale,
        title = "서버 주소 수정",
        onDismiss = onDismiss,
        buttons = {
            // Note(yoochan.kim): the way back to the address this build shipped
            // with, for when the typing went wrong
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .quietClickable { address = ServerAddress.fromBuild },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("초기화", color = Color(0xFF9E9894), fontSize = DIALOG_BODY)
            }
            Spacer(Modifier.width(28.dp))
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .quietClickable(enabled = valid) {
                        val previous = ServerAddress.of(context)
                        ServerAddress.set(context, address)
                        if (ServerAddress.of(context) != previous) onChanged()
                        onDismiss()
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "저장",
                    color = if (valid) Color.White else Color(0xFF6B6664),
                    fontSize = DIALOG_BODY,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(28.dp))
        },
    ) {
        // Note(yoochan.kim): the cursor and its handles take their colour from
        // here, not from the field's own colours
        CompositionLocalProvider(LocalTextSelectionColors provides PANEL_SELECTION) {
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    isError = !valid,
                    textStyle = LocalTextStyle.current.copy(fontSize = DIALOG_BODY),
                    // Note(yoochan.kim): every accent spelled out — the theme's
                    // default is Material purple, which belongs to no other
                    // pixel on this screen
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF262425),
                        unfocusedContainerColor = Color(0xFF262425),
                        errorContainerColor = Color(0xFF262425),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorTextColor = Color.White,
                        cursorColor = Color.White,
                        errorCursorColor = Color(0xFFE05B5B),
                        // Note(yoochan.kim): the filled box already shows where
                        // the field is; the framework's underline only adds a
                        // second edge
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

@Composable
private fun SettingsRule() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.10f)),
    )
}

// Note(yoochan.kim): a dialog is its own window, whose compose root re-installs
// the system density — so the app's zoom is applied again inside it
@Composable
private fun ScaledByApp(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density.density,
            fontScale = density.fontScale.coerceAtMost(1f) * scale,
        ),
        content = content,
    )
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
            color = Color(0xFF34302F),
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
    songChoices: List<Song>,
    songIsLive: Boolean,
    canPlay: Boolean,
    onVolumeChange: (Int) -> Unit,
    onPlaybackToggle: () -> Unit,
    onSongChange: (String) -> Unit,
    processing: Boolean,
    consoleInputs: List<ConsoleInput>,
    onEnableConsole: (String) -> Unit,
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

        // Note(yoochan.kim): the transport keeps a floor. Left to a bare weight
        // it gives up whatever grows beneath it, and a fader too short to aim
        // at is the one thing on this screen that must not happen.
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = Layout.faderMinHeight)
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
            ToggleConsoleButton(inputs = consoleInputs, onEnable = onEnableConsole)
        }
    }
}

/**
 * A button per console input the server offers, named as the server names it.
 *
 * The list, its length and its labels all belong to the building's wiring, so
 * they arrive with the state rather than living here — rewiring or renaming an
 * input is a server change and never a new build of this app.
 */
@Composable
fun ToggleConsoleButton(inputs: List<ConsoleInput>, onEnable: (String) -> Unit) {
    var resting by remember { mutableStateOf(false) }

    // Note(yoochan.kim): a silent desk is a fault, not a neutral — and a press
    // would never reach it anyway
    if (inputs.isEmpty() || inputs.none { it.known }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Layout.consoleButtonPaddingV)
                .background(Color(0xFF262425), RoundedCornerShape(10.dp))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = Color(0xFF9E9894),
                    modifier = Modifier.size(20.dp),
                )
                Text("음향 장비 응답 없음", color = Color(0xFF9E9894), fontSize = Layout.consoleButtonText)
            }
        }
        return
    }

    // Note(yoochan.kim): two to a row whatever the count, so a third input wraps
    // instead of squeezing the names of the first two. Rows share one gap
    // between them, so wrapping costs the fader as little height as possible.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Layout.consoleButtonPaddingV),
    ) {
        inputs.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Layout.consoleButtonGap),
            ) {
                pair.forEach { input ->
                    ConsoleButton(
                        label = if (input.on) "${input.label} 켜져 있음" else "${input.label} 켜기",
                        enabled = !resting && !input.on,
                        alert = input.known && !input.on,
                        modifier = Modifier.weight(1f),
                    ) {
                        onEnable(input.id)
                        resting = true
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
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
    alert: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tap = rememberTap()
    Button(
        modifier = modifier.padding(vertical = Layout.consoleButtonPaddingV),
        onClick = {
            if (enabled) {
                tap()
                onClick()
            }
        },
        colors = ButtonDefaults.buttonColors(
            // Note(yoochan.kim): red is the desk saying this input is off
            containerColor = if (alert) Color(0xFF3B0404) else Color(0xFF302E2F),
            disabledContainerColor = Color(0xFF262425),
            disabledContentColor = Color.DarkGray,
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp),
        enabled = enabled
    ) {
        WrappingLabel(label, fontSize = Layout.consoleButtonText)
    }
}

// Note(yoochan.kim): zoomed text may no longer fit its button — it wraps at
// spaces only, centered. Word joiners weld each word's characters together,
// because Korean otherwise breaks mid-word and the proper line-break config
// only exists on API 33+.
@Composable
private fun WrappingLabel(
    text: String,
    fontSize: TextUnit,
    align: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier,
) {
    val welded = remember(text) {
        text.split(" ").joinToString(" ") { word -> word.toCharArray().joinToString("⁠") }
    }
    Text(welded, fontSize = fontSize, textAlign = align, modifier = modifier)
}

@Composable
fun ConnectionStatusBar(
    status: ConnectionStatus,
    onReconnectClick: () -> Unit,
    processing: Boolean,
    onOpenScale: () -> Unit,
) {
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
                            is ConnectionStatus.Disconnected, is ConnectionStatus.Error,
                            is ConnectionStatus.Outdated -> Color(0xFFF44336)
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
                    is ConnectionStatus.Outdated -> "버전이 맞지 않아요"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = Layout.statusText,
            )
        }

        // Note(yoochan.kim): 연결중 counts as down — a retry that never lands
        // looks the same as a hang, and waiting it out is not something to
        // force on someone.
        val linkDown = status is ConnectionStatus.Disconnected ||
            status is ConnectionStatus.Error ||
            status is ConnectionStatus.Connecting

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (linkDown) {
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
            // Note(yoochan.kim): the gear never leaves. It is how someone
            // reaches the panel's own settings, and hiding it while the link is
            // down would strand a device exactly when it needs attention. A
            // plain box rather than an icon button, whose 48dp minimum would
            // hold it away from the button beside it.
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(Layout.statusBarHeight)
                    .quietClickable { onOpenScale() },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "설정",
                    tint = Color(0xFF9E9894),
                    modifier = Modifier.size(Layout.statusBarHeight * 0.62f),
                )
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

    val uiScale = LocalUiScale.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.record),
            contentDescription = "Record",
            modifier = Modifier.graphicsLayer {
                rotationZ = rotationAngle
                scaleX = uiScale
                scaleY = uiScale
            }
        )
    }
}

/**
 * A button per song the server offers.
 *
 * The list is the server's, so its length is not known when this is written:
 * the column scrolls rather than assuming a count, which is what lets a song be
 * added to the manifest without anyone installing a new app.
 */
@Composable
fun SongSelection(
    currentSong: String,
    choices: List<Song>,
    songIsLive: Boolean,
    onSongChange: (String) -> Unit,
    processing: Boolean,
) {
    val tap = rememberTap()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        choices.forEach { song ->
            // The tick means "this is what you are hearing". While a flow plays
            // its own track that is true of none of them, so none gets one.
            // Note(yoochan.kim): it keeps a column beside the button rather than
            // inside it, so the name has the button's whole width either way.
            val selected = songIsLive && currentSong == song.id
            Button(
                onClick = {
                    tap()
                    onSongChange(song.id)
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = Layout.songButtonPaddingV),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF302E2F),
                    disabledContainerColor = Color(0xFF302E2F),
                    disabledContentColor = Color.DarkGray,
                ),
                shape = RoundedCornerShape(10.dp),
                // Note(yoochan.kim): the framework's 24dp would hold the tick
                // away from the edge it belongs on
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                enabled = !processing
            ) {
                // Note(yoochan.kim): a button packs its content into the middle,
                // so the row claims the whole width itself — otherwise the tick
                // travels inward with the name instead of sitting at the edge.
                // The tick is always drawn and merely turns invisible, which
                // reserves exactly its own width and no more, at any type size.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "✓",
                        fontSize = Layout.songButtonText,
                        color = if (selected) Color.White else Color.Transparent,
                    )
                    Spacer(Modifier.width(8.dp))
                    // Every name begins at the same place: hard against the left
                    // of what the tick leaves. Centring the block would move
                    // that place whenever a name wrapped to another line.
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        WrappingLabel(
                            song.title,
                            fontSize = Layout.songButtonText,
                            align = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
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
    val tap = rememberTap()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Note(yoochan.kim): already enormous at 보통, so the number follows the
        // zoom at half speed — 1.3 on everything else is ~1.15 here
        val uiScale = LocalUiScale.current
        val damped = (1f + (uiScale - 1f) / 2f) / uiScale
        Text(
            text = volume.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = Layout.volumeText * damped),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(Layout.transportGap))
        Button(
            onClick = {
                tap()
                onPlaybackToggle()
            },
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
