package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private var timerJob: Job? = null

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
}
