/*
 * Copyright (c) 2024 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.location.impl

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.element.android.features.location.api.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class LocationForegroundService : Service() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    lateinit var locationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // check for location permissions

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, createNotification())
        }

        handleLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        // move to NotificationChannels
        // check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val locationChannelId = "location_channel"
            val channelName = "Location Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            NotificationChannel(locationChannelId, channelName, importance).apply {
                description = "Running service to find your location"
                with((this@LocationForegroundService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)) {
                    createNotificationChannel(this@apply)
                }
            }
        }

        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Location Service")
            .setContentText("Running")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun handleLocationUpdates() {
        val locationRequest = LocationRequest.Builder(3 * 1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    val matrixLocation = Location(location.latitude, location.longitude, location.accuracy)
                    scope.launch {
                        Timber.e(matrixLocation.toGeoUri())
                        //room.sendUserLocationBeacon(matrixLocation.toGeoUri())
                    }
                }
            }
        }
        // check permissions
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }
}
