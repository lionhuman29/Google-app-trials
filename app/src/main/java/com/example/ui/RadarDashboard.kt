package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiscoveredPerson
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Beautiful Gradient lists for avatars
val AvatarGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))), // Purple-Blue
    Brush.linearGradient(listOf(Color(0xFFFEE140), Color(0xFFFA709A))), // Yellow-Pink
    Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF))), // Peach-Pink
    Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))), // Neon Cyan
    Brush.linearGradient(listOf(Color(0xFF43E97B), Color(0xFF38F9D7))), // Mint Green
    Brush.linearGradient(listOf(Color(0xFFFA8072), Color(0xFF3F51B5))), // Salmon-Indigo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDashboardScreen(viewModel: RadarViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Radar, 1: History/Saved, 2: Profile/My Beacon

    // Active screen layout wrapped with WindowInsets.safeDrawing
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFFDF8F6), // Premium light background
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0xFFCAC4D0), thickness = 1.dp)
                NavigationBar(
                    containerColor = Color(0xFFF3EDF7),
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Radar, contentDescription = "Active Sonar Scan") },
                        label = { Text("Radar Scan") },
                        modifier = Modifier.testTag("radar_tab"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF21005D),
                            selectedTextColor = Color(0xFF21005D),
                            indicatorColor = Color(0xFFEADDFF),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Saved connections") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("saved_tab"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF21005D),
                            selectedTextColor = Color(0xFF21005D),
                            indicatorColor = Color(0xFFEADDFF),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Contactless, contentDescription = "My social beacon") },
                        label = { Text("My Beacon") },
                        modifier = Modifier.testTag("profile_tab"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF21005D),
                            selectedTextColor = Color(0xFF21005D),
                            indicatorColor = Color(0xFFEADDFF),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Elegant top branding bar
            HeaderView(viewModel)

            HorizontalDivider(color = Color(0xFFCAC4D0), thickness = 1.dp)

            // Dynamic Tab selector
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> RadarTabContent(viewModel)
                    1 -> SavedTabContent(viewModel)
                    2 -> ProfileTabContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun HeaderView(viewModel: RadarViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WifiTethering,
                contentDescription = "Social Icon",
                tint = Color(0xFF6750A4),
                modifier = Modifier
                    .size(28.dp)
                    .animateContentSize()
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "InstaRadar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F),
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Social Proximity Scanner",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF49454F)
                )
            }
        }

        // Active transmitter dynamic status pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (viewModel.isAdvertising || viewModel.isScanning) Color(0xFFEADDFF) else Color(0xFFF2F0F4),
            border = BorderStroke(
                1.dp,
                if (viewModel.isAdvertising || viewModel.isScanning) Color(0xFF6750A4) else Color(0xFFCAC4D0)
            ),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isAdvertising || viewModel.isScanning) Color(0xFF6750A4) else Color(0xFF49454F))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isAdvertising || viewModel.isScanning) "SCANNING" else "STANDBY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isAdvertising || viewModel.isScanning) Color(0xFF21005D) else Color(0xFF49454F),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun RadarTabContent(viewModel: RadarViewModel) {
    val activePeers by viewModel.activePeers.collectAsState()
    val listState = remember { mutableStateOf<String?>(null) }
    val ownInsta = viewModel.ownInstagramId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // RADAR CANVAS ELEMENT
        Box(
            modifier = Modifier
                .size(310.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFCAC4D0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Sonar Scan Lines, Grid lines, and sweep
            RadarSweepCanvas(viewModel, activePeers)

            // Center Ring representing own position matching the HTML (bg-[#EADDFF] with text-[#21005D])
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEADDFF))
                    .border(1.5.dp, Color(0xFF6750A4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = "My Position",
                    tint = Color(0xFF21005D),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // MOVEMENT SIMULATION CONTROLLER (TAPPING THIS WALKS IN CORRESPONDING CARDINAL DIRECTIONS)
        if (viewModel.simulationMode) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VIRTUAL NAVIGATION BOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Coord X: ${viewModel.userPositionX.toInt()}m",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Coord Y: ${viewModel.userPositionY.toInt()}m",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Navigation D-Pad Layout
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.moveUserPosition(0f, 15f) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sim_move_up")
                                    .background(Color(0xFFEADDFF), CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color(0xFF21005D), modifier = Modifier.size(16.dp))
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.moveUserPosition(-15f, 0f) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("sim_move_left")
                                        .background(Color(0xFFEADDFF), CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", tint = Color(0xFF21005D), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(36.dp))
                                IconButton(
                                    onClick = { viewModel.moveUserPosition(15f, 0f) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("sim_move_right")
                                        .background(Color(0xFFEADDFF), CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", tint = Color(0xFF21005D), modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = { viewModel.moveUserPosition(0f, -15f) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sim_move_down")
                                    .background(Color(0xFFEADDFF), CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color(0xFF21005D), modifier = Modifier.size(16.dp))
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetUserPosition() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF49454F)),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // DISCOVERED USERS LIST INDEX HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEIGHBORS DETECTED (${activePeers.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.2.sp
            )

            // Simulation grid toggle switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Simulator Data",
                    fontSize = 11.sp,
                    color = if (viewModel.simulationMode) Color(0xFF6750A4) else Color(0xFF49454F)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = viewModel.simulationMode,
                    onCheckedChange = { viewModel.toggleSimulationMode(it) },
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("simulation_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6750A4),
                        checkedTrackColor = Color(0xFFEADDFF),
                        uncheckedThumbColor = Color(0xFF49454F),
                        uncheckedTrackColor = Color(0xFFF2F0F4)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PEERS VERTICAL FEED LIST
        if (activePeers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Radar Idle",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No discoverable signals detected nearby.\nTurn on virtual simulation mode above to populate.",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activePeers, key = { it.id }) { peer ->
                    val isSelected = viewModel.selectedPeerId == peer.id
                    PeerListItem(
                        peer = peer,
                        isSelected = isSelected,
                        onSelect = { viewModel.selectedPeerId = if (isSelected) null else peer.id }
                    )
                }
            }
        }
    }
}

// RADAR SWEEP ANIMATIVE GRID DRAWING
@Composable
fun RadarSweepCanvas(viewModel: RadarViewModel, peers: List<PeerState>) {
    val radarAngle = viewModel.radarAngle

    // Animate subtle expansion waves periodically
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseWaveFraction by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(peers) {
                detectTapGestures { offset ->
                    // Identify closest peer coordinate to click offset
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val xRel = offset.x - center.x
                    val yRel = offset.y - center.y

                    var bestPeer: PeerState? = null
                    var bestDist = 35f // Minimum click tolerance threshold

                    peers.forEach { peer ->
                        // Grid coordinate maps: center + (offsetX, offsetY)
                        // Scale factors mapped to 310x310 canvas size
                        // Coordinate calculations based on bounding box
                        val peerX = center.x + (peer.offsetX * (size.width / 240f))
                        val peerY = center.y + (-peer.offsetY * (size.height / 240f)) // Canvas y invert coordinate

                        val dist = Math.hypot((offset.x - peerX).toDouble(), (offset.y - peerY).toDouble())
                        if (dist < bestDist) {
                            bestDist = dist.toFloat()
                            bestPeer = peer
                        }
                    }

                    if (bestPeer != null) {
                        viewModel.selectedPeerId = bestPeer!!.id
                    } else {
                        viewModel.selectedPeerId = null
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f

        // Draw radial concentric guide rings representing meter distance
        val ringCount = 3
        for (i in 1..ringCount) {
            val r = maxRadius * (i.toFloat() / ringCount)
            drawCircle(
                color = Color(0xFF6750A4).copy(alpha = 0.25f),
                radius = r,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // Pulsing echo radar ripple expander (Professional Polish soft purple)
        drawCircle(
            color = Color(0xFF6750A4).copy(alpha = 0.15f * (1.0f - pulseWaveFraction)),
            radius = maxRadius * pulseWaveFraction,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw cross lines (Y & X compass guides) in light slate
        drawLine(
            color = Color(0xFFCAC4D0).copy(alpha = 0.6f),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFFCAC4D0).copy(alpha = 0.6f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1.dp.toPx()
        )

        // Draw compass text labels
        // N, S, W, E
        // Draw dynamically
        // We'll skip complex font calculations for speed and simplicity using circles/guides

        // Draw sweep line representation
        val angleRad = Math.toRadians(radarAngle.toDouble())
        val endX = center.x + (maxRadius * cos(angleRad)).toFloat()
        val endY = center.y + (maxRadius * sin(angleRad)).toFloat()

        // Draw scanning sweep gradient trail in professional voice
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF6750A4).copy(alpha = 0.7f), Color(0xFF6750A4).copy(alpha = 0.05f)),
                start = center,
                end = Offset(endX, endY)
            ),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx()
        )

        // Plot nearby peer markers (Dots!)
        peers.forEach { peer ->
            // Scale and map offset
            val peerX = center.x + (peer.offsetX * (size.width / 240f))
            val peerY = center.y + (-peer.offsetY * (size.height / 240f))

            val isSelectedItem = viewModel.selectedPeerId == peer.id

            // Select color: Virtual Simulator vs Real Bluetooth beacon signature
            // Real BLE matches Instagram design gradient start/end color, Sim matches active Purple
            val dotColor = if (peer.isRealBle) Color(0xFFEE2A7B) else Color(0xFF6750A4)

            // Glowing core dot
            drawCircle(
                color = dotColor,
                radius = if (isSelectedItem) 7.dp.toPx() else 4.dp.toPx(),
                center = Offset(peerX, peerY)
            )

            // Glowing ring surrounding the dot marker
            drawCircle(
                color = dotColor.copy(alpha = 0.35f),
                radius = if (isSelectedItem) 14.dp.toPx() else 8.dp.toPx(),
                center = Offset(peerX, peerY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // If selected, draw expanding selection beacon
            if (isSelectedItem) {
                drawCircle(
                    color = dotColor.copy(alpha = 0.2f),
                    radius = 22.dp.toPx(),
                    center = Offset(peerX, peerY),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )
                )
            }
        }
    }
}

@Composable
fun PeerListItem(
    peer: PeerState,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val gradientBrush = AvatarGradients[peer.avatarColorIndex % AvatarGradients.size]

    // Surface element wrapped with smooth expanding transition boundaries (Light card with premium border)
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFF3EDF7) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)
        ),
        shadowElevation =if (isSelected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Profile dynamic Gradient Avatar
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(gradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peer.displayName.take(1).uppercase(Locale.getDefault()),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = peer.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1B1F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Signal visual tag (Real Bluetooth vs Virtual Simulation)
                            if (peer.isRealBle) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "BLE",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = peer.instagramId,
                            fontSize = 13.sp,
                            color = Color(0xFF6750A4),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Closeness RSSI and estimated distances indicators
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "%.1f m".format(peer.distance),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "${peer.rssi} dBm",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tagline info
            if (peer.tagline.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = peer.tagline,
                    fontSize = 12.sp,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Expanded utilities (Opens direct Instagram launches or custom profile link browser)
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFCAC4D0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered just now",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )

                    Button(
                        onClick = {
                            val cleanUser = peer.instagramId.replace("@", "").trim()
                            val webUri = "https://instagram.com/$cleanUser"
                            val appUri = "http://instagram.com/_u/$cleanUser"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUri)).apply {
                                setPackage("com.instagram.android")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback browser redirect
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                                context.startActivity(browserIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("open_instagram_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Instagram link launcher",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SavedTabContent(viewModel: RadarViewModel) {
    val history by viewModel.allHistory.collectAsState()
    val savedOnly by viewModel.savedHistory.collectAsState()
    var filterSaved by remember { mutableStateOf(false) }

    val activeList = if (filterSaved) savedOnly else history

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toggle view types (Starred vs All historical log)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOGGED HISTORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.2.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Starred Only",
                    fontSize = 11.sp,
                    color = if (filterSaved) Color(0xFF6750A4) else Color(0xFF49454F)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = filterSaved,
                    onCheckedChange = { filterSaved = it },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6750A4),
                        checkedTrackColor = Color(0xFFEADDFF),
                        uncheckedThumbColor = Color(0xFF49454F),
                        uncheckedTrackColor = Color(0xFFF2F0F4)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeList.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (filterSaved) Icons.Default.StarOutline else Icons.Default.History,
                        contentDescription = "Empty History",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (filterSaved) "No starred connections yet.\nFind someone in the Radar panel and star them!" else "Radar log is empty.\nScan or navigate around to discover nodes.",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList, key = { it.id }) { person ->
                    SavedPersonListItem(person, viewModel)
                }
            }
        }
    }
}

@Composable
fun SavedPersonListItem(
    person: DiscoveredPerson,
    viewModel: RadarViewModel
) {
    val context = LocalContext.current
    var isEditingNotes by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf(person.notes) }
    val gradientBrush = AvatarGradients[person.avatarColorIndex % AvatarGradients.size]

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(gradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = person.displayName.take(1).uppercase(Locale.getDefault()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = person.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1B1F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = person.instagramId,
                            fontSize = 12.sp,
                            color = Color(0xFF6750A4),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val clean = person.instagramId.replace("@", "").trim()
                                val webUri = "https://instagram.com/$clean"
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                                context.startActivity(browserIntent)
                            }
                        )
                    }
                }

                // Bookmark and Delete logs actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.updateSaved(person.id, !person.isSaved) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (person.isSaved) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favorite contact",
                            tint = if (person.isSaved) Color(0xFFF9CE34) else Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.deleteContact(person.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete contact",
                            tint = Color(0xFFB3261E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tagline info
            if (person.tagline.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = person.tagline,
                    fontSize = 11.sp,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFCAC4D0), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // MET AT / NOTE TAKING UTILITY CARD
            if (isEditingNotes) {
                Column {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Add custom notes (location, context, etc.)", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.updateNotes(person.id, notesText)
                            isEditingNotes = false
                        })
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditingNotes = false }) {
                            Text("Cancel", color = Color(0xFF49454F), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateNotes(person.id, notesText)
                                isEditingNotes = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEADDFF),
                                contentColor = Color(0xFF21005D)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = if (person.notes.isBlank()) "No notes added yet..." else person.notes,
                            fontSize = 12.sp,
                            color = if (person.notes.isBlank()) Color(0xFF49454F).copy(alpha = 0.6f) else Color(0xFF1C1B1F),
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Last logged: ${SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(person.lastSeenTimestamp))}",
                            fontSize = 9.sp,
                            color = Color(0xFF49454F)
                        )
                    }

                    IconButton(
                        onClick = { isEditingNotes = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Edit Notes",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTabContent(viewModel: RadarViewModel) {
    var instaInput by remember { mutableStateOf("@" + viewModel.ownInstagramId) }
    var nameInput by remember { mutableStateOf(viewModel.ownDisplayName) }
    var taglineInput by remember { mutableStateOf(viewModel.ownTagline) }
    var selectedAvatarIdx by remember { mutableStateOf(viewModel.ownAvatarIndex) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RADAR ADV-BROADCASTING ACTIVE PILOT
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    if (viewModel.isAdvertising) Color(0xFF6750A4) else Color(0xFFCAC4D0)
                ),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SOLAR ADV-BROADCASTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enabling broadcasting will package your Instagram ID and transmit it locally using BLE. Nearby users running InstaRadar can scan and discover your social card automatically.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Transmitting Beacon",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = if (viewModel.isAdvertising) "Advertising: ${viewModel.ownInstagramId}" else "Beacon is dormant",
                                fontSize = 11.sp,
                                color = if (viewModel.isAdvertising) Color(0xFF2E7D32) else Color(0xFF49454F)
                            )
                        }

                        Switch(
                            checked = viewModel.isAdvertising,
                            onCheckedChange = { viewModel.toggleBleAdvertising(it) },
                            modifier = Modifier.testTag("ble_advertise_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF6750A4),
                                checkedTrackColor = Color(0xFFEADDFF),
                                uncheckedThumbColor = Color(0xFF49454F),
                                uncheckedTrackColor = Color(0xFFF2F0F4)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scan hardware status warning if system doesn't fully support
                    if (!viewModel.isBluetoothSupported) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFB3261E).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ BLE Transmission Hardware unsupported by this device (simulate mode recommended to explore details).",
                                color = Color(0xFFB3261E),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // PROFILE EDITING UTILITY SCREEN CARD
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BEACON VISUAL CARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Avatar Color Selections Matrix
                    Text(
                        text = "Choose Beacon Color Gradient:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AvatarGradients.forEachIndexed { idx, brush ->
                            val isSelected = selectedAvatarIdx == idx
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF6750A4) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarIdx = idx }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = instaInput,
                        onValueChange = { input ->
                            val cleaned = if (input.startsWith("@")) input else "@" + input.replace("@", "")
                            instaInput = cleaned
                        },
                        label = { Text("Instagram ID") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = "Insta ID Prefix", tint = Color(0xFF6750A4)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name / Alias") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = taglineInput,
                        onValueChange = { taglineInput = it },
                        label = { Text("Custom Tagline / Bio") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.updateProfile(instaInput, nameInput, taglineInput, selectedAvatarIdx)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            Toast.makeText(context, "Social beacon profile synchronized!", Toast.LENGTH_SHORT).show()
                        })
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(instaInput, nameInput, taglineInput, selectedAvatarIdx)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            Toast.makeText(context, "Beacon signal initialized!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_profile_button")
                    ) {
                        Text(
                            "Save & Sync Transmissions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // BLE Scanning diagnostics
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RADAR DIAGNOSTIC LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFF2F0F4),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val logs = viewModel.logMessages
                        if (logs.isEmpty()) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("No diagnostic records logged...", color = Color(0xFF49454F), fontSize = 11.sp)
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                                items(logs) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("Error") || log.contains("failed")) Color(0xFFB3261E) else Color(0xFF21005D),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                        // Line wraps automatically in standard Text layouts
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
