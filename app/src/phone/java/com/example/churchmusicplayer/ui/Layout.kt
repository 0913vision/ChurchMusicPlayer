package com.example.churchmusicplayer.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sizes for the phone build.
 *
 * Only absolute values live here. Everything proportional — the left/right
 * split, the record filling its box — is expressed as weights in the screen
 * itself and needs no per-device number, which is why the record has no size:
 * it takes the room its parent gives it and therefore looks right on both.
 *
 * The tablet build has a file of the same shape with larger values. Screen code
 * reads Layout.x and never a literal, so tuning one device cannot disturb the
 * other.
 */
object Layout {
    // Screen frame
    val screenPaddingH = 30.dp
    val screenPaddingV = 50.dp

    // Connection status bar
    val statusBarHeight = 50.dp
    val statusBarPaddingH = 20.dp
    val statusDotSize = 14.dp
    val statusText = 20.sp
    val reconnectText = 18.sp

    // Song selection
    val songButtonPaddingV = 6.dp
    val songButtonText = 18.sp

    // Console buttons (microphone / music)
    val consoleButtonText = 20.sp
    val consoleButtonGap = 20.dp
    val consoleButtonPaddingV = 4.dp

    // Volume readout and transport
    val volumeText = 110.sp
    val transportGap = 30.dp
    val playButtonSize = 80.dp
    val playIconSize = 60.dp

    // Blocking status band. The headline is the largest thing on it because it
    // carries the one fact the operator does not already have — usually the
    // name of the order that is running.
    val overlayEdge = 2.dp
    val overlayBandPaddingV = 28.dp
    val overlaySlide = 12.dp
    val overlayHeadline = 30.sp
    val overlayHeadlineLineHeight = 38.sp
    val overlayNoteGap = 10.dp
    val overlayNote = 17.sp
    val overlayNoteLineHeight = 25.sp
    val overlayHelpGap = 18.dp
    val overlayHelp = 14.sp
    val overlayHelpLineHeight = 20.sp
    val overlayHelpIcon = 17.dp
    val overlayHelpIconGap = 9.dp
    /** Centres the icon on the first line of text: (lineHeight - icon) / 2 */
    val overlayHelpIconTop = 2.dp

    // Footer
    val footerText = 15.sp
}
