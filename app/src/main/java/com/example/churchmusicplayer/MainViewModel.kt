package com.example.churchmusicplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.churchmusicplayer.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val VOLUME_WRITE_INTERVAL_MS = 80L

@OptIn(FlowPreview::class)
class MainViewModel : ViewModel() {
    private val socketManager = SocketManager()

    val connectionStatus = socketManager.connectionStatus
    val rejection = socketManager.rejection

    private val state = socketManager.state
    private val ready = socketManager.ready

    val volume: StateFlow<Int> = state.mapState { it.optInt(Protocol.Attribute.VOLUME, 0) }
    val isPlaying: StateFlow<Boolean> =
        state.mapState { it.optString(Protocol.Attribute.PLAYBACK) == Protocol.Playback.PLAYING }
    val isMuted: StateFlow<Boolean> =
        state.mapState { it.optString(Protocol.Attribute.MUTE) == Protocol.Mute.MUTED }
    val currentSong: StateFlow<String> = state.mapState { it.optString(Protocol.Attribute.SONG) }

    /** The device is mid-transition, so audio writes will be refused. */
    val processing: StateFlow<Boolean> = state.mapState { it.optBoolean(Protocol.Attribute.AUDIO_LOCK) }

    /** An admin has closed the gate; nothing this app sends will be accepted. */
    val adminLocked: StateFlow<Boolean> = state.mapState { it.optBoolean(Protocol.Attribute.ADMIN_LOCK) }

    /** What the server is running, if anything — the reason a gate is closed. */
    val flow: StateFlow<FlowStatus> = state.mapState { readFlow(it) }

    /**
     * A button per song the server offers, in the order it lists them. The app
     * holds no list of its own, so a song added to the server's manifest shows
     * up here on the next connection without a release.
     */
    val songChoices: StateFlow<List<Song>> = ready.mapState { info -> info?.songs.orEmpty() }

    /** With no song to play, the transport has nothing to do. */
    val canPlay: StateFlow<Boolean> = songChoices.mapState { choices -> choices.isNotEmpty() }

    /**
     * Whether the selected song is what is actually sounding.
     *
     * A flow puts its own track on the deck without touching which song is
     * selected, so during one the selection describes what will come back
     * afterwards — not what is playing now. Ticking it then would name the
     * wrong music.
     */
    val songIsLive: StateFlow<Boolean> = flow.mapState { it !is FlowStatus.Playing }

    /**
     * Who to call when something is wrong, as the server names them. Unknown
     * until the handshake lands — including on the very first connection
     * failure, which is exactly when it would have been most useful.
     */
    val helpline: StateFlow<Helpline> = ready.mapState { it?.contact ?: Helpline.Unknown }

    /** Whether the server implements the console commands this screen offers. */
    val consoleAvailable: StateFlow<Boolean> =
        ready.mapState { it?.supportsCommand(Protocol.Command.ENABLE_CONSOLE_INPUT) == true }

    /** The desk's own answer: on gets no second press, off gets the red signal. */
    val micSignal: StateFlow<ConsoleSignal> = state.mapState { readConsoleSignal(it, Protocol.ConsoleInput.MIC) }
    val auxSignal: StateFlow<ConsoleSignal> = state.mapState { readConsoleSignal(it, Protocol.ConsoleInput.AUX) }

    // Note(yoochan.kim): a drag fires per pixel; the device wants only the latest
    private val volumeWrites = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        viewModelScope.launch { socketManager.initSocket() }
        viewModelScope.launch {
            volumeWrites.sample(VOLUME_WRITE_INTERVAL_MS).collect {
                socketManager.write(Protocol.Attribute.VOLUME, it)
            }
        }
    }

    fun changeVolume(newVolume: Int) {
        volumeWrites.tryEmit(newVolume)
    }

    fun togglePlayback() {
        val next = if (isPlaying.value) Protocol.Playback.PAUSED else Protocol.Playback.PLAYING
        socketManager.write(Protocol.Attribute.PLAYBACK, next)
    }

    fun toggleMute() {
        val next = if (isMuted.value) Protocol.Mute.UNMUTED else Protocol.Mute.MUTED
        socketManager.write(Protocol.Attribute.MUTE, next)
    }

    fun changeSong(songId: String) {
        if (songId == currentSong.value) return
        socketManager.write(Protocol.Attribute.SONG, songId)
    }

    fun enableMicrophone() = enableConsoleInput(Protocol.ConsoleInput.MIC)

    fun enableAux() = enableConsoleInput(Protocol.ConsoleInput.AUX)

    private fun enableConsoleInput(input: String) {
        socketManager.invoke(
            Protocol.Command.ENABLE_CONSOLE_INPUT,
            JSONObject().put("input", input),
        )
    }

    fun dismissRejection() = socketManager.clearRejection()

    fun reconnect() = socketManager.reconnect()

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }

    private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> =
        map(transform).stateIn(viewModelScope, SharingStarted.Eagerly, transform(value))
}

/**
 * Reads the flow attribute.
 *
 * A phase this build does not know means the server is newer than the app, so
 * it becomes Unknown and is shown as such. Treating it as idle would tell the
 * operator nothing is running while a service is under way.
 */
/** One console input as the desk answered it; known is false while it is silent. */
data class ConsoleSignal(val known: Boolean = false, val on: Boolean = false)

private fun readConsoleSignal(state: JSONObject, input: String): ConsoleSignal {
    val read = state.optJSONObject(Protocol.Attribute.CONSOLE)?.optJSONObject(input) ?: return ConsoleSignal()
    if (read.optString("kind") != "read") return ConsoleSignal()
    return ConsoleSignal(known = true, on = read.optBoolean("on"))
}

private fun readFlow(state: JSONObject): FlowStatus {
    val flow = state.optJSONObject(Protocol.Attribute.FLOW) ?: return FlowStatus.Idle
    return when (flow.optString("phase")) {
        Protocol.FlowPhase.IDLE -> FlowStatus.Idle
        Protocol.FlowPhase.WAITING -> FlowStatus.Waiting(
            name = flow.optString("name"),
            startsAt = flow.optString("startsAt"),
        )
        Protocol.FlowPhase.PLAYING -> {
            val track = flow.optJSONObject("track")
            FlowStatus.Playing(
                name = flow.optString("name"),
                trackTitle = track?.optString("title").orEmpty(),
                index = track?.optInt("index") ?: 0,
                total = track?.optInt("total") ?: 0,
                endsAt = flow.optString("endsAt"),
            )
        }
        Protocol.FlowPhase.HOLDING -> FlowStatus.Holding(
            name = flow.optString("name"),
            unlockAt = flow.optString("unlockAt"),
        )
        else -> FlowStatus.Unknown
    }
}
