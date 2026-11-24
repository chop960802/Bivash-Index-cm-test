package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.logic.DateUtils
import com.example.models.AttendanceStatus
import com.example.models.DailyAttendanceSummary
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoLabelStyle
import com.example.ui.theme.MonoMetricStyle

@Composable
fun CardHeaderStrip(
  moduleTitle: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(14.dp)),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_ledger_header),
      contentDescription = stringResource(id = R.string.hero_banner_desc),
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color(0xF2152A40), Color(0xCC1B3B5A), Color(0x991B3B5A))
            )
          )
    )
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = "Offline mode active",
            tint = Color(0xFFFBBF58),
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "OFFLINE · SCHEMA v1",
            style = MonoLabelStyle,
            color = Color(0xFFE2E8F0),
          )
        }
        Surface(color = Color(0xFFE59A24), shape = RoundedCornerShape(50)) {
          Text(
            text = "v1.0",
            style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF152A40),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }
      Column {
        Text(
          text = stringResource(id = R.string.app_name),
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        Text(
          text = moduleTitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFD6E4F0),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun DateStepperBar(
  selectedDate: String,
  onPreviousDay: () -> Unit,
  onNextDay: () -> Unit,
  onJumpToday: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val today = DateUtils.todayIso()
  val isToday = selectedDate == today

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = onPreviousDay,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("date_prev_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Previous day",
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
          Modifier.weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isToday, onClick = onJumpToday)
            .padding(horizontal = 6.dp, vertical = 6.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.CalendarToday,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = DateUtils.formatReadableDate(selectedDate),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              if (isToday) "Today ($selectedDate)"
              else "${DateUtils.formatShortDateWithDay(selectedDate, today)} · $selectedDate",
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (!isToday) {
          TextButton(
            onClick = onJumpToday,
            modifier = Modifier.testTag("date_today_button"),
          ) {
            Text("Today")
          }
        }
      }

      IconButton(
        onClick = onNextDay,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("date_next_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Next day",
        )
      }
    }
  }
}

@Composable
fun StatusBadge(
  status: AttendanceStatus?,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val colors = palette.forStatus(status)
  val label = status?.label ?: "Unmarked"

  Surface(
    modifier = modifier,
    color = colors.container,
    shape = RoundedCornerShape(50),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(colors.text))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = colors.text,
      )
    }
  }
}

@Composable
fun ShiftHighlightBadge(
  shiftName: String,
  timeWindow: String?,
  isNightShift: Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val label = if (timeWindow.isNullOrBlank()) shiftName else "$shiftName ($timeWindow)"

  if (isNightShift) {
    Surface(
      modifier = modifier,
      shape = RoundedCornerShape(10.dp),
      color = Color.Transparent,
      border = BorderStroke(1.25.dp, palette.nightShift.border),
    ) {
      Row(
        modifier =
          Modifier.background(
              Brush.horizontalGradient(
                colors = listOf(palette.nightShift.container, palette.nightShift.containerEnd)
              )
            )
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.NightsStay,
          contentDescription = "Night Shift",
          tint = palette.nightShift.iconTint,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = palette.nightShift.text,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Box(
          modifier =
            Modifier.size(5.dp).clip(CircleShape).background(palette.nightShift.iconTint)
        )
      }
    }
  } else {
    Surface(
      modifier = modifier,
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.WbSunny,
          contentDescription = "Day Shift",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(13.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ShiftFilterChip(
  label: String,
  isNightShift: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  if (isNightShift) {
    FilterChip(
      selected = selected,
      onClick = onClick,
      modifier = modifier,
      leadingIcon = {
        Icon(
          imageVector = Icons.Outlined.NightsStay,
          contentDescription = "Night Shift",
          tint = palette.nightShift.iconTint,
          modifier = Modifier.size(16.dp),
        )
      },
      label = {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
      },
      border =
        FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = selected,
          borderColor = palette.nightShift.border,
          selectedBorderColor = palette.nightShift.iconTint,
          borderWidth = 1.25.dp,
          selectedBorderWidth = 1.75.dp,
        ),
      colors =
        FilterChipDefaults.filterChipColors(
          containerColor = palette.nightShift.container.copy(alpha = 0.88f),
          labelColor = palette.nightShift.text,
          iconColor = palette.nightShift.iconTint,
          selectedContainerColor = palette.nightShift.containerEnd,
          selectedLabelColor = palette.nightShift.text,
          selectedLeadingIconColor = palette.nightShift.iconTint,
        ),
    )
  } else {
    FilterChip(
      selected = selected,
      onClick = onClick,
      modifier = modifier,
      label = { Text(label) },
    )
  }
}

@Composable
fun AttendanceStatTile(
  label: String,
  count: Int,
  textColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
      Text(
        text = count.toString(),
        style = MonoMetricStyle,
        color = textColor,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = textColor.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun AttendanceProportionalBar(
  summary: DailyAttendanceSummary,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val total =
    (summary.presentCount +
        summary.lateCount +
        summary.leaveCount +
        summary.offCount +
        summary.absentCount +
        summary.unmarkedSlotsCount)
      .coerceAtLeast(1)

  val segments =
    listOf(
      summary.presentCount to palette.present.text,
      summary.lateCount to palette.late.text,
      summary.leaveCount to palette.leave.text,
      summary.offCount to palette.off.text,
      summary.absentCount to palette.absent.text,
      summary.unmarkedSlotsCount to palette.unmarked.container,
    ).filter { it.first > 0 }

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(9.dp)
        .clip(RoundedCornerShape(50))
        .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    var currentX = 0f
    val gap = if (segments.size > 1) 3.dp.toPx() else 0f
    val usableWidth = (size.width - gap * (segments.size - 1)).coerceAtLeast(1f)
    segments.forEachIndexed { idx, (count, color) ->
      val segWidth = (count.toFloat() / total.toFloat()) * usableWidth
      drawRoundRect(
        color = color,
        topLeft = Offset(currentX, 0f),
        size = Size(segWidth, size.height),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
      )
      currentX += segWidth + if (idx < segments.lastIndex) gap else 0f
    }
  }
}

@Composable
fun EmptyStateCard(
  title: String,
  message: String,
  icon: ImageVector = Icons.Outlined.Info,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun PillTag(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
  borderColor: Color? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = containerColor,
    border = borderColor?.let { BorderStroke(1.dp, it) },
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun TintedPillTag(
  text: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  PillTag(
    text = text,
    containerColor = accentColor.copy(alpha = if (isDark) 0.24f else 0.14f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    borderColor = accentColor.copy(alpha = if (isDark) 0.65f else 0.45f),
    modifier = modifier,
  )
}

@Composable
fun GradientAccentStrip(
  palette: GradientPalette,
  modifier: Modifier = Modifier,
  height: Dp = 4.dp,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .background(
          Brush.horizontalGradient(colors = palette.colors)
        )
  )
}

@Composable
fun VibrantFeatureBanner(
  title: String,
  subtitle: String,
  palette: GradientPalette,
  floralVariant: FloralVariant,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  badgeText: String? = null,
  leadingContent: (@Composable () -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  val shape = RoundedCornerShape(18.dp)
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = shape,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = palette.colors,
              start = Offset.Zero,
              end = Offset(920f, 380f),
            )
          )
    ) {
      FloralTextureOverlay(
        variant = floralVariant,
        motifColor = Color.White,
        modifier = Modifier.matchParentSize(),
      )
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.horizontalGradient(
                colors =
                  listOf(
                    Color(0xAD0B1020),
                    Color(0x6E0B1020),
                    Color(0x360B1020),
                  )
              )
            )
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (leadingContent != null) {
          leadingContent()
          Spacer(modifier = Modifier.width(6.dp))
        } else {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x470B1020),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = title,
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  shadow =
                    Shadow(
                      color = Color(0x66000000),
                      offset = Offset(0f, 1.5f),
                      blurRadius = 3f,
                    ),
                ),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (!badgeText.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x520B1020),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f)),
              ) {
                Text(
                  text = badgeText,
                  style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (trailingContent != null) {
          Spacer(modifier = Modifier.width(10.dp))
          trailingContent()
        }
      }
    }
  }
}

@Composable
fun StatusFilterChip(
  status: AttendanceStatus,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val colors = palette.forStatus(status)
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = {
      Box(
        modifier =
          Modifier.size(8.dp)
            .clip(CircleShape)
            .background(colors.text)
      )
    },
    label = {
      Text(
        text = status.label,
        style =
          MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
          ),
      )
    },
    border =
      FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = colors.text.copy(alpha = 0.40f),
        selectedBorderColor = colors.text,
        borderWidth = 1.dp,
        selectedBorderWidth = 1.5.dp,
      ),
    colors =
      FilterChipDefaults.filterChipColors(
        containerColor = colors.container.copy(alpha = 0.38f),
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = colors.container,
        selectedLabelColor = colors.text,
      ),
  )
}

@Composable
fun SolidPillTag(
  text: String,
  containerColor: Color,
  contentColor: Color = Color.White,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = containerColor,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun DetailMetaPair(
  label: String,
  value: String,
  labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = labelColor,
      maxLines = 1,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
      color = valueColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun ReferenceDetailCard(
  accentColor: Color,
  isNightShift: Boolean = false,
  tintContainerColor: Color? = null,
  modifier: Modifier = Modifier,
  content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
  val palette = LocalStatusPalette.current
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val borderColor =
    if (isNightShift) palette.nightShift.border
    else accentColor.copy(alpha = if (isDark) 0.72f else 0.74f)
  val cardShape = RoundedCornerShape(14.dp)

  val backgroundColor =
    when {
      isNightShift -> Color.Transparent
      tintContainerColor != null ->
        tintContainerColor.copy(alpha = if (isDark) 0.34f else 0.48f)
      else -> accentColor.copy(alpha = if (isDark) 0.14f else 0.08f)
    }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = cardShape,
    color = if (isNightShift) Color.Transparent else MaterialTheme.colorScheme.surface,
    border = BorderStroke(if (isNightShift) 1.75.dp else 1.15.dp, borderColor),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth().let { base ->
          if (isNightShift) {
            base.background(
              Brush.linearGradient(
                colors = listOf(palette.nightShift.container, palette.nightShift.containerEnd),
                start = Offset.Zero,
                end = Offset(900f, 520f),
              )
            )
          } else {
            base.background(backgroundColor)
          }
        }
    ) {
      if (isNightShift) {
        FloralTextureOverlay(
          variant = FloralVariant.RADIAL_PETALS,
          motifColor = palette.nightShift.iconTint.copy(alpha = 0.35f),
          modifier = Modifier.matchParentSize(),
        )
      }
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
      )
    }
  }
}

class SyncedTextFieldBuffer(initialValue: String) {
  private val pendingDispatches = ArrayDeque<String>()
  var currentText: String = initialValue
    private set

  fun onLocalEdit(rawNext: String, sanitize: (String) -> String = { it }): String {
    val cleanNext = sanitize(rawNext)
    if (cleanNext != currentText) {
      currentText = cleanNext
      if (pendingDispatches.size >= 32) {
        pendingDispatches.removeFirst()
      }
      pendingDispatches.addLast(cleanNext)
    }
    return cleanNext
  }

  fun onExternalValueChanged(externalValue: String): Boolean {
    if (externalValue == currentText) {
      pendingDispatches.clear()
      return false
    }
    val idx = pendingDispatches.indexOfFirst { it == externalValue }
    if (idx >= 0) {
      repeat(idx + 1) { pendingDispatches.removeFirst() }
      return false
    }
    pendingDispatches.clear()
    currentText = externalValue
    return true
  }
}

@Composable
fun rememberSyncedText(
  externalValue: String,
  onValueChange: (String) -> Unit,
  sanitize: (String) -> String = { it },
): Pair<String, (String) -> Unit> {
  val buffer = remember { SyncedTextFieldBuffer(externalValue) }
  var localText by remember { mutableStateOf(externalValue) }
  val lastExternal = remember { mutableStateOf(externalValue) }

  if (lastExternal.value != externalValue) {
    lastExternal.value = externalValue
    if (buffer.onExternalValueChanged(externalValue)) {
      localText = buffer.currentText
    }
  }

  val handleEdit: (String) -> Unit = { rawNext ->
    val prev = localText
    val cleanNext = buffer.onLocalEdit(rawNext, sanitize)
    localText = cleanNext
    if (cleanNext != prev) {
      onValueChange(cleanNext)
    }
  }
  return localText to handleEdit
}

fun isPrintableKeyCode(keyCode: Int): Boolean =
  (keyCode in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9) ||
    (keyCode in android.view.KeyEvent.KEYCODE_A..android.view.KeyEvent.KEYCODE_Z) ||
    (keyCode in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9) ||
    keyCode == android.view.KeyEvent.KEYCODE_SPACE ||
    keyCode == android.view.KeyEvent.KEYCODE_COMMA ||
    keyCode == android.view.KeyEvent.KEYCODE_PERIOD ||
    keyCode == android.view.KeyEvent.KEYCODE_MINUS ||
    keyCode == android.view.KeyEvent.KEYCODE_EQUALS ||
    keyCode == android.view.KeyEvent.KEYCODE_SLASH ||
    keyCode == android.view.KeyEvent.KEYCODE_APOSTROPHE ||
    keyCode == android.view.KeyEvent.KEYCODE_SEMICOLON ||
    keyCode == android.view.KeyEvent.KEYCODE_LEFT_BRACKET ||
    keyCode == android.view.KeyEvent.KEYCODE_RIGHT_BRACKET ||
    keyCode == android.view.KeyEvent.KEYCODE_BACKSLASH ||
    keyCode == android.view.KeyEvent.KEYCODE_AT ||
    keyCode == android.view.KeyEvent.KEYCODE_PLUS ||
    keyCode == android.view.KeyEvent.KEYCODE_POUND ||
    keyCode == android.view.KeyEvent.KEYCODE_STAR

fun isHardwareEchoDuplicate(
  keyCode: Int,
  repeatCount: Int,
  eventTimeMs: Long,
  lastKeyCode: Int,
  lastEventTimeMs: Long,
  windowMs: Long = 28L,
): Boolean {
  if (repeatCount != 0 || !isPrintableKeyCode(keyCode)) return false
  if (keyCode != lastKeyCode || lastEventTimeMs < 0L) return false
  val delta = eventTimeMs - lastEventTimeMs
  return delta in 0L..windowMs
}


