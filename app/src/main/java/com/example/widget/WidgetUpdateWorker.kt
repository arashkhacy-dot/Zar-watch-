package com.example.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.ZarWatchDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.sync.PriceSyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = ZarWatchDatabase.getInstance(appContext)
            val prefs = UserPreferencesRepository(appContext)
            val coordinator = PriceSyncCoordinator(
                context = appContext,
                observationDao = db.priceObservationDao(),
                alertDao = db.priceAlertDao(),
                preferencesRepository = prefs
            )
            coordinator.syncPrices()
            WidgetHelper.updateAllWidgets(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
