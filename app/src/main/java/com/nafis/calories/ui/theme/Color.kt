package com.nafis.calories.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand Primary Palette (Refined Health Teal-Emerald)
val Primary = Color(0xFF0D9488)
val PrimaryVariant = Color(0xFF0F766E)
val PrimaryAccent = Color(0xFF10B981)
val PrimarySoft = Color(0xFFCCFBF1)
val SurfaceTint = Color(0xFFECFDF5)

// Studio & App Backgrounds
val LightBackground = Color(0xFFF4F6F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF8FAFC)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextTertiary = Color(0xFF94A3B8)

val DarkBackground = Color(0xFF0B0F17)
val DarkSurface = Color(0xFF131926)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Macro Semantic Colors (Theme-Adaptive)
val CarbColor = Color(0xFF3B82F6)
val CarbBgColor: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFEFF6FF)

val ProteinColor = Color(0xFF0D9488)
val ProteinBgColor: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF112A27) else Color(0xFFF0FDFA)

val FatColor = Color(0xFFF59E0B)
val FatBgColor: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2A2315) else Color(0xFFFFFBEB)

// Status & Special Semantic Colors
val WaterColor = Color(0xFF0284C7)
val WaterBgColor: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0C2A3A) else Color(0xFFE0F2FE)

val DangerColor = Color(0xFFE11D48)
val DangerBgColor: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF32141A) else Color(0xFFFFF1F2)

val CalorieHighlight = Color(0xFF10B981)
val PrimaryDark = Color(0xFF5EEAD4)