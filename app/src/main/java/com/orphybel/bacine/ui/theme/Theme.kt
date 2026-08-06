package com.orphybel.bacine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF06070B)
val Surface1 = Color(0xFF12141C)
val Surface2 = Color(0xFF1C1F2A)
val Gold = Color(0xFFE8B33C)
val TextPrimary = Color(0xFFF2F3F7)
val TextSecondary = Color(0xFF9A9CA6)
val Danger = Color(0xFFE5484D)

/**
 * A projector in a dark room is the target: the palette stays dark so nothing
 * blows out, with a single warm accent carrying focus and primary actions.
 */
private val BaCineColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    secondary = Gold,
    onSecondary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = TextPrimary,
)

/**
 * Type is scaled up from the phone defaults. Everything here is read from a
 * couch several metres away, where Material's 14sp body text is unreadable.
 */
private val BaCineTypography = Typography(
    displayLarge = TextStyle(fontSize = 46.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

/** TV screens overscan; keep content off the physical edges. */
object TvDimens {
    val ScreenPaddingHorizontal = 48.dp
    val ScreenPaddingVertical = 32.dp
    val RowSpacing = 28.dp
    val CardSpacing = 16.dp
    val PosterWidth = 150.dp
    val PosterHeight = 225.dp
}

/** Always dark: this runs on a projector, never in a bright room. */
@Composable
fun BaCineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BaCineColorScheme,
        typography = BaCineTypography,
        content = content,
    )
}
