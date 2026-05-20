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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.RectangleShape
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
                    modifier = Modifier.weight(1.1f),
                    onDateSelected = { date -> selectedDate = date }
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BentoGridLayout(
                    moonInfo = selectedMoonInfo,
                    selectedDate = selectedDate,
                    onLogRequest = { cmd, out -> appendLog(cmd, out) },
                    isLandscape = false,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { date -> selectedDate = date }
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
    isLandscape: Boolean = false,
    onDateSelected: (LocalDate) -> Unit = {}
) {
    val nextFullMoonDate = selectedDate.plusDays(moonInfo.nextFullMoonDays.toLong())
    val nextNewMoonDate = selectedDate.plusDays(moonInfo.nextNewMoonDays.toLong())
    val nextFullStr = nextFullMoonDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    val nextNewStr = nextNewMoonDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))

    Card(
        modifier = modifier
            .testTag("info_card")
            .border(width = 1.dp, color = MochaSurface1, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MochaSurface0)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) 16.dp else 24.dp)
        ) {
            // Transparent, shadow-free, glow-free box containing large emoji
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (isLandscape) 44.dp else 52.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = moonInfo.emoji,
                    fontSize = if (isLandscape) 32.sp else 40.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 16.dp)
            ) {
                // Header (Phase Name & Date)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Phase Name
                    Text(
                        text = moonInfo.phaseName,
                        color = MochaMauve,
                        fontSize = if (isLandscape) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )

                    // Selected formatted Date: e.g., Wednesday, May 20, 2026
                    Text(
                        text = moonInfo.dateString,
                        color = MochaSubtext0,
                        fontSize = if (isLandscape) 13.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Compress detail rows to take up less vertical space
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
                ) {
                    // Detail rows matching the image layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Illumination: ",
                            color = MochaSubtext0,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = String.format(Locale.US, "%.1f%%", moonInfo.illumination * 100),
                            color = MochaYellow,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Moon Age: ",
                            color = MochaSubtext0,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = String.format(Locale.US, "%.1f days", moonInfo.age),
                            color = MochaGreen,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cycle Progress: ",
                            color = MochaSubtext0,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = String.format(Locale.US, "%.1f%%", moonInfo.progress * 100),
                            color = MochaBlue,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trend: ",
                            color = MochaSubtext0,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = moonInfo.trend,
                            color = MochaRed,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next Full Moon: ",
                            color = MochaSubtext0,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = nextFullStr,
                            color = MochaYellow,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next New Moon: ",
                            color = MochaSubtext0,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = nextNewStr,
                            color = MochaLavender,
                            fontSize = if (isLandscape) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
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
            modifier = Modifier
                .padding(if (isLandscape) 10.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
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
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)).uppercase(),
                    color = MochaText,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTitleClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
                        fontFamily = FontFamily.SansSerif,
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
                        fontSize = if (isLandscape) 9.sp else 11.sp,
                        modifier = Modifier.width(if (isLandscape) 30.dp else 36.dp),
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        .size(if (isLandscape) 30.dp else 36.dp, if (isLandscape) 36.dp else 46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "·",
                                        color = MochaOverlay0.copy(alpha = 0.25f),
                                        fontFamily = FontFamily.SansSerif,
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            isSelected -> 1.15f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cell_scale"
    )

    Box(
        modifier = Modifier
            .size(if (isLandscape) 30.dp else 36.dp, if (isLandscape) 36.dp else 46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(if (isLandscape) 6.dp else 8.dp))
            .background(
                when {
                    isSelected -> MochaMauve
                    isToday -> MochaSurface1
                    else -> Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default indie bounds to match our custom spring animation perfectly
                onClick = onClick
            )
            .testTag("day_cell_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp) // Added more bottom padding as requested
        ) {
            Text(
                text = dayNumber.toString(),
                color = when {
                    isSelected -> MochaCrust
                    isToday -> MochaLavender
                    else -> MochaText
                },
                fontFamily = FontFamily.SansSerif,
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

