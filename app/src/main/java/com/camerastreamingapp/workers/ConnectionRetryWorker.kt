package com.camerastreamingapp.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.camerastreamingapp.data.api.CameraStreamManager
import com.camerastreamingapp.data.db.CameraStreamDatabase
import com.camerastreamingapp.domain.connection.ConnectionManager

class ConnectionRetryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = CameraStreamDatabase.getDatabase(applicationContext)
        val manager = ConnectionManager(
            cameraDao = database.cameraDao(),
            connectionDao = database.connectionDao(),
            cameraStreamManager = CameraStreamManager(applicationContext),
            context = applicationContext
        )

        var hasFailure = false
        database.connectionDao().findDisconnectedConnections(System.currentTimeMillis()).forEach { connection ->
            if (!manager.attemptReconnectOnce(connection.cameraId)) {
                hasFailure = true
            }
        }

        return if (hasFailure) Result.retry() else Result.success()
    }
}
