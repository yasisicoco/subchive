package com.example.submate.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.activity.compose.LocalActivity
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val Forest = Color(0xFF3E5668)
val Lime = Color(0xFFE6EBEF)
private val Light = lightColorScheme(primary = Forest, onPrimary = Color.White, primaryContainer = Color(0xFFEDF1F4), onPrimaryContainer = Color(0xFF354C5E), secondary = Color(0xFF65717D), secondaryContainer = Lime, onSecondaryContainer = Color(0xFF354C5E), background = Color(0xFFF5F6F7), surface = Color.White, surfaceVariant = Color(0xFFF0F2F4), onBackground = Color(0xFF20262D), onSurface = Color(0xFF20262D), onSurfaceVariant = Color(0xFF69737E), outline = Color(0xFFB9C1C9), outlineVariant = Color(0xFFE2E6EA), surfaceContainer = Color(0xFFF0F2F4), surfaceContainerLow = Color(0xFFF7F8F9), surfaceContainerHighest = Color(0xFFE9EDF0))
private val Dark = darkColorScheme(primary = Color(0xFFBDD0DE), onPrimary = Color(0xFF213342), primaryContainer = Color(0xFF2B3843), onPrimaryContainer = Color(0xFFD3DFE7), secondary = Color(0xFFADB9C4), secondaryContainer = Color(0xFF303A43), onSecondaryContainer = Color(0xFFD3DFE7), background = Color(0xFF15191D), surface = Color(0xFF1E242A), surfaceVariant = Color(0xFF292F36), onBackground = Color(0xFFE6E9ED), onSurface = Color(0xFFE6E9ED), onSurfaceVariant = Color(0xFFA5AFBA), outline = Color(0xFF65717D), outlineVariant = Color(0xFF343C45), surfaceContainer = Color(0xFF252C33), surfaceContainerLow = Color(0xFF1A2026), surfaceContainerHighest = Color(0xFF303841))
private val CompactTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp)
)

@Composable
fun SubMateTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "dark" || mode == "system" && isSystemInDarkTheme()
    val activity = LocalActivity.current
    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = CompactTypography, shapes = Shapes(extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(20.dp)), content = content)
}
