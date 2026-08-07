package com.example.churchmusicplayer.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The app's own zoom. Type follows it through the density's fontScale; the few
 * controls that should grow with the type — the record, the fader — read it
 * from here and multiply their own sizes.
 */
val LocalUiScale = compositionLocalOf { 1f }

/**
 * Sizes chosen by window width, not by build variant.
 *
 * Only absolute values live here. Everything proportional — the left/right
 * split, the record filling its box — is expressed as weights in the screen
 * itself. The screen reads Layout.x and never a literal; MainScreen calls
 * forWidth() once per composition, so rotation and split-screen re-pick the
 * spec. Wide screens also cap the content column: past a point more width
 * makes controls uglier, not bigger.
 */
data class LayoutSpec(
    val screenPaddingH: Dp,
    val screenPaddingV: Dp,
    val contentMaxWidth: Dp,
    val statusBarHeight: Dp,
    val statusBarPaddingH: Dp,
    val statusDotSize: Dp,
    val statusText: TextUnit,
    val reconnectText: TextUnit,
    val songButtonPaddingV: Dp,
    val songButtonText: TextUnit,
    val faderTrackWidth: Dp,
    val faderThumbWidth: Dp,
    val faderThumbHeight: Dp,
    /** Air above and below the travel, so the ends do not read as broken */
    val faderPaddingV: Dp,
    /** The transport never gives up more than this, whatever grows below it */
    val faderMinHeight: Dp,
    val consoleButtonText: TextUnit,
    val consoleButtonGap: Dp,
    val consoleButtonPaddingV: Dp,
    val volumeText: TextUnit,
    val transportGap: Dp,
    val playButtonSize: Dp,
    val playIconSize: Dp,
    val overlayEdge: Dp,
    val overlayBandPaddingV: Dp,
    val overlaySlide: Dp,
    val overlayHeadline: TextUnit,
    val overlayHeadlineLineHeight: TextUnit,
    val overlayNoteGap: Dp,
    val overlayNote: TextUnit,
    val overlayNoteLineHeight: TextUnit,
    val overlayHelpGap: Dp,
    val overlayHelp: TextUnit,
    val overlayHelpLineHeight: TextUnit,
    val overlayHelpIcon: Dp,
    val overlayHelpIconGap: Dp,
    val overlayActionPaddingV: Dp,
    val footerText: TextUnit,
)

private val COMPACT = LayoutSpec(
    screenPaddingH = 30.dp,
    screenPaddingV = 50.dp,
    contentMaxWidth = 10_000.dp,
    statusBarHeight = 50.dp,
    statusBarPaddingH = 20.dp,
    statusDotSize = 14.dp,
    statusText = 20.sp,
    reconnectText = 18.sp,
    songButtonPaddingV = 6.dp,
    songButtonText = 18.sp,
    faderTrackWidth = 15.dp,
    faderThumbWidth = 90.dp,
    faderThumbHeight = 60.dp,
    faderPaddingV = 18.dp,
    faderMinHeight = 260.dp,
    consoleButtonText = 20.sp,
    consoleButtonGap = 20.dp,
    consoleButtonPaddingV = 4.dp,
    volumeText = 110.sp,
    transportGap = 30.dp,
    playButtonSize = 80.dp,
    playIconSize = 60.dp,
    overlayEdge = 2.dp,
    overlayBandPaddingV = 28.dp,
    overlaySlide = 12.dp,
    overlayHeadline = 30.sp,
    overlayHeadlineLineHeight = 38.sp,
    overlayNoteGap = 10.dp,
    overlayNote = 17.sp,
    overlayNoteLineHeight = 25.sp,
    overlayHelpGap = 18.dp,
    overlayHelp = 16.sp,
    overlayHelpLineHeight = 23.sp,
    overlayHelpIcon = 19.dp,
    overlayHelpIconGap = 9.dp,
    overlayActionPaddingV = 15.dp,
    footerText = 15.sp,
)

private val MEDIUM = LayoutSpec(
    screenPaddingH = 48.dp,
    screenPaddingV = 64.dp,
    contentMaxWidth = 760.dp,
    statusBarHeight = 70.dp,
    statusBarPaddingH = 28.dp,
    statusDotSize = 20.dp,
    statusText = 30.sp,
    reconnectText = 26.sp,
    songButtonPaddingV = 10.dp,
    songButtonText = 30.sp,
    faderTrackWidth = 24.dp,
    faderThumbWidth = 130.dp,
    faderThumbHeight = 88.dp,
    faderPaddingV = 26.dp,
    faderMinHeight = 380.dp,
    consoleButtonText = 30.sp,
    consoleButtonGap = 28.dp,
    consoleButtonPaddingV = 10.dp,
    volumeText = 170.sp,
    transportGap = 44.dp,
    playButtonSize = 130.dp,
    playIconSize = 96.dp,
    overlayEdge = 3.dp,
    overlayBandPaddingV = 44.dp,
    overlaySlide = 18.dp,
    overlayHeadline = 48.sp,
    overlayHeadlineLineHeight = 60.sp,
    overlayNoteGap = 16.dp,
    overlayNote = 26.sp,
    overlayNoteLineHeight = 38.sp,
    overlayHelpGap = 28.dp,
    overlayHelp = 22.sp,
    overlayHelpLineHeight = 32.sp,
    overlayHelpIcon = 26.dp,
    overlayHelpIconGap = 13.dp,
    overlayActionPaddingV = 22.dp,
    footerText = 22.sp,
)

private val EXPANDED = LayoutSpec(
    screenPaddingH = 40.dp,
    screenPaddingV = 60.dp,
    contentMaxWidth = 900.dp,
    statusBarHeight = 68.dp,
    statusBarPaddingH = 28.dp,
    statusDotSize = 18.dp,
    statusText = 28.sp,
    reconnectText = 24.sp,
    songButtonPaddingV = 12.dp,
    songButtonText = 34.sp,
    faderTrackWidth = 28.dp,
    faderThumbWidth = 150.dp,
    faderThumbHeight = 100.dp,
    faderPaddingV = 28.dp,
    faderMinHeight = 420.dp,
    consoleButtonText = 34.sp,
    consoleButtonGap = 28.dp,
    consoleButtonPaddingV = 8.dp,
    volumeText = 190.sp,
    transportGap = 48.dp,
    playButtonSize = 150.dp,
    playIconSize = 110.dp,
    overlayEdge = 3.dp,
    overlayBandPaddingV = 40.dp,
    overlaySlide = 16.dp,
    overlayHeadline = 44.sp,
    overlayHeadlineLineHeight = 56.sp,
    overlayNoteGap = 14.dp,
    overlayNote = 24.sp,
    overlayNoteLineHeight = 34.sp,
    overlayHelpGap = 26.dp,
    overlayHelp = 22.sp,
    overlayHelpLineHeight = 32.sp,
    overlayHelpIcon = 26.dp,
    overlayHelpIconGap = 13.dp,
    overlayActionPaddingV = 20.dp,
    footerText = 20.sp,
)

object Layout {
    private var spec = COMPACT

    /** Standard breakpoints: compact under 600dp, expanded from 840dp. */
    fun forWidth(widthDp: Int) {
        spec = when {
            widthDp >= 840 -> EXPANDED
            widthDp >= 600 -> MEDIUM
            else -> COMPACT
        }
    }

    val screenPaddingH get() = spec.screenPaddingH
    val screenPaddingV get() = spec.screenPaddingV
    val contentMaxWidth get() = spec.contentMaxWidth
    val statusBarHeight get() = spec.statusBarHeight
    val statusBarPaddingH get() = spec.statusBarPaddingH
    val statusDotSize get() = spec.statusDotSize
    val statusText get() = spec.statusText
    val reconnectText get() = spec.reconnectText
    val songButtonPaddingV get() = spec.songButtonPaddingV
    val songButtonText get() = spec.songButtonText
    val faderTrackWidth get() = spec.faderTrackWidth
    val faderThumbWidth get() = spec.faderThumbWidth
    val faderThumbHeight get() = spec.faderThumbHeight
    val faderPaddingV get() = spec.faderPaddingV
    val faderMinHeight get() = spec.faderMinHeight
    val consoleButtonText get() = spec.consoleButtonText
    val consoleButtonGap get() = spec.consoleButtonGap
    val consoleButtonPaddingV get() = spec.consoleButtonPaddingV
    val volumeText get() = spec.volumeText
    val transportGap get() = spec.transportGap
    val playButtonSize get() = spec.playButtonSize
    val playIconSize get() = spec.playIconSize
    val overlayEdge get() = spec.overlayEdge
    val overlayBandPaddingV get() = spec.overlayBandPaddingV
    val overlaySlide get() = spec.overlaySlide
    val overlayHeadline get() = spec.overlayHeadline
    val overlayHeadlineLineHeight get() = spec.overlayHeadlineLineHeight
    val overlayNoteGap get() = spec.overlayNoteGap
    val overlayNote get() = spec.overlayNote
    val overlayNoteLineHeight get() = spec.overlayNoteLineHeight
    val overlayHelpGap get() = spec.overlayHelpGap
    val overlayHelp get() = spec.overlayHelp
    val overlayHelpLineHeight get() = spec.overlayHelpLineHeight
    val overlayHelpIcon get() = spec.overlayHelpIcon
    val overlayHelpIconGap get() = spec.overlayHelpIconGap
    val overlayActionPaddingV get() = spec.overlayActionPaddingV
    val footerText get() = spec.footerText
}
