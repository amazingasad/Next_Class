package com.example.nextclass.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import com.example.nextclass.data.local.toDomain
import com.example.nextclass.domain.model.ClassSession
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
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sessions = AppDatabase.getInstance(context).routineDao().getAll().map { it.toDomain() }
        val now = LocalDateTime.now()
        val state = GetCurrentOrNextClassUseCase().invoke(sessions, now)
        val nowMinutes = now.hour * 60 + now.minute

        val model = when (state) {
            is NextClassState.Ongoing, is NextClassState.Upcoming -> Model(
                state = state,
                rows = sessions.filter { it.dayOfWeek == now.dayOfWeek.value }
                    .sortedWith(compareBy({ it.startMinutes }, { it.endMinutes })),
                nowMinutes = nowMinutes,
                focusId = state.focusSession?.id,
            )
            is NextClassState.NextDayPreview -> Model(
                state = state,
                rows = sessions.filter { it.dayOfWeek == state.firstSession.dayOfWeek }
                    .sortedWith(compareBy({ it.startMinutes }, { it.endMinutes })),
                nowMinutes = -1,
                focusId = state.firstSession.id,
            )
            NextClassState.NoClasses -> Model(state, emptyList(), nowMinutes, null)
        }
        provideContent { WidgetContent(model) }
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
        val openApp = actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))

        // Outer box paints the 1dp hairline, inner box paints the surface.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Palette.hairline)
                .cornerRadius(20.dp)
                .padding(1.dp)
                .clickable(openApp),
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Palette.surface)
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
                                .background(if (live) Palette.vermilion else Palette.sage)
                                .cornerRadius(3.dp),
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = model.state.statusLabel,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (live) Palette.vermilionText else Palette.muted,
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
                                    color = Palette.ink,
                                    fontSize = if (wide) 20.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "Import a routine in NextClass",
                                maxLines = 1,
                                style = TextStyle(color = Palette.muted, fontSize = 12.sp),
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                            items(model.rows, itemId = { it.id }) { session ->
                                SessionRow(
                                    session = session,
                                    past = session.endMinutes <= model.nowMinutes,
                                    focus = session.id == model.focusId,
                                    ongoing = live && session.id == model.focusId,
                                    wide = wide,
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
        wide: Boolean,
    ) {
        val primary = if (past) Palette.inkFaded else Palette.ink
        val secondary = if (past) Palette.mutedFaded else Palette.muted
        val rowBackground = if (focus) Palette.focusTint else Palette.clear

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
                        .background(Palette.vermilion)
                        .cornerRadius(2.dp),
                ) {}
                if (focus) Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = session.startTime,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (focus) Palette.vermilionText else secondary,
                        fontSize = 12.sp,
                        fontWeight = if (focus) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(10.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    if (focus) {
                        Text(
                            text = if (ongoing) "NOW" else "NEXT",
                            maxLines = 1,
                            style = TextStyle(
                                color = Palette.vermilionText,
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
                    Text(
                        text = session.room.uppercase(),
                        maxLines = 1,
                        style = TextStyle(color = secondary, fontSize = 11.sp),
                    )
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

/** Day/night aware colors. Vermilion is reserved for the live and highlighted state. */
private object Palette {
    val surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF18181B))
    val hairline = ColorProvider(day = Color(0xFFDADAD6), night = Color(0xFF34343A))
    val ink = ColorProvider(day = Color(0xFF111113), night = Color(0xFFF4F4F2))
    val muted = ColorProvider(day = Color(0xFF6B6B66), night = Color(0xFFA1A1A8))
    val inkFaded = ColorProvider(day = Color(0x66111113), night = Color(0x66F4F4F2))
    val mutedFaded = ColorProvider(day = Color(0x556B6B66), night = Color(0x55A1A1A8))
    val sage = ColorProvider(day = Color(0xFF5E7568), night = Color(0xFF8FA89A))
    val vermilion = ColorProvider(day = Color(0xFFEA580C), night = Color(0xFFEA580C))
    val vermilionText = ColorProvider(day = Color(0xFFC2410C), night = Color(0xFFFB7A3A))
    val focusTint = ColorProvider(day = Color(0x14EA580C), night = Color(0x26EA580C))
    val clear = ColorProvider(day = Color(0x00000000), night = Color(0x00000000))
}
