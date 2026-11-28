package com.example.nextclass.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nextclass.R
import com.example.nextclass.domain.model.Course
import com.example.nextclass.ui.CourseScanState
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcTextField
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

@Composable
fun CoursesScreen(
    courses: List<Course>,
    scan: CourseScanState,
    hasApiKey: Boolean,
    onBack: () -> Unit,
    onScan: (android.net.Uri) -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (Course) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var newCode by rememberSaveable { mutableStateOf("") }
    var newTitle by rememberSaveable { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onScan(uri)
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
                text = "Courses",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "intro") {
                Text(
                    text = "Upload a photo or PDF of your course list. NextClass reads each code and title and fills the titles into your classes.",
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
            }

            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = "Scan course list",
                        onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
                        enabled = scan !is CourseScanState.Scanning,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!hasApiKey) {
                        Text(
                            text = "Read on this phone with no internet. A free Gemini key helps with photos this reader cannot handle.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                        SecondaryButton(
                            text = "How to get a key",
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SecondaryButton(
                        text = "Add a course manually",
                        onClick = { adding = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item(key = "status") {
                when (scan) {
                    CourseScanState.Idle -> Unit
                    CourseScanState.Scanning -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MicroLabel("Reading course list")
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = NcTheme.extras.hairline,
                        )
                        Text(
                            text = "Gemini is reading your list. This usually takes 10 to 30 seconds.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }

                    is CourseScanState.Done -> Text(
                        text = "Added ${scan.count} ${if (scan.count == 1) "course" else "courses"}. Titles are now filled into matching classes.",
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    is CourseScanState.Failed -> Text(
                        text = scan.message,
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item(key = "header") {
                Column {
                    MicroLabel("${courses.size} ${if (courses.size == 1) "course" else "courses"}")
                    Hairline(Modifier.padding(top = 8.dp))
                }
            }

            items(courses, key = { it.key }) { course ->
                Column(Modifier.animateItem()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = course.code,
                                style = NcText.Title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = course.title,
                                style = NcText.Body,
                                color = NcTheme.extras.muted,
                            )
                        }
                        IconButton(onClick = { onDelete(course) }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove ${course.code}",
                                tint = NcTheme.extras.muted,
                            )
                        }
                    }
                    Hairline(Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Add course", style = NcText.Title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NcTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = "Course code",
                        placeholder = "CSE-2101",
                    )
                    NcTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = "Course title",
                        placeholder = "Algorithms",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newCode.isNotBlank() && newTitle.isNotBlank(),
                    onClick = {
                        onAdd(newCode, newTitle)
                        newCode = ""
                        newTitle = ""
                        adding = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { adding = false }) { Text("Cancel") }
            },
        )
    }
}
