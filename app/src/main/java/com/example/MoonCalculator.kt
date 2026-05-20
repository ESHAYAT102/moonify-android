package com.example

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.PI

object MoonCalculator {
    const val LUNAR_CYCLE = 29.53058867
    
    // Reference new moon: January 6, 2000, 18:14:00 UTC
    private const val REF_NEW_MOON_MS = 947182440000L
    private const val DAY_IN_MS = 86400000.0

    fun calculateForDate(localDate: LocalDate): MoonPhaseInfo {
        // Convert LocalDate to UTC Instant (at noon to get a stable representative time for that day)
        val instant = localDate.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant()
        val timeMs = instant.toEpochMilli()
        
        val diffMs = timeMs - REF_NEW_MOON_MS
        val diffDays = diffMs.toDouble() / DAY_IN_MS
        
        var age = diffDays % LUNAR_CYCLE
        if (age < 0) {
            age += LUNAR_CYCLE
        }
        
        val progress = age / LUNAR_CYCLE
        
        // Illumination: 0% at New Moon (progress=0 or 1), 100% at Full Moon (progress=0.5)
        // Cosine-based formula: (1.0 - cos(progress * 2 * PI)) / 2
        val illumination = (1.0 - cos(progress * 2.0 * PI)) / 2.0
        
        val trend = if (progress < 0.5) "Waxing" else "Waning"
        
        val (phaseName, emoji) = when {
            progress < 0.03 || progress > 0.97 -> Pair("New Moon", "🌑")
            progress >= 0.03 && progress < 0.22 -> Pair("Waxing Crescent", "🌒")
            progress >= 0.22 && progress < 0.28 -> Pair("First Quarter", "🌓")
            progress >= 0.28 && progress < 0.47 -> Pair("Waxing Gibbous", "🌔")
            progress >= 0.47 && progress < 0.53 -> Pair("Full Moon", "🌕")
            progress >= 0.53 && progress < 0.72 -> Pair("Waning Gibbous", "🌖")
            progress >= 0.72 && progress < 0.78 -> Pair("Third Quarter", "🌗")
            else -> Pair("Waning Crescent", "🌘")
        }
        
        // Days till next new moon and full moon
        val nextNewMoonDays = if (age < LUNAR_CYCLE) {
            LUNAR_CYCLE - age
        } else {
            0.0
        }
        
        val halfCycle = LUNAR_CYCLE / 2.0
        val nextFullMoonDays = if (age < halfCycle) {
            halfCycle - age
        } else {
            (LUNAR_CYCLE + halfCycle) - age
        }
        
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
        val dateString = localDate.format(formatter)
        
        return MoonPhaseInfo(
            dateString = dateString,
            phaseName = phaseName,
            emoji = emoji,
            illumination = illumination,
            age = age,
            progress = progress,
            trend = trend,
            nextNewMoonDays = nextNewMoonDays,
            nextFullMoonDays = nextFullMoonDays
        )
    }
}

data class MoonPhaseInfo(
    val dateString: String,
    val phaseName: String,
    val emoji: String,
    val illumination: Double,
    val age: Double,
    val progress: Double,
    val trend: String,
    val nextNewMoonDays: Double,
    val nextFullMoonDays: Double,
)
