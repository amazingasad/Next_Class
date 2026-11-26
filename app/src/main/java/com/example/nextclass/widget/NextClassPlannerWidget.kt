package com.example.nextclass.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.example.nextclass.MainActivity
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.toDomain
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.PlannerTask
import java.time.LocalDate

/**
 * One widget, two pages: the pinned study note first, today's tasks second. Glance cannot swipe
 * between pages, so the arrows in the header (and a tap on the page name) flip them.
 */
class NextClassPlannerWidget : GlanceAppWidget() {

    private data class Model(
        val page: Int,
        val note: Note?,
        val tasks: List<PlannerTask>,
        val themeMode: Int,
        val dynamic: Boolean,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = AppPreferences(context)
        val dao = AppDatabase.getInstance(context).plannerDao()
        val notes = dao.getNotes().map { it.toDomain() }
        val note = notes.firstOrNull { it.pinned } ?: notes.firstOrNull()
        val today = LocalDate.now().toString()
        val tasks = dao.tasksOn(today).map { it.toDomain() }
        val model = Model(
            page = prefs.plannerPage.coerceIn(0, 1),
            note = note,
            tasks = tasks,
            themeMode = prefs.widgetTheme,
            dynamic = prefs.dynamicColor,
        )
        provideContent { Content(model) }
    }

    @Composable
    private fun Content(model: Model) {
        val context = LocalContext.current
        val p = remember(model.themeMode, model.dynamic) { WidgetPalette(model.themeMode, context, model.dynamic) }
        val onNotes = model.page == 0
        val open = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, if (onNotes) MainActivity.OPEN_NOTES else MainActivity.OPEN_TASKS),
        )
        val flipTarget = if (onNotes) 1 else 0

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(p.hairline)
                .cornerRadius(20.dp)
                .padding(1.dp),
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(p.surface)
                    .cornerRadius(19.dp)
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(
                                actionRunCallback<FlipPlannerPageAction>(
                                    actionParametersOf(FlipPlannerPageAction.PAGE to flipTarget),
                                ),
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = GlanceModifier.size(6.dp).background(p.vermilion).cornerRadius(3.dp),
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = if (onNotes) "STUDY NOTE" else tasksHeader(model.tasks),
                            maxLines = 1,
                            style = TextStyle(color = p.vermilionText, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        PageDot(active = onNotes, p = p)
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        PageDot(active = !onNotes, p = p)
                    }
                    Text(
                        text = "OPEN",
                        maxLines = 1,
                        modifier = GlanceModifier.clickable(open).padding(horizontal = 8.dp, vertical = 6.dp),
                        style = TextStyle(color = p.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                    Arrow("‹", flipTarget, p)
                    Arrow("›", flipTarget, p)
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                if (onNotes) NotePage(model.note, p) else TasksPage(model.tasks, p)
            }
        }
    }

    private fun tasksHeader(tasks: List<PlannerTask>): String {
        if (tasks.isEmpty()) return "TODAY"
        return "TODAY  " + tasks.count { it.done } + " OF " + tasks.size
    }

    @Composable
    private fun PageDot(active: Boolean, p: WidgetPalette) {
        Box(
            modifier = GlanceModifier
                .size(if (active) 14.dp else 5.dp, 5.dp)
                .background(if (active) p.ink else p.hairline)
                .cornerRadius(3.dp),
        ) {}
    }

    @Composable
    private fun Arrow(glyph: String, target: Int, p: WidgetPalette) {
        Text(
            text = glyph,
            modifier = GlanceModifier
                .clickable(
                    actionRunCallback<FlipPlannerPageAction>(
                        actionParametersOf(FlipPlannerPageAction.PAGE to target),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 2.dp),
            style = TextStyle(color = p.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
        )
    }

    @Composable
    private fun NotePage(note: Note?, p: WidgetPalette) {
        if (note == null) {
            Column(modifier = GlanceModifier.fillMaxSize().padding(start = 4.dp, top = 8.dp)) {
                Text(
                    text = "No note yet",
                    maxLines = 1,
                    style = TextStyle(color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Write your study steps in NextClass, under Plan, then Notes.",
                    style = TextStyle(color = p.muted, fontSize = 12.sp),
                )
            }
            return
        }
        Column(modifier = GlanceModifier.fillMaxSize().padding(start = 4.dp)) {
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    maxLines = 1,
                    style = TextStyle(color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                itemsIndexed(note.lines, itemId = { index, _ -> index.toLong() }) { _, line ->
                    Text(
                        text = line,
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                        style = TextStyle(color = p.ink, fontSize = 13.sp),
                    )
                }
            }
        }
    }

    @Composable
    private fun TasksPage(tasks: List<PlannerTask>, p: WidgetPalette) {
        if (tasks.isEmpty()) {
            Column(modifier = GlanceModifier.fillMaxSize().padding(start = 4.dp, top = 8.dp)) {
                Text(
                    text = "No tasks today",
                    maxLines = 1,
                    style = TextStyle(color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Add tasks in NextClass, under Plan.",
                    style = TextStyle(color = p.muted, fontSize = 12.sp),
                )
            }
            return
        }
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
            itemsIndexed(
                tasks,
                itemId = { _, task -> task.id * 2L + if (task.done) 1L else 0L },
            ) { _, task ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(
                            actionRunCallback<ToggleTaskAction>(
                                actionParametersOf(ToggleTaskAction.TASK_ID to task.id),
                            ),
                        )
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(18.dp)
                            .background(if (task.done) p.ink else p.hairline)
                            .cornerRadius(6.dp)
                            .padding(1.dp),
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(if (task.done) p.ink else p.surface)
                                .cornerRadius(5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (task.done) {
                                Text(
                                    text = "✓",
                                    style = TextStyle(color = p.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Text(
                        text = task.title,
                        maxLines = 2,
                        style = TextStyle(
                            color = if (task.done) p.mutedFaded else p.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                    )
                }
            }
        }
    }
}
