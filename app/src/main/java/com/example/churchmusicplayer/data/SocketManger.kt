package com.example.churchmusicplayer.data

import com.example.churchmusicplayer.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

private const val CONNECT_TIMEOUT_MS = 45_000L
private const val PING_CHECK_INTERVAL_MS = 15_000L
private const val PING_SILENCE_LIMIT_MS = 45_000L
private const val GRACE_PERIOD_MS = 3_000L

/**
 * The one connection to the media server, and the only place that knows the
 * wire protocol exists.
 *
 * Protocol v1 in three moves: say hello, write an attribute, invoke a command.
 * What comes back is a ready payload describing the server, state patches
 * carrying whatever changed, and a refusal when something is not allowed.
 */
/**
 * @param serverUrl where the media server is. The build supplies a default, but
 *   the address of a machine on a church network is not a fact about this app —
 *   a router swap or a new Pi would otherwise brick every mounted device, since
 *   even the in-app updater is reached over this same address.
 */
class SocketManager(private val serverUrl: String, private val clientName: String) {
    private lateinit var socket: Socket
    private var isInitialized = false
    private var lastPingTime: Long = 0
    private var pingCheckJob: Job? = null
    private var gracePeriodJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    /** What the server said it is. Empty until the handshake completes. */
    private val _ready = MutableStateFlow<ServerInfo?>(null)
    val ready: StateFlow<ServerInfo?> = _ready

    /** Attribute values, merged from patches as they arrive. */
    private val _state = MutableStateFlow(JSONObject())
    val state: StateFlow<JSONObject> = _state

    /** The most recent refusal, for the screen to explain. */
    private val _rejection = MutableStateFlow<Rejection?>(null)
    val rejection: StateFlow<Rejection?> = _rejection

    fun initSocket() {
        if (isInitialized) return
        try {
            _connectionStatus.value = ConnectionStatus.Connecting

            // The standalone server uses the default Socket.IO path; the old
            // Next.js one served it under /api/socket.
            val options = IO.Options().apply {
                timeout = CONNECT_TIMEOUT_MS
            }

            socket = IO.socket(serverUrl, options)
            registerHandlers()

            socket.connect()
            startPingCheck()
            isInitialized = true
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error("초기화 실패: ${e.message}")
        }
    }

    private fun registerHandlers() {
        socket.on(Protocol.S2C.PING) {
            lastPingTime = System.currentTimeMillis()
        }

        socket.on(Socket.EVENT_CONNECT) {
            lastPingTime = System.currentTimeMillis()
            gracePeriodJob?.cancel()
            // Identify before anything else: the server refuses writes until a
            // client has said which protocol version it speaks.
            emit(Protocol.C2S.HELLO, JSONObject().apply {
                put("client", clientName)
                put("protocolVersion", Protocol.VERSION)
            })
        }

        socket.on(Protocol.S2C.READY) { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@on
            _ready.value = ServerInfo.from(payload)
            _connectionStatus.value =
                if (ServerInfo.from(payload).accepted) ConnectionStatus.Connected
                else ConnectionStatus.Outdated
        }

        socket.on(Protocol.S2C.STATE) { args ->
            val patch = args.firstOrNull() as? JSONObject ?: return@on
            // A patch carries only what changed, so it is merged rather than
            // swapped in — dropping the rest would blank fields nobody touched.
            val merged = JSONObject(_state.value.toString())
            for (key in patch.keys()) merged.put(key, patch.get(key))
            _state.value = merged
        }

        socket.on(Protocol.S2C.REJECTED) { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@on
            val target = payload.optString("target")
            val reason = RejectReason.of(payload.optString("reason"))
            // Note(yoochan.kim): a locked fader is inert, not a conversation
            if (target == Protocol.Attribute.VOLUME && reason == RejectReason.DEVICE_BUSY) return@on
            _rejection.value = Rejection(target = target, reason = reason, at = System.currentTimeMillis())
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { handleDisconnection() }
        socket.on(Socket.EVENT_DISCONNECT) { handleDisconnection() }
    }

    /** Sets one attribute. A refusal comes back separately, as a rejection. */
    fun write(field: String, value: Any) {
        emit(Protocol.C2S.WRITE, JSONObject().apply {
            put("field", field)
            put("value", value)
        })
    }

    fun invoke(command: String, args: JSONObject = JSONObject()) {
        emit(Protocol.C2S.INVOKE, JSONObject().apply {
            put("command", command)
            put("args", args)
        })
    }

    /** Asks for every attribute again, after waking or reconnecting. */
    fun read() {
        emit(Protocol.C2S.READ, JSONObject())
    }

    private fun emit(event: String, payload: JSONObject) {
        if (::socket.isInitialized) socket.emit(event, payload)
    }

    fun clearRejection() {
        _rejection.value = null
    }

    private fun startPingCheck() {
        pingCheckJob = coroutineScope.launch {
            while (isActive) {
                delay(PING_CHECK_INTERVAL_MS)
                val silence = System.currentTimeMillis() - lastPingTime
                if (silence > PING_SILENCE_LIMIT_MS && _connectionStatus.value == ConnectionStatus.Connected) {
                    handleDisconnection()
                }
            }
        }
    }

    private fun handleDisconnection() {
        when (_connectionStatus.value) {
            is ConnectionStatus.Connected -> {
                _connectionStatus.value = ConnectionStatus.GracePeriod(System.currentTimeMillis())
                startGracePeriod()
            }
            is ConnectionStatus.GracePeriod -> {
                // Already counting down; nothing to do.
            }
            else -> {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    private fun startGracePeriod() {
        gracePeriodJob?.cancel()
        gracePeriodJob = coroutineScope.launch {
            delay(GRACE_PERIOD_MS)
            if (_connectionStatus.value is ConnectionStatus.GracePeriod) {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    fun reconnect() {
        if (::socket.isInitialized) socket.disconnect()
        // The handshake and every attribute arrive again, so nothing is kept
        // from a connection that is gone.
        _ready.value = null
        _state.value = JSONObject()
        isInitialized = false
        initSocket()
    }

    fun disconnect() {
        if (::socket.isInitialized) socket.disconnect()
        coroutineScope.cancel()
        pingCheckJob?.cancel()
        isInitialized = false
    }
}

/** What the server told us about itself in the ready payload. */
data class ServerInfo(
    val protocolVersion: Int,
    val accepted: Boolean,
    val attributes: Set<String>,
    val commands: Set<String>,
    val songs: List<Song>,
    val contact: Helpline,
) {
    fun supportsCommand(command: String): Boolean = command in commands

    companion object {
        fun from(payload: JSONObject): ServerInfo = ServerInfo(
            protocolVersion = payload.optInt("protocolVersion"),
            accepted = payload.optBoolean("accepted"),
            attributes = payload.optJSONArray("attributes").toStringSet(),
            commands = payload.optJSONArray("commands").toStringSet(),
            songs = payload.optJSONArray("songs").let { array ->
                buildList {
                    for (i in 0 until (array?.length() ?: 0)) {
                        val song = array!!.optJSONObject(i) ?: continue
                        add(Song(id = song.optString("id"), title = song.optString("title")))
                    }
                }
            },
            contact = payload.optJSONObject("contact").let { contact ->
                val name = contact?.optString("name").orEmpty()
                val phone = contact?.optString("phone").orEmpty()
                // A half-filled contact is no contact: printing a name with no
                // number tells the reader to call someone they cannot reach.
                if (name.isEmpty() || phone.isEmpty()) Helpline.Unknown
                else Helpline.Known(name = name, phone = phone)
            },
        )

        private fun org.json.JSONArray?.toStringSet(): Set<String> =
            buildSet { for (i in 0 until (this@toStringSet?.length() ?: 0)) add(this@toStringSet!!.optString(i)) }
    }
}

/** A refusal, kept with its arrival time so the screen can let it fade. */
data class Rejection(val target: String, val reason: RejectReason, val at: Long)

sealed interface ConnectionStatus {
    data object Connected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Disconnected : ConnectionStatus
    data class GracePeriod(val startTime: Long) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus

    /** The server refused this version; reconnecting cannot fix it, only an update can. */
    data object Outdated : ConnectionStatus
}
