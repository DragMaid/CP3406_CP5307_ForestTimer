package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.cp3406_cp5307_utilityappstartertemplate.data.location.LocationRepository
import au.edu.jcu.cp3406_cp5307_utilityappstartertemplate.data.weather.WeatherRepository
import au.edu.jcu.cp3406_cp5307_utilityappstartertemplate.network.NetworkModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

// ponytail: Unified AndroidViewModel manages all state transitions, persistence, and sound/vibe.

class ForestViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPrefs(application)

    // App Settings
    private val _settings = MutableStateFlow(prefs.loadSettings())
    val settings = _settings.asStateFlow()

    // Timer State
    private val _sessionType = MutableStateFlow(SessionType.FOCUS)
    val sessionType = _sessionType.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(25L * 60)
    val secondsRemaining = _secondsRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused = _isTimerPaused.asStateFlow()

    // Cycle & Tree State
    private val _completedSessionsInCycle = MutableStateFlow(prefs.getCycleCompletedSessions())
    val completedSessionsInCycle = _completedSessionsInCycle.asStateFlow()

    private val _activeTreeSpecies = MutableStateFlow(prefs.getActiveTreeSpecies() ?: TreeSpecies.OAK)
    val activeTreeSpecies = _activeTreeSpecies.asStateFlow()

    private val _totalFocusTimeInCurrentCycle = MutableStateFlow(prefs.getCycleTotalFocusMinutes())

    private val _pomodoroCyclesCount = MutableStateFlow(prefs.getPomodoroCyclesCount())

    // Garden & History
    private val _garden = MutableStateFlow(prefs.loadGarden())
    val garden = _garden.asStateFlow()

    private val _completedSessionDates = MutableStateFlow(prefs.loadCompletedSessions())
    val completedSessionDates = _completedSessionDates.asStateFlow()

    // Weather State
    private val _weatherCondition = MutableStateFlow(WeatherCondition.SUNNY)
    val weatherCondition = _weatherCondition.asStateFlow()

    private val _isFetchingWeather = MutableStateFlow(false)
    val isFetchingWeather = _isFetchingWeather.asStateFlow()

    private var timerJob: Job? = null
    private var weatherPollingJob: Job? = null
    // Repositories
    private val locationRepo = LocationRepository(application)
    private val weatherRepo = WeatherRepository(NetworkModule.createOpenMeteoApi())

    init {
        // Automatically set up active tree species if it hasn't been set yet
        if (prefs.getActiveTreeSpecies() == null) {
            plantNewSeed()
        }
        
        // Restore active timer if it was running when app was closed
        restorePersistedTimer()
        
        // Sync default timer duration
        if (!_isTimerRunning.value && !_isTimerPaused.value) {
            resetTimerToCurrentSession()
        }

        // Observe weatherMode settings and start/stop polling accordingly
        viewModelScope.launch {
            var lastMode = _settings.value.weatherMode
            if (lastMode == "Real-time") startWeatherPolling()
            _settings.collect { s ->
                if (s.weatherMode != lastMode) {
                    lastMode = s.weatherMode
                    if (lastMode == "Real-time") startWeatherPolling() else stopWeatherPolling()
                }
            }
        }
    }

    private fun startWeatherPolling() {
        stopWeatherPolling()
        weatherPollingJob = viewModelScope.launch {
            // Initial fetch
            fetchWeatherInternal(forceRefresh = true)
            while (true) {
                delay(5 * 60 * 1000L)
                fetchWeatherInternal(forceRefresh = true)
            }
        }
    }

    private fun stopWeatherPolling() {
        weatherPollingJob?.cancel()
        weatherPollingJob = null
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        _isTimerPaused.value = false
        
        saveTimerState(isPaused = false)

        timerJob = viewModelScope.launch {
            while (_secondsRemaining.value > 0) {
                delay(1000.milliseconds)
                _secondsRemaining.value -= 1
            }
            onSessionComplete()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isTimerPaused.value = true
        saveTimerState(isPaused = true)
    }

    fun resumeTimer() {
        startTimer()
    }

    fun cancelSession() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isTimerPaused.value = false
        
        // ponytail: Cancelling a focus session does not advance the tree (requirement met)
        resetTimerToCurrentSession()
        prefs.saveTimerState(null)
    }

    fun skipFocusSession() {
        if (_sessionType.value == SessionType.FOCUS) {
            timerJob?.cancel()
            _isTimerRunning.value = false
            _isTimerPaused.value = false
            onSessionComplete()
        }
    }

    private fun onSessionComplete() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isTimerPaused.value = false

        triggerCompletionEffects()

        val currentType = _sessionType.value
        val config = _settings.value

        if (currentType == SessionType.FOCUS) {
            // Completed focus session - advance tree and record stats
            val earnedMinutes = config.focusDurationMinutes
            _totalFocusTimeInCurrentCycle.value += earnedMinutes
            prefs.saveCycleTotalFocusMinutes(_totalFocusTimeInCurrentCycle.value)

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val updatedDates = _completedSessionDates.value.toMutableList().apply { add(todayStr) }
            _completedSessionDates.value = updatedDates
            prefs.saveCompletedSessions(updatedDates)

            val nextCompletedCount = _completedSessionsInCycle.value + 1
            if (nextCompletedCount >= 4) {
                // Whole cycle complete - harvest mature tree
                val nextCyclesCount = _pomodoroCyclesCount.value + 1
                _pomodoroCyclesCount.value = nextCyclesCount
                prefs.savePomodoroCyclesCount(nextCyclesCount)

                val matureTree = GardenTree(
                    species = _activeTreeSpecies.value,
                    completionDate = todayStr,
                    totalFocusTimeMinutes = _totalFocusTimeInCurrentCycle.value,
                    pomodoroCycleIndex = nextCyclesCount
                )
                val updatedGarden = _garden.value.toMutableList().apply { add(matureTree) }
                _garden.value = updatedGarden
                prefs.saveGarden(updatedGarden)

                // Reset cycle variables and plant next seed
                _completedSessionsInCycle.value = 0
                prefs.saveCycleCompletedSessions(0)
                plantNewSeed()

                // Session transition: after the fourth focus session, trigger Long Break
                _sessionType.value = SessionType.LONG_BREAK
                _secondsRemaining.value = config.longBreakDurationMinutes * 60L
                if (config.autoStartBreak) {
                    startTimer()
                }
            } else {
                // Focus session complete, advance the stage
                _completedSessionsInCycle.value = nextCompletedCount
                prefs.saveCycleCompletedSessions(nextCompletedCount)

                // Session transition: trigger Short Break
                _sessionType.value = SessionType.SHORT_BREAK
                _secondsRemaining.value = config.shortBreakDurationMinutes * 60L
                if (config.autoStartBreak) {
                    startTimer()
                }
            }
        } else {
            // Break session complete - transition back to Focus
            _sessionType.value = SessionType.FOCUS
            _secondsRemaining.value = config.focusDurationMinutes * 60L
            if (config.autoStartFocus) {
                startTimer()
            }
        }
        
        prefs.saveTimerState(null)
    }

    private fun plantNewSeed() {
        val config = _settings.value
        val nextSpecies = when (config.speciesMode) {
            "Manual" -> config.selectedSpecies
            "Seasonal" -> selectSeasonalSpecies()
            else -> TreeSpecies.entries.random() // "Random"
        }
        _activeTreeSpecies.value = nextSpecies
        prefs.saveActiveTreeSpecies(nextSpecies)
        
        _totalFocusTimeInCurrentCycle.value = 0
        prefs.saveCycleTotalFocusMinutes(0)
    }

    private fun selectSeasonalSpecies(): TreeSpecies {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        // Southern Hemisphere Seasons (since JCU is in QLD, Australia)
        return when (month) {
            // Spring (Sep, Oct, Nov)
            9, 10, 11 -> {
                val rand = (1..100).random()
                when {
                    rand <= 50 -> TreeSpecies.CHERRY_BLOSSOM
                    rand <= 70 -> TreeSpecies.BIRCH
                    rand <= 85 -> TreeSpecies.OAK
                    else -> TreeSpecies.MAPLE
                }
            }
            // Summer (Dec, Jan, Feb)
            12, 1, 2 -> {
                val rand = (1..100).random()
                when {
                    rand <= 50 -> TreeSpecies.OAK
                    rand <= 75 -> TreeSpecies.BIRCH
                    else -> TreeSpecies.MAPLE
                }
            }
            // Autumn (Mar, Apr, May)
            3, 4, 5 -> {
                val rand = (1..100).random()
                when {
                    rand <= 50 -> TreeSpecies.MAPLE
                    rand <= 75 -> TreeSpecies.OAK
                    else -> TreeSpecies.PINE
                }
            }
            // Winter (Jun, Jul, Aug)
            else -> {
                val rand = (1..100).random()
                when {
                    rand <= 50 -> TreeSpecies.PINE
                    rand <= 75 -> TreeSpecies.BIRCH
                    else -> TreeSpecies.OAK
                }
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        prefs.saveSettings(newSettings)
        
        // If the timer is not active, refresh the timer value from settings
        if (!_isTimerRunning.value && !_isTimerPaused.value) {
            resetTimerToCurrentSession()
        }
    }

    fun resetTimerToCurrentSession() {
        val config = _settings.value
        _secondsRemaining.value = when (_sessionType.value) {
            SessionType.FOCUS -> config.focusDurationMinutes * 60L
            SessionType.SHORT_BREAK -> config.shortBreakDurationMinutes * 60L
            SessionType.LONG_BREAK -> config.longBreakDurationMinutes * 60L
        }
    }

    // Persist and restore on app background/kill
    fun savePersistedState() {
        saveTimerState(isPaused = _isTimerPaused.value)
    }

    private fun saveTimerState(isPaused: Boolean) {
        if (_isTimerRunning.value || isPaused) {
            val targetTime = if (isPaused) 0L else System.currentTimeMillis() + _secondsRemaining.value * 1000
            prefs.saveTimerState(
                AppPrefs.SavedTimerState(
                    secondsRemaining = _secondsRemaining.value,
                    targetTimeMillis = targetTime,
                    sessionType = _sessionType.value,
                    isPaused = isPaused
                )
            )
        } else {
            prefs.saveTimerState(null)
        }
    }

    private fun restorePersistedTimer() {
        val saved = prefs.loadTimerState() ?: return
        _sessionType.value = saved.sessionType
        _isTimerPaused.value = saved.isPaused

        if (saved.isPaused) {
            _secondsRemaining.value = saved.secondsRemaining
            _isTimerRunning.value = false
        } else {
            val now = System.currentTimeMillis()
            if (now >= saved.targetTimeMillis) {
                // Completed in background
                _secondsRemaining.value = 0
                _isTimerRunning.value = false
                onSessionComplete()
            } else {
                // Resume running timer
                _secondsRemaining.value = (saved.targetTimeMillis - now) / 1000
                startTimer()
            }
        }
    }

    private fun triggerCompletionEffects() {
        val context = getApplication<Application>()
        val config = _settings.value

        if (config.enableSounds) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (config.enableVibration) {
            try {
                // NOTE: I don't know how to replace this one, but it doesn't seem deprecated yet
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Stats Calculation
    val totalMatureTreesCount: Int get() = _garden.value.size
    
    val totalFocusHours: Double get() {
        val gardenMinutes = _garden.value.sumOf { it.totalFocusTimeMinutes }
        return (gardenMinutes + _totalFocusTimeInCurrentCycle.value) / 60.0
    }

    val currentFocusStreak: Int get() = calculateStreak(_completedSessionDates.value)

    val averageSessionsPerDay: Double get() = calculateAverageSessionsPerDay(_completedSessionDates.value)

    private fun calculateStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val uniqueDates = dates.mapNotNull {
            try { sdf.parse(it) } catch (_: Exception) { null }
        }.distinct().sortedDescending()

        if (uniqueDates.isEmpty()) return 0

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.time

        cal.add(Calendar.DATE, -1)
        val yesterday = cal.time

        val first = uniqueDates.first()
        if (first != today && first != yesterday) {
            return 0
        }

        var streak = 0
        var currentCheck = first

        for (date in uniqueDates) {
            val diffDays = (currentCheck.time - date.time) / (1000 * 60 * 60 * 24)
            if (diffDays == 0L || diffDays == 1L) {
                if (diffDays == 1L) {
                    currentCheck = date
                }
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateAverageSessionsPerDay(dates: List<String>): Double {
        if (dates.isEmpty()) return 0.0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sortedDates = dates.mapNotNull {
            try { sdf.parse(it) } catch (_: Exception) { null }
        }.sorted()
        if (sortedDates.isEmpty()) return 0.0

        val firstDate = sortedDates.first()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.time

        val diffMs = today.time - firstDate.time
        val daysBetween = (diffMs / (1000 * 60 * 60 * 24)) + 1

        return dates.size.toDouble() / daysBetween
    }

    fun fetchWeather() {
        fetchWeatherInternal(forceRefresh = false)
    }

    /**
     * Attempt to fetch real-time weather using device location.
     * For now this falls back to the simulated fetchWeather() when location
     * access isn't available — keeps behaviour safe for builds without
     * runtime permission handling. If location and network are available,
     * one could plug in a real API call here.
     */
    fun fetchWeatherRealTime() {
        fetchWeatherInternal(forceRefresh = true)
    }

    private fun fetchWeatherInternal(forceRefresh: Boolean) {
        if (_isFetchingWeather.value) return
        _isFetchingWeather.value = true

        viewModelScope.launch {
            try {
                // Attempt to reuse cached value first
                val cached = weatherRepo.getCached()
                if (cached != null && !forceRefresh) {
                    _weatherCondition.value = cached
                    return@launch
                }

                // Obtain last-known location (may throw if permissions missing)
                val loc = try {
                    locationRepo.getCurrentLocation()
                } catch (ex: SecurityException) {
                    null
                } catch (ex: Exception) {
                    null
                }

                if (loc == null) {
                    // Nothing to do; keep previous weather and bail out
                    return@launch
                }

                val cond = weatherRepo.fetchWeather(loc.latitude, loc.longitude, forceRefresh)
                if (cond != null) {
                    _weatherCondition.value = cond
                    // persist user-chosen weather preference so settings reflect it
                    val updated = _settings.value.copy(selectedWeather = cond)
                    _settings.value = updated
                    prefs.saveSettings(updated)
                }
            } finally {
                _isFetchingWeather.value = false
            }
        }
    }

    fun setWeather(condition: WeatherCondition) {
        _weatherCondition.value = condition
    }

    /**
     * Skip the current session (works for both focus and break sessions).
     */
    fun skipCurrentSession() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isTimerPaused.value = false
        onSessionComplete()
    }
}
