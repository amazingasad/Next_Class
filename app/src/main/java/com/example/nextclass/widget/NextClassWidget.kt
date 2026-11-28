package com.example.nextclass.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.nextclass.MainActivity
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.effectiveSessions
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.NextClassState
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.domain.model.focusSession
import com.example.nextclass.domain.model.statusLabel
import com.example.nextclass.domain.usecase.GetCurrentOrNextClassUseCase
import java.time.LocalDateTime

/**
 * Swiss-grid widget that lists the whole routine of the day. The ongoing class, or the next one
 * when nothing is running, is highlighted; classes that already finished are faded. Once the day
 * is over it switches to the next scheduled day. Glance has no letter-spacing or font-feature
 * support, so times use a monospaced face to keep digits aligned.
 */
class NextClassWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    private data class Model(
        val state: NextClassState,
        val rows: List<ClassSession>,
        val nowMinutes: Int,
        val focusId: Long?,
        val themeMode: Int,
        val dynamic: Boolean,
        val marks: Map<Long, Boolean>,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = load(context)
        provideContent {
            val tick by WidgetTicker.tick.collectAsState()
            val model by produceState(initial, tick) { value = load(context) }
            WidgetContent(model)
        }
    }

    private suspend fun load(context: Context): Model {
        val sessions = AppDatabase.getInstance(context).routineDao().effectiveSessions()
        val prefs = AppPreferences(context)
        val themeMode = prefs.widgetTheme
        val dynamic = prefs.dynamicColor
        val now = LocalDateTime.now()
        val today = now.toLocalDate().toString()
        val todaysMarks = AppDatabase.getInstance(context).routineDao().getAttendance().filter { it.date == today }
        val marks = HashMap<Long, Boolean>()
        sessions.forEach { s ->
            val mark = todaysMarks.firstOrNull {
                it.courseKey == CourseCodes.key(s.courseCode) && it.startTime == s.startTime
            }
            if (mark != null) marks[s.id] = mark.status == "P"
        }
        val state = GetCurrentOrNextClassUseCase().invoke(sessions, now)
        val nowMinutes = now.hour * 60 + now.minute

        val model = when (state) {
            is NextClassState.Ongoing, is NextClassState.Upcoming -> Model(
                state = state,
                rows = sessions.filter { it.dayOfWeek == now.dayOfWeek.value }
                    .sortedWith(compareBy({ it.startMinutes }, { it.endMinutes })),
                nowMinutes = nowMinutes,
                focusId = state.focusSession?.id,
                themeMode = themeMode,
                dynamic = dynamic,
                marks = marks,
            )
            is NextClassState.NextDayPreview -> Model(
                state = state,
                rows = sessions.filter { it.dayOfWeek == state.firstSession.dayOfWeek }
                    .sortedWith(compareBy({ it.startMinutes }, { it.endMinutes })),
                nowMinutes = -1,
                focusId = state.firstSession.id,
                themeMode = themeMode,
                dynamic = dynamic,
                marks = emptyMap(),
            )
            NextClassState.NoClasses -> Model(state, emptyList(), nowMinutes, null, themeMode, dynamic, emptyMap())
        }
        return model
    }

    private companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val WIDE_BREAKPOINT = 200.dp
    }

    @Composable
    private fun WidgetContent(model: Model) {
        val wide = LocalSize.current.width >= WIDE_BREAKPOINT
        val live = model.state is NextClassState.Ongoing
        val widgetContext = LocalContext.current
        val p = WidgetPalette(model.themeMode, widgetContext, model.dynamic)
        val openApp = actionStartActivity(Intent(widgetContext, MainActivity::class.java))

        // Outer box paints the 1dp hairline, inner box paints the surface.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(p.hairline)
                .cornerRadius(20.dp)
                .padding(1.dp)
                .clickable(openApp),
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(p.surface)
                    .cornerRadius(19.dp)
                    .padding(top = 12.dp, start = 10.dp, end = 10.dp, bottom = 6.dp),
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .background(if (live) p.vermilion else p.sage)
                                .cornerRadius(3.dp),
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = model.state.statusLabel,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (live) p.vermilionText else p.muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    if (model.rows.isEmpty()) {
                        Column(modifier = GlanceModifier.fillMaxSize().padding(start = 4.dp)) {
                            Text(
                                text = "Nothing scheduled",
                                maxLines = 1,
                                style = TextStyle(
                                    color = p.ink,
                                    fontSize = if (wide) 20.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "Import a routine in NextClass",
                                maxLines = 1,
                                style = TextStyle(color = p.muted, fontSize = 12.sp),
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                            // The item id changes with the status, so Android redraws the row after a tap on
                            // Present or Absent instead of reusing the cached one.
                            items(
                                model.rows,
                                itemId = { row ->
                                    val statusCode = when (model.marks[row.id]) {
                                        true -> 1L
                                        false -> 2L
                                        null -> 0L
                                    }
                                    val focusCode = if (row.id == model.focusId) 4L else 0L
                                    val pastCode = if (row.endMinutes <= model.nowMinutes) 8L else 0L
                                    row.id * 16L + statusCode + focusCode + pastCode
                                },
                            ) { session ->
                                SessionRow(
                                    session = session,
                                    past = session.endMinutes <= model.nowMinutes,
                                    focus = session.id == model.focusId,
                                    ongoing = live && session.id == model.focusId,
                                    status = model.marks[session.id],
                                    wide = wide,
                                    p = p,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SessionRow(
        session: ClassSession,
        past: Boolean,
        focus: Boolean,
        ongoing: Boolean,
        status: Boolean?,
        wide: Boolean,
        p: WidgetPalette,
    ) {
        val primary = if (past) p.inkFaded else p.ink
        val secondary = if (past) p.mutedFaded else p.muted
        val rowBackground = if (focus) p.focusTint else p.clear

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(rowBackground)
                    .cornerRadius(10.dp)
                    .padding(vertical = 6.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier
                        .width(3.dp)
                        .height(if (focus) 30.dp else 0.dp)
                        .background(p.vermilion)
                        .cornerRadius(2.dp),
                ) {}
                if (focus) Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = TimeFormat.clock12(session.startTime),
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focus) p.vermilionText else secondary,
                        fontSize = 12.sp,
                        fontWeight = if (focus) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    if (focus) {
                        Text(
                            text = if (ongoing) "NOW" else "NEXT",
                            maxLines = 1,
                            style = TextStyle(
                                color = p.vermilionText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Text(
                        text = session.courseCode,
                        maxLines = 1,
                        style = TextStyle(
                            color = primary,
                            fontSize = if (focus) 15.sp else 13.sp,
                            fontWeight = if (focus) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                    if (session.courseTitle.isNotBlank()) {
                        Text(
                            text = session.courseTitle,
                            maxLines = 1,
                            style = TextStyle(color = secondary, fontSize = 11.sp),
                        )
                    }
                    val roomLine = listOf(session.room.uppercase(), session.teacher)
                        .filter { it.isNotBlank() }
                        .joinToString("  \u2022  ")
                    Text(
                        text = when (status) {
                            true -> roomLine + "  \u2022  PRESENT"
                            false -> roomLine + "  \u2022  ABSENT"
                            null -> roomLine
                        },
                        maxLines = 1,
                        style = TextStyle(color = secondary, fontSize = 11.sp),
                    )
                    if (ongoing) {
                        Spacer(modifier = GlanceModifier.height(5.dp))
                        AttendanceChips(sessionId = session.id, status = status, p = p)
                    }
                }

                if (wide) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = TimeFormat.range(session),
                        maxLines = 1,
                        style = TextStyle(
                            color = secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
    }
}
