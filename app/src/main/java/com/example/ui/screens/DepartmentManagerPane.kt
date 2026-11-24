package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Shift
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.PillTag
import com.example.ui.components.ShiftFilterChip
import com.example.ui.components.ShiftHighlightBadge
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry

@Composable
fun DepartmentManagerPane(
  departments: List<Department>,
  duties: List<DutyType>,
  shifts: List<Shift>,
  onSaveDepartment: (String?, String, String, Boolean) -> Unit,
  onToggleDepartmentActive: (Department) -> Unit,
  onDeleteDepartment: (Department) -> Unit,
  onSaveDuty: (String?, String, String, Boolean, String, String?) -> Unit,
  onToggleDutyActive: (DutyType) -> Unit,
  onDeleteDuty: (DutyType) -> Unit,
  modifier: Modifier = Modifier,
) {
  var editingDept by remember { mutableStateOf<Department?>(null) }
  var deletingDept by remember { mutableStateOf<Department?>(null) }
  var showCreateDeptDialog by remember { mutableStateOf(false) }

  var editingDuty by remember { mutableStateOf<DutyType?>(null) }
  var creatingDutyDeptId by remember { mutableStateOf<String?>(null) }
  var showCreateDutyDialog by remember { mutableStateOf(false) }
  var deletingDuty by remember { mutableStateOf<DutyType?>(null) }

  val deptIds = remember(departments) { departments.map { it.id }.toSet() }
  val dutiesByDept = remember(duties) { duties.groupBy { it.departmentId } }
  val unlinkedDuties = remember(duties, deptIds) {
    duties.filter { it.departmentId.isBlank() || it.departmentId !in deptIds }
  }
  val shiftMap = remember(shifts) { shifts.associateBy { it.id } }

  if (showCreateDeptDialog || editingDept != null) {
    DepartmentEditDialog(
      initial = editingDept,
      onSave = { name, desc, active ->
        onSaveDepartment(editingDept?.id, name, desc, active)
        showCreateDeptDialog = false
        editingDept = null
      },
      onDismiss = {
        showCreateDeptDialog = false
        editingDept = null
      },
    )
  }

  deletingDept?.let { target ->
    val nestedCount = dutiesByDept[target.id].orEmpty().size
    AlertDialog(
      onDismissRequest = { deletingDept = null },
      title = { Text("Delete '${target.name}'?") },
      text = {
        Text(
          text =
            "Deletes '${target.name}' and its $nestedCount linked duty template(s), while clearing future roster links and keeping historical attendance snapshots intact.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteDepartment(target)
            deletingDept = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_department_button"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingDept = null }) { Text("Cancel") } },
    )
  }

  if (showCreateDutyDialog || editingDuty != null) {
    DepartmentDutyEditDialog(
      initial = editingDuty,
      departments = departments,
      shifts = shifts,
      initialDepartmentId = editingDuty?.departmentId ?: creatingDutyDeptId.orEmpty(),
      onSave = { title, desc, active, deptId, defaultShiftId ->
        onSaveDuty(editingDuty?.id, title, desc, active, deptId, defaultShiftId)
        showCreateDutyDialog = false
        editingDuty = null
        creatingDutyDeptId = null
      },
      onDismiss = {
        showCreateDutyDialog = false
        editingDuty = null
        creatingDutyDeptId = null
      },
    )
  }

  deletingDuty?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingDuty = null },
      title = { Text("Delete '${target.title}'?") },
      text = {
        Text(
          text =
            "Deletes this duty template and clears it from future roster assignments without deleting historical attendance logs.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteDuty(target)
            deletingDuty = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_duty_button"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingDuty = null }) { Text("Cancel") } },
    )
  }

  val headerPalette = GradientPaletteRegistry.paletteForCard("DEPARTMENTS", 2)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Departments & Duties",
        subtitle = "${departments.size} departments · ${duties.size} nested duties",
        palette = headerPalette,
        floralVariant = FloralVariant.BLOOMING_ARC,
        icon = Icons.Outlined.Apartment,
        trailingContent = {
          Button(
            onClick = { showCreateDeptDialog = true },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xE6FFFFFF),
                contentColor = Color(0xFF0F172A),
              ),
            modifier = Modifier.testTag("add_department_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Department", fontWeight = FontWeight.Bold)
          }
        },
      )
    }

    if (departments.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No departments configured",
          message = "Tap + Department to add unlimited departments and nested duties.",
          icon = Icons.Outlined.Apartment,
        )
      }
    }

    itemsIndexed(departments, key = { _, dept -> dept.id }) { idx, dept ->
      val deptPalette = GradientPaletteRegistry.paletteForCard(dept.id, idx)
      val deptDuties = dutiesByDept[dept.id].orEmpty()
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, deptPalette.colors.first().copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("department_card_${dept.id}"),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          FloralTextureOverlay(
            variant = GradientPaletteRegistry.floralVariantForCard(dept.id, idx),
            motifColor = deptPalette.colors.first().copy(alpha = 0.18f),
            modifier = Modifier.matchParentSize(),
          )
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = deptPalette, height = 4.dp)
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = dept.name,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    TintedPillTag(
                      text = "${deptDuties.size} ${if (deptDuties.size == 1) "duty" else "duties"}",
                      accentColor = deptPalette.colors.first(),
                    )
                    if (!dept.isActive) {
                      PillTag(
                        text = "Inactive",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  if (dept.description.isNotBlank()) {
                    Text(
                      text = dept.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = "Updated ${DateUtils.formatTimestamp(dept.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { editingDept = dept },
                    modifier =
                      Modifier.minimumInteractiveComponentSize().testTag("edit_dept_${dept.id}"),
                  ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit or rename ${dept.name}")
                  }
                  IconButton(
                    onClick = { deletingDept = dept },
                    modifier =
                      Modifier.minimumInteractiveComponentSize().testTag("delete_dept_${dept.id}"),
                  ) {
                    Icon(
                      Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete ${dept.name}",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                  Switch(
                    checked = dept.isActive,
                    onCheckedChange = { onToggleDepartmentActive(dept) },
                  )
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Duties in ${dept.name}",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                OutlinedButton(
                  onClick = {
                    creatingDutyDeptId = dept.id
                    showCreateDutyDialog = true
                  },
                  modifier = Modifier.testTag("add_duty_in_${dept.id}"),
                ) {
                  Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Duty")
                }
              }

              if (deptDuties.isEmpty()) {
                Text(
                  text = "No duties yet inside ${dept.name}. Tap Add Duty to create unlimited duties.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              } else {
                deptDuties.forEachIndexed { dIdx, duty ->
                  NestedDutyItemCard(
                    duty = duty,
                    defaultShift = duty.defaultShiftId?.let { shiftMap[it] },
                    accentColor = deptPalette.colors[(dIdx + 1) % deptPalette.colors.size],
                    onEdit = { editingDuty = duty },
                    onDelete = { deletingDuty = duty },
                    onToggleActive = { onToggleDutyActive(duty) },
                  )
                }
              }
            }
          }
        }
      }
    }

    if (unlinkedDuties.isNotEmpty() || departments.isEmpty()) {
      item {
        val unlinkedPalette = GradientPaletteRegistry.paletteForCard("unlinked_duties", 9)
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, unlinkedPalette.colors.first().copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = unlinkedPalette, height = 4.dp)
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "General Duties",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  )
                  Text(
                    text = "Duties not linked to a department",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = {
                    creatingDutyDeptId = ""
                    showCreateDutyDialog = true
                  },
                  modifier = Modifier.testTag("add_duty_button"),
                ) {
                  Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("New Duty")
                }
              }
              unlinkedDuties.forEachIndexed { uIdx, duty ->
                NestedDutyItemCard(
                  duty = duty,
                  defaultShift = duty.defaultShiftId?.let { shiftMap[it] },
                  accentColor = unlinkedPalette.colors[uIdx % unlinkedPalette.colors.size],
                  onEdit = { editingDuty = duty },
                  onDelete = { deletingDuty = duty },
                  onToggleActive = { onToggleDutyActive(duty) },
                )
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun NestedDutyItemCard(
  duty: DutyType,
  defaultShift: Shift?,
  accentColor: Color,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onToggleActive: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth().testTag("duty_card_${duty.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Checklist,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = duty.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (duty.description.isNotBlank()) {
          Text(
            text = duty.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (defaultShift != null) {
          ShiftHighlightBadge(
            shiftName = "Default: ${defaultShift.name}",
            timeWindow = "${defaultShift.startTime}–${defaultShift.endTime}",
            isNightShift = defaultShift.isNightShiftVisual(),
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onEdit,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_duty_${duty.id}"),
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = "Edit or rename ${duty.title}")
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_duty_${duty.id}"),
        ) {
          Icon(
            Icons.Outlined.DeleteOutline,
            contentDescription = "Delete ${duty.title}",
            tint = MaterialTheme.colorScheme.error,
          )
        }
        Switch(checked = duty.isActive, onCheckedChange = { onToggleActive() })
      }
    }
  }
}

@Composable
private fun DepartmentEditDialog(
  initial: Department?,
  onSave: (String, String, Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
  var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
  var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "New Department" else "Edit / Rename Department") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Department Name") },
          placeholder = { Text("e.g. Computer, Dispatch, Maintenance") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("department_name_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Notes / Scope (optional)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Active", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, description, isActive) },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("save_department_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepartmentDutyEditDialog(
  initial: DutyType?,
  departments: List<Department>,
  shifts: List<Shift>,
  initialDepartmentId: String,
  onSave: (String, String, Boolean, String, String?) -> Unit,
  onDismiss: () -> Unit,
) {
  var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
  var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
  var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }
  var selectedDeptId by remember(initial, initialDepartmentId) {
    mutableStateOf(initial?.departmentId?.ifBlank { null } ?: initialDepartmentId)
  }
  var selectedDefaultShiftId by remember(initial) { mutableStateOf(initial?.defaultShiftId) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "New Duty" else "Edit / Rename Duty") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (departments.isNotEmpty()) {
          Text("Department", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            departments.forEach { dept ->
              FilterChip(
                selected = selectedDeptId == dept.id,
                onClick = { selectedDeptId = dept.id },
                label = { Text(dept.name) },
              )
            }
          }
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Duty Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("duty_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description / Notes (optional)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        if (shifts.isNotEmpty()) {
          Text("Default Shift (optional)", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = selectedDefaultShiftId == null,
              onClick = { selectedDefaultShiftId = null },
              label = { Text("Flexible") },
            )
            shifts.forEach { shift ->
              ShiftFilterChip(
                label = shift.name,
                isNightShift = shift.isNightShiftVisual(),
                selected = selectedDefaultShiftId == shift.id,
                onClick = {
                  selectedDefaultShiftId =
                    if (selectedDefaultShiftId == shift.id) null else shift.id
                },
              )
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Active", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isActive, onCheckedChange = { isActive = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, description, isActive, selectedDeptId, selectedDefaultShiftId) },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("save_duty_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
