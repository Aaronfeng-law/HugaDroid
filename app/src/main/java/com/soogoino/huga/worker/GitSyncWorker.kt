package com.soogoino.huga.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.soogoino.huga.R
import com.soogoino.huga.domain.SyncRepoUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "git_sync_channel"
private const val NOTIFICATION_ID = 1001

@HiltWorker
class GitSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepoUseCase: SyncRepoUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo("Syncing…"))

        val result = syncRepoUseCase(commitMessage = "Auto-sync: ${java.time.Instant.now()}")

        return if (result.error == null) {
            showNotification(
                title = "Huga Sync",
                text = buildString {
                    if (result.pulled) append("Pulled. ")
                    if (result.pushed) append("Pushed.")
                    if (!result.pulled && !result.pushed) append("Already up to date.")
                },
                isError = false,
            )
            Result.success()
        } else {
            val msg = result.error.message ?: "Unknown error"
            showNotification("Huga Sync Failed", msg, isError = true)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Huga")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showNotification(title: String, text: String, isError: Boolean) {
        createNotificationChannel()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(if (isError) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_menu_upload)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.sync_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = applicationContext.getString(R.string.sync_channel_desc) }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val WORK_NAME = "huga_periodic_sync"

        /** Enqueue a periodic sync every [intervalMinutes] minutes, Wi-Fi only. */
        fun schedule(context: Context, intervalMinutes: Long = 30) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only
                .build()

            val request = PeriodicWorkRequestBuilder<GitSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /** One-shot immediate sync. */
        fun runOnce(context: Context): androidx.work.Operation {
            val request = OneTimeWorkRequestBuilder<GitSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            return WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
