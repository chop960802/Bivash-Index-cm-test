package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.logic.LegalActSectionCatalog
import com.example.models.CaseSectionAttachment
import com.example.models.LegalActType
import com.example.models.OfficeCaseRecord
import com.example.models.RegisteredOffice
import com.example.ui.components.DetailMetaPair
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.SolidPillTag
import com.example.ui.components.TintedPillTag
import com.example.ui.components.rememberSyncedText
import com.example.ui.theme.GradientPaletteRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeIsoDatePickerDialog(
  title: String,
  initialDateIso: String?,
  allowClear: Boolean = false,
  onSelectDate: (String) -> Unit,
  onClearDate: () -> Unit = {},
  onDismiss: () -> Unit,
) {
  val today = DateUtils.todayIso()
  val initialUtc = DateUtils.isoToUtcMillis(initialDateIso ?: today) ?: DateUtils.isoToUtcMillis(today)
  val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          pickerState.selectedDateMillis?.let { millis ->
            onSelectDate(DateUtils.utcMillisToIso(millis))
          }
          onDismiss()
        },
        modifier = Modifier.testTag("office_date_picker_apply"),
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      Row {
        if (allowClear) {
          TextButton(
            onClick = {
              onClearDate()
              onDismiss()
            }
          ) {
            Text("Clear")
          }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
  ) {
    DatePicker(
      state = pickerState,
      title = {
        Text(
          text = title,
          modifier = Modifier.padding(start = 24.dp, top = 16.dp),
        )
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeDropdownField(
  label: String,
  offices: List<RegisteredOffice>,
  selectedOfficeId: String?,
  includeAllOption: Boolean,
  onSelectOfficeId: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedOffice = offices.firstOrNull { it.id == selectedOfficeId }
  val displayText =
    when {
      selectedOffice != null -> "${selectedOffice.officeName} (${selectedOffice.shortCode})"
      includeAllOption -> "All Offices"
      offices.isEmpty() -> "No offices registered"
      else -> "Select Office"
    }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = displayText,
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      singleLine = true,
      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      if (includeAllOption) {
        DropdownMenuItem(
          text = { Text("All Offices") },
          onClick = {
            onSelectOfficeId(null)
            expanded = false
          },
        )
      }
      offices.forEach { office ->
        DropdownMenuItem(
          text = { Text("${office.officeName} (${office.shortCode})") },
          onClick = {
            onSelectOfficeId(office.id)
            expanded = false
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IoAutofillTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  knownIoNames: List<String>,
  placeholder: String,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val (syncedIoText, onIoTextEdit) =
    rememberSyncedText(
      externalValue = value,
      onValueChange = onValueChange,
    )
  val suggestions =
    remember(syncedIoText, knownIoNames) {
      val needle = syncedIoText.trim()
      if (needle.isEmpty()) knownIoNames.take(12)
      else
        knownIoNames
          .filter { it.contains(needle, ignoreCase = true) && !it.equals(needle, ignoreCase = true) }
          .take(12)
    }

  ExposedDropdownMenuBox(
    expanded = expanded && suggestions.isNotEmpty(),
    onExpandedChange = { if (suggestions.isNotEmpty()) expanded = it },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = syncedIoText,
      onValueChange = {
        onIoTextEdit(it)
        expanded = true
      },
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      leadingIcon = { Icon(Icons.Outlined.PersonSearch, contentDescription = null) },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (syncedIoText.isNotBlank()) {
            IconButton(
              onClick = {
                onIoTextEdit("")
                expanded = false
              }
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "Clear $label", modifier = Modifier.size(18.dp))
            }
          }
          if (knownIoNames.isNotEmpty()) {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && suggestions.isNotEmpty())
          }
        }
      },
      singleLine = true,
      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
    )
    if (suggestions.isNotEmpty()) {
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        suggestions.forEach { ioName ->
          DropdownMenuItem(
            text = { Text(ioName) },
            onClick = {
              onIoTextEdit(ioName)
              expanded = false
            },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActAndSectionSelectorBlock(
  selectedAct: LegalActType,
  onSelectAct: (LegalActType) -> Unit,
  customActName: String,
  onCustomActNameChange: (String) -> Unit,
  selectedSections: List<CaseSectionAttachment>,
  onAddSection: (CaseSectionAttachment) -> Unit,
  onRemoveSection: (CaseSectionAttachment) -> Unit,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  var showSectionPicker by remember { mutableStateOf(false) }
  var customSecNumber by remember { mutableStateOf("") }
  var customSecTitle by remember { mutableStateOf("") }

  if (showSectionPicker && selectedAct != LegalActType.OTHERS) {
    LegalSectionSearchDialog(
      actType = selectedAct,
      attachedKeys = selectedSections.map { it.key() }.toSet(),
      onToggleSection = { item ->
        val attachment =
          CaseSectionAttachment(
            actCode = selectedAct.code,
            actDisplayName = selectedAct.shortLabel,
            sectionNumber = item.sectionNumber,
            shortTitle = item.shortTitle,
          )
        if (selectedSections.any { it.key() == attachment.key() }) {
          onRemoveSection(attachment)
        } else {
          onAddSection(attachment)
        }
      },
      onDismiss = { showSectionPicker = false },
    )
  }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Act & Sections",
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      LegalActType.entries.forEach { act ->
        FilterChip(
          selected = selectedAct == act,
          onClick = { onSelectAct(act) },
          label = { Text(act.shortLabel) },
          modifier = Modifier.testTag("office_act_chip_${act.code.lowercase()}"),
        )
      }
    }

    if (selectedAct == LegalActType.OTHERS) {
      OutlinedTextField(
        value = customActName,
        onValueChange = onCustomActNameChange,
        label = { Text("Custom Act Name") },
        placeholder = { Text("e.g. Arms Act, NDPS Act, IT Act") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("office_custom_act_input"),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = customSecNumber,
          onValueChange = { customSecNumber = it },
          label = { Text("Section") },
          placeholder = { Text("e.g. 25") },
          singleLine = true,
          modifier = Modifier.weight(0.38f).testTag("office_custom_section_num_input"),
        )
        OutlinedTextField(
          value = customSecTitle,
          onValueChange = { customSecTitle = it },
          label = { Text("Section Title / Note") },
          placeholder = { Text("e.g. Possession of arms") },
          singleLine = true,
          modifier = Modifier.weight(0.62f).testTag("office_custom_section_title_input"),
        )
      }
      OutlinedButton(
        onClick = {
          val num = customSecNumber.trim()
          val title = customSecTitle.trim()
          val actLabel = customActName.trim().ifBlank { "Others" }
          if (num.isNotEmpty() || title.isNotEmpty()) {
            onAddSection(
              CaseSectionAttachment(
                actCode = LegalActType.OTHERS.code,
                actDisplayName = actLabel,
                sectionNumber = num.ifBlank { "Custom" },
                shortTitle = title.ifBlank { actLabel },
              )
            )
            customSecNumber = ""
            customSecTitle = ""
          }
        },
        enabled = customSecNumber.isNotBlank() || customSecTitle.isNotBlank(),
        modifier = Modifier.fillMaxWidth().testTag("office_add_custom_section_button"),
      ) {
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Attach Custom Section")
      }
    } else {
      val totalCount = LegalActSectionCatalog.sectionsForAct(selectedAct).size
      OutlinedButton(
        onClick = { showSectionPicker = true },
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth().testTag("office_open_section_picker_button"),
      ) {
        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Search & Select ${selectedAct.shortLabel} Sections ($totalCount available)")
      }
    }

    if (selectedSections.isNotEmpty()) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        selectedSections.forEach { sec ->
          InputChip(
            selected = true,
            onClick = { onRemoveSection(sec) },
            label = {
              Text(
                text = "${sec.actDisplayName.ifBlank { sec.actCode }} §${sec.sectionNumber}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            trailingIcon = {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${sec.sectionNumber}",
                modifier = Modifier.size(16.dp),
              )
            },
          )
        }
      }
    }
  }
}

@Composable
fun LegalSectionSearchDialog(
  actType: LegalActType,
  attachedKeys: Set<String>,
  onToggleSection: (com.example.models.LegalSectionItem) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val matches = remember(actType, query) { LegalActSectionCatalog.searchSections(actType, query) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${actType.shortLabel} Sections (${matches.size})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Search section number or keyword") },
          placeholder = { Text("e.g. 103, murder, theft, 6, 42A") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            if (query.isNotBlank()) {
              IconButton(onClick = { query = "" }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear section search")
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("legal_section_search_input"),
        )
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(matches, key = { "${it.actCode}_${it.sectionNumber}" }) { item ->
            val key = "${actType.code.uppercase()}:${item.sectionNumber.trim().uppercase()}"
            val isAttached = key in attachedKeys
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                if (isAttached) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
              border =
                BorderStroke(
                  width = 1.dp,
                  color =
                    if (isAttached) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onToggleSection(item) }
                  .testTag("section_row_${item.actCode}_${item.sectionNumber}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color =
                    if (isAttached) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                  Text(
                    text = "§${item.sectionNumber}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color =
                      if (isAttached) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
                Text(
                  text = item.shortTitle,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (isAttached) {
                  Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Done (${attachedKeys.size})") }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfficeCaseViewDialog(
  record: OfficeCaseRecord,
  officeName: String,
  officeShortCode: String,
  onEdit: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = GradientPaletteRegistry.paletteForCard(record.id, 3)
  val accent = palette.colors.first()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("FIR #${record.firNumber}", fontWeight = FontWeight.Bold)
        if (officeShortCode.isNotBlank()) {
          SolidPillTag(text = officeShortCode, containerColor = accent)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        DetailMetaPair(label = "Office", value = "$officeName (${officeShortCode.ifBlank { "—" }})")
        DetailMetaPair(label = "Record Date", value = DateUtils.formatReadableDate(record.recordDate))
        DetailMetaPair(label = "Current Active I/O", value = record.currentIoName)
        DetailMetaPair(label = "Primary Act", value = record.primaryActBadge())

        if (record.sections.isNotEmpty()) {
          Text(
            text = "Attached Sections (${record.sections.size})",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            record.sections.forEach { sec ->
              TintedPillTag(
                text = "${sec.actDisplayName.ifBlank { sec.actCode }} §${sec.sectionNumber}: ${sec.shortTitle}",
                accentColor = accent,
              )
            }
          }
        }

        if (record.notes.isNotBlank()) {
          DetailMetaPair(label = "Notes", value = record.notes)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "I/O Assignment History (${record.ioHistory.size})",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          )
        }

        if (record.ioHistory.isEmpty()) {
          Text(
            text = "Initial I/O: ${record.currentIoName} (${record.recordDate})",
            style = MaterialTheme.typography.bodySmall,
          )
        } else {
          record.ioHistory.forEachIndexed { idx, h ->
            val isLatest = idx == record.ioHistory.lastIndex
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                if (isLatest) accent.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
              border =
                BorderStroke(
                  1.dp,
                  if (isLatest) accent else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = h.ioName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                  )
                  TintedPillTag(
                    text = if (isLatest) "Current · ${h.assignedDate}" else h.assignedDate,
                    accentColor = accent,
                  )
                }
                if (h.remarks.isNotBlank()) {
                  Text(
                    text = h.remarks,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onEdit, modifier = Modifier.testTag("view_dialog_edit_button")) {
        Text("Edit / Assign I/O")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
fun OfficeCaseEditDialog(
  initial: OfficeCaseRecord,
  offices: List<RegisteredOffice>,
  knownIoNames: List<String>,
  onSave: (
    officeId: String,
    recordDate: String,
    firNumber: Int,
    editedCurrentIoName: String,
    actType: String,
    customActName: String,
    sections: List<CaseSectionAttachment>,
    notes: String,
    newAssignedIoName: String?,
    newIoAssignedDate: String?,
    newIoRemarks: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  var officeId by remember(initial) { mutableStateOf(initial.officeId) }
  var recordDate by remember(initial) { mutableStateOf(initial.recordDate) }
  var firInput by remember(initial) { mutableStateOf(initial.firNumber.toString()) }
  var currentIoInput by remember(initial) { mutableStateOf(initial.currentIoName) }
  var selectedAct by remember(initial) { mutableStateOf(LegalActType.fromCode(initial.actType)) }
  var customActName by remember(initial) { mutableStateOf(initial.customActName) }
  var sections by remember(initial) { mutableStateOf(initial.sections) }
  var notes by remember(initial) { mutableStateOf(initial.notes) }

  // Dedicated New I/O Reassignment fields (preserving old I/Os in ioHistory)
  var newAssignedIoName by remember(initial) { mutableStateOf("") }
  var newIoAssignedDate by remember(initial) { mutableStateOf(DateUtils.todayIso()) }
  var newIoRemarks by remember(initial) { mutableStateOf("") }

  var pickingRecordDate by remember { mutableStateOf(false) }
  var pickingNewIoDate by remember { mutableStateOf(false) }

  if (pickingRecordDate) {
    OfficeIsoDatePickerDialog(
      title = "Select Record Date",
      initialDateIso = recordDate,
      onSelectDate = { recordDate = it },
      onDismiss = { pickingRecordDate = false },
    )
  }

  if (pickingNewIoDate) {
    OfficeIsoDatePickerDialog(
      title = "Select New I/O Assigned Date",
      initialDateIso = newIoAssignedDate,
      onSelectDate = { newIoAssignedDate = it },
      onDismiss = { pickingNewIoDate = false },
    )
  }

  val parsedFir = firInput.trim().toIntOrNull()
  val isValid =
    officeId.isNotBlank() &&
      DateUtils.isValidIsoDate(recordDate) &&
      parsedFir != null &&
      parsedFir > 0 &&
      (currentIoInput.isNotBlank() || newAssignedIoName.isNotBlank()) &&
      (sections.isNotEmpty() || (selectedAct == LegalActType.OTHERS && customActName.isNotBlank()))

  val palette = GradientPaletteRegistry.paletteForCard(initial.id, 4)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit FIR #${initial.firNumber} & Assign I/O") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OfficeDropdownField(
          label = "Office",
          offices = offices,
          selectedOfficeId = officeId,
          includeAllOption = false,
          onSelectOfficeId = { if (it != null) officeId = it },
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = recordDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Record Date") },
            trailingIcon = {
              IconButton(onClick = { pickingRecordDate = true }) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick record date")
              }
            },
            singleLine = true,
            modifier =
              Modifier.weight(1f)
                .clickable { pickingRecordDate = true }
                .testTag("edit_case_record_date"),
          )
          OutlinedTextField(
            value = firInput,
            onValueChange = { txt -> firInput = txt.filter { it.isDigit() } },
            label = { Text("FIR Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(0.9f).testTag("edit_case_fir_input"),
          )
        }

        IoAutofillTextField(
          label = "Current I/O Name",
          value = currentIoInput,
          onValueChange = { currentIoInput = it },
          knownIoNames = knownIoNames,
          placeholder = "Investigating Officer",
          modifier = Modifier.fillMaxWidth().testTag("edit_case_current_io_input"),
        )

        ActAndSectionSelectorBlock(
          selectedAct = selectedAct,
          onSelectAct = { selectedAct = it },
          customActName = customActName,
          onCustomActNameChange = { customActName = it },
          selectedSections = sections,
          onAddSection = { sec ->
            if (sections.none { it.key() == sec.key() }) {
              sections = sections + sec
            }
          },
          onRemoveSection = { sec ->
            sections = sections.filterNot { it.key() == sec.key() }
          },
          accentColor = palette.colors.first(),
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Case Notes (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, palette.colors.first().copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = palette, height = 3.dp)
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.SwapHoriz,
                  contentDescription = null,
                  tint = palette.colors.first(),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Assign New I/O (Keeps Old I/O History)",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
              }
              Text(
                text = "Leave blank to keep ${initial.currentIoName} active without transfer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              IoAutofillTextField(
                label = "New I/O Name",
                value = newAssignedIoName,
                onValueChange = { newAssignedIoName = it },
                knownIoNames = knownIoNames,
                placeholder = "Enter or pick new I/O",
                modifier = Modifier.fillMaxWidth().testTag("edit_case_new_io_input"),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = newIoAssignedDate,
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Assigned Date") },
                  trailingIcon = {
                    IconButton(onClick = { pickingNewIoDate = true }) {
                      Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick assigned date")
                    }
                  },
                  singleLine = true,
                  modifier =
                    Modifier.weight(1f)
                      .clickable { pickingNewIoDate = true }
                      .testTag("edit_case_new_io_date"),
                )
                OutlinedTextField(
                  value = newIoRemarks,
                  onValueChange = { newIoRemarks = it },
                  label = { Text("Remarks") },
                  placeholder = { Text("Transfer reason") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("edit_case_new_io_remarks"),
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            officeId,
            recordDate,
            parsedFir ?: initial.firNumber,
            currentIoInput,
            selectedAct.code,
            customActName,
            sections,
            notes,
            newAssignedIoName.takeIf { it.isNotBlank() },
            newIoAssignedDate,
            newIoRemarks,
          )
        },
        enabled = isValid,
        modifier = Modifier.testTag("save_edit_office_case_button"),
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
