package com.camerastreamingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.camerastreamingapp.data.db.CameraStreamingDatabase
import com.camerastreamingapp.data.repository.CameraRepository
import com.camerastreamingapp.ui.screens.CameraStreamingScreen
import com.camerastreamingapp.ui.viewmodel.CameraStreamingViewModel
import com.camerastreamingapp.ui.viewmodel.CameraStreamingViewModelFactory
import timber.log.Timber

/**
 * Main activity for camera streaming app
 */
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CameraStreamingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Timber logging
        Timber.plant(Timber.DebugTree())

        // Initialize database and repository
        val database = CameraStreamingDatabase.getInstance(this)
        val cameraDao = database.cameraDao()
        val connectionDao = database.connectionDao()
        val recordingDao = database.recordingDao()
        val remoteAccessDao = database.remoteAccessDao()

        val repository = CameraRepository(
            cameraDao, connectionDao, recordingDao, remoteAccessDao
        )

        // Create ViewModel
        val factory = CameraStreamingViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(CameraStreamingViewModel::class.java)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraStreamingScreen(viewModel = viewModel)
                }
            }
        }
    }
}
