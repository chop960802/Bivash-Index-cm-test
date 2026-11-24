package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.CalendarDateBreakdown
import com.example.models.CalendarDayCell
import com.example.models.CalendarShiftGroup
import com.example.models.DailyAttendanceRow
import com.example.models.DailyAttendanceSummary
import com.example.ui.components.AttendanceProportionalBar
import com.example.ui.components.AttendanceStatTile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.PillTag
import com.example.ui.components.StatusBadge
import com.example.ui.components.TintedPillTag
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoLabelStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceGistCounterCard(
  summary: DailyAttendanceSummary,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val gistPalette = GradientPaletteRegistry.paletteForCard("attendance_gist_${summary.date}", 11)
  Surface(
    modifier = modifier.fillMaxWidth().testTag("attendance_gist_card"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, gistPalette.colors.first().copy(alpha = 0.45f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = gistPalette, height = 4.dp)
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Attendance Gist · ${DateUtils.formatShortDateWithDay(summary.date)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          )
          TintedPillTag(
            text = "${summary.totalActiveEmployees} Staff · ${summary.scheduledSlotsCount} Scheduled",
            accentColor = gistPalette.colors.first(),
          )
        }

        AttendanceProportionalBar(summary = summary)

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AttendanceStatTile(
            label = "Total",
            count = summary.totalActiveEmployees,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
          )
          AttendanceStatTile(
            label = "Assigned",
            count = summary.scheduledSlotsCount,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
          )
          AttendanceStatTile(
            label = "Present",
            count = summary.presentCount,
            textColor = palette.present.text,
            containerColor = palette.present.container,
          )
          AttendanceStatTile(
            label = "Absent",
            count = summary.absentCount,
            textColor = palette.absent.text,
            containerColor = palette.absent.container,
          )
          AttendanceStatTile(
            label = "Late",
            count = summary.lateCount,
            textColor = palette.late.text,
            containerColor = palette.late.container,
          )
          AttendanceStatTile(
            label = "Leave",
            count = summary.leaveCount,
            textColor = palette.leave.text,
            containerColor = palette.leave.container,
          )
          AttendanceStatTile(
            label = "Off",
            count = summary.offCount,
            textColor = palette.off.text,
            containerColor = palette.off.container,
          )
        }
      }
    }
  }
}

@Composable
fun AttendanceCalendarGridCard(
  year: Int,
  month1Based: Int,
  selectedDate: String,
  cells: List<CalendarDayCell>,
  onStepMonth: (Int) -> Unit,
  onJumpToday: () -> Unit,
  onSelectDate: (String) -> Unit,
  onOpenDailyRegister: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val calPalette = GradientPaletteRegistry.paletteForCard("attendance_calendar", 2)
  val todayIso = DateUtils.todayIso()
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

  Surface(
    modifier = modifier.fillMaxWidth().testTag("attendance_calendar_card"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, calPalette.colors.first().copy(alpha = 0.45f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = calPalette, height = 4.dp)
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { onStepMonth(-1) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("calendar_prev_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = "Previous month",
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = DateUtils.formatMonthYearTitle(year, month1Based),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (selectedDate != todayIso) {
              TextButton(onClick = onJumpToday) { Text("Jump to Today") }
            }
          }
          IconButton(
            onClick = { onStepMonth(1) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("calendar_next_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Next month",
            )
          }
        }

        // Weekday labels
        Row(modifier = Modifier.fillMaxWidth()) {
          listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
            Text(
              text = dayName,
              style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Calendar grid rows
        cells.chunked(7).forEach { week ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            week.forEach { cell ->
              val isSelected = cell.dateIso == selectedDate
              val isToday = cell.dateIso == todayIso
              val s = cell.summary
              val hasAnyActivity =
                s.scheduledSlotsCount > 0 ||
                  s.presentCount > 0 ||
                  s.absentCount > 0 ||
                  s.lateCount > 0 ||
                  s.leaveCount > 0 ||
                  s.offCount > 0

              val cellBg =
                when {
                  isSelected -> MaterialTheme.colorScheme.primaryContainer
                  isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                  !cell.isInCurrentMonth -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
                  else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
              val cellBorder =
                when {
                  isSelected -> BorderStroke(1.75.dp, MaterialTheme.colorScheme.primary)
                  isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                  else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = cellBg,
                border = cellBorder,
                modifier =
                  Modifier.weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectDate(cell.dateIso) }
                    .testTag("calendar_day_${cell.dateIso}"),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = cell.dayOfMonth.toString(),
                    style =
                      MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                      ),
                    color =
                      when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        !cell.isInCurrentMonth ->
                          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.onSurface
                      },
                  )

                  Spacer(modifier = Modifier.height(3.dp))

                  if (hasAnyActivity || cell.hasNightShift) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      if (cell.hasNightShift) {
                        Box(
                          modifier =
                            Modifier.size(6.dp)
                              .clip(CircleShape)
                              .background(
                                if (isDark) palette.nightShift.border
                                else palette.nightShift.containerEnd
                              )
                        )
                      }
                      if (s.presentCount > 0) {
                        Box(
                          modifier = Modifier.size(5.dp).clip(CircleShape).background(palette.present.text)
                        )
                      }
                      if (s.lateCount > 0) {
                        Box(
                          modifier = Modifier.size(5.dp).clip(CircleShape).background(palette.late.text)
                        )
                      }
                      if (s.absentCount > 0) {
                        Box(
                          modifier = Modifier.size(5.dp).clip(CircleShape).background(palette.absent.text)
                        )
                      }
                      if (s.leaveCount > 0 || s.offCount > 0) {
                        Box(
                          modifier = Modifier.size(5.dp).clip(CircleShape).background(palette.leave.text)
                        )
                      }
                    }
                  } else {
                    Spacer(modifier = Modifier.height(6.dp))
                  }
                }
              }
            }
          }
        }

        // Legend & Quick Action
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.NightsStay,
              contentDescription = null,
              tint = palette.nightShift.border,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "Night Shift · Dots show status",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          FilledTonalButton(
            onClick = onOpenDailyRegister,
            modifier = Modifier.testTag("calendar_open_register_button"),
          ) {
            Text("Edit Day")
          }
        }
      }
    }
  }
}

@Composable
fun CalendarDateBreakdownSection(
  breakdown: CalendarDateBreakdown,
  selectedDate: String,
  onEditRemarks: (DailyAttendanceRow) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().testTag("calendar_breakdown_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (
      breakdown.workingShiftGroups.isEmpty() &&
        breakdown.offOrAbsenceRows.isEmpty() &&
        breakdown.unmarkedUnscheduledRows.isEmpty()
    ) {
      EmptyStateCard(
        title = "No records on $selectedDate",
        message = "Register team members in Settings or schedule shifts in Roster to view grouped shift, department, and duty details here.",
        icon = Icons.Outlined.EventBusy,
      )
      return
    }

    breakdown.workingShiftGroups.forEachIndexed { idx, group ->
      CalendarShiftGroupCard(
        group = group,
        paletteIndex = idx,
        onEditRemarks = onEditRemarks,
      )
    }

    if (breakdown.offOrAbsenceRows.isNotEmpty()) {
      CalendarAbsenceGroupCard(
        rows = breakdown.offOrAbsenceRows,
        onEditRemarks = onEditRemarks,
      )
    }

    if (breakdown.unmarkedUnscheduledRows.isNotEmpty()) {
      CalendarUnscheduledGroupCard(
        rows = breakdown.unmarkedUnscheduledRows,
        onEditRemarks = onEditRemarks,
      )
    }
  }
}

@Composable
private fun CalendarShiftGroupCard(
  group: CalendarShiftGroup,
  paletteIndex: Int,
  onEditRemarks: (DailyAttendanceRow) -> Unit,
) {
  val palette = LocalStatusPalette.current
  val shiftPalette = GradientPaletteRegistry.paletteForCard(group.shiftKey, paletteIndex)
  val isNight = group.isNightShift
  val accentColor =
    if (isNight) palette.nightShift.border else shiftPalette.colors.first()
  val titleColor = if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface
  val subColor =
    if (isNight) palette.nightShift.iconTint else MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isNight) Color.Transparent else MaterialTheme.colorScheme.surface,
    border = BorderStroke(if (isNight) 1.75.dp else 1.dp, accentColor.copy(alpha = 0.60f)),
    modifier = Modifier.fillMaxWidth().testTag("calendar_shift_group_${group.shiftKey}"),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth().let { base ->
          if (isNight) {
            base.background(
              Brush.linearGradient(
                colors = listOf(palette.nightShift.container, palette.nightShift.containerEnd),
                start = Offset.Zero,
                end = Offset(900f, 380f),
              )
            )
          } else {
            base
          }
        }
    ) {
      if (isNight) {
        FloralTextureOverlay(
          variant = FloralVariant.RADIAL_PETALS,
          motifColor = palette.nightShift.iconTint.copy(alpha = 0.25f),
          modifier = Modifier.matchParentSize(),
        )
      }
      Column(modifier = Modifier.fillMaxWidth()) {
        if (!isNight) {
          GradientAccentStrip(palette = shiftPalette, height = 4.dp)
        }
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (isNight) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny,
                contentDescription = null,
                tint = if (isNight) palette.nightShift.iconTint else accentColor,
                modifier = Modifier.size(18.dp),
              )
              Column {
                Text(
                  text = group.shiftName,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = titleColor,
                )
                Text(
                  text = group.timeWindow,
                  style = MonoLabelStyle,
                  color = subColor,
                )
              }
            }
            if (isNight) {
              PillTag(
                text = "NIGHT · ${group.totalCount}",
                containerColor = Color(0x3DFFFFFF),
                contentColor = palette.nightShift.text,
                borderColor = palette.nightShift.border.copy(alpha = 0.70f),
              )
            } else {
              TintedPillTag(
                text = "${group.totalCount} working",
                accentColor = accentColor,
              )
            }
          }

          group.departmentGroups.forEach { deptGroup ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color =
                if (isNight) Color(0x26FFFFFF)
                else MaterialTheme.colorScheme.surfaceContainerLow,
              border =
                BorderStroke(
                  1.dp,
                  if (isNight) palette.nightShift.border.copy(alpha = 0.40f)
                  else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Apartment,
                      contentDescription = null,
                      tint = if (isNight) palette.nightShift.iconTint else accentColor,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = deptGroup.departmentName,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = titleColor,
                    )
                  }
                  Text(
                    text = "${deptGroup.totalCount} staff",
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor,
                  )
                }

                deptGroup.dutyGroups.forEach { dutyGroup ->
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = null,
                        tint = subColor,
                        modifier = Modifier.size(14.dp),
                      )
                      Text(
                        text = "${dutyGroup.dutyTitle} (${dutyGroup.rows.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = subColor,
                      )
                    }
                    dutyGroup.rows.forEach { row ->
                      CalendarCompactEmployeeRow(
                        row = row,
                        isNight = isNight,
                        onEditRemarks = { onEditRemarks(row) },
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
  }
}

@Composable
private fun CalendarAbsenceGroupCard(
  rows: List<DailyAttendanceRow>,
  onEditRemarks: (DailyAttendanceRow) -> Unit,
) {
  val absPalette = GradientPaletteRegistry.paletteForCard("calendar_absence", 15)
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, absPalette.colors.first().copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth().testTag("calendar_absence_group"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = absPalette, height = 4.dp)
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "OFF / Leave / Absent",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          )
          TintedPillTag(
            text = "${rows.size} staff",
            accentColor = absPalette.colors.first(),
          )
        }
        rows.forEach { row ->
          val contextBits =
            listOfNotNull(
              row.shift?.name,
              row.department?.name ?: row.existingRecord?.departmentNameSnapshot?.ifBlank { null },
              row.duty?.title ?: row.existingRecord?.dutyTitleSnapshot?.ifBlank { null },
            ).joinToString(" · ")
          CalendarCompactEmployeeRow(
            row = row,
            isNight = false,
            extraContext = contextBits.ifBlank { null },
            onEditRemarks = { onEditRemarks(row) },
          )
        }
      }
    }
  }
}

@Composable
private fun CalendarUnscheduledGroupCard(
  rows: List<DailyAttendanceRow>,
  onEditRemarks: (DailyAttendanceRow) -> Unit,
) {
  var expanded by remember(rows.size) { mutableStateOf(rows.size <= 8) }
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().testTag("calendar_unmarked_group"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Unscheduled & Unmarked (${rows.size})",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        TextButton(onClick = { expanded = !expanded }) {
          Text(if (expanded) "Hide" else "Show")
        }
      }
      if (expanded) {
        rows.forEach { row ->
          CalendarCompactEmployeeRow(
            row = row,
            isNight = false,
            onEditRemarks = { onEditRemarks(row) },
          )
        }
      }
    }
  }
}

@Composable
private fun CalendarCompactEmployeeRow(
  row: DailyAttendanceRow,
  isNight: Boolean,
  extraContext: String? = null,
  onEditRemarks: () -> Unit,
) {
  val palette = LocalStatusPalette.current
  val currentStatus = row.existingRecord?.status
  val titleColor = if (isNight) palette.nightShift.text else MaterialTheme.colorScheme.onSurface
  val metaColor =
    if (isNight) Color(0xFFC4B5FD) else MaterialTheme.colorScheme.onSurfaceVariant

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = "${row.employee.code} · ${row.employee.name}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val detailLine =
          listOfNotNull(
            row.employee.designation.ifBlank { null },
            extraContext,
            row.existingRecord?.remarks?.takeIf { it.isNotBlank() },
          ).joinToString(" · ")
        if (detailLine.isNotBlank()) {
          Text(
            text = detailLine,
            style = MaterialTheme.typography.bodySmall,
            color = metaColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        StatusBadge(status = currentStatus)
        IconButton(
          onClick = onEditRemarks,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = "Edit attendance for ${row.employee.name}",
            tint = if (isNight) palette.nightShift.iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    HorizontalDivider(
      color =
        if (isNight) palette.nightShift.border.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
  }
}
