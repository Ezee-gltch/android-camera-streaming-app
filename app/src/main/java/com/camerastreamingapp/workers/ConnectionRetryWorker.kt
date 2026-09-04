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

        database.connectionDao().findDisconnectedConnections().forEach { connection ->
            manager.attemptReconnect(connection.cameraId)
        }

        return Result.success()
    }
}
