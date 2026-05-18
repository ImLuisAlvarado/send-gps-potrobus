package mx.itson.sendgpspotrobus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import mx.itson.sendgpspotrobus.utils.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GpsService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val BASE_URL = Constants.BASE_URL

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationToApi(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        requestLocationUpdates()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "gps_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GPS Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val numEco = prefs.getString("numero_economico", "—")
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PotroBus — Tracking activo")
            .setContentText("Unidad $numEco enviando ubicación...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("GpsService", "Sin permiso de ubicación: ${e.message}")
        }
    }

    private fun sendLocationToApi(location: Location) {
        val idUnidad = prefs.getInt("id_unidad", -1)

        if (idUnidad == -1) {
            Log.e("GpsService", "No hay id_unidad en sesión — detener servicio")
            stopSelf()
            return
        }

        val json = """
            {
                "id_unidad": $idUnidad,
                "lat": ${location.latitude},
                "lng": ${location.longitude},
                "timestamp": "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
            }
        """.trimIndent()

        val token = prefs.getString("jwt_token", "") ?: ""
        val request = Request.Builder()
            .url("$BASE_URL/api/gps/position")
            .addHeader("Authorization", "Bearer $token")
            .post(json.toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GpsService", "Error al enviar ubicación: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("GpsService", "GPS enviado — unidad $idUnidad " +
                            "${location.latitude}, ${location.longitude}")
                } else if (response.code == 401) {
                    Log.e("GpsService", "Token expirado — deteniendo servicio")
                    prefs.edit().clear().apply()
                    stopSelf()
                }else {
                    Log.e("GpsService", "Error del servidor: ${response.code}")
                }
                response.close()
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}