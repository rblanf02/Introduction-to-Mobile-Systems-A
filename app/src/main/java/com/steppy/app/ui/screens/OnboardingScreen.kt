package com.steppy.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import com.steppy.app.domain.Suggestions
import com.steppy.app.ui.*
import com.steppy.app.ui.theme.*

@Composable
fun OnboardingFlowScreen(
    uiState: SteppyUiState,
    onSaveSetup: (String, MainMotivation, ActivityLevel, Int) -> Unit,
    onSelectMascot: (Mascot) -> Unit,
    onFinishOnboarding: (manualMode: Boolean) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SteppyBackground)
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(SteppyMintLight.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(SteppyCircleCream)
        )

        when (step) {
            0 -> WelcomeStep(onGetStarted = { step = 1 })
            1 -> ProfileSetupStep(
                initialName = uiState.username,
                initialMotivation = uiState.motivation,
                initialActivity = uiState.activityLevel,
                onContinue = { name, mot, act, goal ->
                    onSaveSetup(name, mot, act, goal)
                    step = 2
                }
            )
            2 -> MascotSelectionStep(
                selectedMascot = uiState.selectedMascot,
                onSelectMascot = onSelectMascot,
                onContinue = { step = 3 }
            )
            3 -> PermissionStep(
                onPermissionGranted = { onFinishOnboarding(false) },
                onPermissionDeniedOrSkipped = { onFinishOnboarding(true) }
            )
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Steppy",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SteppyDarkNavy,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your daily movement, made simple.",
            fontSize = 18.sp,
            color = SteppyTextGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Let's get moving and make every step count.",
            fontSize = 16.sp,
            color = SteppyTextGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(36.dp))
        Button(
            onClick = onGetStarted,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SteppyDarkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
        ) {
            Text("Get started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileSetupStep(
    initialName: String,
    initialMotivation: MainMotivation,
    initialActivity: ActivityLevel,
    onContinue: (String, MainMotivation, ActivityLevel, Int) -> Unit
) {
    var username by remember { mutableStateOf(initialName) }
    var motivation by remember { mutableStateOf(initialMotivation) }
    var activity by remember { mutableStateOf(initialActivity) }
    // The suggested preset is preselected and follows the activity level until the user picks a goal
    var selectedGoal by remember { mutableIntStateOf(Suggestions.suggestedGoal(initialActivity)) }
    var goalPickedByUser by remember { mutableStateOf(false) }
    val suggestedGoal = Suggestions.suggestedGoal(activity)

    val goalOptions = listOf(
        3000 to "Relax (3,000 steps)",
        6000 to "Normal (6,000 steps)",
        10000 to "Intense (10,000 steps)",
        15000 to "Extreme (15,000 steps)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Set up your Steppy",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SteppyDarkNavy
        )
        Text(
            text = "Tell us a little about yourself to get started.",
            fontSize = 15.sp,
            color = SteppyTextGray
        )

        Text("Username", fontWeight = FontWeight.Bold, color = SteppyDarkNavy)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("Enter your username") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Main goal", fontWeight = FontWeight.Bold, color = SteppyDarkNavy)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MainMotivation.entries.forEach { item ->
                SelectableOptionPill(
                    label = item.label,
                    selected = motivation == item,
                    onClick = { motivation = item }
                )
            }
        }

        Text("How active are you currently?", fontWeight = FontWeight.Bold, color = SteppyDarkNavy)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevel.entries.forEach { level ->
                SelectableOptionPill(
                    label = level.label,
                    selected = activity == level,
                    onClick = {
                        activity = level
                        if (!goalPickedByUser) selectedGoal = Suggestions.suggestedGoal(level)
                    }
                )
            }
        }

        Text("Daily step goal", fontWeight = FontWeight.Bold, color = SteppyDarkNavy)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite)
        ) {
            Column {
                goalOptions.forEachIndexed { index, (steps, label) ->
                    val isSelected = selectedGoal == steps
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .background(if (isSelected) SteppyMintLight else SteppyCardWhite)
                            .clickable(role = Role.RadioButton) {
                                selectedGoal = steps
                                goalPickedByUser = true
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SteppyTealDark else SteppyDarkNavy
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (steps == suggestedGoal) {
                                Text(
                                    text = "Suggested",
                                    fontSize = 13.sp,
                                    color = SteppyTextGray
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected goal",
                                    tint = SteppyTealDark
                                )
                            }
                        }
                    }
                    if (index < goalOptions.lastIndex) {
                        HorizontalDivider(color = SteppyDivider)
                    }
                }
            }
        }

        Text(
            text = "Adjust anytime in your profile settings.",
            fontSize = 13.sp,
            color = SteppyTextGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { onContinue(username, motivation, activity, selectedGoal) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SteppyDarkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
        ) {
            Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SelectableOptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SteppyMintLight else SteppyCardWhite)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SteppyTeal else SteppyDivider,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) SteppyTealDark else SteppyDarkNavy
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = SteppyTealDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MascotSelectionStep(
    selectedMascot: Mascot,
    onSelectMascot: (Mascot) -> Unit,
    onContinue: () -> Unit
) {
    val mascots = Mascot.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose your mascot",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SteppyDarkNavy
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick a companion for your journey",
            fontSize = 15.sp,
            color = SteppyTextGray
        )
        Spacer(modifier = Modifier.height(24.dp))

        for (rowIndex in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (colIndex in 0..1) {
                    val mascot = mascots[rowIndex * 2 + colIndex]
                    val isSelected = selectedMascot == mascot
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SteppyMintLight else SteppyCardWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 140.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SteppyTeal else SteppyDivider,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(role = Role.RadioButton) { onSelectMascot(mascot) }
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${mascot.displayName} mascot${if (isSelected) ", selected" else ""}"
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = mascot.emoji, fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = mascot.displayName,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SteppyTealDark else SteppyDarkNavy
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(SteppyTeal)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SteppyDarkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
        ) {
            Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionStep(
    onPermissionGranted: () -> Unit,
    onPermissionDeniedOrSkipped: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onPermissionGranted() else onPermissionDeniedOrSkipped()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SteppyCardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Allow Steppy to access your steps?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteppyDarkNavy,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "We use your step count to track your progress and help you reach your daily goal.",
                    fontSize = 15.sp,
                    color = SteppyTextGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = "📱👣💚", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            onPermissionGranted()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SteppyDarkNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp)
                ) {
                    Text("Allow access", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onPermissionDeniedOrSkipped,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = "Use manual step logging instead",
                        color = SteppyTextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}