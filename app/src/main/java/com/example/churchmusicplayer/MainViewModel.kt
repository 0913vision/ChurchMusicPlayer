package com.example.churchmusicplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.churchmusicplayer.data.SocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val socketManager = SocketManager()

    private val _volume = MutableStateFlow(50)
    val volume: StateFlow<Int> = _volume

    private val _state = MutableStateFlow(0)
    val state: StateFlow<Int> = _state

    private val _mute = MutableStateFlow(0)
    val mute: StateFlow<Int> = _mute

    private val _currentSong = MutableStateFlow("slow")
    val currentSong: StateFlow<String> = _currentSong

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing

    private fun setProcessing() {
        viewModelScope.launch {
            _processing.value = true
            delay(1500)
            _processing.value = false
        }
    }

    val connectionStatus = socketManager.connectionStatus

    init {
        initSocket()
    }

    private fun initSocket() {
        viewModelScope.launch {
            socketManager.initSocket()
            setupSocketListeners()
        }
    }

    private fun setupSocketListeners() {
        socketManager.on("stateChanged") { args ->
            args[0]?.let { _state.value = it as Int }
        }

        socketManager.on("volumeChanged") { args ->
            try {
                args[0]?.let {
                    val newVolume = (it as? Double)?.toInt() ?: (it as? Int) ?: return@let
                    _volume.value = newVolume.coerceIn(0, 100)
                }
            } catch (e: Exception) {
                println(e)
            }
        }

        socketManager.on("muteChanged") { args ->
            args[0]?.let { _mute.value = it as Int }
        }

        socketManager.on("songChanged") { args ->
            args[0]?.let { _currentSong.value = it as String }
        }
    }

    fun changeVolume(newVolume: Int) {
        socketManager.emit("changeVolume", newVolume)
    }

    fun changeState() {
        setProcessing()
        val newState = if (_state.value == 0) 1 else 0
        socketManager.emit("changeState", newState)
    }

    fun changeMute() {
        val newMute = if (_mute.value == 0) 1 else 0
        socketManager.emit("changeMute", newMute)
    }

    fun changeSong(newSong: String) {
        if (_state.value != 0) {
            setProcessing()
        }
        if (_currentSong.value != newSong) {
            socketManager.emit("changeSong", _currentSong.value, newSong)
        }
    }

    fun reconnect() {
        socketManager.reconnect()
        setupSocketListeners()
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }
}