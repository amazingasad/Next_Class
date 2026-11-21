package com.example.nextclass.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/** 1dp hairline divider, outlineVariant at 35% alpha. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NcTheme.extras.hairline),
    )
}

/** Tracked uppercase micro-label used for status tags and section headings. */
@Composable
fun MicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NcTheme.extras.muted,
) {
    Text(
        text = text.uppercase(),
        style = NcText.Micro,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Int = 6) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/** Pill used for the Mon-Sun strip, the type selector and lead-time choices. */
@Composable
fun SelectPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    marker: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val foreground = if (selected) colors.surface else colors.onSurface
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) colors.onSurface else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, NcTheme.extras.hairline, shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = NcText.Micro.copy(letterSpacing = 0.8.sp),
            color = foreground,
            maxLines = 1,
        )
        if (marker) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(foreground.copy(alpha = 0.6f)),
            )
        }
    }
}

/** Primary call to action. The only filled vermilion control in the app. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = NcTheme.extras.muted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text = text, style = NcText.Title.copy(fontSize = 15.sp), maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NcTheme.extras.hairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = NcTheme.extras.muted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text = text, style = NcText.Title.copy(fontSize = 15.sp), maxLines = 1)
    }
}

@Composable
fun NcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, style = NcText.Body.copy(fontSize = 13.sp)) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, style = NcText.Body, color = NcTheme.extras.muted) }
        } else {
            null
        },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        textStyle = NcText.Body.copy(color = colors.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.onSurface,
            unfocusedBorderColor = colors.outlineVariant.copy(alpha = 0.6f),
            errorBorderColor = colors.error,
            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = NcTheme.extras.muted,
            errorLabelColor = colors.error,
            cursorColor = colors.onSurface,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
        ),
    )
}

/** Horizontal arrangement helper so screens share the same 6dp rhythm. */
val PillSpacing = Arrangement.spacedBy(6.dp)
