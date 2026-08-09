package com.example.churchmusicplayer.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A held press over a row of buttons, resolved by how many fingers are on it.
 *
 * One detector for the whole row rather than one per button. Multi-touch is a
 * property of the surface, not of any control on it: two detectors would each
 * see half the gesture, start their own clock, and have to negotiate — and
 * whichever finger landed first would decide the outcome. Here there is a
 * single clock, started by the first finger, and one decision taken when it
 * runs out.
 *
 * Nothing is consumed. The buttons underneath keep their own ordinary presses;
 * this only watches, on the Initial pass, which is also the only way it can see
 * a press on a button that is disabled.
 *
 * @param bounds where each id is on screen, so a finger can be told which
 *   button it is on. Ids absent from the map are not part of the gesture.
 * @param holdMs how long the fingers must stay down.
 * @param graceMs how late a second finger may arrive and still count as "at the
 *   same time" rather than as its own press.
 * @param onHold the ids under the fingers when the hold completed — one id for
 *   a single press, more for a simultaneous one.
 */
fun Modifier.consoleHold(
    bounds: Map<String, Rect>,
    holdMs: Long,
    graceMs: Long,
    onHold: (Set<String>) -> Unit,
): Modifier = this.pointerInput(bounds, holdMs, graceMs) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val startedAt = first.uptimeMillis
        val touched = mutableSetOf<String>()
        bounds.idAt(first.position)?.let(touched::add)

        // Every pointer that arrives in time joins the gesture; the set is what
        // the hold is judged on, so a finger that lands late is not part of it.
        val held = withTimeoutOrNull(holdMs) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.filter { it.pressed }
                if (down.isEmpty()) return@withTimeoutOrNull false
                for (change in down) {
                    if (change.uptimeMillis - startedAt <= graceMs) {
                        bounds.idAt(change.position)?.let(touched::add)
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE") false
        }

        // Timing out is the success case: the fingers never left.
        if (held == null && touched.isNotEmpty()) onHold(touched.toSet())

        // Let the gesture finish so the next one starts clean.
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.none { it.pressed }) break
        }
    }
}

private fun Map<String, Rect>.idAt(position: Offset): String? =
    entries.firstOrNull { it.value.contains(position) }?.key
