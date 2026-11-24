package com.example.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.logic.AttendanceCalculator
import com.example.logic.DateUtils
import com.example.logic.HistorySearchEngine
import com.example.logic.ModuleRegistry
import com.example.logic.OfficeCaseEngine
import com.example.logic.OfflineUpdateEngine
import com.example.logic.RosterConflictDetector
import com.example.logic.UniversalSearchEngine
import com.example.models.AppMeta
import com.example.models.AppModuleId
import com.example.models.AppThemeMode
import com.example.models.AttendanceDisplayMode
import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.BackupValidationResult
import com.example.models.BatchRosterAssignmentRequest
import com.example.models.CalendarDateBreakdown
import com.example.models.CalendarDayCell
import com.example.models.CaseSectionAttachment
import com.example.models.DailyAttendanceRow
import com.example.models.DailyAttendanceSummary
import com.example.models.Department
import com.example.models.DutyType
import com.example.models.Employee
import com.example.models.EmployeeCarryForward
import com.example.models.EmployeeScheduleGroup
import com.example.models.HistoryFilterQuery
import com.example.models.HistorySearchEntry
import com.example.models.IctCategory
import com.example.models.IctCategoryGroup
import com.example.models.IctEntry
import com.example.models.IctSubCategory
import com.example.models.ModuleDefinition
import com.example.models.NoteEntry
import com.example.models.OfficeCaseRecord
import com.example.models.OfficeCaseSearchQuery
import com.example.models.OfficeIndexEntry
import com.example.models.OfficeIndexSummary
import com.example.models.OfflineUpdateApplyResult
import com.example.models.OfflineUpdatePack
import com.example.models.OfflineUpdatePreview
import com.example.models.ReferenceRule
import com.example.models.RegisteredOffice
import com.example.models.RosterApplyOutcome
import com.example.models.RosterAssignment
import com.example.models.RosterAssignmentRequest
import com.example.models.RosterConflict
import com.example.models.RosterViewMode
import com.example.models.SafetySnapshotInfo
import com.example.models.Shift
import com.example.models.TaskCategory
import com.example.models.TaskCategoryGroup
import com.example.models.TaskItem
import com.example.models.UniversalSearchFacets
import com.example.models.UniversalSearchModule
import com.example.models.UniversalSearchQuery
import com.example.models.UniversalSearchResult
import com.example.models.UpcomingCoverageDay
import com.example.storage.BivashDatabase
import com.example.storage.BivashRepository
import com.example.storage.ThemePreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
  DASHBOARD("Dashboard"),
  REGISTER("Register"),
  SETTINGS("Settings"),
}

enum class RegisterSubTab(val label: String) {
  ATTENDANCE("Attendance"),
  ROSTER("Roster"),
  HISTORY("History"),
}

enum class SettingsSection(val title: String, val subtitle: String) {
  EMPLOYEES("Employee Registration", "Add, edit, rename, deactivate, or delete team members"),
  SHIFTS("Shift Management", "Configure shift windows & Night Shift visual highlight"),
  DUTIES("Department", "Unlimited departments & nested duties (default: Computer)"),
  OFFICES("Office Management", "Add, edit, rename, or delete Offices & Short Codes"),
  ICT_DATA("ICT Data Categories", "Add, edit, rename, or delete ICT categories & sub-categories"),
  BACKUP("Backup & Restore", "Export versioned JSON backups and restore atomically"),
  UPDATES("Offline Update Manager", "Apply checksummed local update packs safely"),
  MODULES("Modules Overview", "Active v1 module and reserved Index / Counter slots"),
  ABOUT("About", "App info, developer credit, API key & build date"),
}

enum class AssignSheetFocus {
  BOTH,
  DUTY_FIRST,
  SHIFT_FIRST,
}

data class AssignmentSheetState(
  val isVisible: Boolean = false,
  val focus: AssignSheetFocus = AssignSheetFocus.BOTH,
  val startDate: String = DateUtils.todayIso(),
  val endDate: String = DateUtils.todayIso(),
  val preselectedEmployeeIds: Set<String> = emptySet(),
)

data class PendingConflictState(
  val request: RosterAssignmentRequest? = null,
  val batchRequest: BatchRosterAssignmentRequest? = null,
  val conflicts: List<RosterConflict> = emptyList(),
)

data class PendingBackupRestoreState(
  val rawJson: String,
  val validation: BackupValidationResult,
)

data class DashboardUiState(
  val selectedDate: String = DateUtils.todayIso(),
  val summary: DailyAttendanceSummary =
    DailyAttendanceSummary(
      date = DateUtils.todayIso(),
      totalActiveEmployees = 0,
      scheduledSlotsCount = 0,
      presentCount = 0,
      absentCount = 0,
      lateCount = 0,
      leaveCount = 0,
      offCount = 0,
      unmarkedSlotsCount = 0,
    ),
  val upcomingCoverage: List<UpcomingCoverageDay> = emptyList(),
  val officeIndexSummary: OfficeIndexSummary = OfficeIndexSummary(),
  val activeModules: List<ModuleDefinition> = ModuleRegistry.getEnabledModules(),
  val isLoading: Boolean = true,
)

data class OfficeIndexUiState(
  val offices: List<RegisteredOffice> = emptyList(),
  val activeOffices: List<RegisteredOffice> = emptyList(),
  val caseRecords: List<OfficeCaseRecord> = emptyList(),
  val allCaseRecordsCount: Int = 0,
  val knownIoNames: List<String> = emptyList(),
  val caseQuery: OfficeCaseSearchQuery = OfficeCaseSearchQuery(),
  val entries: List<OfficeIndexEntry> = emptyList(),
  val allEntriesCount: Int = 0,
  val categories: List<String> = emptyList(),
  val selectedCategory: String? = null,
  val searchText: String = "",
)

data class IctDataUiState(
  val categoryGroups: List<IctCategoryGroup> = emptyList(),
  val categories: List<IctCategory> = emptyList(),
  val subCategories: List<IctSubCategory> = emptyList(),
  val entries: List<IctEntry> = emptyList(),
  val totalEntriesCount: Int = 0,
  val selectedCategoryId: String? = null,
  val selectedSubCategoryId: String? = null,
  val searchText: String = "",
)

data class TaskManagerUiState(
  val categories: List<TaskCategory> = emptyList(),
  val categoryGroups: List<TaskCategoryGroup> = emptyList(),
  val selectedCategoryId: String? = null,
  val selectedItems: List<TaskItem> = emptyList(),
  val totalItemsCount: Int = 0,
  val completedItemsCount: Int = 0,
)

data class NotesUiState(
  val notes: List<NoteEntry> = emptyList(),
  val totalNotesCount: Int = 0,
  val searchText: String = "",
)

data class UniversalSearchUiState(
  val query: UniversalSearchQuery =
    UniversalSearchQuery(
      startDate = DateUtils.addDays(DateUtils.todayIso(), -30),
      endDate = DateUtils.todayIso(),
    ),
  val results: List<UniversalSearchResult> = emptyList(),
  val facets: UniversalSearchFacets = UniversalSearchFacets(),
  val employees: List<Employee> = emptyList(),
  val shifts: List<Shift> = emptyList(),
  val departments: List<Department> = emptyList(),
  val duties: List<DutyType> = emptyList(),
  val ictCategories: List<IctCategory> = emptyList(),
  val ictSubCategories: List<IctSubCategory> = emptyList(),
  val taskCategories: List<TaskCategory> = emptyList(),
)

data class RegisterUiState(
  val selectedDate: String = DateUtils.todayIso(),
  val subTab: RegisterSubTab = RegisterSubTab.ATTENDANCE,
  val attendanceDisplayMode: AttendanceDisplayMode = AttendanceDisplayMode.DAILY_LIST,
  val calendarYear: Int = DateUtils.yearMonthOf(DateUtils.todayIso()).first,
  val calendarMonth: Int = DateUtils.yearMonthOf(DateUtils.todayIso()).second,
  val calendarCells: List<CalendarDayCell> = emptyList(),
  val filterShiftId: String? = null,
  val rosterViewMode: RosterViewMode = RosterViewMode.BY_DATE,
  val summary: DailyAttendanceSummary =
    DailyAttendanceSummary(
      date = DateUtils.todayIso(),
      totalActiveEmployees = 0,
      scheduledSlotsCount = 0,
      presentCount = 0,
      absentCount = 0,
      lateCount = 0,
      leaveCount = 0,
      offCount = 0,
      unmarkedSlotsCount = 0,
    ),
  val attendanceRows: List<DailyAttendanceRow> = emptyList(),
  val calendarBreakdown: CalendarDateBreakdown =
    CalendarDateBreakdown(emptyList(), emptyList(), emptyList()),
  val upcomingCoverage: List<UpcomingCoverageDay> = emptyList(),
  val employeeSchedules: List<EmployeeScheduleGroup> = emptyList(),
  val activeEmployees: List<Employee> = emptyList(),
  val activeShifts: List<Shift> = emptyList(),
  val activeDepartments: List<Department> = emptyList(),
  val activeDuties: List<DutyType> = emptyList(),
  val employeeCarryForward: Map<String, EmployeeCarryForward> = emptyMap(),
  val historyQuery: HistoryFilterQuery =
    HistoryFilterQuery(
      startDate = DateUtils.addDays(DateUtils.todayIso(), -14),
      endDate = DateUtils.todayIso(),
    ),
  val historyEntries: List<HistorySearchEntry> = emptyList(),
  val allEmployees: List<Employee> = emptyList(),
  val allShifts: List<Shift> = emptyList(),
  val allDepartments: List<Department> = emptyList(),
  val allDuties: List<DutyType> = emptyList(),
  val distinctDesignations: List<String> = emptyList(),
)

data class SettingsUiState(
  val employees: List<Employee> = emptyList(),
  val shifts: List<Shift> = emptyList(),
  val departments: List<Department> = emptyList(),
  val dutyTypes: List<DutyType> = emptyList(),
  val offices: List<RegisteredOffice> = emptyList(),
  val ictCategories: List<IctCategory> = emptyList(),
  val ictSubCategories: List<IctSubCategory> = emptyList(),
  val ictEntriesCountByCategory: Map<String, Int> = emptyMap(),
  val assignedShiftsByEmployee: Map<String, List<Shift>> = emptyMap(),
  val appMeta: AppMeta = AppMeta(),
  val safetySnapshots: List<SafetySnapshotInfo> = emptyList(),
  val referenceRules: List<ReferenceRule> = emptyList(),
  val modules: List<ModuleDefinition> = ModuleRegistry.getAllModules(),
  val recordCountsSummary: String = "",
)

class BivashViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: BivashRepository by lazy {
    BivashRepository(BivashDatabase.getInstance(application))
  }

  private val themePreferences: ThemePreferences by lazy { ThemePreferences(application) }

  private val _themeMode = MutableStateFlow(themePreferences.getThemeMode())
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
  val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

  private val _selectedDate = MutableStateFlow(DateUtils.todayIso())
  private val _registerSubTab = MutableStateFlow(RegisterSubTab.ATTENDANCE)
  private val _attendanceDisplayMode = MutableStateFlow(AttendanceDisplayMode.DAILY_LIST)
  private val _calendarYearMonth = MutableStateFlow(DateUtils.yearMonthOf(DateUtils.todayIso()))
  private val _filterShiftId = MutableStateFlow<String?>(null)
  private val _rosterViewMode = MutableStateFlow(RosterViewMode.BY_DATE)

  private val _historyQuery =
    MutableStateFlow(
      HistoryFilterQuery(
        startDate = DateUtils.addDays(DateUtils.todayIso(), -14),
        endDate = DateUtils.todayIso(),
      )
    )

  private val _assignmentSheet = MutableStateFlow(AssignmentSheetState())
  val assignmentSheet: StateFlow<AssignmentSheetState> = _assignmentSheet.asStateFlow()

  private val _pendingConflict = MutableStateFlow<PendingConflictState?>(null)
  val pendingConflict: StateFlow<PendingConflictState?> = _pendingConflict.asStateFlow()

  private val _pendingBackupRestore = MutableStateFlow<PendingBackupRestoreState?>(null)
  val pendingBackupRestore: StateFlow<PendingBackupRestoreState?> =
    _pendingBackupRestore.asStateFlow()

  private val _pendingUpdatePreview = MutableStateFlow<OfflineUpdatePreview?>(null)
  val pendingUpdatePreview: StateFlow<OfflineUpdatePreview?> = _pendingUpdatePreview.asStateFlow()

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarEvents = _snackbarEvents.asSharedFlow()

  private val _isOfficeIndexOpen = MutableStateFlow(false)
  val isOfficeIndexOpen: StateFlow<Boolean> = _isOfficeIndexOpen.asStateFlow()

  private val _officeIndexCategory = MutableStateFlow<String?>(null)
  private val _officeIndexSearch = MutableStateFlow("")
  private val _officeCaseQuery = MutableStateFlow(OfficeCaseSearchQuery())

  private val _isIctDataOpen = MutableStateFlow(false)
  val isIctDataOpen: StateFlow<Boolean> = _isIctDataOpen.asStateFlow()

  private val _isUniversalSearchOpen = MutableStateFlow(false)
  val isUniversalSearchOpen: StateFlow<Boolean> = _isUniversalSearchOpen.asStateFlow()

  private val _universalSearchQuery =
    MutableStateFlow(
      UniversalSearchQuery(
        startDate = DateUtils.addDays(DateUtils.todayIso(), -30),
        endDate = DateUtils.todayIso(),
      )
    )

  private val _ictSelectedCategoryId = MutableStateFlow<String?>(null)
  private val _ictSelectedSubCategoryId = MutableStateFlow<String?>(null)
  private val _ictSearchText = MutableStateFlow("")

  private val _isTaskManagerOpen = MutableStateFlow(false)
  val isTaskManagerOpen: StateFlow<Boolean> = _isTaskManagerOpen.asStateFlow()

  private val _selectedTaskCategoryId = MutableStateFlow<String?>(null)

  private val _isNotesOpen = MutableStateFlow(false)
  val isNotesOpen: StateFlow<Boolean> = _isNotesOpen.asStateFlow()

  private val _notesSearchText = MutableStateFlow("")

  private val _requestedSettingsSection = MutableStateFlow<SettingsSection?>(null)
  val requestedSettingsSection: StateFlow<SettingsSection?> = _requestedSettingsSection.asStateFlow()

  private val _isSeeded = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      repository.ensureInitialized()
      _isSeeded.value = true
    }
  }

  private data class CoreSnapshot(
    val employees: List<Employee>,
    val shifts: List<Shift>,
    val departments: List<Department>,
    val duties: List<DutyType>,
    val roster: List<RosterAssignment>,
    val attendance: List<AttendanceRecord>,
  )

  private val orgStructureFlow =
    combine(repository.departments, repository.dutyTypes) { depts, duties -> depts to duties }

  private val coreDataFlow =
    combine(
      repository.employees,
      repository.shifts,
      orgStructureFlow,
      repository.rosterAssignments,
      repository.attendanceRecords,
    ) { employees, shifts, (departments, duties), roster, attendance ->
      CoreSnapshot(employees, shifts, departments, duties, roster, attendance)
    }

  private val officeModuleFlow =
    combine(
      repository.registeredOffices,
      repository.officeCaseRecords,
      repository.officeIndex,
    ) { offices, caseRecords, legacyEntries ->
      Triple(offices, caseRecords, legacyEntries)
    }

  val dashboardState: StateFlow<DashboardUiState> =
    combine(coreDataFlow, officeModuleFlow, _selectedDate, _isSeeded) {
      core,
      (offices, caseRecords, officeEntries),
      date,
      seeded ->
      val assignmentsForDate = core.roster.filter { it.date == date }
      val attendanceForDate = core.attendance.filter { it.date == date }
      val summary =
        AttendanceCalculator.calculateSummary(
          date = date,
          employees = core.employees,
          assignmentsForDate = assignmentsForDate,
          attendanceForDate = attendanceForDate,
        )
      val upcoming =
        RosterConflictDetector.buildUpcomingCoverage(
          startDate = DateUtils.todayIso(),
          dayCount = 4,
          employees = core.employees,
          shifts = core.shifts,
          duties = core.duties,
          assignments = core.roster,
          departments = core.departments,
        )
      val officeSummary =
        OfficeIndexSummary(
          totalEntries = officeEntries.size,
          distinctCategories = officeEntries.map { it.category }.distinct().size,
          integerTotal = officeEntries.sumOf { it.integerValue },
          recentEntries = officeEntries.take(3),
          totalCaseRecords = caseRecords.size,
          totalOffices = offices.size,
        )
      DashboardUiState(
        selectedDate = date,
        summary = summary,
        upcomingCoverage = upcoming,
        officeIndexSummary = officeSummary,
        activeModules = ModuleRegistry.getEnabledModules(),
        isLoading = !seeded,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = DashboardUiState(),
    )

  val officeIndexState: StateFlow<OfficeIndexUiState> =
    combine(
      officeModuleFlow,
      _officeCaseQuery,
      _officeIndexCategory,
      _officeIndexSearch,
    ) { (offices, caseRecords, entries), caseQuery, selectedCat, queryText ->
      val filteredCases = OfficeCaseEngine.filterRecords(caseRecords, caseQuery, offices)
      val knownIos = OfficeCaseEngine.collectKnownIoNames(caseRecords)
      val categories = entries.map { it.category }.distinct().sorted()
      val needle = queryText.trim().lowercase()
      val filtered =
        entries.filter { item ->
          (selectedCat == null || item.category == selectedCat) &&
            (needle.isBlank() ||
              item.textValue.lowercase().contains(needle) ||
              item.category.lowercase().contains(needle) ||
              item.subCategory.lowercase().contains(needle) ||
              item.notes.lowercase().contains(needle) ||
              item.integerValue.toString().contains(needle))
        }
      OfficeIndexUiState(
        offices = offices,
        activeOffices = offices.filter { it.isActive },
        caseRecords = filteredCases,
        allCaseRecordsCount = caseRecords.size,
        knownIoNames = knownIos,
        caseQuery = caseQuery,
        entries = filtered,
        allEntriesCount = entries.size,
        categories = categories,
        selectedCategory = selectedCat,
        searchText = queryText,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = OfficeIndexUiState(),
    )

  private data class IctSnapshot(
    val categories: List<IctCategory>,
    val subCategories: List<IctSubCategory>,
    val entries: List<IctEntry>,
  )

  private val ictDataFlow =
    combine(
      repository.ictCategories,
      repository.ictSubCategories,
      repository.ictEntries,
    ) { cats, subs, entries ->
      IctSnapshot(cats, subs, entries)
    }

  private val ictFilterFlow =
    combine(_ictSelectedCategoryId, _ictSelectedSubCategoryId, _ictSearchText) {
      catId,
      subId,
      search ->
      Triple(catId, subId, search)
    }

  val ictDataState: StateFlow<IctDataUiState> =
    combine(ictDataFlow, ictFilterFlow) { snap, (catId, subId, search) ->
      val subsByCat = snap.subCategories.groupBy { it.categoryId }
      val groups =
        snap.categories.map { cat ->
          IctCategoryGroup(category = cat, subCategories = subsByCat[cat.id].orEmpty())
        }
      val validCatId = catId?.takeIf { id -> snap.categories.any { it.id == id } }
      val validSubId =
        subId?.takeIf { id ->
          snap.subCategories.any {
            it.id == id && (validCatId == null || it.categoryId == validCatId)
          }
        }
      val catMap = snap.categories.associateBy { it.id }
      val subMap = snap.subCategories.associateBy { it.id }
      val needle = search.trim().lowercase()
      val filtered =
        snap.entries.filter { entry ->
          val catMatch = validCatId == null || entry.categoryId == validCatId
          val subMatch = validSubId == null || entry.subCategoryId == validSubId
          val catName = catMap[entry.categoryId]?.name ?: entry.categoryNameSnapshot
          val subName = entry.subCategoryId?.let { subMap[it]?.name } ?: entry.subCategoryNameSnapshot
          val textMatch =
            needle.isBlank() ||
              entry.valueText.lowercase().contains(needle) ||
              entry.note.lowercase().contains(needle) ||
              catName.lowercase().contains(needle) ||
              subName.lowercase().contains(needle)
          catMatch && subMatch && textMatch
        }
      IctDataUiState(
        categoryGroups = groups,
        categories = snap.categories,
        subCategories = snap.subCategories,
        entries = filtered,
        totalEntriesCount = snap.entries.size,
        selectedCategoryId = validCatId,
        selectedSubCategoryId = validSubId,
        searchText = search,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = IctDataUiState(),
    )

  private val taskAndNotesFlow =
    combine(repository.taskCategories, repository.taskItems, repository.notes) { cats, items, notes ->
      Triple(cats, items, notes)
    }

  val taskManagerState: StateFlow<TaskManagerUiState> =
    combine(repository.taskCategories, repository.taskItems, _selectedTaskCategoryId) {
      cats,
      items,
      selectedCatId ->
      val itemsByCat = items.groupBy { it.categoryId }
      val groups = cats.map { cat -> TaskCategoryGroup(cat, itemsByCat[cat.id].orEmpty()) }
      val validCatId = selectedCatId?.takeIf { id -> cats.any { it.id == id } }
      val selectedItems = validCatId?.let { itemsByCat[it].orEmpty() } ?: emptyList()
      TaskManagerUiState(
        categories = cats,
        categoryGroups = groups,
        selectedCategoryId = validCatId,
        selectedItems = selectedItems,
        totalItemsCount = items.size,
        completedItemsCount = items.count { it.isCompleted },
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = TaskManagerUiState(),
    )

  val notesState: StateFlow<NotesUiState> =
    combine(repository.notes, _notesSearchText) { notes, search ->
      val needle = search.trim().lowercase()
      val filtered =
        if (needle.isBlank()) notes
        else {
          notes.filter { note ->
            note.title.lowercase().contains(needle) || note.body.lowercase().contains(needle)
          }
        }
      NotesUiState(
        notes = filtered,
        totalNotesCount = notes.size,
        searchText = search,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = NotesUiState(),
    )

  val universalSearchState: StateFlow<UniversalSearchUiState> =
    combine(
      coreDataFlow,
      ictDataFlow,
      officeModuleFlow,
      taskAndNotesFlow,
      _universalSearchQuery,
    ) { core, ict, (offices, caseRecords, officeEntries), (taskCats, taskItems, notes), query ->
      val output =
        UniversalSearchEngine.search(
          query = query,
          employees = core.employees,
          shifts = core.shifts,
          departments = core.departments,
          duties = core.duties,
          rosterAssignments = core.roster,
          attendanceRecords = core.attendance,
          ictCategories = ict.categories,
          ictSubCategories = ict.subCategories,
          ictEntries = ict.entries,
          officeEntries = officeEntries,
          taskCategories = taskCats,
          taskItems = taskItems,
          notes = notes,
          registeredOffices = offices,
          officeCaseRecords = caseRecords,
        )
      UniversalSearchUiState(
        query = query,
        results = output.results,
        facets = output.facets,
        employees = core.employees,
        shifts = core.shifts,
        departments = core.departments,
        duties = core.duties,
        ictCategories = ict.categories,
        ictSubCategories = ict.subCategories,
        taskCategories = taskCats,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UniversalSearchUiState(),
    )

  private data class RegisterControlState(
    val date: String,
    val subTab: RegisterSubTab,
    val displayMode: AttendanceDisplayMode,
    val calendarYearMonth: Pair<Int, Int>,
    val shiftFilter: String?,
    val viewMode: RosterViewMode,
    val historyQuery: HistoryFilterQuery,
  )

  private val registerDateModeFlow =
    combine(
      _selectedDate,
      _registerSubTab,
      _attendanceDisplayMode,
      _calendarYearMonth,
    ) { date, subTab, displayMode, ym ->
      Quad(date, subTab, displayMode, ym)
    }

  private val registerControlsFlow =
    combine(
      registerDateModeFlow,
      _filterShiftId,
      _rosterViewMode,
      _historyQuery,
    ) { dMode, shiftFilter, viewMode, historyQuery ->
      RegisterControlState(
        date = dMode.first,
        subTab = dMode.second,
        displayMode = dMode.third,
        calendarYearMonth = dMode.fourth,
        shiftFilter = shiftFilter,
        viewMode = viewMode,
        historyQuery = historyQuery,
      )
    }

  val registerState: StateFlow<RegisterUiState> =
    combine(coreDataFlow, registerControlsFlow) { core, ctrl ->
      val assignmentsForDate = core.roster.filter { it.date == ctrl.date }
      val attendanceForDate = core.attendance.filter { it.date == ctrl.date }
      val summary =
        AttendanceCalculator.calculateSummary(
          date = ctrl.date,
          employees = core.employees,
          assignmentsForDate = assignmentsForDate,
          attendanceForDate = attendanceForDate,
        )
      val rows =
        AttendanceCalculator.buildDailyRows(
          date = ctrl.date,
          employees = core.employees,
          shifts = core.shifts,
          duties = core.duties,
          assignmentsForDate = assignmentsForDate,
          attendanceForDate = attendanceForDate,
          filterShiftId = ctrl.shiftFilter,
          departments = core.departments,
        )
      val allDateRows =
        if (ctrl.shiftFilter.isNullOrBlank()) rows
        else
          AttendanceCalculator.buildDailyRows(
            date = ctrl.date,
            employees = core.employees,
            shifts = core.shifts,
            duties = core.duties,
            assignmentsForDate = assignmentsForDate,
            attendanceForDate = attendanceForDate,
            filterShiftId = null,
            departments = core.departments,
          )
      val calendarBreakdown = AttendanceCalculator.buildCalendarDateBreakdown(allDateRows)
      val (calYear, calMonth) = ctrl.calendarYearMonth
      val calendarCells =
        AttendanceCalculator.buildCalendarMonthCells(
          year = calYear,
          month1Based = calMonth,
          employees = core.employees,
          shifts = core.shifts,
          allRoster = core.roster,
          allAttendance = core.attendance,
        )
      val upcoming =
        RosterConflictDetector.buildUpcomingCoverage(
          startDate = ctrl.date,
          dayCount = 7,
          employees = core.employees,
          shifts = core.shifts,
          duties = core.duties,
          assignments = core.roster,
          departments = core.departments,
        )
      val schedules =
        RosterConflictDetector.buildEmployeeSchedules(
          fromDate = DateUtils.todayIso(),
          employees = core.employees,
          shifts = core.shifts,
          duties = core.duties,
          assignments = core.roster,
          departments = core.departments,
        )
      val carryForward =
        RosterConflictDetector.resolveCarryForwardDefaults(
          employees = core.employees,
          departments = core.departments,
          duties = core.duties,
          assignments = core.roster,
          attendanceRecords = core.attendance,
          beforeDate = ctrl.date,
        )
      val historyEntries =
        HistorySearchEngine.search(
          query = ctrl.historyQuery,
          attendanceRecords = core.attendance,
          employees = core.employees,
          shifts = core.shifts,
          duties = core.duties,
          departments = core.departments,
        )
      val designations =
        (core.employees.map { it.designation } +
            core.attendance.mapNotNull { it.designationSnapshot.takeIf { d -> d.isNotBlank() } })
          .distinct()
          .sorted()

      RegisterUiState(
        selectedDate = ctrl.date,
        subTab = ctrl.subTab,
        attendanceDisplayMode = ctrl.displayMode,
        calendarYear = calYear,
        calendarMonth = calMonth,
        calendarCells = calendarCells,
        filterShiftId = ctrl.shiftFilter,
        rosterViewMode = ctrl.viewMode,
        summary = summary,
        attendanceRows = rows,
        calendarBreakdown = calendarBreakdown,
        upcomingCoverage = upcoming,
        employeeSchedules = schedules,
        activeEmployees = core.employees.filter { it.isActive },
        activeShifts = core.shifts.filter { it.isActive },
        activeDepartments = core.departments.filter { it.isActive },
        activeDuties = core.duties.filter { it.isActive },
        employeeCarryForward = carryForward,
        historyQuery = ctrl.historyQuery,
        historyEntries = historyEntries,
        allEmployees = core.employees,
        allShifts = core.shifts,
        allDepartments = core.departments,
        allDuties = core.duties,
        distinctDesignations = designations,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = RegisterUiState(),
    )

  val settingsState: StateFlow<SettingsUiState> =
    combine(
      coreDataFlow,
      ictDataFlow,
      repository.registeredOffices,
      repository.appMeta,
      repository.safetySnapshots,
    ) { core, ict, offices, meta, snapshots ->
      val safeMeta = meta ?: AppMeta()
      val rules = OfflineUpdateEngine.parseReferenceRules(safeMeta.referenceRulesJson)
      val entryCountsByCat = ict.entries.groupingBy { it.categoryId }.eachCount()
      val todayIso = DateUtils.todayIso()
      val shiftMap = core.shifts.associateBy { it.id }
      val shiftsByEmp =
        core.roster
          .asSequence()
          .filter { it.date >= todayIso }
          .groupBy { it.employeeId }
          .mapValues { (_, list) ->
            list
              .mapNotNull { shiftMap[it.shiftId] }
              .distinctBy { it.id }
              .sortedBy { it.startTime }
          }
      SettingsUiState(
        employees = core.employees,
        shifts = core.shifts,
        departments = core.departments,
        dutyTypes = core.duties,
        offices = offices,
        ictCategories = ict.categories,
        ictSubCategories = ict.subCategories,
        ictEntriesCountByCategory = entryCountsByCat,
        assignedShiftsByEmployee = shiftsByEmp,
        appMeta = safeMeta,
        safetySnapshots = snapshots,
        referenceRules = rules,
        modules = ModuleRegistry.getAllModules(),
        recordCountsSummary =
          "${core.employees.size} employees · ${offices.size} offices · ${core.departments.size} depts · ${core.shifts.size} shifts",
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = SettingsUiState(),
    )

  fun setThemeMode(mode: AppThemeMode) {
    themePreferences.setThemeMode(mode)
    _themeMode.value = mode
  }

  private fun closeAllDashboardOverlays() {
    _isOfficeIndexOpen.value = false
    _isIctDataOpen.value = false
    _isUniversalSearchOpen.value = false
    _isTaskManagerOpen.value = false
    _isNotesOpen.value = false
  }

  fun selectTab(tab: AppTab) {
    _currentTab.value = tab
    if (tab != AppTab.DASHBOARD) {
      closeAllDashboardOverlays()
    }
  }

  fun openUniversalSearch() {
    _currentTab.value = AppTab.DASHBOARD
    closeAllDashboardOverlays()
    _isUniversalSearchOpen.value = true
  }

  fun closeUniversalSearch() {
    _isUniversalSearchOpen.value = false
  }

  fun updateUniversalSearchQuery(transform: (UniversalSearchQuery) -> UniversalSearchQuery) {
    val updated = transform(_universalSearchQuery.value)
    val (normStart, normEnd) = DateUtils.normalizeDateRange(updated.startDate, updated.endDate)
    _universalSearchQuery.value = updated.copy(startDate = normStart, endDate = normEnd)
  }

  fun resetUniversalSearchFilters() {
    val today = DateUtils.todayIso()
    _universalSearchQuery.value =
      UniversalSearchQuery(
        startDate = DateUtils.addDays(today, -30),
        endDate = today,
      )
  }

  fun openFromUniversalSearch(result: UniversalSearchResult) {
    _isUniversalSearchOpen.value = false
    when (result.module) {
      UniversalSearchModule.ATTENDANCE -> openAttendanceForDate(result.date)
      UniversalSearchModule.ROSTER -> openRosterForDate(result.date)
      UniversalSearchModule.EMPLOYEES -> {
        _requestedSettingsSection.value = SettingsSection.EMPLOYEES
        _currentTab.value = AppTab.SETTINGS
      }
      UniversalSearchModule.SHIFTS_DUTIES -> {
        _requestedSettingsSection.value =
          if (result.id.startsWith("shift_")) SettingsSection.SHIFTS else SettingsSection.DUTIES
        _currentTab.value = AppTab.SETTINGS
      }
      UniversalSearchModule.ICT_DATA -> openIctData()
      UniversalSearchModule.OFFICE_INDEX -> openOfficeIndex()
      UniversalSearchModule.TASKS -> openTaskManager(result.targetCategoryId)
      UniversalSearchModule.NOTES -> openNotes()
      UniversalSearchModule.ALL -> Unit
    }
  }

  fun openOfficeIndex() {
    _currentTab.value = AppTab.DASHBOARD
    closeAllDashboardOverlays()
    _isOfficeIndexOpen.value = true
  }

  fun closeOfficeIndex() {
    _isOfficeIndexOpen.value = false
  }

  fun openIctData() {
    _currentTab.value = AppTab.DASHBOARD
    closeAllDashboardOverlays()
    _isIctDataOpen.value = true
  }

  fun closeIctData() {
    _isIctDataOpen.value = false
  }

  fun openTaskManager(categoryId: String? = null) {
    _currentTab.value = AppTab.DASHBOARD
    closeAllDashboardOverlays()
    _selectedTaskCategoryId.value = categoryId
    _isTaskManagerOpen.value = true
  }

  fun closeTaskManager() {
    _isTaskManagerOpen.value = false
    _selectedTaskCategoryId.value = null
  }

  fun selectTaskCategory(categoryId: String?) {
    _selectedTaskCategoryId.value = categoryId
  }

  fun saveTaskCategoriesBulk(rawInput: String, useCommaSeparator: Boolean) {
    viewModelScope.launch {
      val added = repository.saveTaskCategories(rawInput, useCommaSeparator)
      if (added > 0) {
        _snackbarEvents.emit("Added $added task ${if (added == 1) "category" else "categories"}.")
      }
    }
  }

  fun renameTaskCategory(category: TaskCategory, newName: String) {
    viewModelScope.launch {
      repository.renameTaskCategory(category, newName)
      _snackbarEvents.emit("Renamed category to '${newName.trim()}'.")
    }
  }

  fun deleteTaskCategory(category: TaskCategory) {
    viewModelScope.launch {
      repository.deleteTaskCategory(category.id)
      _snackbarEvents.emit("Deleted category '${category.name}'.")
    }
  }

  fun saveTaskItemsBulk(categoryId: String, rawInput: String, useCommaSeparator: Boolean) {
    viewModelScope.launch {
      val added = repository.saveTaskItems(categoryId, rawInput, useCommaSeparator)
      if (added > 0) {
        _snackbarEvents.emit("Added $added task ${if (added == 1) "item" else "items"}.")
      }
    }
  }

  fun toggleTaskItem(item: TaskItem) {
    viewModelScope.launch { repository.toggleTaskItemCompleted(item) }
  }

  fun renameTaskItem(item: TaskItem, newTitle: String) {
    viewModelScope.launch {
      repository.renameTaskItem(item, newTitle)
      _snackbarEvents.emit("Task item updated.")
    }
  }

  fun deleteTaskItem(item: TaskItem) {
    viewModelScope.launch {
      repository.deleteTaskItem(item.id)
      _snackbarEvents.emit("Removed task '${item.title}'.")
    }
  }

  fun openNotes() {
    _currentTab.value = AppTab.DASHBOARD
    closeAllDashboardOverlays()
    _isNotesOpen.value = true
  }

  fun closeNotes() {
    _isNotesOpen.value = false
  }

  fun setNotesSearch(query: String) {
    _notesSearchText.value = query
  }

  fun saveNote(existingId: String?, title: String, body: String) {
    viewModelScope.launch {
      repository.saveNote(existingId, title, body)
      _snackbarEvents.emit(if (existingId == null) "Note saved." else "Note updated.")
    }
  }

  fun deleteNote(note: NoteEntry) {
    viewModelScope.launch {
      repository.deleteNote(note.id)
      _snackbarEvents.emit("Deleted note '${note.title}'.")
    }
  }

  fun openIctSettings() {
    closeAllDashboardOverlays()
    _requestedSettingsSection.value = SettingsSection.ICT_DATA
    _currentTab.value = AppTab.SETTINGS
  }

  fun openOfficeSettings() {
    closeAllDashboardOverlays()
    _requestedSettingsSection.value = SettingsSection.OFFICES
    _currentTab.value = AppTab.SETTINGS
  }

  fun consumeRequestedSettingsSection() {
    _requestedSettingsSection.value = null
  }

  fun setIctCategoryFilter(categoryId: String?) {
    _ictSelectedCategoryId.value = categoryId
    _ictSelectedSubCategoryId.value = null
  }

  fun setIctSubCategoryFilter(subCategoryId: String?) {
    _ictSelectedSubCategoryId.value = subCategoryId
  }

  fun setIctSearch(query: String) {
    _ictSearchText.value = query
  }

  fun saveIctCategory(existingId: String?, name: String, description: String) {
    viewModelScope.launch {
      repository.saveIctCategory(existingId, name, description)
      _snackbarEvents.emit(if (existingId == null) "ICT category created." else "ICT category updated.")
    }
  }

  fun deleteIctCategory(category: IctCategory) {
    viewModelScope.launch {
      repository.deleteIctCategory(category.id)
      _snackbarEvents.emit("Deleted ICT category '${category.name}'.")
    }
  }

  fun saveIctSubCategory(
    existingId: String?,
    categoryId: String,
    name: String,
    unitLabel: String,
  ) {
    viewModelScope.launch {
      repository.saveIctSubCategory(existingId, categoryId, name, unitLabel)
      _snackbarEvents.emit(
        if (existingId == null) "ICT sub-category created." else "ICT sub-category updated."
      )
    }
  }

  fun deleteIctSubCategory(subCategory: IctSubCategory) {
    viewModelScope.launch {
      repository.deleteIctSubCategory(subCategory.id)
      _snackbarEvents.emit("Deleted sub-category '${subCategory.name}'.")
    }
  }

  fun saveIctEntry(
    existingId: String?,
    categoryId: String,
    subCategoryId: String?,
    valueText: String,
    note: String,
    recordedAt: Long = System.currentTimeMillis(),
  ) {
    viewModelScope.launch {
      repository.saveIctEntry(existingId, categoryId, subCategoryId, valueText, note, recordedAt)
      _snackbarEvents.emit(if (existingId == null) "ICT entry saved." else "ICT entry updated.")
    }
  }

  fun deleteIctEntry(entry: IctEntry) {
    viewModelScope.launch {
      repository.deleteIctEntry(entry.id)
      _snackbarEvents.emit("ICT entry deleted.")
    }
  }

  fun setOfficeIndexCategory(category: String?) {
    _officeIndexCategory.value = category
  }

  fun setOfficeIndexSearch(query: String) {
    _officeIndexSearch.value = query
  }

  fun saveOfficeIndexEntry(
    existingId: String?,
    integerValue: Int,
    textValue: String,
    category: String,
    subCategory: String,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.saveOfficeIndexEntry(
        existingId = existingId,
        integerValue = integerValue,
        textValue = textValue,
        category = category,
        subCategory = subCategory,
        notes = notes,
      )
      _snackbarEvents.emit(
        if (existingId == null) "Office Index entry added." else "Office Index entry updated."
      )
    }
  }

  fun deleteOfficeIndexEntry(entry: OfficeIndexEntry) {
    viewModelScope.launch {
      repository.deleteOfficeIndexEntry(entry.id)
      _snackbarEvents.emit("Removed '${entry.textValue}' from Office Index.")
    }
  }

  fun saveRegisteredOffice(
    existingId: String?,
    officeName: String,
    shortCode: String,
    isActive: Boolean,
  ) {
    viewModelScope.launch {
      repository.saveRegisteredOffice(existingId, officeName, shortCode, isActive)
      _snackbarEvents.emit(
        if (existingId == null) "Office registered." else "Office updated."
      )
    }
  }

  fun toggleRegisteredOfficeActive(office: RegisteredOffice) {
    viewModelScope.launch {
      repository.toggleRegisteredOfficeActive(office)
      _snackbarEvents.emit(
        "${office.officeName} ${if (office.isActive) "disabled" else "activated"}."
      )
    }
  }

  fun deleteRegisteredOffice(office: RegisteredOffice) {
    viewModelScope.launch {
      repository.deleteRegisteredOffice(office.id)
      _snackbarEvents.emit("Deleted office '${office.officeName}' (past case records preserved).")
    }
  }

  fun updateOfficeCaseQuery(transform: (OfficeCaseSearchQuery) -> OfficeCaseSearchQuery) {
    val updated = transform(_officeCaseQuery.value)
    val (normStart, normEnd) = DateUtils.normalizeDateRange(updated.startDate, updated.endDate)
    _officeCaseQuery.value = updated.copy(startDate = normStart, endDate = normEnd)
  }

  fun resetOfficeCaseFilters() {
    _officeCaseQuery.value = OfficeCaseSearchQuery()
  }

  fun saveNewOfficeCaseRecord(
    officeId: String,
    recordDate: String,
    firNumber: Int,
    ioName: String,
    actType: String,
    customActName: String,
    sections: List<CaseSectionAttachment>,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.saveNewOfficeCaseRecord(
        officeId = officeId,
        recordDate = recordDate,
        firNumber = firNumber,
        ioName = ioName,
        actType = actType,
        customActName = customActName,
        sections = sections,
        notes = notes,
      )
      _snackbarEvents.emit("FIR #$firNumber recorded in Office Index.")
    }
  }

  fun updateOfficeCaseRecord(
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
  ) {
    viewModelScope.launch {
      repository.updateOfficeCaseRecord(
        existing = existing,
        officeId = officeId,
        recordDate = recordDate,
        firNumber = firNumber,
        editedCurrentIoName = editedCurrentIoName,
        actType = actType,
        customActName = customActName,
        sections = sections,
        notes = notes,
        newAssignedIoName = newAssignedIoName,
        newIoAssignedDate = newIoAssignedDate,
        newIoRemarks = newIoRemarks,
      )
      val msg =
        if (!newAssignedIoName.isNullOrBlank()) {
          "FIR #$firNumber updated & assigned to I/O ${newAssignedIoName.trim()} (history kept)."
        } else {
          "FIR #$firNumber updated."
        }
      _snackbarEvents.emit(msg)
    }
  }

  fun deleteOfficeCaseRecord(record: OfficeCaseRecord) {
    viewModelScope.launch {
      repository.deleteOfficeCaseRecord(record.id)
      _snackbarEvents.emit("Deleted FIR #${record.firNumber} from Office Index.")
    }
  }

  fun openAttendanceForDate(dateIso: String) {
    _selectedDate.value = dateIso
    _calendarYearMonth.value = DateUtils.yearMonthOf(dateIso)
    _registerSubTab.value = RegisterSubTab.ATTENDANCE
    _currentTab.value = AppTab.REGISTER
  }

  fun openRosterForDate(dateIso: String) {
    _selectedDate.value = dateIso
    _registerSubTab.value = RegisterSubTab.ROSTER
    _rosterViewMode.value = RosterViewMode.BY_DATE
    _currentTab.value = AppTab.REGISTER
  }

  fun openHistorySearch() {
    _registerSubTab.value = RegisterSubTab.HISTORY
    _currentTab.value = AppTab.REGISTER
  }

  fun stepDate(deltaDays: Int) {
    val next = DateUtils.addDays(_selectedDate.value, deltaDays)
    _selectedDate.value = next
    _calendarYearMonth.value = DateUtils.yearMonthOf(next)
  }

  fun selectAttendanceDate(dateIso: String) {
    _selectedDate.value = dateIso
    _calendarYearMonth.value = DateUtils.yearMonthOf(dateIso)
  }

  fun jumpToToday() {
    val today = DateUtils.todayIso()
    _selectedDate.value = today
    _calendarYearMonth.value = DateUtils.yearMonthOf(today)
  }

  fun setAttendanceDisplayMode(mode: AttendanceDisplayMode) {
    _attendanceDisplayMode.value = mode
    if (mode == AttendanceDisplayMode.CALENDAR) {
      _calendarYearMonth.value = DateUtils.yearMonthOf(_selectedDate.value)
    }
  }

  fun stepCalendarMonth(deltaMonths: Int) {
    val (y, m) = _calendarYearMonth.value
    _calendarYearMonth.value = DateUtils.shiftYearMonth(y, m, deltaMonths)
  }

  fun setRegisterSubTab(subTab: RegisterSubTab) {
    _registerSubTab.value = subTab
  }

  fun setShiftFilter(shiftId: String?) {
    _filterShiftId.value = shiftId
  }

  fun setRosterViewMode(mode: RosterViewMode) {
    _rosterViewMode.value = mode
  }

  fun updateHistoryQuery(transform: (HistoryFilterQuery) -> HistoryFilterQuery) {
    _historyQuery.value = transform(_historyQuery.value)
  }

  fun resetHistoryFilters() {
    val today = DateUtils.todayIso()
    _historyQuery.value =
      HistoryFilterQuery(
        startDate = DateUtils.addDays(today, -14),
        endDate = today,
      )
  }

  fun markAttendance(
    row: DailyAttendanceRow,
    status: AttendanceStatus,
    remarks: String = row.existingRecord?.remarks ?: "",
  ) {
    val date = registerState.value.selectedDate
    viewModelScope.launch {
      repository.markAttendance(
        date = date,
        row = row,
        status = status,
        remarks = remarks,
      )
    }
  }

  fun markRemainingAsPresent(onlyScheduled: Boolean) {
    val state = registerState.value
    viewModelScope.launch {
      val count =
        repository.markRemainingAsPresent(
          date = state.selectedDate,
          rows = state.attendanceRows,
          onlyScheduled = onlyScheduled,
        )
      if (count > 0) {
        _snackbarEvents.emit("Marked $count slot(s) as Present for ${state.selectedDate}.")
      } else {
        _snackbarEvents.emit("No unmarked slots matching filter.")
      }
    }
  }

  fun openAssignmentSheet(
    focus: AssignSheetFocus = AssignSheetFocus.BOTH,
    startDate: String = _selectedDate.value,
    preselectedEmployeeIds: Set<String> = emptySet(),
  ) {
    _assignmentSheet.value =
      AssignmentSheetState(
        isVisible = true,
        focus = focus,
        startDate = startDate,
        endDate = startDate,
        preselectedEmployeeIds = preselectedEmployeeIds,
      )
  }

  fun dismissAssignmentSheet() {
    _assignmentSheet.value = _assignmentSheet.value.copy(isVisible = false)
  }

  fun submitRosterAssignment(request: RosterAssignmentRequest) {
    viewModelScope.launch {
      val outcome: RosterApplyOutcome = repository.evaluateAndApplyRoster(request)
      if (outcome.requiresConfirmation) {
        _pendingConflict.value =
          PendingConflictState(request = request, conflicts = outcome.conflictsFound)
      } else {
        _assignmentSheet.value = _assignmentSheet.value.copy(isVisible = false)
        _snackbarEvents.emit("Assigned ${outcome.appliedCount} roster slot(s).")
      }
    }
  }

  fun submitBatchRosterAssignment(request: BatchRosterAssignmentRequest) {
    viewModelScope.launch {
      val outcome: RosterApplyOutcome = repository.evaluateAndApplyBatchRoster(request)
      if (outcome.requiresConfirmation) {
        _pendingConflict.value =
          PendingConflictState(batchRequest = request, conflicts = outcome.conflictsFound)
      } else {
        _assignmentSheet.value = _assignmentSheet.value.copy(isVisible = false)
        _snackbarEvents.emit(
          "Assigned ${outcome.appliedCount} roster slot(s) across ${request.batches.size} shift(s)."
        )
      }
    }
  }

  fun confirmOverwriteConflicts() {
    val pending = _pendingConflict.value ?: return
    _pendingConflict.value = null
    viewModelScope.launch {
      val outcome =
        when {
          pending.batchRequest != null ->
            repository.evaluateAndApplyBatchRoster(
              pending.batchRequest.copy(overwriteConflicts = true)
            )
          pending.request != null ->
            repository.evaluateAndApplyRoster(pending.request.copy(overwriteConflicts = true))
          else -> null
        }
      _assignmentSheet.value = _assignmentSheet.value.copy(isVisible = false)
      val count = outcome?.appliedCount ?: 0
      _snackbarEvents.emit("Updated $count roster slot(s) (conflicts overwritten).")
    }
  }

  fun dismissConflictDialog() {
    _pendingConflict.value = null
  }

  fun removeRosterAssignment(assignmentId: String) {
    viewModelScope.launch {
      repository.removeRosterAssignment(assignmentId)
      _snackbarEvents.emit("Roster assignment removed.")
    }
  }

  // Settings CRUD (Add, Edit/Rename, Toggle Active, Delete preserving history)
  fun saveEmployee(
    existingId: String?,
    code: String,
    name: String,
    designation: String,
    isActive: Boolean,
    defaultDepartmentId: String? = null,
  ) {
    viewModelScope.launch {
      repository.saveEmployee(
        existingId = existingId,
        code = code,
        name = name,
        designation = designation,
        isActive = isActive,
        defaultDepartmentId = defaultDepartmentId,
      )
      _snackbarEvents.emit(if (existingId == null) "Employee registered." else "Employee updated.")
    }
  }

  fun toggleEmployeeActive(employee: Employee) {
    viewModelScope.launch {
      repository.toggleEmployeeActive(employee)
      val label = if (employee.isActive) "deactivated (history preserved)" else "reactivated"
      _snackbarEvents.emit("${employee.name} $label.")
    }
  }

  fun deleteEmployee(employee: Employee) {
    viewModelScope.launch {
      repository.deleteEmployeePreservingHistory(employee.id)
      _snackbarEvents.emit("${employee.name} removed (attendance history preserved).")
    }
  }

  fun saveShift(
    existingId: String?,
    name: String,
    startTime: String,
    endTime: String,
    isNightShift: Boolean,
    isActive: Boolean,
  ) {
    viewModelScope.launch {
      repository.saveShift(existingId, name, startTime, endTime, isNightShift, isActive)
      _snackbarEvents.emit(if (existingId == null) "Shift created." else "Shift updated.")
    }
  }

  fun toggleShiftActive(shift: Shift) {
    viewModelScope.launch {
      repository.toggleShiftActive(shift)
      _snackbarEvents.emit("${shift.name} ${if (shift.isActive) "disabled" else "activated"}.")
    }
  }

  fun deleteShift(shift: Shift) {
    viewModelScope.launch {
      repository.deleteShiftPreservingHistory(shift.id)
      _snackbarEvents.emit("${shift.name} deleted (past logs preserved).")
    }
  }

  fun saveDepartment(
    existingId: String?,
    name: String,
    description: String,
    isActive: Boolean,
  ) {
    viewModelScope.launch {
      repository.saveDepartment(existingId, name, description, isActive)
      _snackbarEvents.emit(if (existingId == null) "Department created." else "Department updated.")
    }
  }

  fun toggleDepartmentActive(department: Department) {
    viewModelScope.launch {
      repository.toggleDepartmentActive(department)
      _snackbarEvents.emit(
        "${department.name} ${if (department.isActive) "disabled" else "activated"}."
      )
    }
  }

  fun deleteDepartment(department: Department) {
    viewModelScope.launch {
      repository.deleteDepartmentPreservingHistory(department.id)
      _snackbarEvents.emit("${department.name} deleted (past logs preserved).")
    }
  }

  fun saveDutyType(
    existingId: String?,
    title: String,
    description: String,
    isActive: Boolean,
    departmentId: String = "",
    defaultShiftId: String? = null,
  ) {
    viewModelScope.launch {
      repository.saveDutyType(existingId, title, description, isActive, departmentId, defaultShiftId)
      _snackbarEvents.emit(
        if (existingId == null) "Duty template created." else "Duty template updated."
      )
    }
  }

  fun toggleDutyActive(duty: DutyType) {
    viewModelScope.launch {
      repository.toggleDutyActive(duty)
      _snackbarEvents.emit("${duty.title} ${if (duty.isActive) "disabled" else "activated"}.")
    }
  }

  fun deleteDutyType(duty: DutyType) {
    viewModelScope.launch {
      repository.deleteDutyTypePreservingHistory(duty.id)
      _snackbarEvents.emit("${duty.title} deleted (past logs preserved).")
    }
  }

  // Backup & Restore
  suspend fun buildExportBackupJson(): String {
    val (_, json) = repository.exportBackupJson(recordTimestamp = true)
    _snackbarEvents.emit("Backup JSON generated cleanly.")
    return json
  }

  fun inspectBackupJson(rawJson: String) {
    val validation = repository.validateBackupJson(rawJson)
    _pendingBackupRestore.value = PendingBackupRestoreState(rawJson = rawJson, validation = validation)
  }

  fun dismissBackupRestoreDialog() {
    _pendingBackupRestore.value = null
  }

  fun confirmRestoreFromBackup() {
    val pending = _pendingBackupRestore.value ?: return
    _pendingBackupRestore.value = null
    viewModelScope.launch {
      val result = repository.restoreFromBackupJson(pending.rawJson)
      _snackbarEvents.emit(result.message)
    }
  }

  fun createManualSafetySnapshot() {
    viewModelScope.launch {
      val snap = repository.saveManualSafetySnapshot("Manual safety checkpoint")
      _snackbarEvents.emit("Safety snapshot saved (${snap.summary}).")
    }
  }

  fun restoreSafetySnapshot(snapshotId: String) {
    viewModelScope.launch {
      val result = repository.restoreFromSafetySnapshot(snapshotId)
      _snackbarEvents.emit(result.message)
    }
  }

  // Offline Update Manager
  fun inspectOfflineUpdateJson(rawJson: String) {
    viewModelScope.launch {
      val preview = repository.previewOfflineUpdate(rawJson)
      _pendingUpdatePreview.value = preview
    }
  }

  fun loadBuiltInSampleUpdatePack() {
    val sampleJson = OfflineUpdateEngine.createSampleUpdatePackJson()
    inspectOfflineUpdateJson(sampleJson)
  }

  fun dismissUpdatePreview() {
    _pendingUpdatePreview.value = null
  }

  fun applyOfflineUpdate(pack: OfflineUpdatePack) {
    _pendingUpdatePreview.value = null
    viewModelScope.launch {
      val result = repository.applyOfflineUpdate(pack)
      _snackbarEvents.emit(result.message)
    }
  }

  fun emitMessage(msg: String) {
    viewModelScope.launch { _snackbarEvents.emit(msg) }
  }

  private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
