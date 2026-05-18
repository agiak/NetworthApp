package com.agcoding.networkapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.agcoding.networkapp.shared.shortcut.ShortcutEvent
import com.agcoding.networkapp.shared.shortcut.ShortcutEventBus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShortcutIntent(intent)
        setContent {
            NetWorthApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (intent?.action == ACTION_ADD_SNAPSHOT) {
            ShortcutEventBus.post(ShortcutEvent.AddSnapshot)
        }
    }

    companion object {
        const val ACTION_ADD_SNAPSHOT = "com.agcoding.networkapp.ADD_SNAPSHOT"
    }
}
