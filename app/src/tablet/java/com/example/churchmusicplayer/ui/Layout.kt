package com.example.churchmusicplayer.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sizes for the tablet build.
 *
 * Same shape as the phone file, larger numbers. A tablet is further from the
 * operator and has more room, so absolute values are scaled up while every
 * proportion stays identical — the layout is the same layout, just read from
 * further away.
 *
 * Note(yoochan.kim): these are a starting point at roughly 1.4x, not measured
 * against the actual device. Tune them here; nothing else needs to change.
 */
object Layout {
    // Screen frame
    val screenPaddingH = 40.dp
    val screenPaddingV = 60.dp

    // Connection status bar
    val statusBarHeight = 68.dp
    val statusBarPaddingH = 28.dp
    val statusDotSize = 18.dp
    val statusText = 28.sp
    val reconnectText = 24.sp

    // Song selection
    val songButtonPaddingV = 20.dp
    val songButtonText = 26.sp

    // Console buttons (microphone / music)
    val consoleButtonText = 28.sp
    val consoleButtonGap = 28.dp
    val consoleButtonPaddingV = 8.dp

    // Volume readout and transport
    val volumeText = 150.sp
    val transportGap = 40.dp
    val playButtonSize = 110.dp
    val playIconSize = 82.dp

    // Blocking status band. Read from further away, so every step of the scale
    // grows with the rest of the screen.
    val overlayEdge = 3.dp
    val overlayBandPaddingV = 40.dp
    val overlaySlide = 16.dp
    val overlayHeadline = 44.sp
    val overlayHeadlineLineHeight = 56.sp
    val overlayNoteGap = 14.dp
    val overlayNote = 24.sp
    val overlayNoteLineHeight = 34.sp
    val overlayHelpGap = 26.dp
    val overlayHelp = 20.sp
    val overlayHelpLineHeight = 29.sp
    val overlayHelpIcon = 24.dp
    val overlayHelpIconGap = 13.dp
    /** Centres the icon on the first line of text: (lineHeight - icon) / 2 */
    val overlayHelpIconTop = 3.dp

    // Footer
    val footerText = 20.sp
}
