package com.example.splitapp.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- Brand palette: the only place raw colour values live. Screens use MaterialTheme.colorScheme roles, or these
// ---- named tokens on the fixed dark-gradient screens (Login, Hub, Banned).

val BrandInk900 = Color(0xFF0F2027)
val BrandInk700 = Color(0xFF203A43)
val BrandInk500 = Color(0xFF2C5364)
val BrandCyan = Color(0xFF00B4DB)
val BrandCyanLight = Color(0xFF00F2FE)       // use for TEXT on the gradient: plain cyan on ink500 is only 3.4:1
val PositiveGreen = Color(0xFF2E7D32)
val NegativeRed = Color(0xFFC62828)
val OnBrandError = Color(0xFFFF8A80)         // error text on the dark gradient (5.3:1 on ink700)
val LightBackground = Color(0xFFF7F9FC)

/** The dark navy gradient shared by Login, the Hub and the Banned screen. */
val BrandGradient: Brush = Brush.verticalGradient(listOf(BrandInk900, BrandInk700, BrandInk500))

/** The cyan call-to-action gradient. Text on it is BrandInk900 (6.8:1). */
val BrandButtonGradient: Brush = Brush.horizontalGradient(listOf(BrandCyan, BrandCyanLight))

// ---- Light scheme
internal val LightPrimary = BrandInk700
internal val LightOnPrimary = Color.White
internal val LightPrimaryContainer = BrandInk500
internal val LightOnPrimaryContainer = Color.White
internal val LightSecondary = Color(0xFF00789A)          // a darker cyan: readable as text on white (5.1:1)
internal val LightSecondaryContainer = Color(0xFFD5F3FA)
internal val LightOnSecondaryContainer = Color(0xFF00363F)
internal val LightTertiary = PositiveGreen               // "is owed" / positive
internal val LightTertiaryContainer = Color(0xFFDFF2E0)
internal val LightOnTertiaryContainer = Color(0xFF1B5E20)
internal val LightError = NegativeRed                    // "owes" / destructive
internal val LightErrorContainer = Color(0xFFFBE0E0)
internal val LightOnErrorContainer = Color(0xFF8E1B1B)
internal val LightSurface = Color.White
internal val LightOnSurface = BrandInk900
internal val LightSurfaceVariant = Color(0xFFE6EDF0)
internal val LightOnSurfaceVariant = Color(0xFF46606A)
internal val LightOutline = Color(0xFF73898F)
internal val LightOutlineVariant = Color(0xFFC3D0D5)

// ---- Dark scheme
internal val DarkBackground = BrandInk900
internal val DarkSurface = Color(0xFF1A2F38)
internal val DarkOnSurface = Color(0xFFE6EEF1)
internal val DarkSurfaceVariant = Color(0xFF2C4651)
internal val DarkOnSurfaceVariant = Color(0xFFB0C4CC)
internal val DarkPrimary = BrandCyan
internal val DarkOnPrimary = Color(0xFF00212B)
internal val DarkPrimaryContainer = BrandInk700
internal val DarkOnPrimaryContainer = Color.White
internal val DarkSecondary = Color(0xFF4DD0E1)
internal val DarkSecondaryContainer = Color(0xFF0B4655)
internal val DarkOnSecondaryContainer = Color(0xFFCDF4FB)
internal val DarkTertiary = Color(0xFF81C784)
internal val DarkTertiaryContainer = Color(0xFF1B3D22)
internal val DarkOnTertiaryContainer = Color(0xFFC8E6C9)
internal val DarkError = OnBrandError
internal val DarkErrorContainer = Color(0xFF5C1F1F)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkOutline = Color(0xFF8CA4AD)
internal val DarkOutlineVariant = Color(0xFF3A5560)

/** Secondary text on the near-white cards of the fixed gradient screens (6.7:1 on white). */
val BrandMutedText = Color(0xFF46606A)
