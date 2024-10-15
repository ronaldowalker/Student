package com.example.student

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    lateinit var wifiP2pManager: WifiP2pManager
    lateinit var wifiP2pChannel: WifiP2pManager.Channel
    lateinit var wifiP2pReceiver: WifiP2pReceiver
    lateinit var intentFilter: android.content.IntentFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize WifiP2pManager
        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)

        // Initialize BroadcastReceiver for WifiP2p events
        wifiP2pReceiver = WifiP2pReceiver(wifiP2pManager, wifiP2pChannel, this)
        intentFilter = android.content.IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        // Set up peer discovery when the student clicks "Search for Classes"
        val searchButton: Button = findViewById(R.id.searchButton)
        val studentIdInput: EditText = findViewById(R.id.studentIdInput)

        searchButton.setOnClickListener {
            val studentId = studentIdInput.text.toString()
            if (isValidStudentId(studentId)) {
                startPeerDiscovery()
            } else {
                Toast.makeText(this, "Invalid Student ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiP2pReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiP2pReceiver)
    }

    // Start peer discovery for Wi-Fi Direct
    private fun startPeerDiscovery() {
        wifiP2pManager.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@MainActivity, "Discovery started", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reasonCode: Int) {
                Toast.makeText(this@MainActivity, "Discovery failed: $reasonCode", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Simple validation for Student ID
    private fun isValidStudentId(studentId: String): Boolean {
        return studentId.isNotEmpty() && studentId.length == 8 // Example validation
    }
}