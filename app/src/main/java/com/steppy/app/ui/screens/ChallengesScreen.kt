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
import com.steppy.app.ui.ChallengeUiItem
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.components.SteppyTopHeader
import com.steppy.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ChallengesScreen(
    uiState: SteppyUiState,
    onActivateChallenge: (String) -> Unit,
    onNotificationClick: () -> Unit
) {
    val activeChallenges = uiState.challenges.filter { it.isActive }
    val availableChallenges = uiState.challenges.filter { !it.isActive }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SteppyBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SteppyTopHeader(
            title = "Challenges",
            subtitle = uiState.dateLabel,
            mascot = uiState.selectedMascot,
            onNotificationClick = onNotificationClick
        )

        Text(
            text = "Active challenges (${activeChallenges.size}/3)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SteppyDarkNavy
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                activeChallenges.forEach { challenge ->
                    ActiveChallengeRow(challenge = challenge)
                }
            }
        }

        Text(
            text = "Available challenges",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SteppyDarkNavy
        )

        availableChallenges.forEach { challenge ->
            AvailableChallengeCard(
                challenge = challenge,
                onActivate = { onActivateChallenge(challenge.id) }
            )
        }
    }
}

@Composable
private fun ActiveChallengeRow(challenge: ChallengeUiItem) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                val status = if (challenge.isCompleted) "Completed" else "In progress"
                contentDescription = "${challenge.title}, $status, ${challenge.currentProgress} of ${challenge.targetProgress} ${challenge.unit}, plus ${challenge.xpReward} XP"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyDarkNavy
                )
                if (challenge.isCompleted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SteppyTealDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = "+${challenge.xpReward} XP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SteppyTealDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatter.format(challenge.currentProgress)} / ${formatter.format(challenge.targetProgress)} ${challenge.unit}",
            fontSize = 14.sp,
            color = SteppyTextGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { challenge.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = SteppyTeal,
            trackColor = SteppyDivider
        )
    }
}

@Composable
private fun AvailableChallengeCard(
    challenge: ChallengeUiItem,
    onActivate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyDarkNavy,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "+${challenge.xpReward} XP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyOrange
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = challenge.description,
                fontSize = 14.sp,
                color = SteppyTextGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onActivate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteppyMintLight,
                        contentColor = SteppyTealDark
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 48.dp)
                ) {
                    Text("Activate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}