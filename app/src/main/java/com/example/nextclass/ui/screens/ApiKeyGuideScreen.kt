package com.example.nextclass.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nextclass.R
import com.example.nextclass.ui.components.ExpandableCard
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcCard
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.Reveal
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

private const val AI_STUDIO_URL = "https://aistudio.google.com/apikey"

private data class GuideStep(val title: String, val body: String)

private val STEPS = listOf(
    GuideStep(
        title = "Open Google AI Studio",
        body = "Open aistudio.google.com/apikey in your phone's browser. This is Google's free page for " +
            "creating the key that NextClass uses to read your routine.",
    ),
    GuideStep(
        title = "Sign in with Google",
        body = "Use any Google account. A personal Gmail account works best, because some school or " +
            "work accounts have AI Studio switched off. Accept the terms if Google asks.",
    ),
    GuideStep(
        title = "Create a key",
        body = "Tap Create API key. If Google asks for a project, choose Create API key in new project, " +
            "or pick a project you already have. The key appears in a few seconds.",
    ),
    GuideStep(
        title = "Copy the key",
        body = "The key is a long line of letters and numbers that starts with AIza. Tap the copy icon " +
            "next to it. Copy the whole key, with no spaces before or after it.",
    ),
    GuideStep(
        title = "Paste it into NextClass",
        body = "Open Settings in NextClass, paste the key into the API key box and tap Verify key. When " +
            "it says the key works, you can import your routine.",
    ),
)

@Composable
fun ApiKeyGuideScreen(
    hasKey: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val openStudio: () -> Unit = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AI_STUDIO_URL)))
        } catch (e: ActivityNotFoundException) {
            // No browser installed; the steps above still show the address to type.
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Reveal(index = 0) {
                NcCard {
                    MicroLabel(if (hasKey) "A key is saved on this phone" else "No key saved yet")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "NextClass uses Google's Gemini AI to read your routine. You need your own " +
                            "key, and it is free. It takes about two minutes.",
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            STEPS.forEachIndexed { index, step ->
                Reveal(index = index + 1) {
                    NcCard {
                        MicroLabel("Step ${index + 1} of ${STEPS.size}", color = NcTheme.extras.live)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = step.title,
                            style = NcText.Title,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = step.body,
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                        if (index == 0) {
                            Spacer(Modifier.height(14.dp))
                            PrimaryButton(
                                text = "Open AI Studio",
                                onClick = openStudio,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (index == STEPS.lastIndex) {
                            Spacer(Modifier.height(14.dp))
                            SecondaryButton(
                                text = "Go to Settings",
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Reveal(index = STEPS.size + 1) {
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

            Reveal(index = STEPS.size + 2) {
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
