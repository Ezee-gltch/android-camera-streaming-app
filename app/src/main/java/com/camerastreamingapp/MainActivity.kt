package com.camerastreamingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
    }
}
