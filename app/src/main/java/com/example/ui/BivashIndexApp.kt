package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BackupRestoreValidationDialog
import com.example.ui.components.ConflictResolutionDialog
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.components.OfflineUpdatePreviewDialog
import com.example.ui.components.RosterAssignmentBottomSheet
import com.example.ui.components.isHardwareEchoDuplicate
import com.example.ui.components.isPrintableKeyCode
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IctDataScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.OfficeIndexScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskManagerScreen
import com.example.ui.screens.UniversalSearchScreen
import com.example.ui.state.AppTab
import com.example.ui.state.AssignSheetFocus
import com.example.ui.state.BivashViewModel

@Composable
fun BivashIndexApp(
  viewModel: BivashViewModel = viewModel(),
  onExitApp: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
  val officeIndexState by viewModel.officeIndexState.collectAsStateWithLifecycle()
  val isOfficeIndexOpen by viewModel.isOfficeIndexOpen.collectAsStateWithLifecycle()
  val ictDataState by viewModel.ictDataState.collectAsStateWithLifecycle()
  val isIctDataOpen by viewModel.isIctDataOpen.collectAsStateWithLifecycle()
  val taskManagerState by viewModel.taskManagerState.collectAsStateWithLifecycle()
  val isTaskManagerOpen by viewModel.isTaskManagerOpen.collectAsStateWithLifecycle()
  val notesState by viewModel.notesState.collectAsStateWithLifecycle()
  val isNotesOpen by viewModel.isNotesOpen.collectAsStateWithLifecycle()
  val universalSearchState by viewModel.universalSearchState.collectAsStateWithLifecycle()
  val isUniversalSearchOpen by viewModel.isUniversalSearchOpen.collectAsStateWithLifecycle()
  val requestedSettingsSection by viewModel.requestedSettingsSection.collectAsStateWithLifecycle()
  val registerState by viewModel.registerState.collectAsStateWithLifecycle()
  val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
  val assignmentSheet by viewModel.assignmentSheet.collectAsStateWithLifecycle()
  val pendingConflict by viewModel.pendingConflict.collectAsStateWithLifecycle()
  val pendingBackupRestore by viewModel.pendingBackupRestore.collectAsStateWithLifecycle()
  val pendingUpdatePreview by viewModel.pendingUpdatePreview.collectAsStateWithLifecycle()

  var showExitDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel) {
    viewModel.snackbarEvents.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  when {
    currentTab == AppTab.DASHBOARD && isUniversalSearchOpen -> {
      BackHandler { viewModel.closeUniversalSearch() }
    }
    currentTab == AppTab.DASHBOARD && isTaskManagerOpen -> {
      BackHandler { viewModel.closeTaskManager() }
    }
    currentTab == AppTab.DASHBOARD && isNotesOpen -> {
      BackHandler { viewModel.closeNotes() }
    }
    currentTab == AppTab.DASHBOARD && isIctDataOpen -> {
      BackHandler { viewModel.closeIctData() }
    }
    currentTab == AppTab.DASHBOARD && isOfficeIndexOpen -> {
      BackHandler { viewModel.closeOfficeIndex() }
    }
    currentTab != AppTab.DASHBOARD -> {
      BackHandler { viewModel.selectTab(AppTab.DASHBOARD) }
    }
    else -> {
      BackHandler(enabled = !showExitDialog) { showExitDialog = true }
    }
  }

  if (showExitDialog) {
    val hasPendingEdits =
      assignmentSheet.isVisible ||
        pendingConflict != null ||
        pendingBackupRestore != null ||
        pendingUpdatePreview != null
    ExitConfirmationDialog(
      hasUnsavedChanges = hasPendingEdits,
      onStay = { showExitDialog = false },
      onConfirmExit = {
        showExitDialog = false
        if (onExitApp != null) {
          onExitApp()
        } else {
          (context as? Activity)?.finish()
        }
      },
    )
  }

  if (assignmentSheet.isVisible) {
    RosterAssignmentBottomSheet(
      sheetState = assignmentSheet,
      employees = registerState.activeEmployees,
      shifts = registerState.activeShifts,
      duties = registerState.activeDuties,
      departments = registerState.activeDepartments,
      carryForwardByEmployee = registerState.employeeCarryForward,
      onDismiss = viewModel::dismissAssignmentSheet,
      onSubmit = viewModel::submitRosterAssignment,
      onSubmitBatch = viewModel::submitBatchRosterAssignment,
    )
  }

  pendingConflict?.let { conflict ->
    ConflictResolutionDialog(
      pending = conflict,
      onOverwrite = viewModel::confirmOverwriteConflicts,
      onDismiss = viewModel::dismissConflictDialog,
    )
  }

  pendingBackupRestore?.let { backup ->
    BackupRestoreValidationDialog(
      pending = backup,
      onConfirmRestore = viewModel::confirmRestoreFromBackup,
      onDismiss = viewModel::dismissBackupRestoreDialog,
    )
  }

  pendingUpdatePreview?.let { preview ->
    OfflineUpdatePreviewDialog(
      preview = preview,
      onApply = viewModel::applyOfflineUpdate,
      onDismiss = viewModel::dismissUpdatePreview,
    )
  }

  var lastHardwareKeyCode by remember { mutableIntStateOf(-1) }
  var lastHardwareKeyTimeMs by remember { mutableLongStateOf(-1L) }

  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize().onPreviewKeyEvent { keyEvent ->
        val native = keyEvent.nativeKeyEvent
        if (
          keyEvent.type == KeyEventType.KeyDown &&
            isHardwareEchoDuplicate(
              keyCode = native.keyCode,
              repeatCount = native.repeatCount,
              eventTimeMs = native.eventTime,
              lastKeyCode = lastHardwareKeyCode,
              lastEventTimeMs = lastHardwareKeyTimeMs,
            )
        ) {
          true
        } else {
          if (
            keyEvent.type == KeyEventType.KeyDown &&
              native.repeatCount == 0 &&
              isPrintableKeyCode(native.keyCode)
          ) {
            lastHardwareKeyCode = native.keyCode
            lastHardwareKeyTimeMs = native.eventTime
          }
          false
        }
      }
  ) {
    val useRail = maxWidth >= 600.dp
    val isExpandedSettings = maxWidth >= 840.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useRail) {
          Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 0.dp,
            ) {
              AppTab.entries.forEach { tab ->
                val selected = currentTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = {
                    Icon(
                      imageVector = tabIcon(tab, selected),
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                  colors =
                    NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                  modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                )
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            AppTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab, selected),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (currentTab) {
            AppTab.DASHBOARD ->
              when {
                isUniversalSearchOpen ->
                  UniversalSearchScreen(
                    state = universalSearchState,
                    onBack = viewModel::closeUniversalSearch,
                    onUpdateQuery = viewModel::updateUniversalSearchQuery,
                    onResetFilters = viewModel::resetUniversalSearchFilters,
                    onOpenResult = viewModel::openFromUniversalSearch,
                  )
                isTaskManagerOpen ->
                  TaskManagerScreen(
                    state = taskManagerState,
                    onBack = viewModel::closeTaskManager,
                    onSelectCategory = viewModel::selectTaskCategory,
                    onAddCategories = viewModel::saveTaskCategoriesBulk,
                    onRenameCategory = viewModel::renameTaskCategory,
                    onDeleteCategory = viewModel::deleteTaskCategory,
                    onAddItems = viewModel::saveTaskItemsBulk,
                    onToggleItem = viewModel::toggleTaskItem,
                    onRenameItem = viewModel::renameTaskItem,
                    onDeleteItem = viewModel::deleteTaskItem,
                  )
                isNotesOpen ->
                  NotesScreen(
                    state = notesState,
                    onBack = viewModel::closeNotes,
                    onSearchTextChange = viewModel::setNotesSearch,
                    onSaveNote = viewModel::saveNote,
                    onDeleteNote = viewModel::deleteNote,
                  )
                isIctDataOpen ->
                  IctDataScreen(
                    state = ictDataState,
                    onBack = viewModel::closeIctData,
                    onOpenSettings = viewModel::openIctSettings,
                    onSelectCategory = viewModel::setIctCategoryFilter,
                    onSelectSubCategory = viewModel::setIctSubCategoryFilter,
                    onSearchTextChange = viewModel::setIctSearch,
                    onSaveEntry = viewModel::saveIctEntry,
                    onDeleteEntry = viewModel::deleteIctEntry,
                  )
                isOfficeIndexOpen ->
                  OfficeIndexScreen(
                    state = officeIndexState,
                    onBack = viewModel::closeOfficeIndex,
                    onOpenOfficeSettings = viewModel::openOfficeSettings,
                    onUpdateCaseQuery = viewModel::updateOfficeCaseQuery,
                    onResetCaseFilters = viewModel::resetOfficeCaseFilters,
                    onSaveNewCaseRecord = viewModel::saveNewOfficeCaseRecord,
                    onUpdateCaseRecord = viewModel::updateOfficeCaseRecord,
                    onDeleteCaseRecord = viewModel::deleteOfficeCaseRecord,
                  )
                else ->
                  DashboardScreen(
                    onOpenAttendance = {
                      viewModel.openAttendanceForDate(dashboardState.selectedDate)
                    },
                    onOpenOfficeIndex = viewModel::openOfficeIndex,
                    onOpenIctData = viewModel::openIctData,
                    onOpenTaskManager = { viewModel.openTaskManager() },
                    onOpenNotes = viewModel::openNotes,
                    onOpenUniversalSearch = viewModel::openUniversalSearch,
                  )
              }

            AppTab.REGISTER ->
              RegisterScreen(
                state = registerState,
                onSelectSubTab = viewModel::setRegisterSubTab,
                onSelectAttendanceDisplayMode = viewModel::setAttendanceDisplayMode,
                onSelectAttendanceDate = viewModel::selectAttendanceDate,
                onStepCalendarMonth = viewModel::stepCalendarMonth,
                onPreviousDay = { viewModel.stepDate(-1) },
                onNextDay = { viewModel.stepDate(1) },
                onJumpToday = viewModel::jumpToToday,
                onSelectShiftFilter = viewModel::setShiftFilter,
                onMarkAttendance = viewModel::markAttendance,
                onBulkMarkPresent = viewModel::markRemainingAsPresent,
                onSelectRosterViewMode = viewModel::setRosterViewMode,
                onOpenAssignSheet = { date, ids ->
                  viewModel.openAssignmentSheet(
                    focus = AssignSheetFocus.BOTH,
                    startDate = date,
                    preselectedEmployeeIds = ids,
                  )
                },
                onRemoveAssignment = viewModel::removeRosterAssignment,
                onUpdateHistoryQuery = viewModel::updateHistoryQuery,
                onResetHistoryFilters = viewModel::resetHistoryFilters,
              )

            AppTab.SETTINGS ->
              SettingsScreen(
                state = settingsState,
                themeMode = themeMode,
                onSelectThemeMode = viewModel::setThemeMode,
                isExpandedWidth = isExpandedSettings,
                requestedSection = requestedSettingsSection,
                onConsumeRequestedSection = viewModel::consumeRequestedSettingsSection,
                onSaveEmployee = viewModel::saveEmployee,
                onToggleEmployeeActive = viewModel::toggleEmployeeActive,
                onDeleteEmployee = viewModel::deleteEmployee,
                onSaveShift = viewModel::saveShift,
                onToggleShiftActive = viewModel::toggleShiftActive,
                onDeleteShift = viewModel::deleteShift,
                onSaveDepartment = viewModel::saveDepartment,
                onToggleDepartmentActive = viewModel::toggleDepartmentActive,
                onDeleteDepartment = viewModel::deleteDepartment,
                onSaveDutyType = viewModel::saveDutyType,
                onToggleDutyActive = viewModel::toggleDutyActive,
                onDeleteDutyType = viewModel::deleteDutyType,
                onSaveRegisteredOffice = viewModel::saveRegisteredOffice,
                onToggleRegisteredOfficeActive = viewModel::toggleRegisteredOfficeActive,
                onDeleteRegisteredOffice = viewModel::deleteRegisteredOffice,
                onSaveIctCategory = viewModel::saveIctCategory,
                onDeleteIctCategory = viewModel::deleteIctCategory,
                onSaveIctSubCategory = viewModel::saveIctSubCategory,
                onDeleteIctSubCategory = viewModel::deleteIctSubCategory,
                onBuildBackupJson = viewModel::buildExportBackupJson,
                onInspectBackupJson = viewModel::inspectBackupJson,
                onCreateSafetySnapshot = viewModel::createManualSafetySnapshot,
                onRestoreSafetySnapshot = viewModel::restoreSafetySnapshot,
                onInspectOfflineUpdateJson = viewModel::inspectOfflineUpdateJson,
                onLoadBuiltInUpdatePack = viewModel::loadBuiltInSampleUpdatePack,
                onNotifyMessage = viewModel::emitMessage,
              )
          }
        }
      }
    }
  }
}

private fun tabIcon(tab: AppTab, selected: Boolean): ImageVector =
  when (tab) {
    AppTab.DASHBOARD -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
    AppTab.REGISTER -> if (selected) Icons.Filled.FactCheck else Icons.Outlined.FactCheck
    AppTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
  }
