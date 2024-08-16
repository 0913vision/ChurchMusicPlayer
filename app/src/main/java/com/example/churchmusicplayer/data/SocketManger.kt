package com.example.churchmusicplayer.data

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SocketManager {
    private lateinit var socket: Socket

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private var serverUrl = "http://default_server_url:8080"

    fun initSocket() {
        try {
            _connectionStatus.value = ConnectionStatus.Connecting

            val options = IO.Options().apply {
                timeout = 10000 // 10초
                reconnection = true
                reconnectionAttempts = 3
                reconnectionDelay = 1000 // 1초
            }

            socket = IO.socket(serverUrl, options)
            socket.connect()

            socket.on(Socket.EVENT_CONNECT) {
                _connectionStatus.value = ConnectionStatus.Connected
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) {
                _connectionStatus.value = ConnectionStatus.Error("연결 실패: ${it[0]}")
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }

        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error("초기화 실패: ${e.message}")
        }
    }

    fun updateServerUrl(newUrl: String) {
        serverUrl = newUrl
        reconnect()
    }

    fun reconnect() {
        socket.disconnect()
        initSocket()
    }

    fun emit(event: String, vararg args: Any) {
        socket.emit(event, *args)
    }

    fun on(event: String, listener: (Array<Any>) -> Unit) {
        socket.on(event) { listener(it) }
    }

    fun disconnect() {
        socket.disconnect()
    }
}

sealed interface ConnectionStatus {
    data object Connected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Disconnected : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}