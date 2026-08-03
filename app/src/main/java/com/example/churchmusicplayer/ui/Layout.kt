package com.example.churchmusicplayer.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val overlayHelpIconTop: Dp,
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
    overlayHelp = 14.sp,
    overlayHelpLineHeight = 20.sp,
    overlayHelpIcon = 17.dp,
    overlayHelpIconGap = 9.dp,
    overlayHelpIconTop = 2.dp,
    footerText = 15.sp,
)

private val MEDIUM = LayoutSpec(
    screenPaddingH = 36.dp,
    screenPaddingV = 56.dp,
    contentMaxWidth = 700.dp,
    statusBarHeight = 60.dp,
    statusBarPaddingH = 24.dp,
    statusDotSize = 16.dp,
    statusText = 24.sp,
    reconnectText = 21.sp,
    songButtonPaddingV = 7.dp,
    songButtonText = 22.sp,
    consoleButtonText = 24.sp,
    consoleButtonGap = 24.dp,
    consoleButtonPaddingV = 6.dp,
    volumeText = 130.sp,
    transportGap = 36.dp,
    playButtonSize = 96.dp,
    playIconSize = 72.dp,
    overlayEdge = 2.dp,
    overlayBandPaddingV = 34.dp,
    overlaySlide = 14.dp,
    overlayHeadline = 36.sp,
    overlayHeadlineLineHeight = 46.sp,
    overlayNoteGap = 12.dp,
    overlayNote = 20.sp,
    overlayNoteLineHeight = 30.sp,
    overlayHelpGap = 22.dp,
    overlayHelp = 17.sp,
    overlayHelpLineHeight = 25.sp,
    overlayHelpIcon = 20.dp,
    overlayHelpIconGap = 11.dp,
    overlayHelpIconTop = 2.dp,
    footerText = 17.sp,
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
    songButtonPaddingV = 8.dp,
    songButtonText = 26.sp,
    consoleButtonText = 28.sp,
    consoleButtonGap = 28.dp,
    consoleButtonPaddingV = 8.dp,
    volumeText = 150.sp,
    transportGap = 40.dp,
    playButtonSize = 110.dp,
    playIconSize = 82.dp,
    overlayEdge = 3.dp,
    overlayBandPaddingV = 40.dp,
    overlaySlide = 16.dp,
    overlayHeadline = 44.sp,
    overlayHeadlineLineHeight = 56.sp,
    overlayNoteGap = 14.dp,
    overlayNote = 24.sp,
    overlayNoteLineHeight = 34.sp,
    overlayHelpGap = 26.dp,
    overlayHelp = 20.sp,
    overlayHelpLineHeight = 29.sp,
    overlayHelpIcon = 24.dp,
    overlayHelpIconGap = 13.dp,
    overlayHelpIconTop = 3.dp,
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
    val overlayHelpIconTop get() = spec.overlayHelpIconTop
    val footerText get() = spec.footerText
}
