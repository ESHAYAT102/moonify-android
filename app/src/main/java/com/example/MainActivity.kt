package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MochaBase
                ) { innerPadding ->
                    MoonifyApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MoonifyApp(modifier: Modifier = Modifier) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    
    // Terminal Log State
    val terminalLogs = remember { mutableStateListOf<String>() }
    
    // Helper to add log and keep only last 5 entries
    fun appendLog(command: String, output: String) {
        terminalLogs.add("$ $command")
        terminalLogs.add(output)
        while (terminalLogs.size > 8) {
            terminalLogs.removeAt(0)
        }
    }
    
    // Trigger initial log
    LaunchedEffect(Unit) {
        appendLog("moonify --init", "Initializing Moonify CLI core... OK.")
        val info = MoonCalculator.calculateForDate(selectedDate)
        appendLog("moonify --status", "Today: ${info.dateString} | Phase: ${info.emoji} ${info.phaseName}")
    }
    
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus to capture key events on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val selectedMoonInfo = remember(selectedDate) {
        MoonCalculator.calculateForDate(selectedDate)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft, Key.H -> {
                            selectedDate = selectedDate.minusDays(1)
                            currentMonth = selectedDate.withDayOfMonth(1)
                            appendLog("moonify --selectprev", "Moved back 1 day. Date: $selectedDate")
                            true
                        }
                        Key.DirectionRight, Key.L -> {
                            selectedDate = selectedDate.plusDays(1)
                            currentMonth = selectedDate.withDayOfMonth(1)
                            appendLog("moonify --selectnext", "Moved forward 1 day. Date: $selectedDate")
                            true
                        }
                        Key.DirectionUp, Key.K -> {
                            selectedDate = selectedDate.minusWeeks(1)
                            currentMonth = selectedDate.withDayOfMonth(1)
                            appendLog("moonify --selectprevweek", "Moved back 1 week. Date: $selectedDate")
                            true
                        }
                        Key.DirectionDown, Key.J -> {
                            selectedDate = selectedDate.plusWeeks(1)
                            currentMonth = selectedDate.withDayOfMonth(1)
                            appendLog("moonify --selectnextweek", "Moved forward 1 week. Date: $selectedDate")
                            true
                        }
                        Key.N -> {
                            currentMonth = currentMonth.plusMonths(1)
                            selectedDate = selectedDate.plusMonths(1)
                            appendLog("moonify --addmonth", "Jumped forward 1 month. View: ${currentMonth.month}")
                            true
                        }
                        Key.P -> {
                            currentMonth = currentMonth.minusMonths(1)
                            selectedDate = selectedDate.minusMonths(1)
                            appendLog("moonify --submonth", "Jumped backward 1 month. View: ${currentMonth.month}")
                            true
                        }
                        Key.T -> {
                            selectedDate = LocalDate.now()
                            currentMonth = selectedDate.withDayOfMonth(1)
                            appendLog("moonify --today", "Reset viewpoint to today.")
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .background(MochaBase)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BentoGridLayout(
                    moonInfo = selectedMoonInfo,
                    selectedDate = selectedDate,
                    onLogRequest = { cmd, out -> appendLog(cmd, out) },
                    isLandscape = true,
                    modifier = Modifier.weight(1.1f)
                )

                CalendarCard(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = date
                        val info = MoonCalculator.calculateForDate(date)
                        appendLog("moonify --select ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}", "Viewport focused on ${info.emoji} ${info.phaseName}")
                    },
                    onMonthChanged = { nextMonth ->
                        currentMonth = nextMonth
                        appendLog("moonify --view ${nextMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))}", "Jumped to ${nextMonth.month.getDisplayName(TextStyle.FULL, Locale.US)}")
                    },
                    onTitleClick = {
                        selectedDate = LocalDate.now()
                        currentMonth = LocalDate.now().withDayOfMonth(1)
                        appendLog("moonify --today", "Reset view & selection to today.")
                    },
                    isLandscape = true,
                    modifier = Modifier.weight(0.9f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BentoGridLayout(
                        moonInfo = selectedMoonInfo,
                        selectedDate = selectedDate,
                        onLogRequest = { cmd, out -> appendLog(cmd, out) },
                        isLandscape = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CalendarCard(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = date
                        val info = MoonCalculator.calculateForDate(date)
                        appendLog("moonify --select ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}", "Viewport focused on ${info.emoji} ${info.phaseName}")
                    },
                    onMonthChanged = { nextMonth ->
                        currentMonth = nextMonth
                        appendLog("moonify --view ${nextMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))}", "Jumped to ${nextMonth.month.getDisplayName(TextStyle.FULL, Locale.US)}")
                    },
                    onTitleClick = {
                        selectedDate = LocalDate.now()
                        currentMonth = LocalDate.now().withDayOfMonth(1)
                        appendLog("moonify --today", "Reset view & selection to today.")
                    },
                    isLandscape = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BentoGridLayout(
    moonInfo: MoonPhaseInfo,
    selectedDate: LocalDate,
    onLogRequest: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card 1: Main Large Bento Card (scaled down slightly for landscape)
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .testTag("info_card")
                    .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MochaSurface0)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = moonInfo.phaseName,
                                color = MochaMauve,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Illum: ${String.format("%.1f %%", moonInfo.illumination * 100)}",
                                color = MochaText.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        // Interactive/Glowing Moon Emoji
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MochaBase, CircleShape)
                                .border(1.dp, MochaSurface1, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = moonInfo.emoji,
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Retro progress bar
                    RetroProgressBar(
                        progress = moonInfo.illumination.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Right side column of mini Bento boxes
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Lunar Age & Cycle Progress
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "LUNAR AGE",
                                color = MochaSubtext0,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = String.format("%.2f d", moonInfo.age),
                                color = MochaText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "CYCLE PROGRESS",
                                color = MochaSubtext0,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = String.format("%.1f %%", moonInfo.progress * 100),
                                color = MochaText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Row 2: Next Full Moon & Next New Moon
                val nextFullMoonDate = selectedDate.plusDays(moonInfo.nextFullMoonDays.toLong())
                val nextNewMoonDate = selectedDate.plusDays(moonInfo.nextNewMoonDays.toLong())
                val nextFullStr = nextFullMoonDate.format(DateTimeFormatter.ofPattern("MMM d"))
                val nextNewStr = nextNewMoonDate.format(DateTimeFormatter.ofPattern("MMM d"))

                Row(
                    modifier = Modifier.weight(1.1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp))
                            .clickable {
                                onLogRequest("query --full-moon", "Projecting date to next Full Moon: $nextFullMoonDate")
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "NEXT FULL MOON",
                                color = MochaLavender,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = nextFullStr,
                                color = MochaText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp))
                            .clickable {
                                onLogRequest("query --new-moon", "Projecting date to next New Moon: $nextNewMoonDate")
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "NEXT NEW MOON",
                                color = MochaLavender,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = nextNewStr,
                                color = MochaText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Main Large Bento Card (Spans full width)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("info_card")
                    .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MochaSurface0)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = moonInfo.phaseName,
                                color = MochaMauve,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Illumination: ${String.format("%.1f %%", moonInfo.illumination * 100)}",
                                color = MochaText.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        // Interactive/Glowing Moon Emoji
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MochaBase, CircleShape)
                                .border(1.dp, MochaSurface1, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Ambient glowing shadow effect
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.15f
                                        scaleY = 1.15f
                                    }
                                    .shadow(
                                        elevation = 14.dp,
                                        shape = CircleShape,
                                        spotColor = MochaMauve,
                                        ambientColor = MochaMauve
                                    )
                            )
                            
                            Text(
                                text = moonInfo.emoji,
                                fontSize = 32.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Beautiful custom Retro progress bar embedded in Bento
                    RetroProgressBar(
                        progress = moonInfo.illumination.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Row of 2 Bento mini-cards: Age & Cycle Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mini Card Left: Lunar Age
                Card(
                    modifier = Modifier
                        .weight(0.5f)
                        .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LUNAR AGE",
                            color = MochaSubtext0,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = String.format("%.2f d", moonInfo.age),
                            color = MochaText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Mini Card Right: Cycle Progress
                Card(
                    modifier = Modifier
                        .weight(0.5f)
                        .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CYCLE PROGRESS",
                            color = MochaSubtext0,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = String.format("%.1f %%", moonInfo.progress * 100),
                            color = MochaText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Row of 2 Bento clickable upcoming event cards (Next Full / Next New moons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val nextFullMoonDate = selectedDate.plusDays(moonInfo.nextFullMoonDays.toLong())
                val nextNewMoonDate = selectedDate.plusDays(moonInfo.nextNewMoonDays.toLong())
                val nextFullStr = nextFullMoonDate.format(DateTimeFormatter.ofPattern("MMM d"))
                val nextNewStr = nextNewMoonDate.format(DateTimeFormatter.ofPattern("MMM d"))

                // Clickable Bento column for Next Full Moon
                Card(
                    modifier = Modifier
                        .weight(0.5f)
                        .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp))
                        .clickable {
                            onLogRequest("query --full-moon", "Projecting date to next Full Moon: $nextFullMoonDate")
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NEXT FULL MOON",
                            color = MochaLavender,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "$nextFullStr",
                            color = MochaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Clickable Bento column for Next New Moon
                Card(
                    modifier = Modifier
                        .weight(0.5f)
                        .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp))
                        .clickable {
                            onLogRequest("query --new-moon", "Projecting date to next New Moon: $nextNewMoonDate")
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MochaSurface0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NEXT NEW MOON",
                            color = MochaLavender,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "$nextNewStr",
                            color = MochaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCard(
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_card")
            .border(1.dp, MochaSurface0, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MochaMantle)
    ) {
        Column(
            modifier = Modifier.padding(if (isLandscape) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 10.dp)
        ) {
            // Month navigators styled clean style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MochaSurface0)
                        .clickable { onMonthChanged(currentMonth.minusMonths(1)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◀",
                        color = MochaLavender,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)).uppercase(),
                    color = MochaText,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTitleClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MochaSurface0)
                        .clickable { onMonthChanged(currentMonth.plusMonths(1)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = MochaLavender,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Grid column headers for weekdays (Bento Mockup Style S, M, T, W, T...)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        color = MochaSubtext0,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = if (isLandscape) 8.sp else 10.sp,
                        modifier = Modifier.width(if (isLandscape) 28.dp else 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Standard monthly grid calculations
            val yearMonth = YearMonth.of(currentMonth.year, currentMonth.month)
            val daysInMonth = yearMonth.lengthOfMonth()
            val firstDayOfWeek = currentMonth.withDayOfMonth(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val startOffset = (firstDayOfWeek % 7) // Convert to Sunday starting index (0 = Sunday, 1 = Monday...)

            val totalCellsCount = daysInMonth + startOffset
            val rowsEstimate = (totalCellsCount + 6) / 7

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rowIndex in 0 until rowsEstimate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (colIndex in 0 until 7) {
                            val cellIndex = rowIndex * 7 + colIndex
                            val dayNumber = cellIndex - startOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentMonth.withDayOfMonth(dayNumber)
                                val moonInfo = remember(cellDate) {
                                    MoonCalculator.calculateForDate(cellDate)
                                }
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == LocalDate.now()

                                CalendarDayCell(
                                    dayNumber = dayNumber,
                                    moonEmoji = moonInfo.emoji,
                                    isSelected = isSelected,
                                    isToday = isToday,
                                    isLandscape = isLandscape,
                                    onClick = { onDateSelected(cellDate) }
                                )
                            } else {
                                // Background celestial tiny star element for spacing
                                Box(
                                    modifier = Modifier
                                        .size(if (isLandscape) 28.dp else 34.dp, if (isLandscape) 38.dp else 46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "·",
                                        color = MochaOverlay0.copy(alpha = 0.25f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = if (isLandscape) 9.sp else 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    moonEmoji: String,
    isSelected: Boolean,
    isToday: Boolean,
    isLandscape: Boolean = false,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    // Spring feedback animation on tap
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
            )
            scale.animateTo(1.0f)
        }
    }

    Box(
        modifier = Modifier
            .size(if (isLandscape) 28.dp else 34.dp, if (isLandscape) 38.dp else 46.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(if (isLandscape) 6.dp else 8.dp))
            .background(
                when {
                    isSelected -> MochaMauve
                    isToday -> MochaSurface1
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                when {
                    isSelected -> Color.Transparent
                    isToday -> MochaLavender
                    else -> Color.Transparent
                },
                RoundedCornerShape(if (isLandscape) 6.dp else 8.dp)
            )
            .clickable { onClick() }
            .testTag("day_cell_$dayNumber")
            .let { modifier ->
                // Implements glowing shadow for active selected card as specified
                if (isSelected) {
                    modifier.shadow(
                        elevation = if (isLandscape) 4.dp else 8.dp,
                        shape = RoundedCornerShape(if (isLandscape) 6.dp else 8.dp),
                        spotColor = MochaMauve,
                        ambientColor = MochaMauve
                    )
                } else {
                    modifier
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = dayNumber.toString(),
                color = when {
                    isSelected -> MochaCrust
                    isToday -> MochaLavender
                    else -> MochaText
                },
                fontFamily = FontFamily.Monospace,
                fontSize = if (isLandscape) 10.sp else 12.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = moonEmoji,
                fontSize = if (isLandscape) 10.sp else 12.sp,
                lineHeight = if (isLandscape) 10.sp else 12.sp
            )
        }
    }
}

// Removed TerminalLogPanel definition

@Composable
fun MoonVisual(progress: Double, modifier: Modifier = Modifier) {
    val p = progress.toFloat()
    val isWaxing = p < 0.5f
    val isGibbous = p >= 0.25f && p < 0.75f
    
    // Calculate the width factor of the elliptical shadow/light transition
    val ellipticalFactor = abs(cos(p * 2.0 * PI)).toFloat()
    
    val litColor = Color(0xFFF9E2AF)   // Mocha Yellow (representing glowing moon)
    val shadowColor = Color(0xFF313244) // Mocha Surface0 (representing dark crescent/shadow part)

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        
        // Step 1: Draw base circle
        val baseColor = if (isGibbous) litColor else shadowColor
        drawCircle(
            color = baseColor,
            radius = radius,
            center = center
        )
        
        // Step 2: Draw the semi-circle on the other side
        // If isGibbous, we want to cover the dark side with a dark semi-circle
        // If NOT isGibbous (Crescent), we want to cover the lit side with a lit semi-circle
        val semiColor = if (isGibbous) shadowColor else litColor
        
        // Which side is the "other" side?
        val startAngle = if (isGibbous) {
            // Dark semi-circle side
            if (isWaxing) 90f else 270f
        } else {
            // Lit semi-circle side
            if (isWaxing) 270f else 90f
        }
        
        drawArc(
            color = semiColor,
            startAngle = startAngle,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f)
        )
        
        // Step 3: Draw central ellipse to blend
        val ellipseColor = if (isGibbous) litColor else shadowColor
        val ellipseWidth = radius * 2f * ellipticalFactor
        
        drawOval(
            color = ellipseColor,
            topLeft = Offset(center.x - ellipseWidth / 2f, center.y - radius),
            size = Size(ellipseWidth, radius * 2f)
        )
    }
}

@Composable
fun RetroProgressBar(progress: Float, modifier: Modifier = Modifier) {
    // Render string of hashes representing progress, like [#####.....]
    val barSize = 15
    val activeBlocks = (progress * barSize).toInt().coerceIn(0, barSize)
    val inactiveBlocks = barSize - activeBlocks
    
    val barText = buildString {
        append("[")
        for (i in 0 until activeBlocks) append("#")
        for (i in 0 until inactiveBlocks) append("·")
        append("]")
    }
    
    Text(
        text = barText,
        color = MochaMauve,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = modifier
    )
}

