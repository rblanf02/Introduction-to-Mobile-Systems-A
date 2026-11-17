package com.steppy.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.components.ManualStepEntryDialog
import com.steppy.app.ui.components.SteppyTopHeader
import com.steppy.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: SteppyUiState,
    onAddSteps: (Int) -> Unit,
    onNotificationClick: () -> Unit
) {
    val context = LocalContext.current
    var showManualDialog by remember { mutableStateOf(false) }
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    if (showManualDialog) {
        ManualStepEntryDialog(
            onDismiss = { showManualDialog = false },
            onConfirmSteps = onAddSteps
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
            title = "Hello ${uiState.username}!",
            subtitle = uiState.dateLabel,
            mascot = uiState.selectedMascot,
            onNotificationClick = onNotificationClick
        )

        if (uiState.isManualMode) {
            Surface(
                color = SteppyOrangeLight,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Manual tracking mode active. Tap '+ Log steps' to record your walks.",
                    color = SteppyDarkNavy,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        val percentInt = (uiState.stepProgressFraction * 100).toInt()
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "Today's steps: ${uiState.todaySteps} of ${uiState.dailyGoal} steps, $percentInt percent completed. ${uiState.stepsRemaining} steps remaining."
                }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S STEPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteppyTextGray,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = SteppyMintLight,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            if (uiState.isTodayGoalMet) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SteppyTealDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "$percentInt%",
                                color = SteppyTealDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatter.format(uiState.todaySteps),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SteppyDarkNavy
                )
                Text(
                    text = "of ${formatter.format(uiState.dailyGoal)} steps",
                    fontSize = 16.sp,
                    color = SteppyTextGray
                )

                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { uiState.stepProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = SteppyTeal,
                    trackColor = SteppyDivider
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (uiState.isTodayGoalMet) {
                        "Goal reached! Great job keeping your streak alive."
                    } else {
                        "${formatter.format(uiState.stepsRemaining)} steps to reach your goal"
                    },
                    fontSize = 14.sp,
                    color = if (uiState.isTodayGoalMet) SteppyTealDark else SteppyTextGray,
                    fontWeight = if (uiState.isTodayGoalMet) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Current streak: ${uiState.currentStreak} days. ${uiState.streakFreezes} streak freezes available."
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SteppyOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CURRENT STREAK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SteppyTextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${uiState.currentStreak}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SteppyDarkNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "days",
                            fontSize = 15.sp,
                            color = SteppyTextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep it up! (${uiState.streakFreezes} freeze)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteppyOrange
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Weekly goal: ${uiState.weeklyActiveDays} of 7 active days."
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SteppyTeal)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WEEKLY GOAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SteppyTextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${uiState.weeklyActiveDays}/7",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SteppyDarkNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "active days",
                            fontSize = 13.sp,
                            color = SteppyTextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep it up!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteppyTealDark
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "Experience and level: Level ${uiState.level}, ${uiState.totalXP} XP, ${uiState.xpToNextLevel} XP to Level ${uiState.level + 1}."
                }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Experience & Level",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteppyDarkNavy
                    )
                    Surface(color = SteppyMintLight, shape = RoundedCornerShape(50)) {
                        Text(
                            text = "Level ${uiState.level}",
                            color = SteppyTealDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${uiState.totalXP} XP",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SteppyDarkNavy
                    )
                    Text(
                        text = "${uiState.xpToNextLevel} XP to Level ${uiState.level + 1}",
                        fontSize = 13.sp,
                        color = SteppyTextGray
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { uiState.levelProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = SteppyTeal,
                    trackColor = SteppyDivider
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Next reward unlocked at Level ${uiState.level + 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyTealDark
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showManualDialog = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log steps", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val shareText = "I've walked ${formatter.format(uiState.todaySteps)} steps today on Steppy and reached a ${uiState.currentStreak}-day streak! (Level ${uiState.level})"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share your streak"))
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SteppyDarkNavy),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 50.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share streak", fontWeight = FontWeight.Bold)
            }
        }
    }
}