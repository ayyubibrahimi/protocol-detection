import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Handler
import android.telephony.TelephonyManager
import com.example.myapplication.R
import com.example.myapplication.R.layout


class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        checkPermissions()

        startLoop()
    }

    private fun checkPermissions() {
        val permission = Manifest.permission.ACCESS_NETWORK_STATE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startLoop() {
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkNetworkStatus()
                handler.postDelayed(this, 10000) // Run every 10 seconds
            }
        }, 10000) // Start after 10 seconds
    }

    private fun checkNetworkStatus() {
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo == null) {
            showToast("No network connection")
        } else {
            when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> showToast("Connected to Wifi")
                ConnectivityManager.TYPE_MOBILE -> {
                    if (isNetworkType3G(networkInfo.subtype)) {
                        showToast("Network protocol downgraded below 4g/5g")
                        alert()
                    } else {
                        showToast("Connected to 4g/5g")
                    }
                }
                else -> showToast("Unknown network connection")
            }
        }
    }

    private fun isNetworkType3G(networkType: Int): Boolean {
        return networkType == TelephonyManager.NETWORK_TYPE_UMTS ||
                networkType == TelephonyManager.NETWORK_TYPE_HSDPA ||
                networkType == TelephonyManager.NETWORK_TYPE_HSPA ||
                networkType == TelephonyManager.NETWORK_TYPE_HSPAP ||
                networkType == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                networkType == TelephonyManager.NETWORK_TYPE_EVDO_A ||
                networkType == TelephonyManager.NETWORK_TYPE_EVDO_B
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun alert() {
        // Trigger an alert when the phone is connected to a 3G network
        // You can customize this method to display a notification or sound an alarm
        println("Phone is connected to a 3G network!")
    }
}
