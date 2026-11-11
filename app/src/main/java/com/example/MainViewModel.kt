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

/**
 * Primary navigation destinations in the GlidePad user interface.
 *
 * @property title Human-readable screen title for top app bars and navigation rails.
 */
enum class AppScreen(val title: String) {
    /** Primary interactive multitouch trackpad canvas. */
    TOUCHPAD("Touchpad"),

    /** Full PC keyboard simulator with function keys and modifier toggles. */
    KEYBOARD("Keyboard"),

    /** Deck of customizable productivity macros and hotkeys. */
    MACROS("Quick Macros"),

    /** Interactive virtual desktop preview simulating remote mouse cursor movement. */
    VIRTUAL_SCREEN("Live Preview"),

    /** Fine-grained trackpad tuning, gestures, and haptics preferences. */
    SETTINGS("Settings & Tuning"),

    /** Embedded ChromeOS in-app UDP receiver dashboard. */
    RECEIVER("Host Receiver")
}

/**
 * State snapshot representing the interactive Virtual PC Desktop simulation.
 *
 * @property cursorX Normalized horizontal cursor coordinate in `0.0..1.0`.
 * @property cursorY Normalized vertical cursor coordinate in `0.0..1.0`.
 * @property isLeftDown Whether left mouse button is depressed.
 * @property isRightDown Whether right mouse button is depressed.
 * @property clickRippleX Horizontal coordinate for visual click ripple animation, or null.
 * @property clickRippleY Vertical coordinate for visual click ripple animation, or null.
 * @property activeWindowIndex Index of the simulated frontmost OS window.
 * @property recentGestureNotification Optional toast/HUD banner describing the last gesture executed.
 */
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

/**
 * Central ViewModel orchestrating GlidePad's hardware, network, engine, and UI state flows.
 *
 * Bridges the low-level [BluetoothHidManager] and [NetworkStreamManager] transports with
 * the [TouchpadEngine] gesture recognizer, handles cloud profile persistence with Firebase Firestore,
 * and maintains UI state for all Compose screens.
 *
 * @param application Application context for accessing system services and DataStore preferences.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    /** Local persistent preferences storage manager. */
    val preferencesManager = PreferencesManager(application)

    /** Device vibrator and tactile haptics feedback helper. */
    val hapticHelper = HapticFeedbackHelper(application)

    /** Bluetooth HID Device profile controller. */
    val bluetoothManager = BluetoothHidManager(application)

    /** Wi-Fi UDP streaming client manager. */
    val networkManager = NetworkStreamManager(application)

    /** In-app local UDP server receiver for ChromeOS / Android tablet receivers. */
    val inAppReceiverManager = com.example.service.InAppReceiverManager(application)

    // Firebase & Cloud Repositories
    /** Google Sign-In and Firebase Auth session manager. */
    val authManager = AuthManager(application)

    /** Cloud Firestore repository for user-synchronized macros. */
    val cloudMacrosRepo = CloudMacrosRepository(application)

    /** Cloud Firestore repository for user PC sensitivity profiles. */
    val pcProfilesRepo = PcProfilesRepository(application)

    /** Cloud Firestore repository for bug and feature feedback submissions. */
    val feedbackRepo = FeedbackRepository(application)

    /** Cloud Firestore repository for diagnostic error reports. */
    val errorReportsRepo = ErrorReportsRepository(application)

    // Benchmark Engine
    /** Real-time latency, throughput, and packet jitter benchmark test engine. */
    val benchmarkEngine = com.example.engine.BenchmarkEngine(viewModelScope)

    /** State flow observing active network benchmark results. */
    val benchmarkState: StateFlow<com.example.engine.BenchmarkState> = benchmarkEngine.state

    /** State flow observing the currently authenticated Firebase user, or null. */
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    private val _cloudMacros = MutableStateFlow<List<CloudMacro>>(emptyList())

    /** Cloud-synchronized quick macros for the signed-in user. */
    val cloudMacros: StateFlow<List<CloudMacro>> = _cloudMacros.asStateFlow()

    private val _savedProfiles = MutableStateFlow<List<SavedPcProfile>>(emptyList())

    /** Cloud-synchronized PC sensitivity profiles for the signed-in user. */
    val savedProfiles: StateFlow<List<SavedPcProfile>> = _savedProfiles.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)

    /** Transient snackbar or status notification string from cloud sync operations. */
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var macroSyncJob: Job? = null
    private var profileSyncJob: Job? = null

    /** Flow exposing active touchpad sensitivity, acceleration, and gesture mapping settings. */
    val settings: StateFlow<TouchpadSettings> = preferencesManager.settings

    /** Flow exposing live diagnostic telemetry (RTT, packets sent, mode, connected PC name). */
    val stats: StateFlow<DiagnosticStats> = bluetoothManager.stats

    private val _currentScreen = MutableStateFlow(AppScreen.TOUCHPAD)

    /** Flow indicating the currently active screen destination in the app. */
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _virtualScreen = MutableStateFlow(VirtualScreenState())

    /** Flow exposing the state of the interactive virtual PC desktop preview. */
    val virtualScreen: StateFlow<VirtualScreenState> = _virtualScreen.asStateFlow()

    // Sticky modifier keys for keyboard
    private val _ctrlHeld = MutableStateFlow(false)

    /** Flow tracking whether the keyboard Ctrl modifier is held down. */
    val ctrlHeld: StateFlow<Boolean> = _ctrlHeld.asStateFlow()

    private val _shiftHeld = MutableStateFlow(false)

    /** Flow tracking whether the keyboard Shift modifier is held down. */
    val shiftHeld: StateFlow<Boolean> = _shiftHeld.asStateFlow()

    private val _altHeld = MutableStateFlow(false)

    /** Flow tracking whether the keyboard Alt modifier is held down. */
    val altHeld: StateFlow<Boolean> = _altHeld.asStateFlow()

    private val _winHeld = MutableStateFlow(false)

    /** Flow tracking whether the keyboard Windows / Super modifier is held down. */
    val winHeld: StateFlow<Boolean> = _winHeld.asStateFlow()

    /** Built-in preconfigured deck of common OS and productivity macros. */
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

    /** Low-level touchpad acceleration and multitouch gesture processing engine. */
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
                            val ipChanged = networkManager.pcIpAddress != state.ip
                            if (ipChanged) {
                                networkManager.pcIpAddress = state.ip
                            }
                            val savedIp = preferencesManager.getSavedPcIp()
                            if (savedIp != state.ip) {
                                preferencesManager.setSavedPcIp(state.ip)
                            }
                            if (preferencesManager.getSavedConnectionMode() != ConnectionMode.LOCAL_WIFI.name) {
                                preferencesManager.setSavedConnectionMode(ConnectionMode.LOCAL_WIFI.name)
                            }
                            if (ipChanged || !stats.value.connectedDeviceName.contains(state.ip)) {
                                bluetoothManager.setWifiMode(state.ip, state.serverName)
                            }
                            if (!networkManager.isHeartbeatActive()) {
                                networkManager.startHeartbeat()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            networkManager.targetPcIp.collect { newIp ->
                if (newIp.isNotBlank() && stats.value.connectionMode == ConnectionMode.LOCAL_WIFI) {
                    val savedIp = preferencesManager.getSavedPcIp()
                    if (savedIp != newIp) {
                        preferencesManager.setSavedPcIp(newIp)
                    }
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

    /**
     * Navigates to a specific [AppScreen] destination in the application UI.
     *
     * @param screen Target screen destination.
     */
    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    /**
     * Dispatches relative mouse pointer deltas, scroll wheel events, and button states
     * through the active connection transport (Bluetooth HID, Wi-Fi UDP, or simulation).
     *
     * @param buttons Mouse button bitmask (1=left, 2=right, 4=middle).
     * @param dx Relative horizontal movement in counts.
     * @param dy Relative vertical movement in counts.
     * @param wheel Scroll wheel delta.
     */
    fun sendMouse(buttons: Byte, dx: Int, dy: Int, wheel: Int) {
        when (stats.value.connectionMode) {
            ConnectionMode.BLUETOOTH_HID -> {
                bluetoothManager.sendMouseReport(buttons, dx, dy, wheel)
            }
            ConnectionMode.LOCAL_WIFI -> {
                networkManager.sendMousePacket(dx, dy, buttons, wheel)
                // Log non-zero clicks or scroll actions to prevent 120fps UI state thrashing during pure cursor movements
                if (buttons.toInt() != 0 || wheel != 0) {
                    bluetoothManager.recordPacketSent("WIFI MOUSE (btn=$buttons, dx=$dx, dy=$dy, whl=$wheel)")
                }
            }
            ConnectionMode.SIMULATION -> {
                if (buttons.toInt() != 0 || wheel != 0) {
                    bluetoothManager.recordPacketSent("SIM MOUSE (btn=$buttons, dx=$dx, dy=$dy)")
                }
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

    /**
     * Executes a recognized multi-finger desktop OS action, triggering haptics, HUD notification,
     * and transmitting corresponding keystrokes or mouse reports.
     *
     * @param action High-level gesture action to execute.
     */
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

    /**
     * Toggles the sticky state of a modifier key (Ctrl, Shift, Alt, or Win).
     *
     * @param modifier Key name string ("Ctrl", "Shift", "Alt", "Win").
     */
    fun toggleModifier(modifier: String) {
        hapticHelper.click(settings.value.hapticLevel)
        when (modifier) {
            "Ctrl" -> _ctrlHeld.value = !_ctrlHeld.value
            "Shift" -> _shiftHeld.value = !_shiftHeld.value
            "Alt" -> _altHeld.value = !_altHeld.value
            "Win" -> _winHeld.value = !_winHeld.value
        }
    }

    /**
     * Sends a keyboard key press with current sticky modifier masks to the host.
     *
     * @param keyLabel Printable or descriptive key label for display.
     * @param hidCode USB HID Usage code for keyboard report.
     */
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

    /**
     * Transmits a string directly to the host PC for automated typing.
     *
     * @param text Text string to type on the target computer.
     */
    fun sendTextDirect(text: String) {
        if (text.isBlank()) return
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendKeyPacket(listOf(text), "text")
        bluetoothManager.recordPacketSent("SEND TEXT: \"$text\"")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Sent: \"$text\""
        )
    }

    /**
     * Executes a predefined macro shortcut item.
     *
     * @param macro Macro descriptor item containing key chords or actions.
     */
    fun executeMacro(macro: MacroItem) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendKeyPacket(macro.shortcutKeys, "macro")
        bluetoothManager.recordPacketSent("MACRO: ${macro.label} (${macro.description})")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Macro: ${macro.label}"
        )
    }

    /**
     * Updates the cursor pointer tracking sensitivity.
     *
     * @param value Sensitivity scalar.
     */
    fun setPointerSensitivity(value: Float) {
        preferencesManager.updateSettings(settings.value.copy(pointerSensitivity = value))
    }

    /**
     * Updates the active ballistic acceleration profile curve.
     *
     * @param profile Ballistic profile (Linear, Smooth, Dynamic, Gaming).
     */
    fun setAccelerationProfile(profile: AccelerationProfile) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(accelerationProfile = profile))
    }

    /**
     * Updates the cursor acceleration velocity multiplier.
     *
     * @param multiplier Velocity exponent multiplier factor.
     */
    fun setAccelerationMultiplier(multiplier: Float) {
        preferencesManager.updateSettings(settings.value.copy(accelerationMultiplier = multiplier))
    }

    /**
     * Updates the two-finger scroll speed sensitivity.
     *
     * @param value Scroll sensitivity scalar.
     */
    fun setScrollSensitivity(value: Float) {
        preferencesManager.updateSettings(settings.value.copy(scrollSensitivity = value))
    }

    /**
     * Sets whether vertical scroll direction is natural (content follows fingers) or inverted.
     *
     * @param natural True for natural scroll; false for traditional wheel direction.
     */
    fun setNaturalScroll(natural: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(naturalScroll = natural))
    }

    /**
     * Toggles smooth kinetic inertia momentum after two-finger flick scrolling.
     *
     * @param inertia True to enable kinetic inertia decay.
     */
    fun setSmoothInertia(inertia: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(smoothInertia = inertia))
    }

    /**
     * Toggles single-finger tap-to-click gesture recognition.
     *
     * @param tap True to enable tap-to-click.
     */
    fun setTapToClick(tap: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(tapToClick = tap))
    }

    /**
     * Toggles drag lock mode for double-tap drag gestures.
     *
     * @param dragLock True to keep drag active until next tap.
     */
    fun setDragLock(dragLock: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(dragLock = dragLock))
    }

    /**
     * Toggles secondary right click trigger via two-finger tap.
     *
     * @param enabled True to trigger right click on two-finger tap.
     */
    fun setTwoFingerRightClick(enabled: Boolean) {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.updateSettings(settings.value.copy(twoFingerRightClick = enabled))
    }

    /**
     * Configures the tactile haptic feedback vibration intensity.
     *
     * @param level Tactile feedback intensity level.
     */
    fun setHapticLevel(level: HapticLevel) {
        hapticHelper.click(level)
        preferencesManager.updateSettings(settings.value.copy(hapticLevel = level))
    }

    /**
     * Maps a specific multi-finger gesture type to an operating system action.
     *
     * @param type Touchpad gesture pattern.
     * @param action Target desktop action to execute.
     */
    fun setGestureAction(type: GestureType, action: GestureAction) {
        hapticHelper.click(settings.value.hapticLevel)
        val updated = settings.value.gestures.toMutableMap()
        updated[type] = action
        preferencesManager.updateSettings(settings.value.copy(gestures = updated))
    }

    /**
     * Resets all trackpad, acceleration, and gesture settings to factory defaults.
     */
    fun resetSettingsToDefault() {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        preferencesManager.resetToDefaults()
    }

    /**
     * Connects to a bonded target Bluetooth peripheral device.
     *
     * @param device Target Bluetooth host.
     */
    fun connectToBluetoothDevice(device: BluetoothDevice) {
        bluetoothManager.connectToDevice(device)
    }

    /**
     * Disconnects from the current Bluetooth host.
     */
    fun disconnect() {
        bluetoothManager.disconnect()
    }

    /**
     * Switches the active connection transport mode (Bluetooth HID, Wi-Fi UDP, or Simulation).
     *
     * @param mode Target connection mode.
     * @param param Optional IP address parameter for Wi-Fi mode.
     */
    fun switchConnectionMode(mode: ConnectionMode, param: String = "") {
        hapticHelper.click(settings.value.hapticLevel)
        preferencesManager.setSavedConnectionMode(mode.name)
        when (mode) {
            ConnectionMode.BLUETOOTH_HID -> {
                networkManager.stopHeartbeat()
                bluetoothManager.setBluetoothMode()
            }
            ConnectionMode.LOCAL_WIFI -> {
                val targetIp = param.trim().ifBlank { networkManager.pcIpAddress }
                if (targetIp.isNotBlank()) {
                    networkManager.pcIpAddress = targetIp
                    preferencesManager.setSavedPcIp(targetIp)
                }
                val currentTest = networkManager.testState.value
                val serverName = if (currentTest is com.example.service.WifiConnectionTestState.Success && currentTest.ip == targetIp) {
                    currentTest.serverName
                } else {
                    ""
                }
                bluetoothManager.setWifiMode(networkManager.pcIpAddress, serverName)
                networkManager.startHeartbeat()
            }
            ConnectionMode.SIMULATION -> {
                networkManager.stopHeartbeat()
                bluetoothManager.setSimulationMode()
            }
        }
    }

    /**
     * Instructs the connected PC receiver to launch its default web browser to [url].
     *
     * @param url Web URL to open on the PC host.
     */
    fun openUrlOnPc(url: String) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendOpenUrl(url)
        bluetoothManager.recordPacketSent("OPEN URL: $url")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Opened URL: $url"
        )
    }

    /**
     * Dispatches a named system command string to the PC receiver (e.g. lock, sleep).
     *
     * @param command Command keyword recognized by the receiver script.
     * @param displayLabel Human-readable notification banner text.
     */
    fun sendSystemCmdToPc(command: String, displayLabel: String) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendSystemCommand(command)
        bluetoothManager.recordPacketSent("SYSTEM CMD: $command")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = displayLabel
        )
    }

    /**
     * Dispatches a standard Wake-on-LAN magic packet and updates persistent saved MAC in preferences.
     *
     * @param macAddress Target PC network card MAC address string.
     */
    fun sendWakeOnLan(macAddress: String) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        preferencesManager.setSavedMacAddress(macAddress)
        networkManager.sendWakeOnLan(macAddress)
        bluetoothManager.recordPacketSent("WOL PACKET: $macAddress")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "WOL Sent to $macAddress"
        )
    }

    /**
     * Transmits clipboard text directly to the PC receiver for instant remote typing.
     *
     * @param text Text string to send.
     */
    fun sendClipboardTextToPc(text: String) {
        if (text.isBlank()) return
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        networkManager.sendTextDirect(text)
        bluetoothManager.recordPacketSent("CLIPBOARD SENT (${text.length} chars)")
        _virtualScreen.value = _virtualScreen.value.copy(
            recentGestureNotification = "Pasted Clipboard to PC"
        )
    }

    /**
     * Initiates Google Sign-In using Android Credential Manager and Firebase Authentication.
     *
     * @param context Activity context for launching Credential Manager bottom sheet.
     * @param onResult Completion callback returning success boolean and optional error message.
     */
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

    /**
     * Signs out of the active Google and Firebase Authentication sessions.
     *
     * @param context Context for Credential Manager clear request.
     */
    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
            _syncMessage.value = "Signed out from Google Account"
        }
    }

    /**
     * Clears any active cloud synchronization message notification banner.
     */
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Saves or updates a custom macro definition in Cloud Firestore for the signed-in user.
     *
     * @param macro Cloud macro descriptor.
     * @param onComplete Callback returning success flag and optional error string.
     */
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

    /**
     * Deletes a cloud-stored macro definition from Cloud Firestore.
     *
     * @param macroId Unique identifier of the macro document.
     * @param onComplete Callback with success boolean.
     */
    fun deleteCloudMacro(macroId: String, onComplete: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = cloudMacrosRepo.deleteMacro(user.uid, macroId)
            onComplete(res.isSuccess)
        }
    }

    /**
     * Executes a cloud-synced macro, dispatching either a URL launch, system command, or hotkey sequence.
     *
     * @param macro Cloud macro instance to run.
     */
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

    /**
     * Snapshots the active PC connection and touchpad sensitivity settings into a named Cloud Firestore profile.
     *
     * @param name Descriptive profile name (e.g. "Workstation Desktop", "MacBook Pro").
     * @param onComplete Callback with success flag and optional error message.
     */
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

    /**
     * Deletes a saved PC profile from Cloud Firestore.
     *
     * @param profileId Unique document ID of the profile.
     * @param onComplete Callback with success boolean.
     */
    fun deletePcProfile(profileId: String, onComplete: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = pcProfilesRepo.deleteProfile(user.uid, profileId)
            onComplete(res.isSuccess)
        }
    }

    /**
     * Applies a saved cloud PC profile's IP, MAC address, sensitivity, and acceleration curve to the active session.
     *
     * @param profile PC profile to apply.
     */
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

    /**
     * Submits user feedback (bug report, feature request, or general commentary) to Cloud Firestore.
     *
     * @param title Brief summary title.
     * @param description Detailed issue description or request.
     * @param category Classification category ("Bug", "Feature Request", "Question", "Praise").
     * @param onComplete Callback with success flag and optional error message.
     */
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

    /**
     * Submits an automated diagnostic snapshot and recent telemetry log excerpt to Cloud Firestore.
     *
     * @param userComments Optional user explanation accompanying the report.
     * @param onComplete Callback with success flag and optional error message.
     */
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

    /**
     * Executes the comprehensive network benchmark suite (latency, packet jitter, and throughput).
     *
     * @param targetIp Host IP address to benchmark against.
     */
    fun runNetworkBenchmark(targetIp: String = networkManager.pcIpAddress) {
        hapticHelper.gestureTriggered(settings.value.hapticLevel)
        benchmarkEngine.runFullBenchmark(targetIp)
    }

    /**
     * Resets active benchmark test state back to idle.
     */
    fun resetBenchmark() {
        benchmarkEngine.reset()
    }

    /**
     * Toggles the local ChromeOS in-app UDP receiver server state.
     *
     * @param port UDP port number to listen on (default 8089).
     * @return True if receiver was started, false if stopped or failed to bind.
     */
    fun toggleInAppReceiver(port: Int = 8089): Boolean {
        hapticHelper.click(settings.value.hapticLevel)
        return if (inAppReceiverManager.isRunning.value) {
            inAppReceiverManager.stop()
            false
        } else {
            inAppReceiverManager.start(port)
        }
    }

    /**
     * Resets packet and throughput telemetry statistics on the in-app receiver.
     */
    fun resetReceiverStats() {
        inAppReceiverManager.resetStats()
    }

    override fun onCleared() {
        super.onCleared()
        inAppReceiverManager.stop()
    }
}
