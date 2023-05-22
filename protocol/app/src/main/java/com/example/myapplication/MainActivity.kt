package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.DialogInterface
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
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var telephonyManager: TelephonyManager
    private val handler = Handler()
    private val networkCheckIntervalMs = 10000L
    private var isCheckingNetworkStatus = false
    private lateinit var statusTextView: TextView

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Check and request the READ_PHONE_STATE permission if not granted
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

        // Set the initial message as "Not Checking Network Status"
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
    }

    private fun startCheckingNetworkStatus() {
        if (isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = true
        statusTextView.apply {
            text = "Checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCheckingStatus))
            visibility = View.VISIBLE
        }
        handler.postDelayed(networkCheckRunnable, networkCheckIntervalMs)
    }

    private fun stopCheckingNetworkStatus() {
        isCheckingNetworkStatus = false
        handler.removeCallbacks(networkCheckRunnable)
        statusTextView.apply {
            text = "Not checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotCheckingStatus))
            visibility = View.VISIBLE
        }
    }


    private val networkCheckRunnable = object : Runnable {
        override fun run() {
            if (!isCheckingNetworkStatus) {
                return
            }

            logNetworkType()
            handler.postDelayed(this, networkCheckIntervalMs)
        }
    }

    private fun logNetworkType() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = networkCapabilities != null

        if (isConnected) {
            when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                    val networkType = telephonyManager.networkType
                    val mobileNetworkType = getMobileNetworkType(networkType)
                    Log.d("Network", "Connected to Mobile Data: $mobileNetworkType")

                    if (mobileNetworkType == "2G" || mobileNetworkType == "3G") {
                        showDangerAlert()
                    }
                }
                else -> {
                    Log.d("Network", "Connected to Other Network")
                }
            }
        } else {
            Log.d("Network", "Not Connected")
        }
    }

    private fun getMobileNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"

            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN,
            TelephonyManager.NETWORK_TYPE_NR -> "4G/5G"

            else -> "Unknown"
        }
    }

    private fun showDangerAlert() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setMessage("You are connected to a 2G or 3G network.")
            .setPositiveButton("OK", null)
            .create()

        alertDialog.show()
    }
}
