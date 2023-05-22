package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat

class NetworkService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var telephonyManager: TelephonyManager
    private val handler = Handler()
    private val networkCheckIntervalMs = 10000L
    private var isCheckingNetworkStatus = false

    override fun onCreate() {
        super.onCreate()

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        initializeNetworkCallback()
        startCheckingNetworkStatus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckingNetworkStatus()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val channelId = CHANNEL_ID
        val channelName = CHANNEL_NAME

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Network Service")
            .setContentText("Checking network status")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use the system icon as the default icon
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setChannelId(channelId) // Associate the notification with the channel
            .build()
    }

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

    private fun startCheckingNetworkStatus() {
        if (isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = true
        handler.postDelayed(networkCheckRunnable, networkCheckIntervalMs)
    }

    private fun stopCheckingNetworkStatus() {
        isCheckingNetworkStatus = false
        handler.removeCallbacks(networkCheckRunnable)
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
        val notificationId = 2
        val channelId = "network_alert_channel"
        val channelName = "Network Alert"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Network Alert")
            .setContentText("Connected to 2G/3G network")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }


    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "network_service_channel"
        private const val CHANNEL_NAME = "Network Service"
    }
}
