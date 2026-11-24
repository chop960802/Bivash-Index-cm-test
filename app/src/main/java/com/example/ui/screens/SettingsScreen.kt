package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logic.DateUtils
import com.example.models.AppThemeMode
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.IctCategory
import com.example.models.IctSubCategory
import com.example.models.RegisteredOffice
import com.example.models.Shift
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.PillTag
import com.example.ui.components.TintedPillTag
import com.example.ui.components.VibrantFeatureBanner
import com.example.ui.state.SettingsSection
import com.example.ui.state.SettingsUiState
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.LocalStatusPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
  state: SettingsUiState,
  themeMode: AppThemeMode,
  onSelectThemeMode: (AppThemeMode) -> Unit,
  isExpandedWidth: Boolean,
  requestedSection: SettingsSection? = null,
  onConsumeRequestedSection: () -> Unit = {},
  onSaveEmployee: (String?, String, String, String, Boolean, String?) -> Unit,
  onToggleEmployeeActive: (Employee) -> Unit,
  onDeleteEmployee: (Employee) -> Unit,
  onSaveShift: (String?, String, String, String, Boolean, Boolean) -> Unit,
  onToggleShiftActive: (Shift) -> Unit,
  onDeleteShift: (Shift) -> Unit,
  onSaveDepartment: (String?, String, String, Boolean) -> Unit,
  onToggleDepartmentActive: (Department) -> Unit,
  onDeleteDepartment: (Department) -> Unit,
  onSaveDutyType: (String?, String, String, Boolean, String, String?) -> Unit,
  onToggleDutyActive: (DutyType) -> Unit,
  onDeleteDutyType: (DutyType) -> Unit,
  onSaveRegisteredOffice: (String?, String, String, Boolean) -> Unit,
  onToggleRegisteredOfficeActive: (RegisteredOffice) -> Unit,
  onDeleteRegisteredOffice: (RegisteredOffice) -> Unit,
  onSaveIctCategory: (String?, String, String) -> Unit,
  onDeleteIctCategory: (IctCategory) -> Unit,
  onSaveIctSubCategory: (String?, String, String, String) -> Unit,
  onDeleteIctSubCategory: (IctSubCategory) -> Unit,
  onBuildBackupJson: suspend () -> String,
  onInspectBackupJson: (String) -> Unit,
  onCreateSafetySnapshot: () -> Unit,
  onRestoreSafetySnapshot: (String) -> Unit,
  onInspectOfflineUpdateJson: (String) -> Unit,
  onLoadBuiltInUpdatePack: () -> Unit,
  onNotifyMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSection by remember {
    mutableStateOf(if (isExpandedWidth) SettingsSection.EMPLOYEES else null)
  }

  androidx.compose.runtime.LaunchedEffect(requestedSection) {
    if (requestedSection != null) {
      selectedSection = requestedSection
      onConsumeRequestedSection()
    }
  }

  if (!isExpandedWidth && selectedSection != null) {
    BackHandler { selectedSection = null }
  }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val exportBackupLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          try {
            val json = onBuildBackupJson()
            withContext(Dispatchers.IO) {
              context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
              } ?: error("Unable to open output stream.")
            }
            onNotifyMessage("Backup saved cleanly to device storage.")
          } catch (ex: Exception) {
            onNotifyMessage("Export failed: ${ex.localizedMessage ?: "I/O error"}")
          }
        }
      }
    }

  val importBackupLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          try {
            val content =
              withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                  it.readText()
                } ?: error("Unable to read selected file.")
              }
            onInspectBackupJson(content)
          } catch (ex: Exception) {
            onNotifyMessage("Read failed: ${ex.localizedMessage ?: "I/O error"}")
          }
        }
      }
    }

  val importUpdatePackLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          try {
            val content =
              withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                  it.readText()
                } ?: error("Unable to read update pack.")
              }
            onInspectOfflineUpdateJson(content)
          } catch (ex: Exception) {
            onNotifyMessage("Read failed: ${ex.localizedMessage ?: "I/O error"}")
          }
        }
      }
    }

  if (isExpandedWidth) {
    val active = selectedSection ?: SettingsSection.EMPLOYEES
    Row(modifier = modifier.fillMaxSize()) {
      SettingsHubList(
        state = state,
        themeMode = themeMode,
        onSelectThemeMode = onSelectThemeMode,
        selectedSection = active,
        onSelectSection = { selectedSection = it },
        modifier = Modifier.width(340.dp),
      )
      Box(modifier = Modifier.weight(1f).fillMaxSize()) {
        SettingsDetailRouter(
          section = active,
          state = state,
          onSaveEmployee = onSaveEmployee,
          onToggleEmployeeActive = onToggleEmployeeActive,
          onDeleteEmployee = onDeleteEmployee,
          onSaveShift = onSaveShift,
          onToggleShiftActive = onToggleShiftActive,
          onDeleteShift = onDeleteShift,
          onSaveDepartment = onSaveDepartment,
          onToggleDepartmentActive = onToggleDepartmentActive,
          onDeleteDepartment = onDeleteDepartment,
          onSaveDutyType = onSaveDutyType,
          onToggleDutyActive = onToggleDutyActive,
          onDeleteDutyType = onDeleteDutyType,
          onSaveRegisteredOffice = onSaveRegisteredOffice,
          onToggleRegisteredOfficeActive = onToggleRegisteredOfficeActive,
          onDeleteRegisteredOffice = onDeleteRegisteredOffice,
          onSaveIctCategory = onSaveIctCategory,
          onDeleteIctCategory = onDeleteIctCategory,
          onSaveIctSubCategory = onSaveIctSubCategory,
          onDeleteIctSubCategory = onDeleteIctSubCategory,
          onExportBackup = {
            exportBackupLauncher.launch("bivash_index_backup_${DateUtils.todayIso()}.json")
          },
          onImportBackup = {
            importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
          },
          onCreateSafetySnapshot = onCreateSafetySnapshot,
          onRestoreSafetySnapshot = onRestoreSafetySnapshot,
          onPickOfflineUpdate = {
            importUpdatePackLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
          },
          onLoadBuiltInUpdatePack = onLoadBuiltInUpdatePack,
        )
      }
    }
  } else {
    val active = selectedSection
    if (active == null) {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SettingsHubList(
          state = state,
          themeMode = themeMode,
          onSelectThemeMode = onSelectThemeMode,
          selectedSection = null,
          onSelectSection = { selectedSection = it },
          modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
        )
      }
    } else {
      Column(modifier = modifier.fillMaxSize()) {
        val activePalette = GradientPaletteRegistry.paletteForCard(active.name, active.ordinal)
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = { selectedSection = null },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back to Settings",
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = active.title,
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text = active.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            GradientAccentStrip(palette = activePalette, height = 3.dp)
          }
        }
        SettingsDetailRouter(
          section = active,
          state = state,
          onSaveEmployee = onSaveEmployee,
          onToggleEmployeeActive = onToggleEmployeeActive,
          onDeleteEmployee = onDeleteEmployee,
          onSaveShift = onSaveShift,
          onToggleShiftActive = onToggleShiftActive,
          onDeleteShift = onDeleteShift,
          onSaveDepartment = onSaveDepartment,
          onToggleDepartmentActive = onToggleDepartmentActive,
          onDeleteDepartment = onDeleteDepartment,
          onSaveDutyType = onSaveDutyType,
          onToggleDutyActive = onToggleDutyActive,
          onDeleteDutyType = onDeleteDutyType,
          onSaveRegisteredOffice = onSaveRegisteredOffice,
          onToggleRegisteredOfficeActive = onToggleRegisteredOfficeActive,
          onDeleteRegisteredOffice = onDeleteRegisteredOffice,
          onSaveIctCategory = onSaveIctCategory,
          onDeleteIctCategory = onDeleteIctCategory,
          onSaveIctSubCategory = onSaveIctSubCategory,
          onDeleteIctSubCategory = onDeleteIctSubCategory,
          onExportBackup = {
            exportBackupLauncher.launch("bivash_index_backup_${DateUtils.todayIso()}.json")
          },
          onImportBackup = {
            importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
          },
          onCreateSafetySnapshot = onCreateSafetySnapshot,
          onRestoreSafetySnapshot = onRestoreSafetySnapshot,
          onPickOfflineUpdate = {
            importUpdatePackLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
          },
          onLoadBuiltInUpdatePack = onLoadBuiltInUpdatePack,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun SettingsHubList(
  state: SettingsUiState,
  themeMode: AppThemeMode,
  onSelectThemeMode: (AppThemeMode) -> Unit,
  selectedSection: SettingsSection?,
  onSelectSection: (SettingsSection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Settings & Data",
        subtitle = state.recordCountsSummary,
        palette = GradientPaletteRegistry.paletteForCard("settings_hub", 4),
        floralVariant = FloralVariant.LEAF_GARLAND,
        icon = Icons.Outlined.Settings,
        badgeText = "OFFLINE",
      )
    }

    item {
      ThemeModeSelectorCard(
        themeMode = themeMode,
        onSelectThemeMode = onSelectThemeMode,
      )
    }

    itemsIndexed(SettingsSection.entries, key = { _, s -> s.name }) { idx, section ->
      val isSelected = selectedSection == section
      val sectionPalette = GradientPaletteRegistry.paletteForCard(section.name, idx)
      val primaryAccent = sectionPalette.colors.first()
      Surface(
        shape = RoundedCornerShape(16.dp),
        color =
          if (isSelected) primaryAccent.copy(alpha = if (isDark) 0.22f else 0.11f)
          else MaterialTheme.colorScheme.surface,
        border =
          BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = primaryAccent.copy(alpha = if (isSelected) 0.90f else 0.36f),
          ),
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onSelectSection(section) }
            .testTag("settings_item_${section.name.lowercase()}"),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          if (isSelected) {
            FloralTextureOverlay(
              variant = GradientPaletteRegistry.floralVariantForCard(section.name, idx),
              motifColor = primaryAccent.copy(alpha = 0.28f),
              modifier = Modifier.matchParentSize(),
            )
          }
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = sectionPalette, height = 3.dp)
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                modifier = Modifier.size(40.dp),
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxSize()
                      .background(
                        Brush.linearGradient(
                          colors = sectionPalette.colors,
                          start = Offset.Zero,
                          end = Offset(120f, 120f),
                        )
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(modifier = Modifier.matchParentSize().background(Color(0x660B1020)))
                  Icon(
                    imageVector = sectionIcon(section),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = section.title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text = section.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeModeSelectorCard(
  themeMode: AppThemeMode,
  onSelectThemeMode: (AppThemeMode) -> Unit,
) {
  val themePalette = GradientPaletteRegistry.paletteForCard("theme_mode", 12)
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, themePalette.colors.first().copy(alpha = 0.42f)),
    modifier = Modifier.fillMaxWidth().testTag("settings_theme_card"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      GradientAccentStrip(palette = themePalette, height = 4.dp)
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color.Transparent,
              modifier = Modifier.size(34.dp),
            ) {
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(Brush.linearGradient(themePalette.colors)),
                contentAlignment = Alignment.Center,
              ) {
                Box(modifier = Modifier.matchParentSize().background(Color(0x660B1020)))
                Icon(
                  imageVector = Icons.Outlined.Palette,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              )
              Text(
                text =
                  when (themeMode) {
                    AppThemeMode.SYSTEM -> "Follows your phone's Day / Night mode automatically"
                    AppThemeMode.DAY -> "Bright light appearance"
                    AppThemeMode.NIGHT -> "Deep dark appearance"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          TintedPillTag(
            text = themeMode.label,
            accentColor = themePalette.colors.first(),
          )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          AppThemeMode.entries.forEachIndexed { idx, mode ->
            val icon =
              when (mode) {
                AppThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                AppThemeMode.DAY -> Icons.Outlined.LightMode
                AppThemeMode.NIGHT -> Icons.Outlined.DarkMode
              }
            SegmentedButton(
              selected = themeMode == mode,
              onClick = { onSelectThemeMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(idx, AppThemeMode.entries.size),
              icon = {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
            ) {
              Text(mode.label, maxLines = 1)
            }
          }
        }
      }
    }
  }
}

private fun sectionIcon(section: SettingsSection): ImageVector =
  when (section) {
    SettingsSection.EMPLOYEES -> Icons.Outlined.Badge
    SettingsSection.SHIFTS -> Icons.Outlined.Schedule
    SettingsSection.DUTIES -> Icons.Outlined.Apartment
    SettingsSection.OFFICES -> Icons.Outlined.AccountBalance
    SettingsSection.ICT_DATA -> Icons.Outlined.Category
    SettingsSection.BACKUP -> Icons.Outlined.Backup
    SettingsSection.UPDATES -> Icons.Outlined.SystemUpdateAlt
    SettingsSection.MODULES -> Icons.Outlined.Extension
    SettingsSection.ABOUT -> Icons.Outlined.Info
  }

@Composable
private fun SettingsDetailRouter(
  section: SettingsSection,
  state: SettingsUiState,
  onSaveEmployee: (String?, String, String, String, Boolean, String?) -> Unit,
  onToggleEmployeeActive: (Employee) -> Unit,
  onDeleteEmployee: (Employee) -> Unit,
  onSaveShift: (String?, String, String, String, Boolean, Boolean) -> Unit,
  onToggleShiftActive: (Shift) -> Unit,
  onDeleteShift: (Shift) -> Unit,
  onSaveDepartment: (String?, String, String, Boolean) -> Unit,
  onToggleDepartmentActive: (Department) -> Unit,
  onDeleteDepartment: (Department) -> Unit,
  onSaveDutyType: (String?, String, String, Boolean, String, String?) -> Unit,
  onToggleDutyActive: (DutyType) -> Unit,
  onDeleteDutyType: (DutyType) -> Unit,
  onSaveRegisteredOffice: (String?, String, String, Boolean) -> Unit,
  onToggleRegisteredOfficeActive: (RegisteredOffice) -> Unit,
  onDeleteRegisteredOffice: (RegisteredOffice) -> Unit,
  onSaveIctCategory: (String?, String, String) -> Unit,
  onDeleteIctCategory: (IctCategory) -> Unit,
  onSaveIctSubCategory: (String?, String, String, String) -> Unit,
  onDeleteIctSubCategory: (IctSubCategory) -> Unit,
  onExportBackup: () -> Unit,
  onImportBackup: () -> Unit,
  onCreateSafetySnapshot: () -> Unit,
  onRestoreSafetySnapshot: (String) -> Unit,
  onPickOfflineUpdate: () -> Unit,
  onLoadBuiltInUpdatePack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (section) {
    SettingsSection.EMPLOYEES ->
      EmployeeManagerPane(
        employees = state.employees,
        departments = state.departments,
        assignedShiftsByEmployee = state.assignedShiftsByEmployee,
        onSaveEmployee = onSaveEmployee,
        onToggleActive = onToggleEmployeeActive,
        onDeleteEmployee = onDeleteEmployee,
        modifier = modifier,
      )
    SettingsSection.SHIFTS ->
      ShiftManagerPane(
        shifts = state.shifts,
        onSaveShift = onSaveShift,
        onToggleActive = onToggleShiftActive,
        onDeleteShift = onDeleteShift,
        modifier = modifier,
      )
    SettingsSection.DUTIES ->
      DepartmentManagerPane(
        departments = state.departments,
        duties = state.dutyTypes,
        shifts = state.shifts,
        onSaveDepartment = onSaveDepartment,
        onToggleDepartmentActive = onToggleDepartmentActive,
        onDeleteDepartment = onDeleteDepartment,
        onSaveDuty = onSaveDutyType,
        onToggleDutyActive = onToggleDutyActive,
        onDeleteDuty = onDeleteDutyType,
        modifier = modifier,
      )
    SettingsSection.OFFICES ->
      OfficeSettingsPane(
        offices = state.offices,
        onSaveOffice = onSaveRegisteredOffice,
        onToggleActive = onToggleRegisteredOfficeActive,
        onDeleteOffice = onDeleteRegisteredOffice,
        modifier = modifier,
      )
    SettingsSection.ICT_DATA ->
      IctSettingsPane(
        categories = state.ictCategories,
        subCategories = state.ictSubCategories,
        entriesCountByCategory = state.ictEntriesCountByCategory,
        onSaveCategory = onSaveIctCategory,
        onDeleteCategory = onDeleteIctCategory,
        onSaveSubCategory = onSaveIctSubCategory,
        onDeleteSubCategory = onDeleteIctSubCategory,
        modifier = modifier,
      )
    SettingsSection.BACKUP ->
      BackupRestorePane(
        state = state,
        onExportBackup = onExportBackup,
        onImportBackup = onImportBackup,
        onCreateSafetySnapshot = onCreateSafetySnapshot,
        onRestoreSafetySnapshot = onRestoreSafetySnapshot,
        modifier = modifier,
      )
    SettingsSection.UPDATES ->
      OfflineUpdatePane(
        state = state,
        onPickOfflineUpdate = onPickOfflineUpdate,
        onLoadBuiltInUpdatePack = onLoadBuiltInUpdatePack,
        modifier = modifier,
      )
    SettingsSection.MODULES ->
      ModulesOverviewPane(state = state, modifier = modifier)
    SettingsSection.ABOUT ->
      AboutSettingsPane(modifier = modifier)
  }
}

@Composable
private fun BackupRestorePane(
  state: SettingsUiState,
  onExportBackup: () -> Unit,
  onImportBackup: () -> Unit,
  onCreateSafetySnapshot: () -> Unit,
  onRestoreSafetySnapshot: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val backupPalette = GradientPaletteRegistry.paletteForCard("BACKUP", 3)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Versioned Local Backup",
        subtitle =
          "Schema v${state.appMeta.schemaVersion} · Last export: ${DateUtils.formatTimestamp(state.appMeta.lastBackupAt)}",
        palette = backupPalette,
        floralVariant = FloralVariant.CORNER_CLUSTER,
        icon = Icons.Outlined.Backup,
        badgeText = "SHA-256",
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, backupPalette.colors.first().copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          GradientAccentStrip(palette = backupPalette, height = 4.dp)
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text =
                "Exports employees, shifts, duty templates, roster assignments, attendance records, and metadata into a SHA-256 checksummed JSON file.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onExportBackup,
                modifier = Modifier.weight(1f).testTag("export_backup_button"),
              ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export JSON")
              }
              FilledTonalButton(
                onClick = onImportBackup,
                modifier = Modifier.weight(1f).testTag("import_backup_button"),
              ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore JSON")
              }
            }
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Local Safety Snapshots", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = onCreateSafetySnapshot,
          modifier = Modifier.testTag("create_safety_snapshot_button"),
        ) {
          Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Checkpoint")
        }
      }
    }

    if (state.safetySnapshots.isEmpty()) {
      item {
        Text(
          text =
            "Pre-restore safety snapshots are automatically saved here whenever a backup restore runs, or you can save a manual checkpoint above.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      itemsIndexed(state.safetySnapshots, key = { _, snap -> snap.id }) { idx, snap ->
        val snapPalette = GradientPaletteRegistry.paletteForCard(snap.id, idx)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, snapPalette.colors.first().copy(alpha = 0.40f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = snapPalette, height = 3.dp)
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = snap.reason, style = MaterialTheme.typography.labelLarge)
                TintedPillTag(
                  text = DateUtils.formatTimestamp(snap.createdAt),
                  accentColor = snapPalette.colors.first(),
                )
                Text(
                  text = snap.summary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              FilledTonalButton(onClick = { onRestoreSafetySnapshot(snap.id) }) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OfflineUpdatePane(
  state: SettingsUiState,
  onPickOfflineUpdate: () -> Unit,
  onLoadBuiltInUpdatePack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val updatePalette = GradientPaletteRegistry.paletteForCard("UPDATES", 4)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Offline Update Pack",
        subtitle =
          "Installed: ${state.appMeta.lastUpdatePackId ?: "Base Seed"} (${state.appMeta.lastUpdatePackVersion ?: "v1.0.0"})",
        palette = updatePalette,
        floralVariant = FloralVariant.SCATTERED_BLOSSOMS,
        icon = Icons.Outlined.SystemUpdateAlt,
        badgeText = "PACK",
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, updatePalette.colors.first().copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          GradientAccentStrip(palette = updatePalette, height = 4.dp)
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text =
                "Applies preset shifts, duty templates, and reference rules from a checksummed local JSON update pack without touching employee or attendance records.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = onPickOfflineUpdate,
                modifier = Modifier.fillMaxWidth().testTag("pick_offline_update_button"),
              ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Update Pack (.json)")
              }
              OutlinedButton(
                onClick = onLoadBuiltInUpdatePack,
                modifier = Modifier.fillMaxWidth().testTag("load_sample_update_pack_button"),
              ) {
                Text("Preview Bundled Sample Pack (v1.1.0)")
              }
            }
          }
        }
      }
    }

    if (state.referenceRules.isNotEmpty()) {
      item {
        Text("Active Reference Rules", style = MaterialTheme.typography.titleMedium)
      }
      itemsIndexed(state.referenceRules, key = { _, r -> r.key }) { idx, rule ->
        val rulePalette = GradientPaletteRegistry.paletteForCard(rule.key, idx)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, rulePalette.colors.first().copy(alpha = 0.40f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = rulePalette, height = 3.dp)
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = rule.title, style = MaterialTheme.typography.labelLarge)
              Text(
                text = rule.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ModulesOverviewPane(
  state: SettingsUiState,
  modifier: Modifier = Modifier,
) {
  val palette = LocalStatusPalette.current
  val modulesPalette = GradientPaletteRegistry.paletteForCard("MODULES", 5)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      VibrantFeatureBanner(
        title = "Module Registry",
        subtitle = "Feature flags & reserved workspace slots",
        palette = modulesPalette,
        floralVariant = FloralVariant.RADIAL_PETALS,
        icon = Icons.Outlined.Extension,
        badgeText = "${state.modules.size} MODULES",
      )
    }
    itemsIndexed(state.modules, key = { _, mod -> mod.id.name }) { idx, mod ->
      val modPalette = GradientPaletteRegistry.paletteForCard(mod.id.name, idx)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, modPalette.colors.first().copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          FloralTextureOverlay(
            variant = GradientPaletteRegistry.floralVariantForCard(mod.id.name, idx),
            motifColor = modPalette.colors.first().copy(alpha = 0.22f),
            modifier = Modifier.matchParentSize(),
          )
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = modPalette, height = 4.dp)
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = mod.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  )
                  TintedPillTag(
                    text = "${mod.subtitle} · ${mod.versionLabel}",
                    accentColor = modPalette.colors.first(),
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PillTag(
                  text = mod.badgeText,
                  containerColor =
                    if (mod.isEnabled) palette.present.container
                    else MaterialTheme.colorScheme.surfaceVariant,
                  contentColor =
                    if (mod.isEnabled) palette.present.text
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = mod.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
