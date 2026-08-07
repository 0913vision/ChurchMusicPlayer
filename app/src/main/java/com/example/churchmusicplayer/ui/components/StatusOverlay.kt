package com.example.churchmusicplayer.ui.components

import android.animation.ValueAnimator
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.churchmusicplayer.data.FlowStatus
import com.example.churchmusicplayer.data.Helpline
import com.example.churchmusicplayer.ui.Layout

/**
 * Whether the panel is being held on purpose or has broken.
 *
 * These are opposites and the person at the panel should not have to read to
 * find out which one they are looking at: a held panel comes back by itself, a
 * broken one needs someone to do something. The colour of the edge says which
 * before any word is read.
 */
enum class OverlayTone { HELD, FAULT }

/** Two lines: what is going on, and when the panel comes back. */
data class OverlayNotice(
    val tone: OverlayTone,
    val headline: String,
    val note: String,
    val action: OverlayAction? = null,
)

/** The one thing pressing would actually help, when there is one. */
data class OverlayAction(val label: String, val onClick: () -> Unit)

// The shutter's own colours. Nothing new is introduced: amber and red are the
// two lamps the connection bar already uses.
private val SCRIM = Color(0xFF15100F)
private val BAND = Color(0xFF232021)
private val HELD_EDGE = Color(0xFFFFC107)
private val FAULT_EDGE = Color(0xFFF44336)

// Text on the band, three levels and no more: the headline, the sentence that
// matters, and everything standing. The lower two are set where they clear
// 4.5:1 against the band — this panel is read in a lit room, at arm's length,
// often by someone who is not looking for trouble yet.
private val NOTE = Color.White.copy(alpha = 0.68f)
private val HELP = Color.White.copy(alpha = 0.50f)

/**
 * The connection is gone.
 *
 * Two things are worth trying and the person cannot tell which applies from
 * here, so both are offered: the panel's own Wi-Fi is the usual culprit, and
 * the button above covers the rest. The server's address is the third, rarest
 * cause — and this is the only screen from which anyone would ever suspect it,
 * so that is where it can be corrected.
 */
fun disconnectedNotice(onAddress: () -> Unit): OverlayNotice = OverlayNotice(
    tone = OverlayTone.FAULT,
    headline = "연결이 끊겼어요",
    note = "와이파이가 켜져 있는지 확인해 주세요.\n위쪽 [다시 연결하기]를 눌러도 좋아요.",
    action = OverlayAction("서버 주소", onAddress),
)

/**
 * The server refused this version. Reconnecting cannot help, so the notice
 * carries the one act that can: the download link the media server relays.
 */
fun outdatedNotice(onDownload: () -> Unit): OverlayNotice = OverlayNotice(
    tone = OverlayTone.FAULT,
    headline = "앱이 오래됐어요",
    note = "새 버전을 받아 설치해 주세요.",
    action = OverlayAction("새 버전 받기", onDownload),
)

/**
 * The admin gate is closed, said in the only terms that mean anything here.
 *
 * The server distinguishes a scheduled run from a hand-closed gate, and within
 * a run it distinguishes waiting from playing from holding. None of that is a
 * distinction the person at the panel can act on, or has a word for: from
 * where they stand there is one situation, which is that 방송실 has the music
 * and this tablet does not. So every held case says exactly that.
 *
 * What does change is whether the server knows when the gate opens. Holding
 * carries that time and says it. Everything else — a hand-closed gate, a run
 * still waiting, a run playing music whose end is not the gate's end — has no
 * time that would be true, so it promises nothing more than "when it is over".
 */
fun lockedNotice(flow: FlowStatus, onDownload: () -> Unit): OverlayNotice {
    val booth = { note: String -> OverlayNotice(OverlayTone.HELD, "방송실에서 사용 중이에요", note) }
    return when (flow) {
        is FlowStatus.Holding -> booth("${clockOf(flow.unlockAt)}에 다시 사용할 수 있어요.")
        FlowStatus.Idle, is FlowStatus.Waiting, is FlowStatus.Playing ->
            booth("끝나면 다시 사용할 수 있어요.")
        // A phase this build does not know means the server is newer than the
        // app — the same situation a refused handshake names, shown the same way.
        FlowStatus.Unknown -> outdatedNotice(onDownload)
    }
}

/**
 * Covers the controls and swallows touches, so nothing can be half-pressed.
 *
 * A full-width band rather than a card in the middle of the screen: this
 * layout is already built from bands — the connection bar and the footer — so
 * a shutter across the middle belongs to it, where a floating panel would not.
 * Its text starts on the same left margin as everything underneath it.
 */
@Composable
fun BoxScope.StatusOverlay(notice: OverlayNotice, helpline: Helpline) {
    val edge = if (notice.tone == OverlayTone.HELD) HELD_EDGE else FAULT_EDGE

    // The one movement: the shutter drops into place. Short enough to read as
    // the panel closing rather than as an effect.
    val appear = remember { Animatable(if (animationsEnabled()) 0f else 1f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }

    Box(
        modifier = Modifier
            .matchParentSize()
            // Not quite opaque: the controls stay faintly visible underneath,
            // so the panel reads as unavailable rather than gone.
            .background(SCRIM.copy(alpha = 0.93f * appear.value))
            .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = appear.value
                    translationY = -(1f - appear.value) * Layout.overlaySlide.toPx()
                },
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Layout.overlayEdge)
                    .background(edge.copy(alpha = 0.85f)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BAND)
                    .padding(
                        horizontal = Layout.screenPaddingH,
                        vertical = Layout.overlayBandPaddingV,
                    ),
            ) {
                Headline(notice.headline)
                Spacer(Modifier.height(Layout.overlayNoteGap))
                Text(
                    notice.note,
                    color = NOTE,
                    fontSize = Layout.overlayNote,
                    lineHeight = Layout.overlayNoteLineHeight,
                )
                Helpline(helpline)
            }
            notice.action?.let { action -> ActionRow(action) }
        }
    }
}

/**
 * The action as the band's own bottom row: the label starts on the same left
 * margin as the text above, and the surface runs the full width — nothing
 * floats, and the chevron at the far edge says "press to go".
 */
@Composable
private fun ActionRow(action: OverlayAction) {
    Column(Modifier.fillMaxWidth().background(BAND)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.045f))
                .clickable(onClick = action.onClick)
                .padding(horizontal = Layout.screenPaddingH, vertical = Layout.overlayActionPaddingV),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(action.label, color = Color.White, fontSize = Layout.overlayNote, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("›", color = Color.White.copy(alpha = 0.5f), fontSize = Layout.overlayNote * 1.2)
        }
    }
}

/**
 * Standing information, not about right now — hence the quieter voice and the
 * icon marking it off. It is on every notice, held or broken: the moment
 * someone needs a number is the moment they are least able to go looking for
 * one. It is absent only before the handshake, when nobody has been named.
 */
@Composable
private fun Helpline(helpline: Helpline) {
    Spacer(Modifier.height(Layout.overlayHelpGap))
    // The icon rides the first line's row, so it centers on that line exactly
    // instead of chasing it with a hand-tuned offset.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = HELP,
            modifier = Modifier.size(Layout.overlayHelpIcon),
        )
        Spacer(Modifier.width(Layout.overlayHelpIconGap))
        HelpLine("문제가 발생했나요?", FontWeight.Normal)
    }
    // The number takes weight rather than a fourth colour: the scale
    // stays three deep and the data still reads first. A device that has never
    // reached the server has no number to print, and an empty line where one
    // belongs reads worse than saying where to go.
    HelpLine(
        if (helpline is Helpline.Known) "${helpline.name} ${helpline.phone}" else "방송실에 알려 주세요",
        FontWeight.SemiBold,
        Modifier.padding(start = Layout.overlayHelpIcon + Layout.overlayHelpIconGap),
    )
}

@Composable
private fun HelpLine(text: String, weight: FontWeight, modifier: Modifier = Modifier) {
    Text(
        text,
        color = HELP,
        fontSize = Layout.overlayHelp,
        lineHeight = Layout.overlayHelpLineHeight,
        fontWeight = weight,
        modifier = modifier,
    )
}

/**
 * The headline, kept to two lines.
 *
 * Korean wraps at any character, so a headline left to itself can split
 * mid-word. Two lines are allowed before the size gives way, which keeps the
 * longest of these whole on a narrow phone without shrinking the rest to match.
 */
@Composable
private fun Headline(text: String) {
    var size by remember(text) { mutableStateOf(Layout.overlayHeadline) }
    Text(
        text,
        color = Color.White,
        fontSize = size,
        lineHeight = Layout.overlayHeadlineLineHeight,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp,
        maxLines = 2,
        onTextLayout = { result -> if (result.hasVisualOverflow) size *= 0.92f },
    )
}

/**
 * The wall-clock time of an instant, for reading.
 *
 * The server sends absolute instants rather than clock times, because it will
 * not guess which day a bare "21:30" belongs to. Turning one back into
 * something to read is the client's job, and it is done in the phone's own
 * timezone. An instant this build cannot parse is shown as it arrived: wrong
 * and visible beats quietly plausible.
 *
 * SimpleDateFormat rather than java.time — this app still runs on API 21.
 */
private val WIRE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
private val SHOWN_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun clockOf(instant: String): String =
    runCatching { SHOWN_FORMAT.format(WIRE_FORMAT.parse(instant)!!) }.getOrDefault(instant)

/** Honours the system setting that turns animations off. */
private fun animationsEnabled(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
