package com.example.nextclass.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nextclass.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at the exact minute the widget's state changes (class starts or ends) and at 00:05 each
 * day. It redraws the widget and schedules the next single alarm.
 */
class WidgetTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        runRefresh(context)
    }

    companion object {
        const val ACTION_MIDNIGHT = "com.example.nextclass.action.MIDNIGHT"
    }
}

/** Runs [WidgetUpdateHelper.refreshAll] while keeping the receiver alive until it finishes. */
fun BroadcastReceiver.runRefresh(context: Context) {
    val pending: BroadcastReceiver.PendingResult? = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.Default).launch {
        try {
            WidgetUpdateHelper.refreshAll(appContext)
        } finally {
            pending?.finish()
        }
    }
}
