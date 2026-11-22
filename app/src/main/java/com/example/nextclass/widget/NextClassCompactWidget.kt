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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.NextClassState
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.domain.model.focusSession
import com.example.nextclass.domain.model.statusLabel
import com.example.nextclass.domain.usecase.GetCurrentOrNextClassUseCase
import java.time.LocalDateTime

/** One-row widget: only the ongoing or next class, for people who want a small strip. */
class NextClassCompactWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sessions = AppDatabase.getInstance(context).routineDao().effectiveSessions()
        val state = GetCurrentOrNextClassUseCase().invoke(sessions, LocalDateTime.now())
        val prefs = AppPreferences(context)
        val themeMode = prefs.widgetTheme
        val dynamic = prefs.dynamicColor
        val focus = state.focusSession
        val today = LocalDateTime.now().toLocalDate().toString()
        val status = if (focus == null) {
            null
        } else {
            AppDatabase.getInstance(context).routineDao().getAttendance().firstOrNull {
                it.date == today &&
                    it.courseKey == CourseCodes.key(focus.courseCode) &&
                    it.startTime == focus.startTime
            }?.let { it.status == "P" }
        }
        provideContent { Content(state, themeMode, dynamic, status) }
    }

    @Composable
    private fun Content(state: NextClassState, themeMode: Int, dynamic: Boolean, status: Boolean?) {
        val widgetContext = LocalContext.current
        val p = remember(themeMode, dynamic) { WidgetPalette(themeMode, widgetContext, dynamic) }
        val live = state is NextClassState.Ongoing
        val session = state.focusSession
        val openApp = actionStartActivity(Intent(widgetContext, MainActivity::class.java))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(p.hairline)
                .cornerRadius(18.dp)
                .padding(1.dp)
                .clickable(openApp),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(p.surface)
                    .cornerRadius(17.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .background(if (live) p.vermilion else p.sage)
                                .cornerRadius(3.dp),
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = state.statusLabel,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (live) p.vermilionText else p.muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Text(
                        text = session?.courseCode ?: "Nothing scheduled",
                        maxLines = 1,
                        style = TextStyle(
                            color = p.ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    if (session != null && session.courseTitle.isNotBlank()) {
                        Text(
                            text = session.courseTitle,
                            maxLines = 1,
                            style = TextStyle(color = p.muted, fontSize = 11.sp),
                        )
                    }
                }
                if (session != null) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = session.room.uppercase(),
                            maxLines = 1,
                            style = TextStyle(
                                color = p.ink,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (live) {
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            AttendanceChips(sessionId = session.id, status = status, p = p, compact = true)
                        } else {
                            Text(
                                text = TimeFormat.range(session),
                                maxLines = 1,
                                style = TextStyle(
                                    color = p.muted,
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
    }
}
