package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.SharedPreferences
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DiscoveredPerson
import com.example.data.PersonRepository
import java.util.*
import kotlin.math.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PersonRepository(db.personDao())

    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("insta_radar_prefs", Context.MODE_PRIVATE)

    // Own User Profile
    var ownInstagramId by mutableStateOf(sharedPrefs.getString("own_insta_id", "john_doe") ?: "john_doe")
        private set
    var ownDisplayName by mutableStateOf(sharedPrefs.getString("own_display_name", "John Doe") ?: "John Doe")
        private set
    var ownTagline by mutableStateOf(sharedPrefs.getString("own_tagline", "Hi there! Let's connect.") ?: "Hi there! Let's connect.")
        private set
    var ownAvatarIndex by mutableStateOf(sharedPrefs.getInt("own_avatar_index", 0))
        private set

    // BLE status
    var isScanning by mutableStateOf(false)
        private set
    var isAdvertising by mutableStateOf(false)
        private set
    var isBluetoothSupported by mutableStateOf(false)
        private set
    var isBluetoothEnabled by mutableStateOf(false)
        private set
    var logMessages = mutableStateListOf<String>()
        private set

    // UI and Simulation States
    var simulationMode by mutableStateOf(true)
        private set
    var userPositionX by mutableStateOf(0f)
        private set
    var userPositionY by mutableStateOf(0f)
        private set
    var radarAngle by mutableStateOf(0f)
        private set
    var selectedPeerId by mutableStateOf<String?>(null)

    // Combined active peer scan findings (Real + Simulated)
    private val _activePeers = MutableStateFlow<List<PeerState>>(emptyList())
    val activePeers: StateFlow<List<PeerState>> = _activePeers.asStateFlow()

    // History and Bookmarked database items
    val allHistory: StateFlow<List<DiscoveredPerson>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedHistory: StateFlow<List<DiscoveredPerson>> = repository.savedPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    // UUID for identifying our custom social BLE network (16-bit 0x1854 equivalent UUID)
    private val SOCIAL_SERVICE_UUID = UUID.fromString("00001854-0000-1000-8000-00805f9b34fb")

    // Coroutine Jobs
    private var tickerJob: Job? = null
    private var scanJob: Job? = null

    // Simulated static users with initial offset positions
    private val staticSimulatedUsers = listOf(
        SimulatedPeer("101", "travel_leo", "Leo Chen", "Exploring local hidden coffee shops ☕", -40f, -60f, 1),
        SimulatedPeer("102", "fitness_mia", "Mia Johnson", "Morning sprints & healthy clean life 🏃‍♀️", 70f, 50f, 2),
        SimulatedPeer("103", "code_builder", "Aris Thorne", "Building beautiful Android apps in Kotlin 🚀", -25f, 55f, 3),
        SimulatedPeer("104", "lens_story", "Sarah Miller", "Seeking interesting geometry and photo walk captures 📸", 55f, -65f, 4),
        SimulatedPeer("105", "art_wanderer", "Clara Sterling", "Watercolors, pastel skies & daily architecture sketching 🎨", -80f, 20f, 5),
        SimulatedPeer("106", "latte_art_pro", "Mark Vance", "Latte art geek & barista at Heartwood Coffee ☕", 20f, -80f, 0),
        SimulatedPeer("107", "musica_pulse", "Elena Rostova", "Street musician looking for visual artists 🎸", -55f, -30f, 1),
        SimulatedPeer("108", "pixel_craft", "Devon Cole", "UI Designer trying to find micro-interactions everywhere 📐", 85f, -15f, 2)
    )

    init {
        checkBluetoothStatus()
        startPeriodicTicker()
        updateScanList()
    }

    private fun checkBluetoothStatus() {
        val manager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (manager != null) {
            bluetoothAdapter = manager.adapter
            isBluetoothSupported = bluetoothAdapter != null
            isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
            if (isBluetoothSupported) {
                bleScanner = bluetoothAdapter?.bluetoothLeScanner
                bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            }
        } else {
            isBluetoothSupported = false
            isBluetoothEnabled = false
        }
        addLog("Bluetooth support checked: supported=$isBluetoothSupported, enabled=$isBluetoothEnabled")
    }

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logMessages.add(0, "[$time] $message")
        if (logMessages.size > 25) {
            logMessages.removeLast()
        }
    }

    // Update Profile Values
    fun updateProfile(id: String, name: String, taglineStr: String, avatarIdx: Int) {
        ownInstagramId = id.replace("@", "").trim()
        ownDisplayName = name.trim()
        ownTagline = taglineStr.trim()
        ownAvatarIndex = avatarIdx

        sharedPrefs.edit()
            .putString("own_insta_id", ownInstagramId)
            .putString("own_display_name", ownDisplayName)
            .putString("own_tagline", ownTagline)
            .putInt("own_avatar_index", ownAvatarIndex)
            .apply()

        addLog("Profile updated: @$ownInstagramId")

        // Restart advertiser if running to reflect profile name updates
        if (isAdvertising) {
            stopBleAdvertising()
            startBleAdvertising()
        }
    }

    // Start/Stop BLE scan
    fun toggleBleScanning(enable: Boolean) {
        if (enable) {
            startBleScanning()
        } else {
            stopBleScanning()
        }
    }

    private fun startBleScanning() {
        if (!isBluetoothSupported || !isBluetoothEnabled) {
            addLog("BLE Scan failed: Bluetooth is off or unsupported.")
            isScanning = false
            return
        }

        try {
            val scanner = bleScanner ?: bluetoothAdapter?.bluetoothLeScanner
            if (scanner == null) {
                addLog("BLE scanner object is null.")
                isScanning = false
                return
            }

            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SOCIAL_SERVICE_UUID))
                    .build()
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(filters, settings, scanCallback)
            isScanning = true
            addLog("BLE Scan started looking for Service UUID.")
        } catch (e: Exception) {
            addLog("BLE Scan Error: ${e.message}")
            isScanning = false
        }
    }

    private fun stopBleScanning() {
        try {
            val scanner = bleScanner ?: bluetoothAdapter?.bluetoothLeScanner
            scanner?.stopScan(scanCallback)
            isScanning = false
            addLog("BLE Scan stopped.")
        } catch (e: Exception) {
            addLog("Stop BLE Scan Error: ${e.message}")
        }
    }

    // BLE Advertise
    fun toggleBleAdvertising(enable: Boolean) {
        if (enable) {
            startBleAdvertising()
        } else {
            stopBleAdvertising()
        }
    }

    private fun startBleAdvertising() {
        if (!isBluetoothSupported || !isBluetoothEnabled) {
            addLog("BLE Broadcast failed: Bluetooth is off or unsupported.")
            isAdvertising = false
            return
        }

        try {
            val advertiser = bleAdvertiser ?: bluetoothAdapter?.bluetoothLeAdvertiser
            if (advertiser == null) {
                addLog("BLE advertiser object is null or unsupported by hardware.")
                isAdvertising = false
                return
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build()

            // Package the Instagram ID clearly as custom Service Data payload
            val cleanInsta = "@$ownInstagramId"
            val serviceData = cleanInsta.toByteArray(Charsets.UTF_8)

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(SOCIAL_SERVICE_UUID))
                .addServiceData(ParcelUuid(SOCIAL_SERVICE_UUID), serviceData)
                .build()

            advertiser.startAdvertising(settings, data, advertiseCallback)
            isAdvertising = true
            addLog("BLE Broadcasting active: advertising '@$ownInstagramId'")
        } catch (e: Exception) {
            addLog("BLE Broadcast Error: ${e.message}")
            isAdvertising = false
        }
    }

    private fun stopBleAdvertising() {
        try {
            val advertiser = bleAdvertiser ?: bluetoothAdapter?.bluetoothLeAdvertiser
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            addLog("BLE Broadcast stopped.")
        } catch (e: Exception) {
            addLog("Stop BLE Broadcast Error: ${e.message}")
        }
    }

    // BLE Callbacks
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            processBleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                processBleScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            addLog("BLE Scan Failed. Code: $errorCode")
            isScanning = false
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            addLog("BLE Broadcast successfully started!")
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            addLog("BLE Broadcast failed. Code: $errorCode")
            isAdvertising = false
        }
    }

    private fun processBleScanResult(result: ScanResult) {
        val record = result.scanRecord ?: return
        val rawData = record.getServiceData(ParcelUuid(SOCIAL_SERVICE_UUID))
        if (rawData != null) {
            val instagramIdStr = String(rawData, Charsets.UTF_8).trim()
            val rssi = result.rssi
            // Distance approximation in meters: distance = 10 ^ ((Measured Power - RSSI) / (10 * N))
            // Under typical conditions, Measured Power is around -59dBm at 1m, path loss index N=2
            val distance = 10.0.pow((-59 - rssi) / 20.0)

            handlePeerDiscovered(
                id = result.device.address ?: instagramIdStr,
                instagramId = instagramIdStr,
                displayName = "BLE Signal Node",
                tagline = "Met nearby via Bluetooth scan",
                rssi = rssi,
                distance = distance,
                avatarIndex = abs(instagramIdStr.hashCode()) % 6,
                isRealBle = true
            )
        }
    }

    // Toggle Simulation Grid
    fun toggleSimulationMode(enabled: Boolean) {
        simulationMode = enabled
        addLog("Virtual scan simulation mode: ${if (enabled) "ENABLED" else "DISABLED"}")
        updateScanList()
    }

    // Walk around simulator coordinates slider logic
    fun moveUserPosition(dx: Float, dy: Float) {
        userPositionX = (userPositionX + dx).coerceIn(-100f, 100f)
        userPositionY = (userPositionY + dy).coerceIn(-100f, 100f)
        updateScanList()
    }

    fun resetUserPosition() {
        userPositionX = 0f
        userPositionY = 0f
        updateScanList()
    }

    // Periodic tasks: Radar rotation sweep animation, simulated coordinates slight drift
    private fun startPeriodicTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                // Sonar rotation angle sweep helper
                radarAngle = (radarAngle + 8f) % 360f

                if (simulationMode) {
                    // Update lists periodically with slight coordinate drift on simulated peers to look lively
                    updateScanList()
                }
            }
        }
    }

    private fun updateScanList() {
        val currentPeers = mutableListOf<PeerState>()

        // 1. Maintain existing real BLE entries that are fresh (within 10 seconds)
        val oldRealPeers = _activePeers.value.filter { it.isRealBle && (System.currentTimeMillis() - it.lastSeenTimestamp) < 10000 }
        currentPeers.addAll(oldRealPeers)

        // 2. Compute live offsets and distances of simulated peer array
        if (simulationMode) {
            staticSimulatedUsers.forEach { simUser ->
                // Live coordinate drift
                val noiseX = (Math.sin(System.currentTimeMillis() / 8000.0 + simUser.id.hashCode()) * 2.5).toFloat()
                val noiseY = (Math.cos(System.currentTimeMillis() / 10000.0 + simUser.id.hashCode()) * 2.5).toFloat()

                // Relative coordinate grid relative to user simulator position
                val finalRelX = simUser.initX + noiseX - userPositionX
                val finalRelY = simUser.initY + noiseY - userPositionY

                // Distance in arbitrary social radar units
                val rawDistanceUnits = sqrt(finalRelX.pow(2) + finalRelY.pow(2))
                val metersDistance = max(1.2, rawDistanceUnits.toDouble() * 0.15) // Scale radar grid to realistic meter range

                // Synthesize a corresponding BLE RSSI
                val dLog = max(1.0, metersDistance)
                val rssi = (-40 - (20 * log10(dLog)) + (Math.sin(System.currentTimeMillis() / 2000.0 + simUser.initX) * 2)).toInt()

                // Only show people within active maximum boundary zone of 35 meters (radar detection boundary)
                if (metersDistance < 35.0) {
                    currentPeers.add(
                        PeerState(
                            id = simUser.id,
                            instagramId = "@${simUser.instagramId}",
                            displayName = simUser.displayName,
                            tagline = simUser.tagline,
                            offsetX = finalRelX,
                            offsetY = finalRelY,
                            rssi = rssi,
                            distance = metersDistance,
                            avatarColorIndex = simUser.avatarColorIndex,
                            lastSeenTimestamp = System.currentTimeMillis(),
                            isRealBle = false
                        )
                    )

                    // Automatic logger: automatically commit to History Room DB if extremely near (within 3 meters) for a brief instant
                    if (metersDistance < 4.0 && Math.random() < 0.05) {
                        viewModelScope.launch {
                            repository.insert(
                                DiscoveredPerson(
                                    instagramId = "@${simUser.instagramId}",
                                    displayName = simUser.displayName,
                                    tagline = simUser.tagline,
                                    rssi = rssi,
                                    estimatedDistance = metersDistance,
                                    avatarColorIndex = simUser.avatarColorIndex,
                                    notes = "Met virtually adjacent on local Social Radar grid"
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sort peers by closeness
        _activePeers.value = currentPeers.sortedBy { it.distance }
    }

    private fun handlePeerDiscovered(
        id: String,
        instagramId: String,
        displayName: String,
        tagline: String,
        rssi: Int,
        distance: Double,
        avatarIndex: Int,
        isRealBle: Boolean
    ) {
        val existingIndex = _activePeers.value.indexOfFirst { it.id == id }

        // Form current peer state
        // Derive approximate polar/cartesian offsets for real BLE using signal RSSI
        val angleRad = (abs(instagramId.hashCode()) % 360) * (PI / 180.0)
        val relativeDistMultiplier = distance * 5.0
        val computedX = (cos(angleRad) * relativeDistMultiplier).toFloat()
        val computedY = (sin(angleRad) * relativeDistMultiplier).toFloat()

        val item = PeerState(
            id = id,
            instagramId = instagramId,
            displayName = displayName,
            tagline = tagline,
            offsetX = computedX,
            offsetY = computedY,
            rssi = rssi,
            distance = distance,
            avatarColorIndex = avatarIndex,
            lastSeenTimestamp = System.currentTimeMillis(),
            isRealBle = isRealBle
        )

        val updated = _activePeers.value.toMutableList()
        if (existingIndex >= 0) {
            updated[existingIndex] = item
        } else {
            updated.add(item)
            addLog("📡 Discovered new Bluetooth peer nearby: $instagramId")
        }

        _activePeers.value = updated.sortedBy { it.distance }

        // Automatically persist in Room database
        viewModelScope.launch {
            repository.insert(
                DiscoveredPerson(
                    instagramId = instagramId,
                    displayName = if (displayName == "BLE Signal Node") instagramId.replace("@", "") else displayName,
                    tagline = tagline,
                    rssi = rssi,
                    estimatedDistance = distance,
                    avatarColorIndex = avatarIndex,
                    notes = if (isRealBle) "Discovered via physical Bluetooth Low Energy signature" else "Met on dynamic coordinates simulator"
                )
            )
        }
    }

    // Room persistence updates
    fun updateSaved(id: Int, saved: Boolean) {
        viewModelScope.launch {
            repository.updateSavedStatus(id, saved)
            addLog("Contact status modified: id=$id, saved=$saved")
        }
    }

    fun updateNotes(id: Int, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(id, notes)
        }
    }

    fun deleteContact(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            addLog("Contact deleted from history database")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopBleScanning()
        stopBleAdvertising()
        tickerJob?.cancel()
    }
}

// Represent state of a peer on active coordinates map
data class PeerState(
    val id: String,
    val instagramId: String,
    val displayName: String,
    val tagline: String,
    val offsetX: Float,
    val offsetY: Float,
    val rssi: Int,
    val distance: Double,
    val avatarColorIndex: Int,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isRealBle: Boolean = false
)

// Data class for initial simulation nodes
data class SimulatedPeer(
    val id: String,
    val instagramId: String,
    val displayName: String,
    val tagline: String,
    val initX: Float,
    val initY: Float,
    val avatarColorIndex: Int
)

// Snapshot state helpers
