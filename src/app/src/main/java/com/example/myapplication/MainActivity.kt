package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.maps.model.LatLng
import android.view.View
import android.content.Intent
import android.media.MediaPlayer
import android.provider.Settings
import android.media.RingtoneManager


class MainActivity : AppCompatActivity() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var telephonyManager: TelephonyManager
    private val handler = Handler()
    private val networkCheckIntervalMs = 10000L
    private var isCheckingNetworkStatus = false
    private lateinit var statusTextView: TextView
    private var lastKnownNetworkType: String? = null
    private lateinit var networkProtocolTextView: TextView

    // Initialize the network callback for monitoring network availability
    private fun initializeNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                logNetworkType()
            }

            override fun onLost(network: Network) {
                logNetworkType()
            }
        }

        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    companion object {
        private const val PERMISSION_REQUEST_READ_PHONE_STATE = 1
        private const val PERMISSION_REQUEST_LOCATION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        networkProtocolTextView = findViewById(R.id.networkProtocolTextView)
        networkProtocolTextView.visibility = View.VISIBLE

        // Request READ_PHONE_STATE permission if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSION_REQUEST_READ_PHONE_STATE
            )
        } else {
            initializeNetworkCallback()
        }

        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)

        statusTextView.apply {
            text = "Not checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotCheckingStatus))
            visibility = View.VISIBLE
        }

        startButton.setOnClickListener {
            startCheckingNetworkStatus()
        }

        stopButton.setOnClickListener {
            stopCheckingNetworkStatus()
        }

        // Initialize and setup your network button here
        val networkButton: Button = findViewById(R.id.networkButton)
        networkButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
        }

        // Start network checking right after the app opens
        startCheckingNetworkStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // Start checking the network status
    private fun startCheckingNetworkStatus() {
        if (isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = true
        statusTextView.apply {
            text = "Checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCheckingStatus))
        }

        handler.post(networkCheckRunnable)
    }

    // Stop checking the network status
    private fun stopCheckingNetworkStatus() {
        if (!isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = false
        statusTextView.apply {
            text = "Not checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotCheckingStatus))
        }

        handler.removeCallbacks(networkCheckRunnable)
    }

    // Log the current network type
    private fun logNetworkType() {
        val networkType = telephonyManager.networkType
        val currentNetworkType = getMobileNetworkType(networkType)
        lastKnownNetworkType = currentNetworkType
        networkProtocolTextView.text = "Current Network Protocol: $currentNetworkType"
    }

    private var isNetworkDowngraded = false
    private var mediaPlayer: MediaPlayer? = null

    // Runnable for network status checks
    private val networkCheckRunnable = object : Runnable {
        override fun run() {
            if (!isCheckingNetworkStatus) {
                return
            }

            val previousNetworkType = lastKnownNetworkType

            logNetworkType()
            val currentNetworkType = lastKnownNetworkType

            val currentDateTime = getCurrentDateTime()
            var logMessage = ""

            // Generate alert if network is 2G or 3G
            if (currentNetworkType in listOf("2G", "3G")) {
                logMessage =
                    "$currentDateTime - Warning: Current network protocol is $currentNetworkType. Possible network interference detected!"

                playNotificationSound()
            }

            if (logMessage.isNotEmpty()) {
                appendToLog(logMessage)
            }

            handler.postDelayed(this, networkCheckIntervalMs)
        }
    }

    private fun playNotificationSound() {
        try {
            val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mediaPlayer = MediaPlayer.create(applicationContext, alertSound)
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get the mobile network type based on network type constant
    private fun getMobileNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GSM -> "2G"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            TelephonyManager.NETWORK_TYPE_CDMA -> "2G"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "3G"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "3G"
            TelephonyManager.NETWORK_TYPE_HSPA -> "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "3G"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }

    // Get the current date and time in a specific format
    private fun getCurrentDateTime(): String {
        val currentDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
        return currentDateTime
    }

    // Append a log message to the status text view and logcat
    private fun appendToLog(logMessage: String) {
        Log.d("MainActivity", logMessage)
        runOnUiThread {
            statusTextView.append("\n$logMessage")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_PHONE_STATE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initializeNetworkCallback()
                } else {
                    val alertDialog = AlertDialog.Builder(this)
                        .setTitle("Permission needed")
                        .setMessage("This app needs the READ_PHONE_STATE permission to monitor network status.")
                        .setPositiveButton("OK") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.READ_PHONE_STATE),
                                PERMISSION_REQUEST_READ_PHONE_STATE
                            )
                        }
                        .create()
                    alertDialog.show()
                }
                return
            }
        }
    }
}
