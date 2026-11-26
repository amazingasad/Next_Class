package com.example.nextclass.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences

/** Flips the planner widget between its note page (0) and its tasks page (1). */
class FlipPlannerPageAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val page = parameters[PAGE] ?: return
        AppPreferences(context).plannerPage = page
        WidgetUpdateHelper.updateWidgets(context)
    }

    companion object {
        val PAGE = ActionParameters.Key<Int>("plannerPage")
    }
}

/** Ticks or unticks one task straight from the widget. */
class ToggleTaskAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TASK_ID] ?: return
        AppDatabase.getInstance(context).plannerDao().toggle(id)
        WidgetUpdateHelper.updateWidgets(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<Long>("plannerTaskId")
    }
}
