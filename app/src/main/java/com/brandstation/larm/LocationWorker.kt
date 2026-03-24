package com.brandstation.larm

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

class LocationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.geoFilterEnabled) return Result.success()

        return try {
            val client = LocationServices.getFusedLocationProviderClient(applicationContext)
            val location: Location? = Tasks.await(
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
                15, TimeUnit.SECONDS
            )

            if (location != null) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    prefs.stationLat, prefs.stationLng,
                    distance
                )
                val distanceKm = distance[0] / 1000f
                prefs.withinRadius = distanceKm <= prefs.geoRadiusKm
                prefs.lastLocationCheckMs = System.currentTimeMillis()
                Log.d(TAG, "Avstånd till station: %.1f km, inom radie: %b".format(distanceKm, prefs.withinRadius))
            } else {
                Log.w(TAG, "Kunde inte hämta position — behåller tidigare status")
            }
            Result.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "Platsbehörighet saknas", e)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Positionsfel", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LocationWorker"
        private const val WORK_NAME = "location_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LocationWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
