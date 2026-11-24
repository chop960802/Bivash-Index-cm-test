package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.models.AttendanceStatus

data class StatusColorPair(
  val text: Color,
  val container: Color,
  val border: Color = text,
  val containerEnd: Color = container,
  val iconTint: Color = text,
)

data class BivashStatusPalette(
  val present: StatusColorPair,
  val absent: StatusColorPair,
  val late: StatusColorPair,
  val leave: StatusColorPair,
  val off: StatusColorPair,
  val unmarked: StatusColorPair,
  val nightShift: StatusColorPair,
) {
  fun forStatus(status: AttendanceStatus?): StatusColorPair =
    when (status) {
      AttendanceStatus.PRESENT -> present
      AttendanceStatus.ABSENT -> absent
      AttendanceStatus.LATE -> late
      AttendanceStatus.LEAVE -> leave
      AttendanceStatus.OFF -> off
      null -> unmarked
    }
}

private val LightStatusPalette =
  BivashStatusPalette(
    present = StatusColorPair(PresentLightText, PresentLightBg),
    absent = StatusColorPair(AbsentLightText, AbsentLightBg),
    late = StatusColorPair(LateLightText, LateLightBg),
    leave = StatusColorPair(LeaveLightText, LeaveLightBg),
    off = StatusColorPair(OffLightText, OffLightBg),
    unmarked = StatusColorPair(UnmarkedLightText, UnmarkedLightBg),
    nightShift =
      StatusColorPair(
        text = NightShiftLightText,
        container = NightShiftLightBg,
        border = NightShiftLightBorder,
        containerEnd = NightShiftLightBgEnd,
        iconTint = NightShiftMoonAccent,
      ),
  )

private val DarkStatusPalette =
  BivashStatusPalette(
    present = StatusColorPair(PresentDarkText, PresentDarkBg),
    absent = StatusColorPair(AbsentDarkText, AbsentDarkBg),
    late = StatusColorPair(LateDarkText, LateDarkBg),
    leave = StatusColorPair(LeaveDarkText, LeaveDarkBg),
    off = StatusColorPair(OffDarkText, OffDarkBg),
    unmarked = StatusColorPair(UnmarkedDarkText, UnmarkedDarkBg),
    nightShift =
      StatusColorPair(
        text = NightShiftDarkText,
        container = NightShiftDarkBg,
        border = NightShiftDarkBorder,
        containerEnd = NightShiftDarkBgEnd,
        iconTint = NightShiftMoonAccent,
      ),
  )

data class DashboardCardColors(
  val startColor: Color,
  val endColor: Color,
  val contentColor: Color,
  val borderColor: Color,
  val badgeBg: Color,
)

data class DashboardCardPalette(
  val attendance: DashboardCardColors,
  val officeIndex: DashboardCardColors,
  val extra: DashboardCardColors,
)

private val LightDashboardPalette =
  DashboardCardPalette(
    attendance =
      DashboardCardColors(
        startColor = DashAttendanceDayStart,
        endColor = DashAttendanceDayEnd,
        contentColor = Color.White,
        borderColor = Color(0x33FFFFFF),
        badgeBg = Color(0x2EFFFFFF),
      ),
    officeIndex =
      DashboardCardColors(
        startColor = DashIndexDayStart,
        endColor = DashIndexDayEnd,
        contentColor = Color.White,
        borderColor = Color(0x33FFFFFF),
        badgeBg = Color(0x2EFFFFFF),
      ),
    extra =
      DashboardCardColors(
        startColor = DashExtraDayStart,
        endColor = DashExtraDayEnd,
        contentColor = Color.White,
        borderColor = Color(0x33FFFFFF),
        badgeBg = Color(0x2EFFFFFF),
      ),
  )

private val DarkDashboardPalette =
  DashboardCardPalette(
    attendance =
      DashboardCardColors(
        startColor = DashAttendanceNightStart,
        endColor = DashAttendanceNightEnd,
        contentColor = Color(0xFFF8FAFC),
        borderColor = DashAttendanceNightBorder.copy(alpha = 0.55f),
        badgeBg = Color(0x33FFFFFF),
      ),
    officeIndex =
      DashboardCardColors(
        startColor = DashIndexNightStart,
        endColor = DashIndexNightEnd,
        contentColor = Color(0xFFF8FAFC),
        borderColor = DashIndexNightBorder.copy(alpha = 0.55f),
        badgeBg = Color(0x33FFFFFF),
      ),
    extra =
      DashboardCardColors(
        startColor = DashExtraNightStart,
        endColor = DashExtraNightEnd,
        contentColor = Color(0xFFF8FAFC),
        borderColor = DashExtraNightBorder.copy(alpha = 0.55f),
        badgeBg = Color(0x33FFFFFF),
      ),
  )

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }
val LocalDashboardPalette = staticCompositionLocalOf { LightDashboardPalette }

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkSlatePrimary,
    onPrimary = DarkSlateOnPrimary,
    primaryContainer = DarkSlateContainer,
    onPrimaryContainer = DarkSlateOnContainer,
    secondary = DarkSteelSecondary,
    onSecondary = DarkSteelOnSecondary,
    secondaryContainer = DarkSteelContainer,
    onSecondaryContainer = DarkSteelOnContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = DarkAmberOnContainer,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = Color(0xFF0E121B),
    surfaceContainer = Color(0xFF141A26),
    surfaceContainerHigh = Color(0xFF1C2434),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SlateNavyPrimary,
    onPrimary = SlateNavyOnPrimary,
    primaryContainer = SlateNavyContainer,
    onPrimaryContainer = SlateNavyOnContainer,
    secondary = SteelSecondary,
    onSecondary = SteelOnSecondary,
    secondaryContainer = SteelContainer,
    onSecondaryContainer = SteelOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = CoolParchmentBg,
    onBackground = CoolParchmentOnBg,
    surface = CoolSurface,
    onSurface = CoolParchmentOnBg,
    surfaceVariant = CoolSurfaceVariant,
    onSurfaceVariant = CoolOnSurfaceVariant,
    surfaceContainerLow = Color(0xFFF0F4F9),
    surfaceContainer = Color(0xFFE9EFF6),
    surfaceContainerHigh = Color(0xFFE1E8F2),
    outline = CoolOutline,
    outlineVariant = CoolOutlineVariant,
  )

private val BivashShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun BivashIndexTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }
  val statusPalette = if (darkTheme) DarkStatusPalette else LightStatusPalette
  val dashboardPalette = if (darkTheme) DarkDashboardPalette else LightDashboardPalette

  CompositionLocalProvider(
    LocalStatusPalette provides statusPalette,
    LocalDashboardPalette provides dashboardPalette,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = BivashShapes,
      content = content,
    )
  }
}
