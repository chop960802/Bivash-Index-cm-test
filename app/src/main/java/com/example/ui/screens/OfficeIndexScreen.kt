package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.logic.OfficeCaseEngine
import com.example.models.CaseSectionAttachment
import com.example.models.LegalActType
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeCaseSearchQuery
import com.example.models.RegisteredOffice
import com.example.ui.components.DetailMetaPair
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.ReferenceDetailCard
import com.example.ui.components.SolidPillTag
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.components.rememberSyncedText
import com.example.ui.state.OfficeIndexUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPalette
import com.example.ui.theme.GradientPaletteRegistry

private enum class OfficeIndexDestination {
  HUB,
  RECORD_DATA,
  VIEW_SEARCH,
}

@Composable
fun OfficeIndexScreen(
  state: OfficeIndexUiState,
  onBack: () -> Unit,
  onOpenOfficeSettings: () -> Unit,
  onUpdateCaseQuery: ((OfficeCaseSearchQuery) -> OfficeCaseSearchQuery) -> Unit,
  onResetCaseFilters: () -> Unit,
  onSaveNewCaseRecord: (
    officeId: String,
    recordDate: String,
    firNumber: Int,
    ioName: String,
    actType: String,
    customActName: String,
    sections: List<CaseSectionAttachment>,
    notes: String,
  ) -> Unit,
  onUpdateCaseRecord: (
    existing: OfficeCaseRecord,
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
  onDeleteCaseRecord: (OfficeCaseRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  var destination by remember { mutableStateOf(OfficeIndexDestination.HUB) }
  var viewingRecord by remember { mutableStateOf<OfficeCaseRecord?>(null) }
  var editingRecord by remember { mutableStateOf<OfficeCaseRecord?>(null) }
  var deletingRecord by remember { mutableStateOf<OfficeCaseRecord?>(null) }

  BackHandler(enabled = destination != OfficeIndexDestination.HUB) {
    destination = OfficeIndexDestination.HUB
  }

  val officesById = remember(state.offices) { state.offices.associateBy { it.id } }

  viewingRecord?.let { target ->
    val officeName = OfficeCaseEngine.resolveOfficeName(target, officesById)
    val shortCode = OfficeCaseEngine.resolveOfficeShortCode(target, officesById)
    OfficeCaseViewDialog(
      record = target,
      officeName = officeName,
      officeShortCode = shortCode,
      onEdit = {
        viewingRecord = null
        editingRecord = target
      },
      onDismiss = { viewingRecord = null },
    )
  }

  editingRecord?.let { target ->
    val selectableOffices =
      remember(state.offices, target.officeId) {
        val currentIfInactive = state.offices.firstOrNull { it.id == target.officeId && !it.isActive }
        listOfNotNull(currentIfInactive) + state.activeOffices
      }
    OfficeCaseEditDialog(
      initial = target,
      offices = selectableOffices.ifEmpty { state.offices },
      knownIoNames = state.knownIoNames,
      onSave = {
        officeId,
        recordDate,
        firNumber,
        editedIo,
        actType,
        customActName,
        sections,
        notes,
        newIo,
        newIoDate,
        newIoRemarks ->
        onUpdateCaseRecord(
          target,
          officeId,
          recordDate,
          firNumber,
          editedIo,
          actType,
          customActName,
          sections,
          notes,
          newIo,
          newIoDate,
          newIoRemarks,
        )
        editingRecord = null
      },
      onDismiss = { editingRecord = null },
    )
  }

  deletingRecord?.let { target ->
    val shortCode = OfficeCaseEngine.resolveOfficeShortCode(target, officesById)
    AlertDialog(
      onDismissRequest = { deletingRecord = null },
      title = { Text("Delete FIR #${target.firNumber}?") },
      text = {
        Text(
          text =
            "Permanently delete FIR #${target.firNumber} (${shortCode.ifBlank { target.officeNameSnapshot }} · I/O: ${target.currentIoName}) and its I/O history?",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCaseRecord(target)
            deletingRecord = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_office_case_button"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { deletingRecord = null }) { Text("Cancel") } },
    )
  }

  val bannerPalette = GradientPaletteRegistry.paletteForCard("office_index_banner", 1)

  val bannerSubtitle =
    when (destination) {
      OfficeIndexDestination.HUB -> "${state.allCaseRecordsCount} FIR cases · ${state.offices.size} offices"
      OfficeIndexDestination.RECORD_DATA -> "Record Data · New FIR & I/O entry"
      OfficeIndexDestination.VIEW_SEARCH -> "View & Search · ${state.caseRecords.size} of ${state.allCaseRecordsCount} FIR cases"
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 880.dp)) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        VibrantFeatureBanner(
          title = "Office Index",
          subtitle = bannerSubtitle,
          palette = bannerPalette,
          floralVariant = FloralVariant.BOTANICAL_VINE,
          icon = Icons.Outlined.Inventory2,
          leadingContent = {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x470B1020),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            ) {
              IconButton(
                onClick = {
                  if (destination == OfficeIndexDestination.HUB) onBack()
                  else destination = OfficeIndexDestination.HUB
                },
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("office_index_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription =
                    if (destination == OfficeIndexDestination.HUB) "Back to Dashboard"
                    else "Back to Office Index",
                  tint = Color.White,
                )
              }
            }
          },
          trailingContent = {
            Button(
              onClick = onOpenOfficeSettings,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xE6FFFFFF),
                  contentColor = Color(0xFF0F172A),
                ),
              modifier = Modifier.height(44.dp).testTag("office_index_manage_offices_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Offices", fontWeight = FontWeight.Bold)
            }
          },
        )
      }

      when (destination) {
        OfficeIndexDestination.HUB ->
          OfficeIndexHubContent(
            activeOfficesCount = state.activeOffices.size,
            totalOfficesCount = state.offices.size,
            totalCasesCount = state.allCaseRecordsCount,
            onOpenRecordData = { destination = OfficeIndexDestination.RECORD_DATA },
            onOpenViewSearch = { destination = OfficeIndexDestination.VIEW_SEARCH },
            modifier = Modifier.weight(1f),
          )
        OfficeIndexDestination.RECORD_DATA ->
          OfficeRecordDataTab(
            activeOffices = state.activeOffices,
            knownIoNames = state.knownIoNames,
            onOpenOfficeSettings = onOpenOfficeSettings,
            onSaveCase = {
              officeId,
              recordDate,
              firNumber,
              ioName,
              actType,
              customActName,
              sections,
              notes ->
              onSaveNewCaseRecord(
                officeId,
                recordDate,
                firNumber,
                ioName,
                actType,
                customActName,
                sections,
                notes,
              )
              destination = OfficeIndexDestination.VIEW_SEARCH
            },
            modifier = Modifier.weight(1f),
          )
        OfficeIndexDestination.VIEW_SEARCH ->
          OfficeViewAndSearchTab(
            state = state,
            officesById = officesById,
            onUpdateQuery = onUpdateCaseQuery,
            onResetFilters = onResetCaseFilters,
            onSwitchToRecord = { destination = OfficeIndexDestination.RECORD_DATA },
            onViewRecord = { viewingRecord = it },
            onEditRecord = { editingRecord = it },
            onDeleteRecord = { deletingRecord = it },
            modifier = Modifier.weight(1f),
          )
      }
    }
  }
}

@Composable
private fun OfficeIndexHubContent(
  activeOfficesCount: Int,
  totalOfficesCount: Int,
  totalCasesCount: Int,
  onOpenRecordData: () -> Unit,
  onOpenViewSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val recordPalette = GradientPaletteRegistry.paletteForCard("office_hub_record_data", 2)
  val viewSearchPalette = GradientPaletteRegistry.paletteForCard("office_hub_view_search", 10)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OfficeIndexVerticalActionCard(
      title = "Record Data",
      subtitle = "Log FIR number, Office, Date, I/O with autofill & BNS / POCSO / Custom sections",
      badgeText =
        when {
          activeOfficesCount > 0 -> "$activeOfficesCount active offices ready"
          totalOfficesCount > 0 -> "No active office selected"
          else -> "Register office in Settings -> Offices"
        },
      icon = Icons.Outlined.NoteAdd,
      palette = recordPalette,
      floralVariant = FloralVariant.RADIAL_PETALS,
      testTag = "office_hub_record_data_card",
      onClick = onOpenRecordData,
    )

    OfficeIndexVerticalActionCard(
      title = "View & Search",
      subtitle = "Filter FIR register by Date range, Office, FIR #, I/O & Act sections · View, Edit & I/O history",
      badgeText =
        if (totalCasesCount == 1) "1 FIR record logged" else "$totalCasesCount FIR records logged",
      icon = Icons.Outlined.Search,
      palette = viewSearchPalette,
      floralVariant = FloralVariant.CORNER_CLUSTER,
      testTag = "office_hub_view_search_card",
      onClick = onOpenViewSearch,
    )

    Spacer(modifier = Modifier.height(12.dp))
  }
}

@Composable
private fun OfficeIndexVerticalActionCard(
  title: String,
  subtitle: String,
  badgeText: String,
  icon: ImageVector,
  palette: GradientPalette,
  floralVariant: FloralVariant,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(22.dp)
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 148.dp)
        .shadow(elevation = 6.dp, shape = shape, clip = false)
        .clip(shape)
        .minimumInteractiveComponentSize()
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = shape,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            brush =
              Brush.linearGradient(
                colors = palette.colors,
                start = Offset.Zero,
                end = Offset(980f, 480f),
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
              brush =
                Brush.horizontalGradient(
                  colors =
                    listOf(
                      Color(0xA60B1020),
                      Color(0x660B1020),
                      Color(0x260B1020),
                    )
                )
            )
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = title,
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Color(0x66000000), offset = Offset(0f, 1.5f), blurRadius = 4f),
              ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.94f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0x470B1020),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0x4D0B1020),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f)),
            modifier = Modifier.size(60.dp),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xE6FFFFFF),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
              contentDescription = "Open $title",
              tint = Color(0xFF0F172A),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp).size(18.dp),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficeRecordDataTab(
  activeOffices: List<RegisteredOffice>,
  knownIoNames: List<String>,
  onOpenOfficeSettings: () -> Unit,
  onSaveCase: (
    officeId: String,
    recordDate: String,
    firNumber: Int,
    ioName: String,
    actType: String,
    customActName: String,
    sections: List<CaseSectionAttachment>,
    notes: String,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedOfficeId by remember(activeOffices) {
    mutableStateOf(activeOffices.firstOrNull()?.id)
  }
  LaunchedEffect(activeOffices) {
    if (selectedOfficeId == null || activeOffices.none { it.id == selectedOfficeId }) {
      selectedOfficeId = activeOffices.firstOrNull()?.id
    }
  }

  var recordDate by remember { mutableStateOf(DateUtils.todayIso()) }
  var firInput by remember { mutableStateOf("") }
  var ioNameInput by remember { mutableStateOf("") }
  var selectedAct by remember { mutableStateOf(LegalActType.BNS) }
  var customActName by remember { mutableStateOf("") }
  var selectedSections by remember { mutableStateOf<List<CaseSectionAttachment>>(emptyList()) }
  var notes by remember { mutableStateOf("") }
  var pickingDate by remember { mutableStateOf(false) }

  if (pickingDate) {
    OfficeIsoDatePickerDialog(
      title = "Select Record Date",
      initialDateIso = recordDate,
      onSelectDate = { recordDate = it },
      onDismiss = { pickingDate = false },
    )
  }

  val parsedFir = firInput.trim().toIntOrNull()
  val isValid =
    !selectedOfficeId.isNullOrBlank() &&
      DateUtils.isValidIsoDate(recordDate) &&
      parsedFir != null &&
      parsedFir > 0 &&
      ioNameInput.isNotBlank() &&
      (selectedSections.isNotEmpty() || (selectedAct == LegalActType.OTHERS && customActName.isNotBlank()))

  val formPalette = GradientPaletteRegistry.paletteForCard("office_record_form", 8)
  val accent = formPalette.colors.first()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (activeOffices.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "No Active Office Registered",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text =
                "Register at least one Office Name & Short Code in Settings -> Office Management before recording an FIR.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = onOpenOfficeSettings,
              modifier = Modifier.testTag("record_go_to_office_settings"),
            ) {
              Icon(Icons.Outlined.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open Office Management")
            }
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("office_record_form_card"),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          GradientAccentStrip(palette = formPalette, height = 4.dp)
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "New FIR / Case Entry",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            OfficeDropdownField(
              label = "Select Office",
              offices = activeOffices,
              selectedOfficeId = selectedOfficeId,
              includeAllOption = false,
              onSelectOfficeId = { selectedOfficeId = it },
              modifier = Modifier.fillMaxWidth().testTag("record_select_office_dropdown"),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = recordDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Record Date") },
                trailingIcon = {
                  IconButton(onClick = { pickingDate = true }) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick record date")
                  }
                },
                singleLine = true,
                modifier =
                  Modifier.weight(1f)
                    .clickable { pickingDate = true }
                    .testTag("record_date_field"),
              )
              OutlinedTextField(
                value = firInput,
                onValueChange = { txt -> firInput = txt.filter { it.isDigit() } },
                label = { Text("FIR Number") },
                placeholder = { Text("e.g. 104") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.95f).testTag("record_fir_number_input"),
              )
            }

            IoAutofillTextField(
              label = "Investigation Officer (I/O) Name",
              value = ioNameInput,
              onValueChange = { ioNameInput = it },
              knownIoNames = knownIoNames,
              placeholder = "Type I/O name (auto-saved for autofill)",
              modifier = Modifier.fillMaxWidth().testTag("record_io_name_input"),
            )

            ActAndSectionSelectorBlock(
              selectedAct = selectedAct,
              onSelectAct = { selectedAct = it },
              customActName = customActName,
              onCustomActNameChange = { customActName = it },
              selectedSections = selectedSections,
              onAddSection = { sec ->
                if (selectedSections.none { it.key() == sec.key() }) {
                  selectedSections = selectedSections + sec
                }
              },
              onRemoveSection = { sec ->
                selectedSections = selectedSections.filterNot { it.key() == sec.key() }
              },
              accentColor = accent,
            )

            OutlinedTextField(
              value = notes,
              onValueChange = { notes = it },
              label = { Text("Remarks / Notes (optional)") },
              maxLines = 3,
              modifier = Modifier.fillMaxWidth().testTag("record_notes_input"),
            )

            Button(
              onClick = {
                val oid = selectedOfficeId ?: return@Button
                val fir = parsedFir ?: return@Button
                onSaveCase(
                  oid,
                  recordDate,
                  fir,
                  ioNameInput,
                  selectedAct.code,
                  customActName,
                  selectedSections,
                  notes,
                )
                firInput = ""
                ioNameInput = ""
                customActName = ""
                selectedSections = emptyList()
                notes = ""
              },
              enabled = isValid,
              modifier = Modifier.fillMaxWidth().testTag("save_office_case_button"),
            ) {
              Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save FIR Record", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficeViewAndSearchTab(
  state: OfficeIndexUiState,
  officesById: Map<String, RegisteredOffice>,
  onUpdateQuery: ((OfficeCaseSearchQuery) -> OfficeCaseSearchQuery) -> Unit,
  onResetFilters: () -> Unit,
  onSwitchToRecord: () -> Unit,
  onViewRecord: (OfficeCaseRecord) -> Unit,
  onEditRecord: (OfficeCaseRecord) -> Unit,
  onDeleteRecord: (OfficeCaseRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFilters by remember { mutableStateOf(true) }
  var pickingStart by remember { mutableStateOf(false) }
  var pickingEnd by remember { mutableStateOf(false) }
  val q = state.caseQuery
  val today = DateUtils.todayIso()
  val searchPalette = GradientPaletteRegistry.paletteForCard("office_search", 10)
  val accent = searchPalette.colors.first()

  val (syncedSearchText, onSearchTextEdit) =
    rememberSyncedText(
      externalValue = q.searchText,
      onValueChange = { txt -> onUpdateQuery { it.copy(searchText = txt) } },
    )
  val (syncedFirQuery, onFirQueryEdit) =
    rememberSyncedText(
      externalValue = q.firNumberQuery,
      onValueChange = { txt -> onUpdateQuery { it.copy(firNumberQuery = txt) } },
      sanitize = { raw -> raw.filter { ch -> ch.isDigit() } },
    )
  val (syncedSectionQuery, onSectionQueryEdit) =
    rememberSyncedText(
      externalValue = q.sectionQuery,
      onValueChange = { txt -> onUpdateQuery { it.copy(sectionQuery = txt) } },
    )

  if (pickingStart || pickingEnd) {
    val isStart = pickingStart
    OfficeIsoDatePickerDialog(
      title = if (isStart) "Select From Date" else "Select To Date",
      initialDateIso = if (isStart) q.startDate else q.endDate,
      allowClear = true,
      onSelectDate = { iso ->
        onUpdateQuery { curr ->
          val rawStart = if (isStart) iso else curr.startDate
          val rawEnd = if (isStart) curr.endDate else iso
          curr.copy(startDate = rawStart, endDate = rawEnd)
        }
      },
      onClearDate = {
        onUpdateQuery { curr ->
          if (isStart) curr.copy(startDate = null) else curr.copy(endDate = null)
        }
      },
      onDismiss = {
        pickingStart = false
        pickingEnd = false
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = syncedSearchText,
          onValueChange = onSearchTextEdit,
          label = { Text("Quick search FIR, I/O, office, or section") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            if (syncedSearchText.isNotBlank()) {
              IconButton(onClick = { onSearchTextEdit("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("office_case_quick_search"),
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (showFilters) accent.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
        ) {
          IconButton(
            onClick = { showFilters = !showFilters },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("office_toggle_filters_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.FilterList,
              contentDescription = if (showFilters) "Hide Filters" else "Show Filters",
              tint = accent,
            )
          }
        }
      }
    }

    if (showFilters) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth().testTag("office_filters_card"),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = searchPalette, height = 3.dp)
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Search Filters (${q.activeFilterCount} active · ${state.caseRecords.size} results)",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                if (q.activeFilterCount > 0) {
                  TextButton(
                    onClick = onResetFilters,
                    modifier = Modifier.testTag("office_reset_filters_button"),
                  ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                  }
                }
              }

              // Date range presets
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = q.startDate == null && q.endDate == null,
                  onClick = { onUpdateQuery { it.copy(startDate = null, endDate = null) } },
                  label = { Text("All Dates") },
                )
                FilterChip(
                  selected = q.startDate == today && q.endDate == today,
                  onClick = { onUpdateQuery { it.copy(startDate = today, endDate = today) } },
                  label = { Text("Today") },
                )
                FilterChip(
                  selected = q.startDate == DateUtils.addDays(today, -6) && q.endDate == today,
                  onClick = {
                    onUpdateQuery { it.copy(startDate = DateUtils.addDays(today, -6), endDate = today) }
                  },
                  label = { Text("7 Days") },
                )
                FilterChip(
                  selected = q.startDate == DateUtils.addDays(today, -30) && q.endDate == today,
                  onClick = {
                    onUpdateQuery { it.copy(startDate = DateUtils.addDays(today, -30), endDate = today) }
                  },
                  label = { Text("30 Days") },
                )
              }

              // From / To Date Pickers
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = q.startDate ?: "",
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("From Date") },
                  placeholder = { Text("Any") },
                  trailingIcon = {
                    Row {
                      if (q.startDate != null) {
                        IconButton(onClick = { onUpdateQuery { it.copy(startDate = null) } }) {
                          Icon(Icons.Outlined.Close, contentDescription = "Clear From Date", modifier = Modifier.size(16.dp))
                        }
                      }
                      IconButton(onClick = { pickingStart = true }) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick From Date", modifier = Modifier.size(18.dp))
                      }
                    }
                  },
                  singleLine = true,
                  modifier =
                    Modifier.weight(1f)
                      .clickable { pickingStart = true }
                      .testTag("office_filter_from_date"),
                )
                OutlinedTextField(
                  value = q.endDate ?: "",
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("To Date") },
                  placeholder = { Text("Any") },
                  trailingIcon = {
                    Row {
                      if (q.endDate != null) {
                        IconButton(onClick = { onUpdateQuery { it.copy(endDate = null) } }) {
                          Icon(Icons.Outlined.Close, contentDescription = "Clear To Date", modifier = Modifier.size(16.dp))
                        }
                      }
                      IconButton(onClick = { pickingEnd = true }) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick To Date", modifier = Modifier.size(18.dp))
                      }
                    }
                  },
                  singleLine = true,
                  modifier =
                    Modifier.weight(1f)
                      .clickable { pickingEnd = true }
                      .testTag("office_filter_to_date"),
                )
              }

              // Office & FIR Number filters
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OfficeDropdownField(
                  label = "Office",
                  offices = state.offices,
                  selectedOfficeId = q.officeId,
                  includeAllOption = true,
                  onSelectOfficeId = { oid -> onUpdateQuery { it.copy(officeId = oid) } },
                  modifier = Modifier.weight(1.25f).testTag("office_filter_office_dropdown"),
                )
                OutlinedTextField(
                  value = syncedFirQuery,
                  onValueChange = onFirQueryEdit,
                  label = { Text("FIR #") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(0.75f).testTag("office_filter_fir_input"),
                )
              }

              // I/O Name (Current or Previous) & Section filters
              IoAutofillTextField(
                label = "I/O Name (Current or Previous)",
                value = q.ioNameQuery,
                onValueChange = { txt -> onUpdateQuery { it.copy(ioNameQuery = txt) } },
                knownIoNames = state.knownIoNames,
                placeholder = "Search any past or current I/O",
                modifier = Modifier.fillMaxWidth().testTag("office_filter_io_input"),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = syncedSectionQuery,
                  onValueChange = onSectionQueryEdit,
                  label = { Text("Section # / Keyword") },
                  placeholder = { Text("e.g. 103, 6, theft") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("office_filter_section_input"),
                )
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                FilterChip(
                  selected = q.actFilter == null,
                  onClick = { onUpdateQuery { it.copy(actFilter = null) } },
                  label = { Text("All Acts") },
                )
                LegalActType.entries.forEach { act ->
                  val selected = q.actFilter.equals(act.code, ignoreCase = true)
                  FilterChip(
                    selected = selected,
                    onClick = {
                      onUpdateQuery { it.copy(actFilter = if (selected) null else act.code) }
                    },
                    label = { Text(act.shortLabel) },
                  )
                }
              }
            }
          }
        }
      }
    }

    if (state.caseRecords.isEmpty()) {
      item {
        EmptyStateCard(
          title =
            if (state.allCaseRecordsCount == 0) "No FIR records recorded yet"
            else "No matching FIR records",
          message =
            if (state.allCaseRecordsCount == 0) {
              "Switch to Record Data to log your first FIR with Office, Date, I/O & Act Sections."
            } else {
              "No FIR cases match the selected date range, office, I/O, or section filters."
            },
          icon = Icons.Outlined.Inventory2,
        )
        if (state.allCaseRecordsCount == 0) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = onSwitchToRecord,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Outlined.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Go to Record Data")
          }
        }
      }
    } else {
      itemsIndexed(state.caseRecords, key = { _, rec -> rec.id }) { idx, record ->
        OfficeCaseResultCard(
          record = record,
          officeName = OfficeCaseEngine.resolveOfficeName(record, officesById),
          officeShortCode = OfficeCaseEngine.resolveOfficeShortCode(record, officesById),
          paletteIndex = idx,
          onView = { onViewRecord(record) },
          onEdit = { onEditRecord(record) },
          onDelete = { onDeleteRecord(record) },
        )
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficeCaseResultCard(
  record: OfficeCaseRecord,
  officeName: String,
  officeShortCode: String,
  paletteIndex: Int,
  onView: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val palette = GradientPaletteRegistry.paletteForCard(record.officeId.ifBlank { record.id }, paletteIndex)
  val accent = palette.colors.first()

  ReferenceDetailCard(
    accentColor = accent,
    modifier = Modifier.testTag("office_case_card_${record.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f).padding(end = 8.dp),
      ) {
        SolidPillTag(
          text = officeShortCode.ifBlank { "OFC" },
          containerColor = accent,
        )
        Text(
          text = "FIR #${record.firNumber}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      TintedPillTag(
        text = record.recordDate,
        accentColor = palette.colors[1],
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      DetailMetaPair(label = "Current I/O", value = record.currentIoName)
      DetailMetaPair(label = "Office", value = officeName)
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val badgeText = record.primaryActBadge()
      if (badgeText.isNotBlank()) {
        TintedPillTag(text = badgeText, accentColor = accent)
      }
      record.sections.take(4).forEach { sec ->
        TintedPillTag(
          text = "${sec.actDisplayName.ifBlank { sec.actCode }} §${sec.sectionNumber}",
          accentColor = palette.colors[1],
        )
      }
      if (record.sections.size > 4) {
        TintedPillTag(
          text = "+${record.sections.size - 4} more",
          accentColor = MaterialTheme.colorScheme.outline,
        )
      }
      if (record.ioHistory.size > 1) {
        TintedPillTag(
          text = "${record.ioHistory.size} I/Os in history",
          accentColor = palette.colors.last(),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(
        onClick = onView,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("view_case_${record.id}"),
      ) {
        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("View")
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onEdit,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_case_${record.id}"),
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Edit / I/O")
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_case_${record.id}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete FIR #${record.firNumber}",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
