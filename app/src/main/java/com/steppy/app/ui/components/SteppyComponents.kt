package com.steppy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steppy.app.ui.Mascot
import com.steppy.app.ui.theme.*

@Composable
fun SteppyTopHeader(
    title: String,
    subtitle: String,
    mascot: Mascot,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SteppyMintLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mascot.emoji,
                    fontSize = 26.sp,
                    modifier = Modifier.semantics {
                        contentDescription = "${mascot.displayName} mascot avatar"
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyDarkNavy
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = SteppyTextGray
                )
            }
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .background(SteppyCardWhite)
                .border(1.dp, SteppyDivider, CircleShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open reminders settings",
                    onClick = onNotificationClick
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "Reminders and notifications"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = SteppyDarkNavy,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Default.Home),
    PROGRESS("progress", "Progress", Icons.Default.CheckCircle),
    CHALLENGES("challenges", "Challenges", Icons.Default.Star),
    PROFILE("profile", "Profile", Icons.Default.Person)
}

@Composable
fun SteppyBottomNavigationBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = SteppyCardWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val tint = if (selected) SteppyTealDark else SteppyTextGray

                Column(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            role = Role.Tab,
                            onClick = { onTabSelected(tab.route) }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${tab.label} tab${if (selected) ", selected" else ""}"
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tint
                    )
                }
            }
        }
    }
}

@Composable
fun ManualStepEntryDialog(
    onDismiss: () -> Unit,
    onConfirmSteps: (Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log steps manually", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Add steps walked without your phone or when the step sensor is unavailable.",
                    fontSize = 14.sp,
                    color = SteppyTextGray
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorText = null
                    },
                    label = { Text("Steps to add (1 - 50,000)") },
                    singleLine = true,
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        errorText?.let { Text(it, color = SteppyErrorRed) }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = input.toIntOrNull()
                    if (value != null && value in 1..50000) {
                        onConfirmSteps(value)
                        onDismiss()
                    } else {
                        errorText = "Enter a valid number between 1 and 50,000"
                    }
                },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Add steps")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GoalEditDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSaveGoal: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily step goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a preset or enter a custom goal (500 - 50,000):", fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3000, 6000, 10000).forEach { preset ->
                        FilterChip(
                            selected = goalText == preset.toString(),
                            onClick = {
                                goalText = preset.toString()
                                errorText = null
                            },
                            label = { Text("${preset / 1000}k") },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = goalText,
                    onValueChange = {
                        goalText = it
                        errorText = null
                    },
                    label = { Text("Custom daily goal") },
                    singleLine = true,
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        errorText?.let { Text(it, color = SteppyErrorRed) }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = goalText.toIntOrNull()
                    if (parsed != null && parsed in 500..50000) {
                        onSaveGoal(parsed)
                        onDismiss()
                    } else {
                        errorText = "Goal must be between 500 and 50,000 steps"
                    }
                },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Save goal")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}