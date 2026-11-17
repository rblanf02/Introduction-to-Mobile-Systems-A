package com.steppy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.components.SteppyTopHeader
import com.steppy.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProgressScreen(
    uiState: SteppyUiState,
    onNotificationClick: () -> Unit
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SteppyBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SteppyTopHeader(
            title = "Your progress",
            subtitle = uiState.dateLabel,
            mascot = uiState.selectedMascot,
            onNotificationClick = onNotificationClick
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "Current level: Level ${uiState.level}, ${uiState.totalXP} XP, ${uiState.xpToNextLevel} XP to level up."
                }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "CURRENT LEVEL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyTextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = SteppyMintLight, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "Level ${uiState.level}",
                        color = SteppyTealDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                        text = "${uiState.xpToNextLevel} XP to level up",
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep moving to reach Level ${uiState.level + 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyTealDark
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniMetricCard(
                label = "TOTAL STEPS",
                value = formatter.format(uiState.totalHistoricalSteps),
                unit = "steps",
                dotColor = SteppyTeal,
                modifier = Modifier.weight(1f)
            )
            MiniMetricCard(
                label = "ACTIVE DAYS",
                value = "${uiState.totalActiveDays}",
                unit = "days",
                dotColor = SteppyOrange,
                modifier = Modifier.weight(1f)
            )
            MiniMetricCard(
                label = "CURRENT STREAK",
                value = "${uiState.currentStreak}",
                unit = "days",
                dotColor = SteppyTeal,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Steps this week",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteppyDarkNavy
                    )
                    Text(
                        text = "Goal: ${formatter.format(uiState.dailyGoal)}",
                        fontSize = 13.sp,
                        color = SteppyTextGray
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    uiState.weeklyBars.forEach { day ->
                        val barColor = when {
                            day.isToday -> SteppyOrange
                            day.goalMet -> SteppyTeal
                            else -> SteppyTeal.copy(alpha = 0.45f)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {
                                    val status = if (day.goalMet) "Goal met" else "Goal not met"
                                    contentDescription = "${day.fullDayName}: ${day.steps} steps, $status"
                                }
                        ) {
                            if (day.goalMet) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SteppyTealDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(125.dp)
                                    .clip(CircleShape)
                                    .background(SteppyDivider),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(day.fillFraction)
                                        .clip(CircleShape)
                                        .background(barColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = day.dayLabel,
                                fontWeight = if (day.isToday || day.goalMet) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.isToday) SteppyDarkNavy else SteppyTextGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    unit: String,
    dotColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value $unit"
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SteppyTextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SteppyDarkNavy
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = SteppyTextGray
            )
        }
    }
}