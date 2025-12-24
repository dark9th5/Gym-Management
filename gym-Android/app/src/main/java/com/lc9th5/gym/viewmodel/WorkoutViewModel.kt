package com.lc9th5.gym.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.lc9th5.gym.data.model.*
import com.lc9th5.gym.data.remote.PagedResponse
import com.lc9th5.gym.data.repository.WorkoutRepository
import com.lc9th5.gym.service.WorkoutReminderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadStreak()
        loadTodayPlan()
        loadTodaySessions()
        loadOverviewStatistics()
    }

    // Helper to refresh all related data after session changes
    private fun refreshAfterSessionChange() {
        loadStreak()
        loadTodaySessions()
        loadTodayPlan()
        loadOverviewStatistics()
        // Refresh current month calendar
        val now = java.time.YearMonth.now()
        loadMonthlyCalendar(now.year, now.monthValue)
    }

    // ==================== Session Methods ====================

    fun startWorkoutSession(name: String, notes: String? = null, startedAt: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val request = CreateSessionRequest(
                name = name,
                notes = notes,
                startedAt = startedAt ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            
            repository.createSession(request)
                .onSuccess { session ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentSession = session,
                            currentSessionDetail = WorkoutSessionDetail(
                                id = session.id,
                                name = session.name,
                                notes = session.notes,
                                startedAt = session.startedAt,
                                endedAt = session.endedAt,
                                durationMinutes = session.durationMinutes,
                                caloriesBurned = session.caloriesBurned,
                                exercises = emptyList()
                            )
                        )
                    }
                    // Load today plan again to ensure we have latest exercises
                    loadTodayPlan()
                    refreshPendingExercises()
                    
                    // Reload sessions to see the new one in history
                    loadSessions()
                    loadTodaySessions()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ... (keep startWorkoutSessionWithExercises as is or update similarly if needed)

    // Helper to refresh pending exercises list based on current session
    private fun refreshPendingExercises() {
        val currentExercises = _uiState.value.currentSessionDetail?.exercises ?: emptyList()
        val planExercises = _uiState.value.todayPlan?.exercises ?: emptyList()
        
        // Filter out exercises that are already in the session
        val pending = planExercises.filter { planEx ->
            currentExercises.none { sessionEx -> 
                sessionEx.exerciseName.equals(planEx.exerciseName, ignoreCase = true) 
            }
        }
        
        _uiState.update { it.copy(pendingExercises = pending) }
    }

    fun completePendingExercise(exercise: WorkoutPlanExercise) {
        viewModelScope.launch {
            val currentSession = _uiState.value.currentSession
            val sessionId = if (currentSession == null) {
                // Auto-create session
                _uiState.update { it.copy(isLoading = true) }
                val sessionName = "Buổi tập ${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}"
                val request = CreateSessionRequest(
                    name = sessionName,
                    startedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
                
                val result = repository.createSession(request)
                if (result.isSuccess) {
                    val session = result.getOrThrow()
                     _uiState.update { 
                        it.copy(
                            currentSession = session,
                            currentSessionDetail = WorkoutSessionDetail(
                                id = session.id,
                                name = session.name,
                                notes = session.notes,
                                startedAt = session.startedAt,
                                endedAt = session.endedAt,
                                durationMinutes = session.durationMinutes,
                                caloriesBurned = session.caloriesBurned,
                                exercises = emptyList()
                            )
                        )
                    }
                    loadTodaySessions()
                    session.id
                } else {
                     _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                     return@launch
                }
            } else {
                currentSession.id
            }

            // Add exercise to session
            val request = AddExerciseRequest(exercise.exerciseName, exercise.lessonId, null)
            
            repository.addExerciseToSession(sessionId, request)
                .onSuccess { addedExercise ->
                    // Add sets from encoded notes if available; fallback to targetSets/targetReps
                    val decodedSets = decodeSetsNote(exercise.notes)
                    if (decodedSets.isNotEmpty()) {
                        decodedSets.forEach { (reps, weight) ->
                            repository.addSetToExercise(
                                addedExercise.id,
                                AddSetRequest(
                                    reps = reps,
                                    weightKg = weight,
                                    isWarmup = false
                                )
                            )
                        }
                    } else if (exercise.targetSets > 0) {
                        for (i in 1..exercise.targetSets) {
                            repository.addSetToExercise(
                                addedExercise.id,
                                AddSetRequest(
                                    reps = exercise.targetReps.toIntOrNull() ?: 0,
                                    weightKg = exercise.targetWeightKg,
                                    isWarmup = false
                                )
                            )
                        }
                    }
                    
                    // Refresh session detail which will also update pending list
                    loadCurrentSessionDetail()
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun deletePendingExercise(exerciseId: Long) {
        // Remove from pending list locally (pending exercises are not saved to backend until completed)
        _uiState.update { state ->
            state.copy(
                pendingExercises = state.pendingExercises.filter { it.id != exerciseId }
            )
        }
    }

    fun updatePendingExercise(updatedExercise: WorkoutPlanExercise) {
        // Update exercise in pending list locally
        _uiState.update { state ->
            state.copy(
                pendingExercises = state.pendingExercises.map { exercise ->
                    if (exercise.id == updatedExercise.id) updatedExercise else exercise
                }
            )
        }
    }

    fun removeCompletedExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.deleteSessionExercise(exerciseId)
                .onSuccess {
                    loadCurrentSessionDetail()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun updateSessionExercise(exerciseId: Long, newSetData: List<SetRowData>) {
        viewModelScope.launch {
            val sessionDetail = _uiState.value.currentSessionDetail ?: return@launch
            val exercise = sessionDetail.exercises.find { it.id == exerciseId } ?: return@launch
            
            val existingSets = exercise.sets
            
            // 1. Update existing sets
            val commonSize = minOf(existingSets.size, newSetData.size)
            for (i in 0 until commonSize) {
                val set = existingSets[i]
                val data = newSetData[i]
                val reps = data.reps.toIntOrNull() ?: 0
                val weight = data.weight.toDoubleOrNull()
                repository.updateSet(set.id, UpdateSetRequest(reps = reps, weightKg = weight))
            }
            
            // 2. Add new sets
            if (newSetData.size > existingSets.size) {
                 for (i in existingSets.size until newSetData.size) {
                     val data = newSetData[i]
                     val reps = data.reps.toIntOrNull() ?: 0
                     val weight = data.weight.toDoubleOrNull()
                     repository.addSetToExercise(exerciseId, 
                        AddSetRequest(reps = reps, weightKg = weight))
                 }
            }
            
            // 3. Delete excess sets
            if (existingSets.size > newSetData.size) {
                for (i in newSetData.size until existingSets.size) {
                    repository.deleteSet(existingSets[i].id)
                }
            }
            
            loadCurrentSessionDetail()
        }
    }

    fun undoCompletedExercise(exercise: WorkoutExerciseDetail) {
        viewModelScope.launch {
            // Mark all sets as not completed
            val updates = exercise.sets.map { set ->
                repository.updateSet(set.id, UpdateSetRequest(isCompleted = false))
            }
            // If any failed, surface error but still refresh
            updates.firstOrNull { it.isFailure }?.exceptionOrNull()?.let { err ->
                _uiState.update { it.copy(error = err.message) }
            }
            loadCurrentSessionDetail()
        }
    }

    fun markExerciseSetsCompleted(exercise: WorkoutExerciseDetail) {
        viewModelScope.launch {
            val updates = exercise.sets.map { set ->
                repository.updateSet(set.id, UpdateSetRequest(isCompleted = true))
            }
            // If any failed, surface error but still refresh
            updates.firstOrNull { it.isFailure }?.exceptionOrNull()?.let { err ->
                _uiState.update { it.copy(error = err.message) }
            }
            loadCurrentSessionDetail()
        }
    }

    private fun loadCurrentSessionDetail() {
        val sessionId = _uiState.value.currentSession?.id ?: return
        
        viewModelScope.launch {
            repository.getSessionDetail(sessionId)
                .onSuccess { detail ->
                    _uiState.update { it.copy(currentSessionDetail = detail) }
                    refreshPendingExercises()
                }
        }
    }

    fun startWorkoutSessionWithExercises(
        exercises: List<SelectedExercise>,
        startedAt: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Create session with first exercise name or generic name
            val sessionName = if (exercises.size == 1) {
                exercises.first().name
            } else {
                "Buổi tập ${exercises.size} bài"
            }
            
            val request = CreateSessionRequest(
                name = sessionName,
                notes = null,
                startedAt = startedAt ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            
            repository.createSession(request)
                .onSuccess { session ->
                    _uiState.update { 
                        it.copy(
                            currentSession = session,
                            currentSessionDetail = WorkoutSessionDetail(
                                id = session.id,
                                name = session.name,
                                notes = session.notes,
                                startedAt = session.startedAt,
                                endedAt = session.endedAt,
                                durationMinutes = session.durationMinutes,
                                caloriesBurned = session.caloriesBurned,
                                exercises = emptyList()
                            )
                        )
                    }
                    
                    // Add each exercise with its sets
                    for (exercise in exercises) {
                        repository.addExerciseToSession(
                            session.id,
                            AddExerciseRequest(exercise.name, exercise.templateId, null)
                        ).onSuccess { addedExercise ->
                            // Add sets for this exercise
                            val weight = exercise.weight.toDoubleOrNull()
                            for (setNum in 1..exercise.sets) {
                                repository.addSetToExercise(
                                    addedExercise.id,
                                    AddSetRequest(
                                        reps = exercise.reps,
                                        weightKg = weight,
                                        durationSeconds = null,
                                        isWarmup = false,
                                        isCompleted = false,
                                        notes = null
                                    )
                                )
                            }
                        }
                    }
                    
                    // Refresh to show all exercises
                    loadCurrentSessionDetail()
                    loadSessions()
                    loadTodaySessions()
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun endWorkoutSession() {
        val sessionId = _uiState.value.currentSession?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.endSession(sessionId)
                .onSuccess { session ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentSession = null,
                            currentSessionDetail = null
                        )
                    }
                    // Refresh data
                    loadStreak()
                    loadTodaySessions()
                    loadOverviewStatistics()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    // Save workout session - ends the current session and saves to statistics
    fun saveWorkoutSession() {
        val sessionId = _uiState.value.currentSession?.id
        if (sessionId == null) {
            _uiState.update { it.copy(error = "Không có buổi tập để lưu") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.endSession(sessionId)
                .onSuccess { session ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentSession = null,
                            currentSessionDetail = null,
                            pendingExercises = emptyList()
                        )
                    }
                    // Refresh all related data after save
                    refreshAfterSessionChange()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    // Reset workout diary - clears pending and completed exercises
    fun resetWorkoutDiary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Delete current session if exists
            val sessionId = _uiState.value.currentSession?.id
            if (sessionId != null) {
                repository.deleteSession(sessionId)
            }
            
            // Clear the UI state
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    currentSession = null,
                    currentSessionDetail = null,
                    pendingExercises = emptyList()
                )
            }
            
            // Reload pending exercises and sessions
            loadTodayPlan()
            loadTodaySessions()
        }
    }

    fun loadSessions(page: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true) }
            
            repository.getSessions(page)
                .onSuccess { pagedResponse ->
                    _uiState.update { 
                        it.copy(
                            isLoadingSessions = false,
                            sessions = if (page == 0) pagedResponse.content else it.sessions + pagedResponse.content,
                            hasMoreSessions = !pagedResponse.last,
                            currentPage = pagedResponse.number
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingSessions = false, error = error.message) }
                }
        }
    }

    fun loadTodaySessions() {
        viewModelScope.launch {
            repository.getTodaySessions()
                .onSuccess { sessions ->
                    _uiState.update { it.copy(todaySessions = sessions) }
                    
                    // Auto-set current session if there's an active (not ended) session today
                    val activeSession = sessions.find { it.endedAt == null }
                    if (activeSession != null && _uiState.value.currentSession == null) {
                        _uiState.update { it.copy(currentSession = activeSession) }
                        // Load session detail to get exercises
                        loadCurrentSessionDetail()
                    }
                }
        }
    }

    fun loadSessionDetail(sessionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getSessionDetail(sessionId)
                .onSuccess { detail ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            selectedSessionDetail = detail
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun addExerciseToSession(exerciseName: String, lessonId: Long? = null, sets: Int = 3, reps: String = "8", weight: Double? = null, notes: String? = null) {
        viewModelScope.launch {
            val currentSession = _uiState.value.currentSession
            val sessionId = if (currentSession == null) {
                // Auto-create session if not exists
                _uiState.update { it.copy(isLoading = true) }
                val sessionName = "Buổi tập ${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}"
                val request = CreateSessionRequest(
                    name = sessionName,
                    startedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
                
                val result = repository.createSession(request)
                if (result.isSuccess) {
                    val session = result.getOrThrow()
                    _uiState.update { 
                        it.copy(
                            currentSession = session,
                            currentSessionDetail = WorkoutSessionDetail(
                                id = session.id,
                                name = session.name,
                                notes = session.notes,
                                startedAt = session.startedAt,
                                endedAt = session.endedAt,
                                durationMinutes = session.durationMinutes,
                                caloriesBurned = session.caloriesBurned,
                                exercises = emptyList()
                            )
                        )
                    }
                    loadTodaySessions()
                    session.id
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                    return@launch
                }
            } else {
                currentSession.id
            }
            
            val request = AddExerciseRequest(exerciseName, lessonId, notes)
            
            repository.addExerciseToSession(sessionId, request)
                .onSuccess { addedExercise ->
                    // Add initial sets according to parameters
                    val repsInt = reps.toIntOrNull() ?: 8
                    for (i in 1..sets) {
                        repository.addSetToExercise(
                            addedExercise.id,
                            AddSetRequest(
                                reps = repsInt,
                                weightKg = weight,
                                isWarmup = false
                            )
                        )
                    }
                    
                    // Refresh session detail
                    loadCurrentSessionDetail()
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    // New function for AddExerciseDialog with individual set rows
    fun addExerciseToSessionWithSets(
        exerciseName: String, 
        lessonId: Long? = null, 
        setRows: List<SetRowData>, 
        notes: String? = null,
        scheduledTime: String? = null
    ) {
        viewModelScope.launch {
            val currentSession = _uiState.value.currentSession
            val sessionId = if (currentSession == null) {
                // Auto-create session if not exists
                _uiState.update { it.copy(isLoading = true) }
                val sessionName = "Buổi tập ${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}"
                val request = CreateSessionRequest(
                    name = sessionName,
                    startedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
                
                val result = repository.createSession(request)
                if (result.isSuccess) {
                    val session = result.getOrThrow()
                    _uiState.update { 
                        it.copy(
                            currentSession = session,
                            currentSessionDetail = WorkoutSessionDetail(
                                id = session.id,
                                name = session.name,
                                notes = session.notes,
                                startedAt = session.startedAt,
                                endedAt = session.endedAt,
                                durationMinutes = session.durationMinutes,
                                caloriesBurned = session.caloriesBurned,
                                exercises = emptyList()
                            )
                        )
                    }
                    loadTodaySessions()
                    session.id
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                    return@launch
                }
            } else {
                currentSession.id
            }
            
            val request = AddExerciseRequest(exerciseName, lessonId, notes)
            
            repository.addExerciseToSession(sessionId, request)
                .onSuccess { addedExercise ->
                    // Add individual sets with their specific reps and weights
                    for (setData in setRows) {
                        val repsInt = setData.reps.toIntOrNull() ?: 0
                        val weightKg = setData.weight.toDoubleOrNull()
                        if (repsInt > 0) {
                            repository.addSetToExercise(
                                addedExercise.id,
                                AddSetRequest(
                                    reps = repsInt,
                                    weightKg = weightKg,
                                    isWarmup = false,
                                    isCompleted = false
                                )
                            )
                        }
                    }
                    
                    // Refresh session detail
                    loadCurrentSessionDetail()
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun addSetToExercise(exerciseId: Long, reps: Int, weightKg: Double? = null, isWarmup: Boolean = false) {
        viewModelScope.launch {
            val request = AddSetRequest(reps, weightKg, null, isWarmup, true, null)
            
            repository.addSetToExercise(exerciseId, request)
                .onSuccess { set ->
                    // Refresh session detail
                    loadCurrentSessionDetail()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }



    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
                .onSuccess {
                    _uiState.update { 
                        it.copy(sessions = it.sessions.filter { s -> s.id != sessionId })
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    // ==================== Plan Methods ====================

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlans = true) }
            
            repository.getPlans()
                .onSuccess { plans ->
                    _uiState.update { it.copy(isLoadingPlans = false, plans = plans) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingPlans = false, error = error.message) }
                }
        }
    }

    fun loadTodayPlan() {
        viewModelScope.launch {
            repository.getTodayPlan()
                .onSuccess { day ->
                    _uiState.update { it.copy(todayPlan = day) }
                    // Auto-sync removed - users now apply plan manually via button
                }
        }
    }

    fun loadActivePlan() {
        viewModelScope.launch {
            repository.getActivePlan()
                .onSuccess { plan ->
                    _uiState.update { it.copy(activePlan = plan) }
                }
        }
    }

    fun loadPlanDetail(planId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getPlanDetail(planId)
                .onSuccess { plan ->
                    _uiState.update { it.copy(isLoading = false, selectedPlan = plan) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun createPlan(name: String, description: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val request = CreatePlanRequest(name, description)
            
            repository.createPlan(request)
                .onSuccess { plan ->
                    // Deactivate the newly created plan so it doesn't start as active
                    if (plan.isActive) {
                        repository.setActivePlan(null)
                            .onSuccess {
                                loadPlans()
                                loadActivePlan()
                                _uiState.update { it.copy(isLoading = false) }
                            }
                            .onFailure { error ->
                                _uiState.update { it.copy(isLoading = false, error = error.message) }
                            }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                plans = it.plans + plan
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun addDayToPlan(planId: Long, dayOfWeek: DayOfWeek, name: String, isRestDay: Boolean = false) {
        viewModelScope.launch {
            val request = AddPlanDayRequest(dayOfWeek, name, isRestDay)
            
            repository.addDayToPlan(planId, request)
                .onSuccess { day ->
                    // Refresh plan detail
                    loadPlanDetail(planId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun addExerciseToPlanDay(
        dayId: Long,
        exerciseName: String,
        setRows: List<com.lc9th5.gym.ui.view.PlanSetRowData>,
        scheduledTime: String?,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            // Encode per-set details into notes so backend preserves them
            val notes = encodeSetsNote(setRows, scheduledTime)
            val targetSets = setRows.size
            val targetReps = setRows.firstOrNull()?.reps ?: "8"
            val targetWeightKg = setRows.firstOrNull()?.weight?.toDoubleOrNull()

            val request = AddPlanExerciseRequest(
                exerciseName = exerciseName,
                lessonId = null,
                targetSets = targetSets,
                targetReps = targetReps,
                targetWeightKg = targetWeightKg,
                notes = notes
            )
            
            repository.addExerciseToPlanDay(dayId, request)
                .onSuccess { exercise ->
                    // Refresh current plan
                    _uiState.value.selectedPlan?.let { plan ->
                        loadPlanDetail(plan.id)
                    }
                    // Refresh today plan and active plan to sync with Diary
                    loadTodayPlan()
                    loadActivePlan()

                    // Schedule reminder LOCAL ONLY if this plan is ACTIVE
                    val activePlanId = _uiState.value.activePlan?.id
                    val currentPlanId = _uiState.value.selectedPlan?.id
                    
                    if (!scheduledTime.isNullOrBlank() && activePlanId != null && activePlanId == currentPlanId) {
                        val dayOfWeek = findDayOfWeekById(dayId)
                        if (dayOfWeek != null) {
                            scheduleExerciseReminder(
                                context = context,
                                exerciseId = exercise.id,
                                exerciseName = exerciseName,
                                time = scheduledTime,
                                dayOfWeek = dayOfWeek
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun deletePlanExercise(planId: Long, exerciseId: Long) {
        viewModelScope.launch {
            repository.deletePlanExercise(exerciseId)
                .onSuccess {
                    loadPlanDetail(planId)
                    // Refresh today plan and active plan to sync with Diary
                    loadTodayPlan()
                    loadActivePlan()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun updatePlanExercise(
        planId: Long,
        exerciseId: Long,
        exerciseName: String,
        setRows: List<com.lc9th5.gym.ui.view.PlanSetRowData>,
        scheduledTime: String?,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            // Encode per-set details into notes so backend preserves them
            val notes = encodeSetsNote(setRows, scheduledTime)
            val targetSets = setRows.size
            val targetReps = setRows.firstOrNull()?.reps ?: "8"
            val targetWeightKg = setRows.firstOrNull()?.weight?.toDoubleOrNull()

            val request = UpdatePlanExerciseRequest(
                exerciseName = exerciseName,
                lessonId = null,
                targetSets = targetSets,
                targetReps = targetReps,
                targetWeightKg = targetWeightKg,
                notes = notes
            )
            
            repository.updatePlanExercise(exerciseId, request)
                .onSuccess { exercise ->
                    // Refresh current plan
                    loadPlanDetail(planId)
                    // Refresh today plan and active plan to sync with Diary
                    loadTodayPlan()
                    loadActivePlan()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun setActivePlan(planId: Long) {
        viewModelScope.launch {
            val currentActiveId = _uiState.value.activePlan?.id
            
            // If clicking on already active plan, deactivate it
            val targetPlanId = if (currentActiveId == planId) null else planId
            
            repository.setActivePlan(targetPlanId)
                .onSuccess { 
                    loadPlans()
                    loadTodayPlan()
                    loadActivePlan()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            repository.deletePlan(planId)
                .onSuccess {
                    _uiState.update { 
                        it.copy(plans = it.plans.filter { p -> p.id != planId })
                    }
                    // Refresh active plan and today plan in case deleted plan was active
                    loadActivePlan()
                    loadTodayPlan()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    // ==================== Streak Methods ====================

    fun loadStreak() {
        viewModelScope.launch {
            repository.getStreak()
                .onSuccess { streak ->
                    _uiState.update { it.copy(streak = streak) }
                }
        }
    }

    // ==================== Statistics Methods ====================

    fun loadOverviewStatistics(days: Int = 30) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStatistics = true, statisticsError = null) }
            repository.getOverviewStatistics(days)
                .onSuccess { stats ->
                    _uiState.update { it.copy(isLoadingStatistics = false, overviewStatistics = stats) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingStatistics = false, statisticsError = error.message) }
                }
        }
    }

    fun loadWorkoutHistory(days: Int = 30) {
        viewModelScope.launch {
            repository.getWorkoutHistory(days)
                .onSuccess { history ->
                    _uiState.update { it.copy(workoutHistory = history) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun loadWeeklyProgress(weeks: Int = 4) {
        viewModelScope.launch {
            repository.getWeeklyProgress(weeks)
                .onSuccess { progress ->
                    _uiState.update { it.copy(weeklyProgress = progress) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun loadMonthlyCalendar(year: Int, month: Int) {
        viewModelScope.launch {
            repository.getMonthlyCalendar(year, month)
                .onSuccess { calendar ->
                    _uiState.update { it.copy(calendarDays = calendar) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun loadFrequentExercises() {
        viewModelScope.launch {
            repository.getFrequentExercises()
                .onSuccess { exercises ->
                    _uiState.update { it.copy(frequentExercises = exercises) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun loadExerciseStatistics(exerciseName: String, startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExerciseStats = true) }
            
            repository.getExerciseStatistics(exerciseName, startDate, endDate)
                .onSuccess { stats ->
                    _uiState.update { 
                        it.copy(
                            isLoadingExerciseStats = false,
                            selectedExerciseStats = stats
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoadingExerciseStats = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun loadSessionsByDate(date: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoadingDaySessions = true,
                    selectedCalendarDate = date
                )
            }
            
            repository.getSessionsInRange(date, date)
                .onSuccess { sessions ->
                    // Load exercise details for each session
                    val allExercises = mutableListOf<WorkoutExerciseDetail>()
                    sessions.forEach { session ->
                        repository.getSessionDetail(session.id)
                            .onSuccess { detail ->
                                allExercises.addAll(detail.exercises)
                            }
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isLoadingDaySessions = false,
                            selectedDateSessions = sessions,
                            selectedDateCompletedExercises = allExercises
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoadingDaySessions = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun loadPlanForDate(date: String) {
        viewModelScope.launch {
            // Ensure active plan is loaded
             var activePlan = _uiState.value.activePlan
             if (activePlan == null) {
                 repository.getActivePlan().onSuccess { activePlan = it }
             }
             
             activePlan?.let { plan ->
                 try {
                     // Parse date and get day of week safe for Java Time
                     val localDate = java.time.LocalDate.parse(date)
                     // Map java DayOfWeek to our DayOfWeek enum assuming names match
                     val dayOfWeekName = localDate.dayOfWeek.name
                     
                     val planDay = plan.days.find { it.dayOfWeek.name == dayOfWeekName }
                     _uiState.update { it.copy(selectedDatePlannedExercises = planDay?.exercises ?: emptyList()) }
                 } catch (e: Exception) {
                     // Ignore parse errors
                 }
             }
        }
    }

    fun clearSelectedDateSessions() {
        _uiState.update { 
            it.copy(
                selectedDateSessions = emptyList(),
                selectedCalendarDate = null,
                selectedDatePlannedExercises = emptyList(),
                selectedDateCompletedExercises = emptyList()
            )
        }
    }

    fun clearExerciseStats() {
        _uiState.update { it.copy(selectedExerciseStats = null) }
    }

    // ==================== Reminder Methods ====================

    fun loadReminders() {
        viewModelScope.launch {
            repository.getReminders()
                .onSuccess { reminders ->
                    _uiState.update { it.copy(reminders = reminders) }
                }
        }
    }

    fun createReminder(title: String, reminderTime: String, daysOfWeek: Set<DayOfWeek>, message: String? = null) {
        viewModelScope.launch {
            val request = CreateReminderRequest(title, message, reminderTime, daysOfWeek)
            
            repository.createReminder(request)
                .onSuccess { reminder ->
                    _uiState.update { it.copy(reminders = it.reminders + reminder) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun toggleReminder(reminderId: Long) {
        viewModelScope.launch {
            repository.toggleReminder(reminderId)
                .onSuccess { updatedReminder ->
                    _uiState.update { 
                        it.copy(
                            reminders = it.reminders.map { r -> 
                                if (r.id == reminderId) updatedReminder else r 
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
                .onSuccess {
                    _uiState.update { 
                        it.copy(reminders = it.reminders.filter { r -> r.id != reminderId })
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    // ==================== Exercise Template Methods ====================

    fun loadExerciseTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTemplates = true) }
            
            repository.getExerciseTemplates()
                .onSuccess { templates ->
                    _uiState.update { 
                        it.copy(
                            isLoadingTemplates = false,
                            exerciseTemplates = templates
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingTemplates = false, error = error.message) }
                }
        }
    }

    fun loadExercisePresets() {
        viewModelScope.launch {
            repository.getExercisePresets()
                .onSuccess { presets ->
                    _uiState.update { it.copy(exercisePresets = presets) }
                }
        }
    }

    // ==================== Utility Methods ====================

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSelectedSession() {
        _uiState.update { it.copy(selectedSessionDetail = null) }
    }

    fun clearSelectedPlan() {
        _uiState.update { it.copy(selectedPlan = null) }
    }

    // ==================== Start Workout from Today Plan ====================

    fun startWorkoutFromTodayPlan() {
        val todayPlan = _uiState.value.todayPlan
        if (todayPlan == null || todayPlan.isRestDay || todayPlan.exercises.isEmpty()) {
            _uiState.update { it.copy(error = "Không có kế hoạch tập luyện hôm nay") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // First, reset/delete current session if exists
            val sessionId = _uiState.value.currentSession?.id
            if (sessionId != null) {
                repository.deleteSession(sessionId)
            }
            
            // Create a new empty session (no exercises added yet)
            val sessionName = "Buổi tập ${todayPlan.exercises.size} bài"
            val request = CreateSessionRequest(
                name = sessionName,
                notes = null,
                startedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            
            repository.createSession(request)
                .onSuccess { session ->
                    // Set the pending exercises from the plan (NOT added to session yet)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentSession = session,
                            currentSessionDetail = WorkoutSessionDetail(
                                id = session.id,
                                name = session.name,
                                notes = session.notes,
                                startedAt = session.startedAt,
                                endedAt = session.endedAt,
                                durationMinutes = session.durationMinutes,
                                caloriesBurned = session.caloriesBurned,
                                exercises = emptyList() // Empty - no exercises completed yet
                            ),
                            pendingExercises = todayPlan.exercises // Show plan exercises as pending
                        )
                    }
                    loadTodaySessions()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ==================== Helpers for plan sets & reminders ====================

    private fun encodeSetsNote(setRows: List<com.lc9th5.gym.ui.view.PlanSetRowData>, scheduledTime: String? = null): String {
        // Format: SETS:reps@weight;reps@weight|TIME:HH:mm
        val encoded = setRows.joinToString(";") { row ->
            val weight = row.weight.ifBlank { "" }
            "${row.reps}@${weight}"
        }
        var note = "SETS:$encoded"
        if (scheduledTime != null) {
            note += "|TIME:$scheduledTime"
        }
        return note
    }

    private fun decodeSetsNote(note: String?): List<Pair<Int, Double?>> {
        if (note.isNullOrBlank()) return emptyList()
        
        // Handle legacy format (just SETS:...) and new format (SETS:...|TIME:...)
        val parts = note.split("|")
        val setsPart = parts.find { it.startsWith("SETS:") } ?: (if (note.startsWith("SETS:")) note else null) ?: return emptyList()
        
        val raw = setsPart.removePrefix("SETS:")
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { token ->
            val p = token.split("@")
            val reps = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val weight = p.getOrNull(1)?.toDoubleOrNull()
            reps to weight
        }
    }
    
    private fun extractTimeFromNote(note: String?): String? {
        if (note.isNullOrBlank()) return null
        val parts = note.split("|")
        return parts.find { it.startsWith("TIME:") }?.removePrefix("TIME:")
    }

    private fun findDayOfWeekById(dayId: Long): DayOfWeek? {
        val selectedPlanDay = _uiState.value.selectedPlan?.days?.firstOrNull { it.id == dayId }
        if (selectedPlanDay != null) return selectedPlanDay.dayOfWeek
        val todayPlanDay = _uiState.value.todayPlan
        if (todayPlanDay?.id == dayId) return todayPlanDay.dayOfWeek
        return null
    }

    private fun scheduleExerciseReminder(
        context: Context, 
        exerciseId: Long, 
        exerciseName: String, 
        time: String, 
        dayOfWeek: DayOfWeek
    ) {
        val dummy = WorkoutReminder(
            id = exerciseId,
            title = "Tập $exerciseName",
            message = "Đến giờ tập $exerciseName rồi!",
            reminderTime = time,
            daysOfWeek = setOf(dayOfWeek),
            isEnabled = true,
            isActive = true
        )
        WorkoutReminderService(context).scheduleReminder(dummy)
    }

    private fun cancelPlanReminders(context: Context, plan: WorkoutPlanDetail) {
        val service = WorkoutReminderService(context)
        plan.days.forEach { day ->
            day.exercises.forEach { exercise ->
                 service.cancelReminder(exercise.id)
            }
        }
    }

    private fun reschedulePlanReminders(context: Context, plan: WorkoutPlanDetail) {
         plan.days.forEach { day ->
             day.exercises.forEach { exercise ->
                 val time = extractTimeFromNote(exercise.notes)
                 if (time != null) {
                     scheduleExerciseReminder(context, exercise.id, exercise.exerciseName, time, day.dayOfWeek)
                 }
             }
         }
    }

    private fun currentAppDayOfWeek(): DayOfWeek {
        return when (java.time.LocalDate.now().dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }

    // Factory
    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(repository) as T
        }
    }
}

// SelectedExercise for starting workouts
data class SelectedExercise(
    val templateId: Long,
    val name: String,
    val sets: Int = 3,
    val reps: Int = 8,
    val weight: String = ""
)

data class WorkoutUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isLoadingPlans: Boolean = false,
    val isLoadingTemplates: Boolean = false,
    val isLoadingStatistics: Boolean = false,
    
    // Error
    val error: String? = null,
    val statisticsError: String? = null,
    
    // Current workout session (đang tập)
    val currentSession: WorkoutSession? = null,
    val currentSessionDetail: WorkoutSessionDetail? = null,
    
    // Sessions history
    val sessions: List<WorkoutSession> = emptyList(),
    val todaySessions: List<WorkoutSession> = emptyList(),
    val selectedSessionDetail: WorkoutSessionDetail? = null,
    val hasMoreSessions: Boolean = true,
    val currentPage: Int = 0,
    
    // Plans
    val plans: List<WorkoutPlan> = emptyList(),
    val activePlan: WorkoutPlanDetail? = null,
    val todayPlan: WorkoutPlanDay? = null,
    val selectedPlan: WorkoutPlanDetail? = null,
    
    // Pending Exercises (Chưa tập)
    val pendingExercises: List<WorkoutPlanExercise> = emptyList(),
    
    // Streak
    val streak: StreakSummary? = null,
    
    // Statistics
    val overviewStatistics: OverviewStatistics? = null,
    val workoutHistory: List<DailyWorkoutSummary> = emptyList(),
    val weeklyProgress: List<WeeklyProgress> = emptyList(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val frequentExercises: List<ExerciseFrequency> = emptyList(),
    
    // Exercise Statistics for Chart
    val selectedExerciseStats: ExerciseStatistics? = null,
    val isLoadingExerciseStats: Boolean = false,
    
    // Calendar day sessions
    val selectedDateSessions: List<WorkoutSession> = emptyList(),
    val selectedCalendarDate: String? = null,
    val isLoadingDaySessions: Boolean = false,
    val selectedDatePlannedExercises: List<WorkoutPlanExercise> = emptyList(),
    val selectedDateCompletedExercises: List<WorkoutExerciseDetail> = emptyList(),
    
    // Reminders
    val reminders: List<WorkoutReminder> = emptyList(),
    
    // Exercise Templates
    val exerciseTemplates: List<ExerciseTemplate> = emptyList(),
    val exercisePresets: ExercisePresets? = null
)
