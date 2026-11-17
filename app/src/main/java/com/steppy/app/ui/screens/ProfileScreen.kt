package com.steppy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steppy.app.ui.Mascot
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.components.GoalEditDialog
import com.steppy.app.ui.components.SteppyTopHeader
import com.steppy.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    uiState: SteppyUiState,
    onUpdateGoal: (Int) -> Unit,
    onUpdateProfile: (String, Mascot) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onResetAllData: () -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    if (showGoalDialog) {
        GoalEditDialog(
            currentGoal = uiState.dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSaveGoal = onUpdateGoal
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = uiState.username,
            currentMascot = uiState.selectedMascot,
            onDismiss = { showEditProfileDialog = false },
            onSave = onUpdateProfile
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data Control", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Steppy stores your daily steps, goal, and mascot locally on your device. No weight, BMI, GPS location, or personal accounts are collected.\n\nYou can permanently erase all your local progress at any time."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPrivacyDialog = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SteppyErrorRed),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Delete my data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPrivacyDialog = false },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "• Daily Goal: Reach your target steps to grow your streak.\n" +
                            "• Streak Freeze: Protects your streak if you miss a day.\n" +
                            "• Manual Logging: Use '+ Log steps' on Today if your device lacks a step sensor."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelpDialog = false },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Got it")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SteppyBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SteppyTopHeader(
            title = "Profile",
            subtitle = uiState.dateLabel,
            mascot = uiState.selectedMascot,
            onNotificationClick = { onToggleReminders(!uiState.remindersEnabled) }
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SteppyMintLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.selectedMascot.emoji, fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = uiState.username,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteppyDarkNavy
                    )
                    TextButton(
                        onClick = { showEditProfileDialog = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text(
                            text = "Edit profile",
                            color = SteppyTealDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    title = "Language",
                    trailingText = uiState.language,
                    onClick = { }
                )
                HorizontalDivider(color = SteppyDivider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteppyDarkNavy
                    )
                    Switch(
                        checked = uiState.remindersEnabled,
                        onCheckedChange = onToggleReminders
                    )
                }
                HorizontalDivider(color = SteppyDivider)

                SettingsRow(
                    title = "Daily step goal",
                    trailingText = "${formatter.format(uiState.dailyGoal)}",
                    onClick = { showGoalDialog = true }
                )
                HorizontalDivider(color = SteppyDivider)

                SettingsRow(
                    title = "Privacy & security",
                    onClick = { showPrivacyDialog = true }
                )
                HorizontalDivider(color = SteppyDivider)

                SettingsRow(
                    title = "Help & support",
                    onClick = { showHelpDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onResetAllData,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteppyMintLight,
                contentColor = SteppyTealDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
        ) {
            Text("Log out / Reset app", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = SteppyDarkNavy
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    color = SteppyTealDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SteppyTextGray
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentMascot: Mascot,
    onDismiss: () -> Unit,
    onSave: (String, Mascot) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var mascot by remember { mutableStateOf(currentMascot) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display name") },
                    singleLine = true
                )
                Text("Companion mascot:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Mascot.entries.forEach { item ->
                        FilterChip(
                            selected = mascot == item,
                            onClick = { mascot = item },
                            label = { Text(item.emoji, fontSize = 20.sp) },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, mascot)
                    onDismiss()
                },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Save")
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