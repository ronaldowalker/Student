package com.example.student

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.Socket
import java.security.MessageDigest

class StudentActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: Channel
    private lateinit var peerListListener: PeerListListener

    private lateinit var studentIdInput: EditText
    private lateinit var searchClassesButton: Button
    private lateinit var classListView: ListView
    private lateinit var classTitle: TextView

    private var isConnected = false
    private var className: String = ""
    private var lecturerIpAddress = "" // Assume lecturer's IP is known/obtained through Wi-Fi Direct

    private val port = 8888 // Predefined port for communication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        studentIdInput = findViewById(R.id.studentIdInput)
        searchClassesButton = findViewById(R.id.searchClassesButton)
        classListView = findViewById(R.id.classListView)
        classTitle = findViewById(R.id.classTitle)

        checkWifiAdapter()

        peerListListener = PeerListListener { peerList ->
            val peers = peerList.deviceList
            if (peers.isEmpty()) {
                Toast.makeText(this, "No nearby classes found. Please try again.", Toast.LENGTH_SHORT).show()
                return@PeerListListener
            }

            val peerNames = peers.map { it.deviceName }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, peerNames)
            classListView.adapter = adapter

            classListView.setOnItemClickListener { _, _, position, _ ->
                connectToClass(peerNames[position], peers.elementAt(position).deviceAddress) // Assuming deviceAddress is the IP
            }
        }

        searchClassesButton.setOnClickListener {
            val studentId = studentIdInput.text.toString()
            if (studentId.isEmpty()) {
                Toast.makeText(this, "Please enter a valid student ID", Toast.LENGTH_SHORT).show()
            } else {
                // Initiate peer discovery for nearby classes using Wi-Fi Direct
                discoverPeers()
            }
        }
    }

    private fun checkWifiAdapter() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable WiFi to continue", Toast.LENGTH_LONG).show()
            // Logic to ask user to enable WiFi adapter
        }
    }

    private fun discoverPeers() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@StudentActivity, "Searching for nearby classes...", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@StudentActivity, "Failed to search for classes. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun connectToClass(className: String, lecturerIp: String) {
        this.className = className
        this.lecturerIpAddress = lecturerIp // Capture lecturer's IP address from peer
        isConnected = true
        classTitle.text = "Class: $className"
        classTitle.visibility = TextView.VISIBLE

        Toast.makeText(this, "Connected to class: $className", Toast.LENGTH_SHORT).show()

        // Start the challenge-response authentication in the background
        AuthenticateTask(studentIdInput.text.toString()).execute(lecturerIpAddress)

        // Optionally navigate to the chat interface
        startActivity(Intent(this, ChatActivity::class.java))
    }

    inner class AuthenticateTask(private val studentId: String) : AsyncTask<String, Void, Void>() {
        override fun doInBackground(vararg params: String): Void? {
            try {
                // Connect to the lecturer's device
                val socket = Socket(params[0], port)

                val output = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                // Step 1: Send "I am here" to lecturer
                output.println("I am here")

                // Step 2: Receive random number (R) from lecturer
                val randomNumber = input.readLine().toInt()

                // Step 3: Encrypt R with Hash(StudentID) and send it back
                val encryptedResponse = encryptRandomNumber(randomNumber, studentId)
                output.println(encryptedResponse)

                // Step 4: Close connection
                socket.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun encryptRandomNumber(randomNumber: Int, studentId: String): String {
            val studentIdHash = MessageDigest.getInstance("SHA-256")
                .digest(studentId.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
            return (randomNumber.toString() + studentIdHash).hashCode().toString()
        }
    }
}
