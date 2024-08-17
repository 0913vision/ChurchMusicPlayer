package com.example.churchmusicplayer.data

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.*

class SocketManager {
    private lateinit var socket: Socket
    private var isInitialized = false
    private var lastPingTime: Long = 0
    private var pingCheckJob: Job? = null
    private var gracePeriodJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private var serverUrl = "http://192.168.0.4:3000/"

    fun initSocket() {
        if (isInitialized) return
        try {
            _connectionStatus.value = ConnectionStatus.Connecting

            val options = IO.Options().apply {
                timeout = 45000
                path = "/api/socket"
            }

            socket = IO.socket(serverUrl, options)

            socket.on("ping") {
                lastPingTime = System.currentTimeMillis()
            }

            socket.on(Socket.EVENT_CONNECT) {
                _connectionStatus.value = ConnectionStatus.Connected
                lastPingTime = System.currentTimeMillis()
                gracePeriodJob?.cancel()
                requestInitialState()
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) {
//                _connectionStatus.value = ConnectionStatus.Error("연결 실패: ${it[0]}")
//                _connectionStatus.value = ConnectionStatus.Disconnected
                handleDisconnection()
            }

            socket.on(Socket.EVENT_DISCONNECT ) {
//                _connectionStatus.value = ConnectionStatus.Disconnected
                handleDisconnection()
            }

            socket.connect()
            startPingCheck()
            isInitialized = true

        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error("초기화 실패: ${e.message}")
        }
    }

    private fun startPingCheck() {
        pingCheckJob = coroutineScope.launch {
            while (isActive) {
                delay(15000)
                val timeSinceLastPing = System.currentTimeMillis() - lastPingTime
                if (timeSinceLastPing > 45000 && _connectionStatus.value == ConnectionStatus.Connected) {
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
                // 이미 GracePeriod 상태이므로 아무것도 하지 않음
            }
            else -> {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    private fun startGracePeriod() {
        gracePeriodJob?.cancel()
        gracePeriodJob = coroutineScope.launch {
            delay(3000) // 3초 대기
            if (_connectionStatus.value is ConnectionStatus.GracePeriod) {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    fun reconnect()  {
        if (::socket.isInitialized) {
            socket.disconnect()
        }
        isInitialized = false
        initSocket()
    }

    fun emit(event: String, vararg args: Any) {
        if (::socket.isInitialized) {
            socket.emit(event, *args)
        }
    }

    fun on(event: String, listener: (Array<Any>) -> Unit) {
        if (::socket.isInitialized) {
            socket.on(event) { listener(it) }
        }
    }

    fun disconnect() {
        if (::socket.isInitialized) {
            socket.disconnect()
        }
        coroutineScope.cancel()
        pingCheckJob?.cancel()
        isInitialized = false
    }

    private fun requestInitialState() {
        emit("getVolume")
        emit("getState")
        emit("getMute")
        emit("getCurrentSong")
    }

}

sealed interface ConnectionStatus {
    data object Connected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Disconnected : ConnectionStatus
    data class GracePeriod(val startTime: Long) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}