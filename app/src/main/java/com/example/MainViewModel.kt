package com.example

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.repository.CloudMacrosRepository
import com.example.data.repository.ErrorReportsRepository
import com.example.data.repository.FeedbackRepository
import com.example.data.repository.PcProfilesRepository
import com.example.engine.TouchpadEngine
import com.example.model.AccelerationProfile
import com.example.model.CloudMacro
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.DiagnosticStats
import com.example.model.ErrorReport
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.HapticLevel
import com.example.model.MacroItem
import com.example.model.SavedPcProfile
import com.example.model.TouchpadSettings
import com.example.model.UserFeedback
import com.example.service.AuthManager
import com.example.service.BluetoothHidManager
import com.example.service.HapticFeedbackHelper
import com.example.service.NetworkStreamManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    TOUCHPAD("Touchpad"),
    KEYBOARD("Keyboard"),
    MACROS("Quick Macros"),
    VIRTUAL_SCREEN("Live Preview"),
    SETTINGS("Settings & Tuning")
}

data class VirtualScreenState(
    val cursorX: Float = 0.5f, // Normalized 0f..1f
    val cursorY: Float = 0.5f, // Normalized 0f..1f
    val isLeftDown: Boolean = false,
    val isRightDown: Boolean = false,
    val clickRippleX: Float? = null,
    val clickRippleY: Float? = null,
    val activeWindowIndex: Int = 0,
    val recentGestureNotification: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesManager = PreferencesManager(application)
    val hapticHelper = HapticFeedbackHelper(application)
    val bluetoothManager = BluetoothHidManager(application)
    val networkManager = NetworkStreamManager(application)

    // Firebase & Cloud Repositories
    val authManager = AuthManager(application)
    val cloudMacrosRepo = CloudMacrosRepository(application)
    val pcProfilesRepo = PcProfilesRepository(application)
    val feedbackRepo = FeedbackRepository(application)
    val errorReportsRepo = ErrorReportsRepository(application)

    // Benchmark Engine
    val benchmarkEngine = com.example.engine.BenchmarkEngine(viewModelScope)
    val benchmarkState: StateFlow<com.example.engine.BenchmarkState> = benchmarkEngine.state

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    private val _cloudMacros = MutableStateFlow<List<CloudMacro>>(emptyList())
    val cloudMacros: StateFlow<List<CloudMacro>> = _cloudMacros.asStateFlow()

    private val _savedProfiles = MutableStateFlow<List<SavedPcProfile>>(emptyList())
    val savedProfiles: StateFlow<List<SavedPcProfile>> = _savedProfiles.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var macroSyncJob: Job? = null
    private var profileSyncJob: Job? = null

    val settings: StateFlow<TouchpadSettings> = preferencesManager.settings
    val stats: StateFlow<DiagnosticStats> = bluetoothManager.stats

    private val _currentScreen = MutableStateFlow(AppScreen.TOUCHPAD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _virtualScreen = MutableStateFlow(VirtualScreenState())
    val virtualScreen: StateFlow<VirtualScreenState> = _virtualScreen.asStateFlow()

    // Sticky modifier keys for keyboard
    private val _ctrlHeld = MutableStateFlow(false)
    val ctrlHeld: StateFlow<Boolean> = _ctrlHeld.asStateFlow()

    private val _shiftHeld = MutableStateFlow(false)
    val shiftHeld: StateFlow<Boolean> = _shiftHeld.asStateFlow()

    private val _altHeld = MutableStateFlow(false)
    val altHeld: StateFlow<Boolean> = _altHeld.asStateFlow()

    private val _winHeld = MutableStateFlow(false)
    val winHeld: StateFlow<Boolean> = _winHeld.asStateFlow()

    // Default macro decks
    val defaultMacros = listOf(
        MacroItem("copy", "Copy", "Ctrl + C", listOf("Ctrl", "C")),
        MacroItem("paste", "Paste", "Ctrl + V", listOf("Ctrl", "V")),
        MacroItem("cut", "Cut", "Ctrl + X", listOf("Ctrl", "X")),
        MacroItem("undo", "Undo", "Ctrl + Z", listOf("Ctrl", "Z")),
        MacroItem("redo", "Redo", "Ctrl + Y", listOf("Ctrl", "Y")),
        MacroItem("select_all", "Select All", "Ctrl + A", listOf("Ctrl", "A")),
        MacroItem("save", "Save", "Ctrl + S", listOf("Ctrl", "S")),
        MacroItem("close_tab", "Close Tab", "Ctrl + W", listOf("Ctrl", "W")),
        MacroItem("new_tab", "New Tab", "Ctrl + T", listOf("Ctrl", "T")),
        MacroItem("task_mgr", "Task Manager", "Ctrl + Shift + Esc", listOf("Ctrl", "Shift", "Esc")),
        MacroItem("screenshot", "Screenshot", "Win + Shift + S", listOf("Win", "Shift", "S")),
        MacroItem("lock_pc", "Lock PC", "Win + L", listOf("Win", "L")),
        MacroItem("desktop", "Desktop", "Win + D", listOf("Win", "D")),
        MacroItem("switch_app", "Switch App", "Alt + Tab", listOf("Alt", "Tab"))
    )

    val touchpadEngine = TouchpadEngine(
        settings = settings.value,
        onSendMouse = { buttons, dx, dy, wheel ->
            sendMouse(buttons, dx, dy, wheel)
        },
        onTriggerAction = { action ->
            executeGestureAction(action)
        }
    )

    init {
        networkManager.pcIpAddress = preferencesManager.getSavedPcIp()
        val savedModeStr = preferencesManager.getSavedConnectionMode()
        try {
            val savedMode = ConnectionMode.valueOf(savedModeStr)
            if (savedMode == ConnectionMode.LOCAL_WIFI) {
                bluetoothManager.setWifiMode(networkManager.pcIpAddress)
            }
        } catch (e: Exception) {
            // Keep default
        }

        viewModelScope.launch {
            settings.collect { newSettings ->
                touchpadEngine.updateSettings(newSettings)
            }
        }

        viewModelScope.launch {
            networkManager.logCallback.collect { log ->
                if (log != null) {
                    bluetoothManager.addLog(log)
                }
            }
        }

        viewModelScope.launch {
            networkManager.liveLatencyMs.collect { latency ->
                if (stats.value.connectionMode == ConnectionMode.LOCAL_WIFI) {
                    bluetoothManager.updateWifiLatency(latency, networkManager.isPcOnline.value)
                }
            }
        }

        viewModelScope.launch {
            networkManager.isPcOnline.collect { online ->
                if (stats.value.connectionMode == ConnectionMode.LOCAL_WIFI) {
                    bluetoothManager.updateWifiOnline(online, networkManager.pcIpAddress)
                }
            }
        }

        viewModelScope.launch {
            networkManager.testState.collect { state ->
                when (state) {
                    is com.example.service.WifiConnectionTestState.Success -> {
                        if (state.ip.isNotBlank()) {
                            networkManager.pcIpAddress = state.ip
                            preferencesManager.setSavedPcIp(state.ip)
                            preferencesManager.setSavedConnectionMode(ConnectionMode.LOCAL_WIFI.name)
                            bluetoothManager.setWifiMode(state.ip, state.serverName)
                            networkManager.startHeartbeat()
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            networkManager.targetPcIp.collect { newIp ->
                if (newIp.isNotBlank() && stats.value.connectionMode == ConnectionMode.LOCAL_WIFI) {
                    preferencesManager.setSavedPcIp(newIp)
                    bluetoothManager.updateWifiOnline(networkManager.isPcOnline.value, newIp)
                }
            }
        }

        if (savedModeStr == ConnectionMode.LOCAL_WIFI.name) {
            networkManager.startHeartbeat()
        }

        // Cloud sync lifecycle when user signs in or out
        viewModelScope.launch {
            currentUser.collect { user ->
                macroSyncJob?.cancel()
                profileSyncJob?.cancel()
                if (user != null) {
                    macroSyncJob = viewModelScope.launch {
                        cloudMacrosRepo.observeMacros(user.uid).collect { macros ->
                            _cloudMacros.value = macros
                        }
                    }
                    profileSyncJob = viewModelScope.launch {
                        pcProfilesRepo.observeProfiles(user.uid).collect { profiles ->
                            _savedProfiles.value = profiles
                        }
                    }
                } else {
                    _cloudMacros.value = emptyList()
                    _savedProfiles.value = emptyList()
                }
            }
        }

        bluetoothManager.initialize()
    }

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun sendMouse(buttons: Byte, dx: Int, dy: Int, wheel: Int) {
        when (stats.value.connectionMode) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothManager.sendMouseReport(buttons, dx, dy, wheel)
            }
            ConnectionMode.LOCAL_WIFI -> {
                networkManager.sendMousePacket(dx, dy, buttons, wheel)
                bluetoothManager.recordPacketSent("WIFI MOUSE (btn=$buttons, dx=$dx, dy=$dy, whl=$wheel)")
            }
            ConnectionMode.SIMULATION -> {
                bluetoothManager.recordPacketSent("SIM MOUSE (btn=$buttons, dx=$dx, dy=$dy)")
            }
        }

        // Update virtual PC screen simulation
        updateVirtualScreen(dx, dy, buttons, wheel)
    }

    private fun updateVirtualScreen(dx: Int, dy: Int, buttons: Byte, wheel: Int) {
        val curr = _virtualScreen.value
        // Virtual canvas sensitivity mapping
        val factor = 0.0016f
        val newX = (curr.cursorX + dx * factor).coerceIn(0.02f, 0.98f)
        val newY = (curr.cursorY + dy * factor).coerceIn(0.04f, 0.96f)

        val leftDown = (buttons.toInt() and 1) != 0
        val rightDown = (buttons.toInt() and 2) != 0

        var rippleX = curr.clickRippleX
        var rippleY = curr.clickRippleY
        if (leftDown && !curr.isLeftDown) {
            rippleX = newX
            rippleY = newY
            hapticHelper.click(settings.value.hapticLevel)
        } else if (rightDown && !curr.isRightDown) {
            rippleX = newX
            rippleY = newY
            hapticHelper.click(settings.value.hapticLevel)
        }

        _virtualScreen.value = curr.copy(
            cursorX = newX,
            cursorY = newY,
            isLeftDown = leftDown,
            isRightDown = rightDown,
            clickRippleX = rippleX,
            clickRippleY = rippleY
        )
    }

    fun executeGestureAction(action: GestureAction) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = action.title
        )
        viewModelScope.launch {
            delay(1800)
            if (_virtualScreen.value.recentGestureNotification == action.title) {
                _virtualScreen.value = _virtualScreen.value.copy(recentGestureNotification = null)
            }
        }

        // Map gesture action to Bluetooth HID reports or network packets
        when (action) {
            GestureAction.LEFT_CLICK -> sendMouse(1, 0, 0, 0)
            GestureAction.RIGHT_CLICK -> sendMouse(2, 0, 0, 0)
            GestureAction.MIDDLE_CLICK -> sendMouse(4, 0, 0, 0)
            GestureAction.SHOW_DESKTOP -> {
                // Win + D (Win modifier: 0x08, D keycode: 0x07)
                bluetoothManager.sendKeyboardReport(0x08.toByte(), 0x07.toByte())
                networkManager.sendKeyPacket(listOf("Win", "d"), "hotkey")
            }
            GestureAction.TASK_VIEW -> {
                // Win + Tab (Win: 0x08, Tab: 0x2B)
                bluetoothManager.sendKeyboardReport(0x08.toByte(), 0x2B.toByte())
                networkManager.sendKeyPacket(listOf("Win", "Tab"), "hotkey")
            }
            GestureAction.CYCLE_APPS_FORWARD -> {
                // Alt + Tab (Alt: 0x04, Tab: 0x2B)
                bluetoothManager.sendKeyboardReport(0x04.toByte(), 0x2B.toByte())
                networkManager.sendKeyPacket(listOf("Alt", "Tab"), "hotkey")
            }
            GestureAction.CYCLE_APPS_BACKWARD -> {
                // Alt + Shift + Tab (Alt: 0x04, Shift: 0x02, Tab: 0x2B)
                bluetoothManager.sendKeyboardReport(0x06.toByte(), 0x2B.toByte())
                networkManager.sendKeyPacket(listOf("Alt", "Shift", "Tab"), "hotkey")
            }
            GestureAction.SEARCH -> {
                bluetoothManager.sendKeyboardReport(0x08.toByte(), 0x16.toByte()) // Win + S
                networkManager.sendKeyPacket(listOf("Win", "s"), "hotkey")
            }
            GestureAction.ACTION_CENTER -> {
                bluetoothManager.sendKeyboardReport(0x08.toByte(), 0x04.toByte()) // Win + A
                networkManager.sendKeyPacket(listOf("Win", "a"), "hotkey")
            }
            GestureAction.SWITCH_DESKTOP_LEFT -> {
                // Ctrl + Win + Left (Ctrl: 0x01, Win: 0x08 -> 0x09, Left arrow: 0x50)
                bluetoothManager.sendKeyboardReport(0x09.toByte(), 0x50.toByte())
                networkManager.sendKeyPacket(listOf("Ctrl", "Win", "Left"), "hotkey")
            }
            GestureAction.SWITCH_DESKTOP_RIGHT -> {
                bluetoothManager.sendKeyboardReport(0x09.toByte(), 0x4F.toByte()) // Ctrl + Win + Right
                networkManager.sendKeyPacket(listOf("Ctrl", "Win", "Right"), "hotkey")
            }
            GestureAction.SCREENSHOT -> {
                bluetoothManager.sendKeyboardReport(0x0A.toByte(), 0x16.toByte()) // Win + Shift + S
                networkManager.sendKeyPacket(listOf("Win", "Shift", "s"), "hotkey")
            }
            GestureAction.LOCK_PC -> {
                bluetoothManager.sendKeyboardReport(0x08.toByte(), 0x0F.toByte()) // Win + L
                networkManager.sendKeyPacket(listOf("Win", "l"), "hotkey")
            }
            GestureAction.MUTE -> {
                // Consumer Volume Mute (0x7F keycode or consumer)
                bluetoothManager.sendKeyboardReport(0, 0x7F.toByte())
                networkManager.sendKeyPacket(listOf("AudioMute"), "media")
            }
            GestureAction.VOLUME_UP -> {
                bluetoothManager.sendKeyboardReport(0, 0x80.toByte())
                networkManager.sendKeyPacket(listOf("AudioVolUp"), "media")
            }
            GestureAction.VOLUME_DOWN -> {
                bluetoothManager.sendKeyboardReport(0, 0x81.toByte())
                networkManager.sendKeyPacket(listOf("AudioVolDown"), "media")
            }
            GestureAction.PLAY_PAUSE -> {
                networkManager.sendKeyPacket(listOf("MediaPlayPause"), "media")
                bluetoothManager.recordPacketSent("MEDIA PLAY/PAUSE")
            }
            else -> {}
        }
    }

    fun toggleModifier(modifier: String) {
        hapticHelper.click(settings.value.hapticLevel)
        when (modifier) {
            "Ctrl" -> _ctrlHeld.value = !_ctrlHeld.value
            "Shift" -> _shiftHeld.value = !_shiftHeld.value
            "Alt" -> _altHeld.value = !_altHeld.value
            "Win" -> _winHeld.value = !_winHeld.value
        }
    }

    fun sendKeyPress(keyLabel: String, hidCode: Byte) {
        hapticHelper.click(settings.value.hapticLevel)
        var modifierMask: Byte = 0
        if (_ctrlHeld.value) modifierMask = (modifierMask.toInt() or 0x01).toByte()
        if (_shiftHeld.value) modifierMask = (modifierMask.toInt() or 0x02).toByte()
        if (_altHeld.value) modifierMask = (modifierMask.toInt() or 0x04).toByte()
        if (_winHeld.value) modifierMask = (modifierMask.toInt() or 0x08).toByte()

        bluetoothManager.sendKeyboardReport(modifierMask, hidCode)
        networkManager.sendKeyPacket(listOf(keyLabel), "key")

        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Key: $keyLabel"
        )
    }

    fun sendTextDirect(text: String) {
        if (text.isBlank()) return
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendKeyPacket(listOf(text), "text")
        bluetoothManager.recordPacketSent("SEND TEXT: \"$text\"")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Sent: \"$text\""
        )
    }

    fun executeMacro(macro: MacroItem) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendKeyPacket(macro.shortcutKeys, "macro")
        bluetoothManager.recordPacketSent("MACRO: ${macro.label} (${macro.description})")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Macro: ${macro.label}"
        )
    }

    // Settings adjustments
    fun setPointerSensitivity(value: Float) {
        preferencesManager.updateSettings(settings.value.copy(pointerSensitivity = value))
    }

    fun setAccelerationProfile(profile: AccelerationProfile) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(accelerationProfile = profile))
    }

    fun setAccelerationMultiplier(multiplier: Float) {
        preferencesManager.updateSettings(settings.value.copy(accelerationMultiplier = multiplier))
    }

    fun setScrollSensitivity(value: Float) {
        preferencesManager.updateSettings(settings.value.copy(scrollSensitivity = value))
    }

    fun setNaturalScroll(natural: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(naturalScroll = natural))
    }

    fun setSmoothInertia(inertia: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(smoothInertia = inertia))
    }

    fun setTapToClick(tap: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(tapToClick = tap))
    }

    fun setDragLock(dragLock: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(dragLock = dragLock))
    }

    fun setTwoFingerRightClick(enabled: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(twoFingerRightClick = enabled))
    }

    fun setHapticLevel(level: HapticLevel) {
        hapticHelper.click(level)
        preferencesManager.updateSettings(settings.value.copy(hapticLevel = level))
    }

    fun setGestureAction(type: GestureType, action: GestureAction) {
        hapticHelper.click(settings.value.hapticLevel)
        val updated = settings.value.gestures.toMutableMap()
        updated[type] = action
        preferencesManager.updateSettings(settings.value.copy(gestures = updated))
    }

    fun resetSettingsToDefault() {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        preferencesManager.resetToDefaults()
    }

    fun connectToBluetoothDevice(device: BluetoothDevice) {
        bluetoothManager.connectToDevice(device)
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }

    fun switchConnectionMode(mode: ConnectionMode, param: String = "") {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.setSavedConnectionMode(mode.name)
        when (mode) {
            ConnectionMode.BLUETOOTH_HID -> {
                networkManager.stopHeartbeat()
                bluetoothManager.setBluetoothMode()
            }
            ConnectionMode.LOCAL_WIFI -> {
                if (param.isNotBlank()) {
                    networkManager.pcIpAddress = param
                    preferencesManager.setSavedPcIp(param)
                }
                bluetoothManager.setWifiMode(networkManager.pcIpAddress)
                networkManager.startHeartbeat()
            }
            ConnectionMode.SIMULATION -> {
                networkManager.stopHeartbeat()
                bluetoothManager.setSimulationMode()
            }
        }
    }

    fun openUrlOnPc(url: String) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendOpenUrl(url)
        bluetoothManager.recordPacketSent("OPEN URL: $url")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Opened URL: $url"
        )
    }

    fun sendSystemCmdToPc(command: String, displayLabel: String) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendSystemCommand(command)
        bluetoothManager.recordPacketSent("SYSTEM CMD: $command")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = displayLabel
        )
    }

    fun sendWakeOnLan(macAddress: String) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        preferencesManager.setSavedMacAddress(macAddress)
        networkManager.sendWakeOnLan(macAddress)
        bluetoothManager.recordPacketSent("WOL PACKET: $macAddress")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "WOL Sent to $macAddress"
        )
    }

    fun sendClipboardTextToPc(text: String) {
        if (text.isBlank()) return
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendTextDirect(text)
        bluetoothManager.recordPacketSent("CLIPBOARD SENT (${text.length} chars)")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Pasted Clipboard to PC"
        )
    }

    // Google Sign-In & Authentication
    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(context)
            if (result.isSuccess) {
                _syncMessage.value = "Signed in as ${result.getOrNull()?.email}"
                onResult(true, null)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Sign-in failed"
                _syncMessage.value = "Sign-in error: $errorMsg"
                onResult(false, errorMsg)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
            _syncMessage.value = "Signed out from Google Account"
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Cloud Macros Management
    fun saveCloudMacro(macro: CloudMacro, onComplete: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onComplete(false, "Must be signed in to sync macros")
            return
        }
        viewModelScope.launch {
            val res = cloudMacrosRepo.saveMacro(user.uid, macro)
            if (res.isSuccess) {
                _syncMessage.value = "Saved macro \"${macro.label}\" to cloud"
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteCloudMacro(macroId: String, onComplete: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = cloudMacrosRepo.deleteMacro(user.uid, macroId)
            onComplete(res.isSuccess)
        }
    }

    fun executeCloudMacro(macro: CloudMacro) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        when (macro.actionType) {
            "URL" -> {
                if (macro.targetPayload.isNotBlank()) openUrlOnPc(macro.targetPayload)
            }
            "SYSTEM_CMD" -> {
                if (macro.targetPayload.isNotBlank()) sendSystemCmdToPc(macro.targetPayload, macro.label)
            }
            else -> {
                if (macro.shortcutKeys.isNotEmpty()) {
                    networkManager.sendKeyPacket(macro.shortcutKeys, "macro")
                    bluetoothManager.recordPacketSent("CLOUD MACRO: ${macro.label}")
                    _virtualScreen.value = _virtualScreen.value.copy(
                        recentGestureNotification = "Macro: ${macro.label}"
                    )
                }
            }
        }
    }

    // Cloud PC Profiles Management
    fun saveCurrentAsPcProfile(name: String, onComplete: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onComplete(false, "Must be signed in to sync PC profiles")
            return
        }
        val currentSettings = settings.value
        val profile = SavedPcProfile(
            name = name,
            ipAddress = networkManager.pcIpAddress,
            port = 8889,
            macAddress = preferencesManager.getSavedMacAddress(),
            pointerSensitivity = currentSettings.pointerSensitivity.toDouble(),
            accelerationMultiplier = currentSettings.accelerationMultiplier.toDouble(),
            accelerationProfile = currentSettings.accelerationProfile.name
        )
        viewModelScope.launch {
            val res = pcProfilesRepo.saveProfile(user.uid, profile)
            if (res.isSuccess) {
                _syncMessage.value = "Saved profile \"$name\" to cloud"
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun deletePcProfile(profileId: String, onComplete: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = pcProfilesRepo.deleteProfile(user.uid, profileId)
            onComplete(res.isSuccess)
        }
    }

    fun applyPcProfile(profile: SavedPcProfile) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        preferencesManager.setSavedPcIp(profile.ipAddress)
        networkManager.pcIpAddress = profile.ipAddress
        if (profile.macAddress.isNotBlank()) {
            preferencesManager.setSavedMacAddress(profile.macAddress)
        }
        val profileEnum = try {
            AccelerationProfile.valueOf(profile.accelerationProfile)
        } catch (e: Exception) {
            AccelerationProfile.DYNAMIC
        }
        val updatedSettings = settings.value.copy(
            pointerSensitivity = profile.pointerSensitivity.toFloat(),
            accelerationMultiplier = profile.accelerationMultiplier.toFloat(),
            accelerationProfile = profileEnum
        )
        preferencesManager.updateSettings(updatedSettings)
        bluetoothManager.setWifiMode(profile.ipAddress)
        _syncMessage.value = "Applied profile \"${profile.name}\""
    }

    // Feedback & Error Reporting
    fun submitFeedback(
        title: String,
        description: String,
        category: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val user = currentUser.value
        if (user == null) {
            onComplete(false, "Please sign in to submit feedback")
            return
        }
        val feedback = UserFeedback(
            userId = user.uid,
            title = title,
            description = description,
            category = category,
            appVersion = "5.0",
            userEmail = user.email ?: ""
        )
        viewModelScope.launch {
            val res = feedbackRepo.submitFeedback(feedback)
            if (res.isSuccess) {
                _syncMessage.value = "Thank you! Your feedback has been sent."
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun submitErrorReport(
        userComments: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val user = currentUser.value
        if (user == null) {
            onComplete(false, "Please sign in to submit a diagnostic report")
            return
        }
        val recentLogs = stats.value.recentLogs.takeLast(10)
        val lastError = if (recentLogs.isNotEmpty()) recentLogs.last() else "User triggered diagnostic snapshot"
        val report = ErrorReport(
            userId = user.uid,
            errorMessage = lastError.take(1000),
            connectionMode = stats.value.connectionMode.name,
            targetIp = networkManager.pcIpAddress,
            userComments = userComments.take(1000),
            recentLogs = recentLogs,
            appVersion = "5.0"
        )
        viewModelScope.launch {
            val res = errorReportsRepo.submitErrorReport(report)
            if (res.isSuccess) {
                _syncMessage.value = "Diagnostic report submitted successfully."
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    // Benchmark Controls
    fun runNetworkBenchmark(targetIp: String = networkManager.pcIpAddress) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        benchmarkEngine.runFullBenchmark(targetIp)
    }

    fun resetBenchmark() {
        benchmarkEngine.reset()
    }
}
