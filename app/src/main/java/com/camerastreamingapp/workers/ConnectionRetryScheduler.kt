package com.camerastreamingapp.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

object ConnectionRetryScheduler {
    fun schedule(context: Context) {
        val work = PeriodicWorkRequestBuilder<ConnectionRetryWorker>(
            15,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "camera_reconnect",
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }
}
