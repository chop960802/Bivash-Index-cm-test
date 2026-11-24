package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.logic.DateUtils
import com.example.models.Department
import com.example.models.Employee
import com.example.models.Shift
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.PillTag
import com.example.ui.components.ShiftHighlightBadge
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeManagerPane(
  employees: List<Employee>,
  departments: List<Department>,
  assignedShiftsByEmployee: Map<String, List<Shift>> = emptyMap(),
  onSaveEmployee: (String?, String, String, String, Boolean, String?) -> Unit,
  onToggleActive: (Employee) -> Unit,
  onDeleteEmployee: (Employee) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showInactive by remember { mutableStateOf(true) }
  var filterDeptId by remember { mutableStateOf<String?>(null) }
  var editingEmployee by remember { mutableStateOf<Employee?>(null) }
  var deletingEmployee by remember { mutableStateOf<Employee?>(null) }
  var showCreateDialog by remember { mutableStateOf(false) }

  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val deptMap = remember(departments) { departments.associateBy { it.id } }

  val displayed =
    remember(employees, showInactive, filterDeptId) {
      employees.filter { emp ->
        val statusMatch = showInactive || emp.isActive
        val deptMatch =
          when (filterDeptId) {
            null -> true
            FILTER_UNASSIGNED_DEPT -> emp.defaultDepartmentId.isNullOrBlank()
            else -> emp.defaultDepartmentId == filterDeptId
          }
        statusMatch && deptMatch
      }
    }

  if (showCreateDialog || editingEmployee != null) {
    EmployeeRegistrationDialog(
      initial = editingEmployee,
      departments = departments,
      assignedShifts =
        editingEmployee?.let { assignedShiftsByEmployee[it.id].orEmpty() }.orEmpty(),
      onSave = { code, name, designation, isActive, defaultDepartmentId ->
        onSaveEmployee(
          editingEmployee?.id,
          code,
          name,
          designation,
          isActive,
          defaultDepartmentId,
        )
        showCreateDialog = false
        editingEmployee = null
      },
      onDismiss = {
        showCreateDialog = false
        editingEmployee = null
      },
    )
  }

  deletingEmployee?.let { target ->
    DeleteConfirmDialog(
      title = "Delete ${target.name}?",
      message =
        "Removes ${target.name} (${target.code}) from staff and clears future roster slots, while keeping historical attendance logs intact.",
      onConfirm = {
        onDeleteEmployee(target)
        deletingEmployee = null
      },
      onDismiss = { deletingEmployee = null },
    )
  }

  val headerPalette = GradientPaletteRegistry.paletteForCard("EMPLOYEES", 0)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Employee Registration",
        subtitle =
          "${employees.count { it.isActive }} active of ${employees.size} staff · ${departments.size} departments",
        palette = headerPalette,
        floralVariant = FloralVariant.CORNER_CLUSTER,
        icon = Icons.Outlined.Badge,
        badgeText = "${employees.size} STAFF",
        trailingContent = {
          Button(
            onClick = { showCreateDialog = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xF0FFFFFF),
                contentColor = Color(0xFF0F172A),
              ),
            modifier = Modifier.testTag("add_employee_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Register", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = showInactive,
            onClick = { showInactive = !showInactive },
            label = {
              Text(if (showInactive) "All Staff (${employees.size})" else "Active Only")
            },
          )
          FilterChip(
            selected = filterDeptId == null,
            onClick = { filterDeptId = null },
            label = { Text("All Depts") },
          )
          FilterChip(
            selected = filterDeptId == FILTER_UNASSIGNED_DEPT,
            onClick = {
              filterDeptId =
                if (filterDeptId == FILTER_UNASSIGNED_DEPT) null else FILTER_UNASSIGNED_DEPT
            },
            label = { Text("Unassigned") },
          )
          departments.forEachIndexed { idx, dept ->
            val deptAccent = GradientPaletteRegistry.paletteForCard(dept.id, idx).colors.first()
            val selected = filterDeptId == dept.id
            FilterChip(
              selected = selected,
              onClick = { filterDeptId = if (selected) null else dept.id },
              label = { Text(dept.name) },
              border =
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = deptAccent.copy(alpha = 0.40f),
                  selectedBorderColor = deptAccent,
                ),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = deptAccent.copy(alpha = if (isDark) 0.28f else 0.16f),
                  selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
          }
        }
      }
    }

    if (displayed.isEmpty()) {
      item {
        EmptyStateCard(
          title =
            if (employees.isEmpty()) "No employees registered yet"
            else "No matching employees",
          message =
            if (employees.isEmpty()) {
              "Tap Register to enroll team members and optionally pick a default department."
            } else {
              "Try switching the status or department filter above."
            },
          icon = Icons.Outlined.Badge,
        )
      }
    }

    itemsIndexed(displayed, key = { _, emp -> emp.id }) { idx, emp ->
      val empPalette = GradientPaletteRegistry.paletteForCard(emp.id, idx)
      val empAccent = empPalette.colors.first()
      val defaultDept = emp.defaultDepartmentId?.let { deptMap[it] }
      val assignedShifts = assignedShiftsByEmployee[emp.id].orEmpty()

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
          BorderStroke(
            width = if (emp.isActive) 1.dp else 0.75.dp,
            color =
              if (emp.isActive) empAccent.copy(alpha = if (isDark) 0.55f else 0.45f)
              else MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier = Modifier.fillMaxWidth().testTag("employee_card_${emp.code}"),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          FloralTextureOverlay(
            variant = GradientPaletteRegistry.floralVariantForCard(emp.id, idx),
            motifColor = empAccent.copy(alpha = if (isDark) 0.16f else 0.10f),
            modifier = Modifier.matchParentSize(),
          )
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = empPalette, height = 4.dp)
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                EmployeeMonogramAvatar(
                  name = emp.name,
                  paletteColors = empPalette.colors,
                  isActive = emp.isActive,
                )
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    TintedPillTag(
                      text = emp.code,
                      accentColor = empAccent,
                    )
                    if (!emp.isActive) {
                      PillTag(
                        text = "Inactive",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Text(
                    text = emp.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = emp.designation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { editingEmployee = emp },
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("edit_employee_${emp.code}"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Edit,
                      contentDescription = "Edit or rename ${emp.name}",
                      tint = empAccent,
                    )
                  }
                  IconButton(
                    onClick = { deletingEmployee = emp },
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("delete_employee_${emp.code}"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete ${emp.name}",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                  Switch(
                    checked = emp.isActive,
                    onCheckedChange = { onToggleActive(emp) },
                  )
                }
              }

              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (defaultDept != null) {
                  val deptAccent =
                    GradientPaletteRegistry.paletteForCard(defaultDept.id, 3).colors.first()
                  TintedPillTag(
                    text = "Dept: ${defaultDept.name}",
                    accentColor = deptAccent,
                  )
                } else {
                  PillTag(
                    text = "Dept: Flexible / Unassigned",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                assignedShifts.forEach { shift ->
                  ShiftHighlightBadge(
                    shiftName = shift.name,
                    timeWindow = "${shift.startTime}–${shift.endTime}",
                    isNightShift = shift.isNightShiftVisual(),
                  )
                }
              }

              Text(
                text = "Updated ${DateUtils.formatTimestamp(emp.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun EmployeeMonogramAvatar(
  name: String,
  paletteColors: List<Color>,
  isActive: Boolean,
) {
  val initials =
    remember(name) {
      val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
      when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
      }
    }

  Surface(
    shape = CircleShape,
    color = Color.Transparent,
    border = BorderStroke(1.25.dp, Color.White.copy(alpha = 0.40f)),
    modifier = Modifier.size(46.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.linearGradient(
              colors =
                if (isActive) paletteColors
                else listOf(Color(0xFF64748B), Color(0xFF475569)),
              start = Offset.Zero,
              end = Offset(110f, 110f),
            )
          ),
      contentAlignment = Alignment.Center,
    ) {
      Box(modifier = Modifier.matchParentSize().background(Color(0x520B1020)))
      Text(
        text = initials,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EmployeeRegistrationDialog(
  initial: Employee?,
  departments: List<Department>,
  assignedShifts: List<Shift>,
  onSave: (String, String, String, Boolean, String?) -> Unit,
  onDismiss: () -> Unit,
) {
  var code by remember(initial) { mutableStateOf(initial?.code ?: "") }
  var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
  var designation by remember(initial) { mutableStateOf(initial?.designation ?: "") }
  var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

  val initialDept =
    remember(initial, departments) {
      if (initial != null) {
        initial.defaultDepartmentId?.takeIf { id -> departments.any { it.id == id } }
      } else {
        departments.firstOrNull { it.isActive }?.id
      }
    }
  var selectedDeptId by remember(initial, departments) { mutableStateOf(initialDept) }
  var deptMenuExpanded by remember { mutableStateOf(false) }

  val selectedDept = departments.firstOrNull { it.id == selectedDeptId }
  val isValid = code.isNotBlank() && name.isNotBlank() && designation.isNotBlank()
  val formPalette = GradientPaletteRegistry.paletteForCard("EMP_FORM_${initial?.id ?: "new"}", 6)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.25.dp, formPalette.colors.first().copy(alpha = 0.50f)),
      tonalElevation = 6.dp,
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 24.dp)
          .widthIn(max = 540.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Gradient & Floral Header
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.linearGradient(
                  colors = formPalette.colors,
                  start = Offset.Zero,
                  end = Offset(900f, 320f),
                )
              )
        ) {
          FloralTextureOverlay(
            variant = FloralVariant.BLOOMING_ARC,
            motifColor = Color.White,
            modifier = Modifier.matchParentSize(),
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(Color(0xB80B1020), Color(0x7A0B1020))
                  )
                )
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x470B1020),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.size(40.dp),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  Icon(
                    imageVector =
                      if (initial == null) Icons.Outlined.PersonAdd else Icons.Outlined.Badge,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = if (initial == null) "Register Employee" else "Edit / Rename Employee",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Text(
                  text = "Identity, role & optional initial department",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.88f),
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close dialog",
                tint = Color.White,
              )
            }
          }
        }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 18.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Employee Code") },
            placeholder = { Text("EMP-107") },
            leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("employee_code_input"),
          )

          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("employee_name_input"),
          )

          OutlinedTextField(
            value = designation,
            onValueChange = { designation = it },
            label = { Text("Designation") },
            placeholder = { Text("Operations Lead") },
            leadingIcon = { Icon(Icons.Outlined.WorkOutline, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("employee_designation_input"),
          )

          ExposedDropdownMenuBox(
            expanded = deptMenuExpanded,
            onExpandedChange = { deptMenuExpanded = !deptMenuExpanded },
            modifier = Modifier.fillMaxWidth(),
          ) {
            OutlinedTextField(
              value = selectedDept?.name ?: "Unassigned (Flexible)",
              onValueChange = {},
              readOnly = true,
              label = { Text("Default Department (optional)") },
              leadingIcon = { Icon(Icons.Outlined.Apartment, contentDescription = null) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptMenuExpanded) },
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth()
                  .testTag("employee_department_dropdown"),
            )
            ExposedDropdownMenu(
              expanded = deptMenuExpanded,
              onDismissRequest = { deptMenuExpanded = false },
            ) {
              DropdownMenuItem(
                text = { Text("Unassigned (Flexible)") },
                leadingIcon = {
                  Icon(Icons.Outlined.Apartment, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                onClick = {
                  selectedDeptId = null
                  deptMenuExpanded = false
                },
                modifier = Modifier.testTag("employee_dept_option_none"),
              )
              if (departments.isNotEmpty()) {
                HorizontalDivider()
              }
              departments.forEach { dept ->
                val suffix = if (dept.isActive) "" else " (Inactive)"
                DropdownMenuItem(
                  text = { Text("${dept.name}$suffix") },
                  onClick = {
                    selectedDeptId = dept.id
                    deptMenuExpanded = false
                  },
                  modifier = Modifier.testTag("employee_dept_option_${dept.id}"),
                )
              }
            }
          }

          if (assignedShifts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Current / Upcoming Shifts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                assignedShifts.forEach { shift ->
                  ShiftHighlightBadge(
                    shiftName = shift.name,
                    timeWindow = "${shift.startTime}–${shift.endTime}",
                    isNightShift = shift.isNightShiftVisual(),
                  )
                }
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = formPalette.colors.first().copy(alpha = 0.08f),
            border = BorderStroke(1.dp, formPalette.colors.first().copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = formPalette.colors.first(),
                modifier = Modifier.size(18.dp),
              )
              Text(
                text =
                  "Pre-fills new roster slots until changed; staff stay completely free to take any department, duty, or shift later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Active for new rosters", style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = if (isActive) "Included in daily attendance & shifts" else "Archived (past logs kept)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text("Cancel")
            }
            Button(
              onClick = { onSave(code, name, designation, isActive, selectedDeptId) },
              enabled = isValid,
              modifier =
                Modifier.weight(1.3f).heightIn(min = 48.dp).testTag("save_employee_button"),
            ) {
              Text(if (initial == null) "Register" else "Save Changes")
            }
          }
        }
      }
    }
  }
}

private const val FILTER_UNASSIGNED_DEPT = "__unassigned__"
