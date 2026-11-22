package com.example.nextclass.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.R
import com.example.nextclass.ui.VerifyState
import com.example.nextclass.ui.components.ExpandableCard
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcTextField
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.Reveal
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

private const val AI_STUDIO_URL = "https://aistudio.google.com/apikey"

private data class GuideStep(val title: String, val body: String, val hint: String)

private val STEPS = listOf(
    GuideStep(
        title = "Log in to Google AI Studio",
        body = "Open Google's free AI Studio page in your phone's browser. This is where you create the key " +
            "NextClass uses to read your routine.",
        hint = "aistudio.google.com/apikey",
    ),
    GuideStep(
        title = "Sign in with Google",
        body = "Use any Google account. A personal Gmail works best, because some school or work accounts " +
            "have AI Studio switched off. Accept the terms if Google asks.",
        hint = "Personal Gmail recommended",
    ),
    GuideStep(
        title = "Create an API key",
        body = "Tap Create API key. If Google asks for a project, choose Create API key in new project. " +
            "The key appears in a few seconds.",
        hint = "Create API key",
    ),
    GuideStep(
        title = "Copy the key",
        body = "The key is a long line of letters and numbers that starts with AIza. Tap the copy icon next " +
            "to it. Copy all of it, with no spaces before or after.",
        hint = "AIza...",
    ),
    GuideStep(
        title = "Paste it here",
        body = "Paste the key below and verify it. It is stored encrypted on this phone and sent only to Google.",
        hint = "",
    ),
)

@Composable
fun ApiKeyGuideScreen(
    hasKey: Boolean,
    verify: VerifyState,
    onVerifyKey: (String) -> Unit,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var keyText by rememberSaveable { mutableStateOf("") }
    var keyVisible by rememberSaveable { mutableStateOf(false) }

    val openStudio: () -> Unit = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AI_STUDIO_URL)))
        } catch (e: ActivityNotFoundException) {
            // No browser installed; the card still shows the address to type.
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Get a Gemini key",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(index = 0) {
                Text(
                    text = if (hasKey) {
                        "A key is already saved on this phone. You can replace it by following the steps again."
                    } else {
                        "NextClass uses Google's Gemini AI to read your routine. The key is free and takes about two minutes."
                    },
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
            }

            StepDots(current = step, total = STEPS.size)

            // A deck: two quiet layers peek out from under the current card.
            Box(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(horizontal = 28.dp)
                        .offset(y = 14.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(horizontal = 14.dp)
                        .offset(y = 7.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, NcTheme.extras.hairline, RoundedCornerShape(24.dp)),
                )
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (
                            slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { w ->
                                if (forward) w / 3 else -w / 3
                            } + fadeIn(tween(300))
                            ) togetherWith (
                            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { w ->
                                if (forward) -w / 3 else w / 3
                            } + fadeOut(tween(160))
                            )
                    },
                    label = "guideStep",
                ) { index ->
                    val item = STEPS[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, NcTheme.extras.hairline, RoundedCornerShape(24.dp))
                            .padding(22.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "0${index + 1}",
                                style = NcText.Display.copy(fontSize = 56.sp, lineHeight = 56.sp),
                                color = NcTheme.extras.live,
                            )
                            Spacer(Modifier.width(10.dp))
                            MicroLabel(
                                text = "of ${STEPS.size}",
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = item.title,
                            style = NcText.Headline,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = item.body,
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )

                        if (item.hint.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = item.hint,
                                    style = NcText.Time.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        if (index == 0) {
                            Spacer(Modifier.height(16.dp))
                            SecondaryButton(
                                text = "Open AI Studio",
                                onClick = openStudio,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (index == STEPS.lastIndex) {
                            Spacer(Modifier.height(16.dp))
                            NcTextField(
                                value = keyText,
                                onValueChange = { keyText = it.trim() },
                                label = "API key",
                                placeholder = "AIza...",
                                visualTransformation = if (keyVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SecondaryButton(
                                    text = "Paste",
                                    onClick = {
                                        val pasted = clipboard.getText()?.text.orEmpty().trim()
                                        if (pasted.isNotEmpty()) keyText = pasted
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                SecondaryButton(
                                    text = if (keyVisible) "Hide" else "Show",
                                    onClick = { keyVisible = !keyVisible },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            PrimaryButton(
                                text = when (verify) {
                                    VerifyState.Checking -> "Verifying"
                                    VerifyState.Success -> "Verified"
                                    else -> "Verify key"
                                },
                                enabled = keyText.isNotBlank() && verify !is VerifyState.Checking,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onVerifyKey(keyText)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            when (verify) {
                                VerifyState.Success -> {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "The key works and is saved. You can import your routine now.",
                                        style = NcText.Body,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                is VerifyState.Failure -> {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = verify.message,
                                        style = NcText.Body,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    SecondaryButton(
                        text = "Back",
                        onClick = { step -= 1 },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (step < STEPS.lastIndex) {
                    PrimaryButton(
                        text = "Next step",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step += 1
                        },
                        modifier = Modifier.weight(1.6f),
                    )
                } else if (verify == VerifyState.Success) {
                    PrimaryButton(
                        text = "Done",
                        onClick = onFinished,
                        modifier = Modifier.weight(1.6f),
                    )
                }
            }

            Reveal(index = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MicroLabel("Good to know", modifier = Modifier.padding(top = 8.dp))

                    ExpandableCard(title = "Is it really free?") {
                        Text(
                            text = "Yes. The free tier in AI Studio needs no payment card. It has per-minute " +
                                "and daily limits, which is plenty for scanning a routine or a course list " +
                                "a few times. If you reach a limit, wait a few minutes or try again tomorrow.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    ExpandableCard(title = "Keep your key private") {
                        Text(
                            text = "Treat the key like a password. NextClass stores it encrypted on this " +
                                "phone and sends it only to Google. Do not post it or send it in a chat. " +
                                "Each person in your group should create their own. If a key leaks, delete " +
                                "it in AI Studio and create a new one.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    ExpandableCard(title = "What gets sent to Google") {
                        Text(
                            text = "Only when you scan, the routine or course image (or the first pages of " +
                                "the PDF) and the section text you typed are sent to Google's Gemini service. " +
                                "NextClass has no server of its own, and your saved routine never leaves the " +
                                "phone unless you share it yourself.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }
                }
            }

            Reveal(index = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MicroLabel("Troubleshooting", modifier = Modifier.padding(top = 8.dp))

                    ExpandableCard(title = "Google rejected the key") {
                        Text(
                            text = "Copy the key again and paste it without spaces. Make sure you copied all " +
                                "of it. Check that it still exists in AI Studio, and create a new one if it " +
                                "was deleted. A key from a project with the Gemini API switched off will also " +
                                "be rejected.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    ExpandableCard(title = "Too many requests or quota reached") {
                        Text(
                            text = "The key works, but the free limit for now has been used. Wait a few " +
                                "minutes and try again. If it keeps happening, try again tomorrow, or create " +
                                "a key in a new project.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    ExpandableCard(title = "Network problem or cannot reach Google") {
                        Text(
                            text = "Check that your phone is online. Then open system settings, go to Private " +
                                "DNS and set it to Automatic or Off. Turn off any VPN, ad blocker or DNS " +
                                "filter app, and try switching between Wi-Fi and mobile data. NextClass also " +
                                "tries a backup DNS by itself, and shows a short check result under the error.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    ExpandableCard(title = "AI Studio will not open or says it is not available") {
                        Text(
                            text = "Try a different Google account, preferably a personal one. School and " +
                                "work accounts are often blocked. Make sure you are signed in and have " +
                                "accepted the terms. Try the page again later if Google shows an error.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    ExpandableCard(title = "The scan finds wrong or missing classes") {
                        Text(
                            text = "Use a straight, sharp screenshot or the original PDF instead of a tilted " +
                                "photo. Type your section or group in the box so other sections are skipped. " +
                                "You can fix, add or delete any class on the review screen before saving.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 28.dp else 8.dp,
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                label = "dot",
            )
            Box(
                Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (index <= current) NcTheme.extras.live else NcTheme.extras.hairline,
                    ),
            )
        }
    }
}
