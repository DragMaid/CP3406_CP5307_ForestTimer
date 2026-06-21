package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// Note: Using text labels instead of emojis for accessibility and clarity.

@SuppressLint("DefaultLocale")
@Composable
fun ForestTimerScreen(viewModel: ForestViewModel) {
    val settings by viewModel.settings.collectAsState()
    val sessionType by viewModel.sessionType.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val isPaused by viewModel.isTimerPaused.collectAsState()
    val completedSessionsInCycle by viewModel.completedSessionsInCycle.collectAsState()
    val activeTreeSpecies by viewModel.activeTreeSpecies.collectAsState()
    val completedDates by viewModel.completedSessionDates.collectAsState()

    val totalSeconds = when (sessionType) {
        SessionType.FOCUS -> settings.focusDurationMinutes * 60L
        SessionType.SHORT_BREAK -> settings.shortBreakDurationMinutes * 60L
        SessionType.LONG_BREAK -> settings.longBreakDurationMinutes * 60L
    }
    
    val progress = if (totalSeconds > 0) secondsRemaining.toFloat() / totalSeconds else 0f

    // Daily focus goal progress calculation
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todaySessions = completedDates.count { it == todayStr }
    val todayMinutes = todaySessions * settings.focusDurationMinutes
    val dailyGoalProgress = if (settings.dailyFocusGoalMinutes > 0) {
        todayMinutes.toFloat() / settings.dailyFocusGoalMinutes
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome, Header
        Text(
            text = "Welcome back, ${settings.displayName}!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Session Type Header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = sessionType.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // Active Tree Information
        val currentStage = TreeStage.fromInt(completedSessionsInCycle)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (sessionType == SessionType.FOCUS) "Growing a: ${activeTreeSpecies.displayName}" else "Relaxing break...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Current Stage: ${currentStage.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }

        // Tree visual display box
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(16.dp)
        ) {
            TreeCanvas(
                species = activeTreeSpecies,
                stage = if (sessionType == SessionType.FOCUS) currentStage else TreeStage.MATURE
            )
        }

        // Pomodoro Cycle session progress indicator (4 steps)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                val symbol = when {
                    i < completedSessionsInCycle -> "Tree"
                    i == completedSessionsInCycle && sessionType == SessionType.FOCUS && isRunning -> "Growing"
                    else -> "Empty"
                }
                Text(text = symbol, fontSize = 18.sp)
            }
        }

        // Circular Countdown Progress Ring
        val mins = secondsRemaining / 60
        val secs = secondsRemaining % 60
        val timeString = String.format("%02d:%02d", mins, secs)

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthValue = 10.dp.toPx()
                // Track
                drawCircle(
                    color = trackColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthValue, cap = StrokeCap.Round)
                )
                // Progress (starts from top -90f and sweeps clockwise)
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthValue, cap = StrokeCap.Round)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRunning) "Focusing" else if (isPaused) "Paused" else "Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Timer control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRunning && !isPaused) {
                Button(
                    onClick = { viewModel.startTimer() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (sessionType == SessionType.FOCUS) "Start Focus" else "Start Break", fontWeight = FontWeight.Bold)
                }
            } else {
                // Pause/Resume button
                Button(
                    onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer() },
                    modifier = Modifier.weight(1f),
                    colors = if (isRunning) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isRunning) "Pause" else "Resume", fontWeight = FontWeight.Bold)
                }

                // Skip button (only shown for FOCUS sessions)
                if (sessionType == SessionType.FOCUS) {
                    Button(
                        onClick = { viewModel.skipFocusSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Skip", fontWeight = FontWeight.Bold)
                    }
                }

                // Cancel button
                OutlinedButton(
                    onClick = { viewModel.cancelSession() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Daily Goal Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Goal Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("$todayMinutes / ${settings.dailyFocusGoalMinutes} min", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                LinearProgressIndicator(
                    progress = { dailyGoalProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                if (todayMinutes >= settings.dailyFocusGoalMinutes) {
                    Text("Goal achieved! Keep growing your forest!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun GardenScreen(viewModel: ForestViewModel) {
    val garden by viewModel.garden.collectAsState()
    
    // Sort and filter state
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSortOrder by remember { mutableStateOf("Newest") } // Newest, Oldest
    var selectedDetailTree by remember { mutableStateOf<GardenTree?>(null) }
    var showEmptyPlotDialog by remember { mutableStateOf(false) }

    // Statistics calculations
    val totalTrees = viewModel.totalMatureTreesCount
    val totalHours = viewModel.totalFocusHours
    val streak = viewModel.currentFocusStreak
    val avgSessions = viewModel.averageSessionsPerDay

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Productivity Garden",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Statistics Dashboard (2x2 grid)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Overall Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(title = "Mature Trees", value = "$totalTrees mature", modifier = Modifier.weight(1f))
                    StatCard(title = "Focus Hours", value = String.format("%.1f hrs", totalHours), modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(title = "Current Streak", value = "$streak days", modifier = Modifier.weight(1f))
                    StatCard(title = "Avg Sessions/Day", value = String.format("%.1f / day", avgSessions), modifier = Modifier.weight(1f))
                }
            }
        }

        // Filtering and Sorting Controls
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Filter by Species", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All") }
                    )
                }
                items(TreeSpecies.entries) { species ->
                    FilterChip(
                        selected = selectedFilter == species.displayName,
                        onClick = { selectedFilter = species.displayName },
                        label = { Text(species.displayName) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort order", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedSortOrder == "Newest",
                        onClick = { selectedSortOrder = "Newest" },
                        label = { Text("Newest First") }
                    )
                    FilterChip(
                        selected = selectedSortOrder == "Oldest",
                        onClick = { selectedSortOrder = "Oldest" },
                        label = { Text("Oldest First") }
                    )
                }
            }
        }

        // Processed list of garden trees
        val filteredAndSortedTrees = remember(garden, selectedFilter, selectedSortOrder) {
            var result = garden.asSequence()
            
            if (selectedFilter != "All") {
                result = result.filter { it.species.displayName == selectedFilter }
            }
            
            result = if (selectedSortOrder == "Newest") {
                result.sortedByDescending { it.completionDate }
            } else {
                result.sortedBy { it.completionDate }
            }
            
            result.toList()
        }

        // Garden plot calculation (minimum of 12 plots, multiple of 3)
        val plotsList = remember(filteredAndSortedTrees) {
            val listSize = filteredAndSortedTrees.size
            val targetSize = maxOf(12, ((listSize + 2) / 3) * 3)
            List(targetSize) { index ->
                if (index < listSize) filteredAndSortedTrees[index] else null
            }
        }

        // Visual Garden Meadows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.08f),
                            Color(0xFF2E7D32).copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(12.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(plotsList) { plotItem ->
                    GardenPlot(
                        tree = plotItem,
                        onClick = {
                            if (plotItem != null) {
                                selectedDetailTree = plotItem
                            } else {
                                showEmptyPlotDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedDetailTree?.let { tree ->
        Dialog(onDismissRequest = { selectedDetailTree = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = tree.species.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        TreeCanvas(species = tree.species, stage = TreeStage.MATURE)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow(label = "Date Completed", value = tree.completionDate)
                        DetailRow(label = "Time Invested", value = "${tree.totalFocusTimeMinutes} minutes")
                        DetailRow(label = "Cycle Reference", value = "Cycle #${tree.pomodoroCycleIndex}")
                    }

                    Button(
                        onClick = { selectedDetailTree = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Empty Plot Dialog
    if (showEmptyPlotDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyPlotDialog = false },
            title = { Text("Empty Plot") },
            text = { Text("Plant a new tree here by completing a focus session!") },
            confirmButton = {
                TextButton(onClick = { showEmptyPlotDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun GardenPlot(
    tree: GardenTree?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1B3B26) else Color(0xFFE8F5E9)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color(0xFF2E7D32) else Color(0xFFC8E6C9))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (tree != null) {
                TreeCanvas(species = tree.species, stage = TreeStage.MATURE)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tree.species.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // Empty garden plot with a cute daisy flower drawn on canvas
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Center disc
                    drawCircle(Color(0xFFFFD54F), radius = 3.dp.toPx(), center = Offset(cx, cy))
                    // Petals
                    for (angle in 0 until 360 step 72) {
                        val rad = Math.toRadians(angle.toDouble())
                        val px = cx + (5.dp.toPx() * Math.cos(rad)).toFloat()
                        val py = cy + (5.dp.toPx() * Math.sin(rad)).toFloat()
                        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.5f.dp.toPx(), center = Offset(px, py))
                    }
                    // Grass blades
                    drawLine(
                        color = Color(0xFF81C784),
                        start = Offset(cx - 10.dp.toPx(), cy + 10.dp.toPx()),
                        end = Offset(cx - 12.dp.toPx(), cy + 5.dp.toPx()),
                        strokeWidth = 1.5f.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF81C784),
                        start = Offset(cx - 10.dp.toPx(), cy + 10.dp.toPx()),
                        end = Offset(cx - 8.dp.toPx(), cy + 4.dp.toPx()),
                        strokeWidth = 1.5f.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsFormScreen(viewModel: ForestViewModel) {
    val currentSettings by viewModel.settings.collectAsState()

    // Local state for settings form input validation
    var displayName by remember { mutableStateOf(currentSettings.displayName) }
    var themeMode by remember { mutableStateOf(currentSettings.themeMode) }
    var accentColor by remember { mutableStateOf(currentSettings.accentColor) }
    var focusDuration by remember { mutableStateOf(currentSettings.focusDurationMinutes.toString()) }
    var shortBreak by remember { mutableStateOf(currentSettings.shortBreakDurationMinutes.toString()) }
    var longBreak by remember { mutableStateOf(currentSettings.longBreakDurationMinutes.toString()) }
    var autoStartBreak by remember { mutableStateOf(currentSettings.autoStartBreak) }
    var autoStartFocus by remember { mutableStateOf(currentSettings.autoStartFocus) }
    var enableSounds by remember { mutableStateOf(currentSettings.enableSounds) }
    var enableVibration by remember { mutableStateOf(currentSettings.enableVibration) }
    var dailyGoal by remember { mutableStateOf(currentSettings.dailyFocusGoalMinutes.toString()) }
    var speciesMode by remember { mutableStateOf(currentSettings.speciesMode) }
    var selectedSpecies by remember { mutableStateOf(currentSettings.selectedSpecies) }

    var saveSuccessMessage by remember { mutableStateOf(false) }

    // Validation Results
    val focusDurationVal = focusDuration.toIntOrNull() ?: 0
    val shortBreakVal = shortBreak.toIntOrNull() ?: 0
    val longBreakVal = longBreak.toIntOrNull() ?: 0
    val dailyGoalVal = dailyGoal.toIntOrNull() ?: 0

    val isFocusValid = focusDurationVal >= 25
    val isBreakValid = shortBreakVal in 1..<longBreakVal
    val isGoalValid = dailyGoalVal > 0
    
    val isFormValid = isFocusValid && isBreakValid && isGoalValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Theme and Accent Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode
            var themeExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = themeMode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Theme Mode") },
                    trailingIcon = {
                        IconButton(onClick = { themeExpanded = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Choose Theme")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                    listOf("System", "Light", "Dark").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                themeMode = option
                                themeExpanded = false
                            }
                        )
                    }
                }
            }

            // Accent Color
            var accentExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = accentColor,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Accent Color") },
                    trailingIcon = {
                        IconButton(onClick = { accentExpanded = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Choose Accent")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = accentExpanded, onDismissRequest = { accentExpanded = false }) {
                    listOf("Forest Green", "Emerald Green", "Cherry Pink", "Maple Red", "Birch Amber", "Ocean Blue").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                accentColor = option
                                accentExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Timer durations configuration with validations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Timer Durations (minutes)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = focusDuration,
                    onValueChange = { focusDuration = it },
                    label = { Text("Focus Session Duration") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isFocusValid,
                    supportingText = {
                        if (!isFocusValid) {
                            Text("Must be at least 25 minutes for growth eligibility.", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = shortBreak,
                        onValueChange = { shortBreak = it },
                        label = { Text("Short Break") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isBreakValid,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longBreak,
                        onValueChange = { longBreak = it },
                        label = { Text("Long Break") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isBreakValid,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!isBreakValid) {
                    Text(
                        text = "Short break duration must be less than long break duration.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Automatic Start options
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Automation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-start Break session")
                Switch(checked = autoStartBreak, onCheckedChange = { autoStartBreak = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-start Focus session")
                Switch(checked = autoStartFocus, onCheckedChange = { autoStartFocus = it })
            }
        }

        // Haptic feedback & Audio options
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Effects & Sounds", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Alarm Sound")
                Switch(checked = enableSounds, onCheckedChange = { enableSounds = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Vibration")
                Switch(checked = enableVibration, onCheckedChange = { enableVibration = it })
            }
        }

        // Species Mode & Goals
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Species Preferences & Goals", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = dailyGoal,
                    onValueChange = { dailyGoal = it },
                    label = { Text("Daily Focus Goal (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isGoalValid,
                    modifier = Modifier.fillMaxWidth()
                )

                var speciesModeExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = speciesMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tree Selection Mode") },
                        trailingIcon = {
                            IconButton(onClick = { speciesModeExpanded = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Choose Mode")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = speciesModeExpanded, onDismissRequest = { speciesModeExpanded = false }) {
                        listOf("Random", "Seasonal", "Manual").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    speciesMode = mode
                                    speciesModeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (speciesMode == "Manual") {
                    var treeSpeciesExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedSpecies.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selected Tree Species") },
                            trailingIcon = {
                                IconButton(onClick = { treeSpeciesExpanded = true }) {
                                    Icon(Icons.Default.Info, contentDescription = "Choose Tree")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = treeSpeciesExpanded, onDismissRequest = { treeSpeciesExpanded = false }) {
                            TreeSpecies.entries.forEach { species ->
                                DropdownMenuItem(
                                    text = { Text(species.displayName) },
                                    onClick = {
                                        selectedSpecies = species
                                        treeSpeciesExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = {
                if (isFormValid) {
                    val updated = AppSettings(
                        displayName = displayName,
                        themeMode = themeMode,
                        accentColor = accentColor,
                        focusDurationMinutes = focusDurationVal,
                        shortBreakDurationMinutes = shortBreakVal,
                        longBreakDurationMinutes = longBreakVal,
                        autoStartBreak = autoStartBreak,
                        autoStartFocus = autoStartFocus,
                        enableSounds = enableSounds,
                        enableVibration = enableVibration,
                        dailyFocusGoalMinutes = dailyGoalVal,
                        speciesMode = speciesMode,
                        selectedSpecies = selectedSpecies
                    )
                    viewModel.updateSettings(updated)
                    saveSuccessMessage = true
                }
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Settings", fontWeight = FontWeight.Bold)
        }

        if (saveSuccessMessage) {
            Snackbar(
                action = {
                    TextButton(onClick = { saveSuccessMessage = false }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Settings saved successfully!")
            }
        }
    }
}
