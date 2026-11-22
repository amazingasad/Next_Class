package com.example.nextclass.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

enum class DockTab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.Today),
    WEEK("Week", Icons.Rounded.CalendarViewWeek),
    ATTENDANCE("Attendance", Icons.Rounded.FactCheck),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

/**
 * Floating pill navigation. The selected tab expands into a filled capsule with its label; the
 * others stay as bare icons.
 */
@Composable
fun NcDock(
    selected: DockTab,
    onSelect: (DockTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, NcTheme.extras.hairline, shape)
                .padding(6.dp)
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockTab.entries.forEach { tab ->
                DockItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun DockItem(tab: DockTab, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.onSurface else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(240),
        label = "dockBackground",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.surface else NcTheme.extras.muted,
        animationSpec = tween(240),
        label = "dockContent",
    )
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .height(46.dp)
            .pressScale(source)
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .clickable(interactionSource = source, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = content,
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(260)) + fadeIn(tween(220)),
            exit = shrinkHorizontally(tween(220)) + fadeOut(tween(120)),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tab.label,
                    style = NcText.Title.copy(fontSize = 14.sp),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}
