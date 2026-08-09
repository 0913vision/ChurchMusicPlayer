package com.example.churchmusicplayer.data

/**
 * The media server's wire protocol, v1.
 *
 * The server describes itself as a device: attributes are state you read and
 * write, commands are actions you invoke, and one event carries whatever
 * changed. See docs/PROTOCOL.md in church-media-server for the full reference.
 *
 * Note(yoochan.kim): kept deliberately small — names and shapes only. Anything
 * this file decides on its own is a second copy of a rule the server already
 * owns, which is exactly what v1 set out to remove.
 */
object Protocol {
    const val VERSION = 3

    object C2S {
        const val HELLO = "hello"
        const val READ = "read"
        const val WRITE = "write"
        const val INVOKE = "invoke"
    }

    object S2C {
        const val READY = "ready"
        const val STATE = "state"
        const val REJECTED = "rejected"
        const val PING = "ping"
    }

    /** Attribute names, the things a write can target */
    object Attribute {
        const val PLAYBACK = "playback"
        const val VOLUME = "volume"
        const val SONG = "song"
        const val ADMIN_LOCK = "adminLock"
        const val AUDIO_LOCK = "audioLock"
        const val IS_ADMIN = "isAdmin"
        const val FLOW = "flow"
        const val CONSOLE = "console"
    }

    object Command {
        const val ENABLE_CONSOLE_INPUT = "enableConsoleInput"
        const val INITIALIZE_CONSOLE = "initializeConsole"
    }

    object Playback {
        const val PLAYING = "playing"
        const val PAUSED = "paused"
    }

    /** Flow phases. An unrecognised one is a fault, not a case — see FlowStatus. */
    object FlowPhase {
        const val IDLE = "idle"
        const val WAITING = "waiting"
        const val PLAYING = "playing"
        const val HOLDING = "holding"
    }
}

/**
 * A song the user may select.
 *
 * How many there are, what they are called and what order they come in is
 * entirely the server's answer, arriving in `ready`. The app draws a button per
 * song it is given, so adding one is a change to the server's manifest and
 * never a release of this app.
 */
data class Song(val id: String, val title: String)

/**
 * Who to call when this panel is not working.
 *
 * The server supplies it in `ready`, so the person on duty can change without
 * anyone shipping a new build. Before the handshake lands there is nobody to
 * name — and a notice printing a blank line where a phone number belongs is
 * worse than one that simply does not offer a number, so the two cases are
 * kept apart rather than papered over with an empty string.
 */
sealed interface Helpline {
    data object Unknown : Helpline
    data class Known(val name: String, val phone: String) : Helpline
}

/**
 * What the server's flow slot is doing.
 *
 * Modelled as a closed set with an explicit Unknown, because a phase this build
 * does not recognise means the server is newer than the app. Showing nothing
 * would read as "no flow is running", which may be false while a service is
 * under way — so Unknown is carried through and surfaced.
 */
sealed interface FlowStatus {
    data object Idle : FlowStatus
    data class Waiting(val name: String, val startsAt: String) : FlowStatus
    data class Playing(val name: String, val trackTitle: String, val index: Int, val total: Int, val endsAt: String) : FlowStatus
    data class Holding(val name: String, val unlockAt: String) : FlowStatus
    data object Unknown : FlowStatus
}

/**
 * Why a write or invoke was refused, as text the person at the panel can act
 * on. Same voice as the rest of the screen, and the same word for the same
 * thing: a closed gate is 방송실 here too, never "관리자".
 */
enum class RejectReason(val wire: String, val message: String) {
    ADMIN_LOCKED("adminLocked", "방송실에서 사용 중이에요"),
    DEVICE_BUSY("deviceBusy", "바뀌는 중이에요. 잠시 후 다시 눌러 주세요"),
    NOT_ADMIN("notAdmin", "방송실에서만 할 수 있어요"),
    NOT_WRITABLE("notWritable", "바꿀 수 없어요"),
    INVALID_VALUE("invalidValue", "값이 올바르지 않아요"),
    UNKNOWN_TARGET("unknownTarget", "서버가 모르는 요청이에요"),
    PROTOCOL_MISMATCH("protocolMismatch", "앱을 업데이트해 주세요"),
    UNKNOWN("", "요청이 거부됐어요");

    companion object {
        fun of(wire: String?): RejectReason = entries.firstOrNull { it.wire == wire } ?: UNKNOWN
    }
}
