package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.RadarDashboardScreen
import com.example.ui.RadarViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: RadarViewModel by viewModels()

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val scanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
    } else {
      permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    }

    if (scanGranted) {
      Toast.makeText(this, "Radar scanning permissions verified!", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(this, "Simulation Mode activated. Grant Bluetooth permissions to test physical BLE scans.", Toast.LENGTH_LONG).show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    requestRequiredPermissions()

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          // RadarDashboardScreen takes full responsibility for layouts and interior safe areas
          RadarDashboardScreen(viewModel = viewModel)
        }
      }
    }
  }

  private fun requestRequiredPermissions() {
    val permissionsToRequest = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
      permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
      permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
      permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
      permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val ungranted = permissionsToRequest.filter {
      ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    if (ungranted.isNotEmpty()) {
      requestPermissionLauncher.launch(ungranted.toTypedArray())
    }
  }
}

