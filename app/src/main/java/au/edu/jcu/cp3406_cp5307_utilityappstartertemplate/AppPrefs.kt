package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

import android.content.Context
import android.content.SharedPreferences

// ponytail: Using native SharedPreferences with custom string serialization. No Room/Gson overhead.

class AppPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ForestPrefs", Context.MODE_PRIVATE)

    // Settings Keys
    private val KEY_DISPLAY_NAME = "pref_display_name"
    private val KEY_THEME_MODE = "pref_theme_mode"
    private val KEY_ACCENT_COLOR = "pref_accent_color"
    private val KEY_FOCUS_DURATION = "pref_focus_duration"
    private val KEY_SHORT_BREAK = "pref_short_break"
    private val KEY_LONG_BREAK = "pref_long_break"
    private val KEY_AUTO_START_BREAK = "pref_auto_break"
    private val KEY_AUTO_START_FOCUS = "pref_auto_focus"
    private val KEY_ENABLE_SOUNDS = "pref_sounds"
    private val KEY_ENABLE_VIBRATION = "pref_vibrate"
    private val KEY_DAILY_FOCUS_GOAL = "pref_daily_goal"
    private val KEY_SPECIES_MODE = "pref_species_mode"
    private val KEY_SELECTED_SPECIES = "pref_selected_species"

    // State Keys
    private val KEY_GARDEN = "state_garden"
    private val KEY_COMPLETED_SESSIONS = "state_completed_sessions"
    private val KEY_CYCLE_COMPLETED_SESSIONS = "state_cycle_completed_sessions"
    private val KEY_CYCLE_TOTAL_FOCUS_MINUTES = "state_cycle_focus_minutes"
    private val KEY_ACTIVE_TREE_SPECIES = "state_active_tree_species"
    private val KEY_POMODORO_CYCLES_COUNT = "state_pomodoro_cycles_count"

    // Timer Keys
    private val KEY_TIMER_SECONDS_REMAINING = "timer_seconds_remaining"
    private val KEY_TIMER_TARGET_MILLIS = "timer_target_millis"
    private val KEY_TIMER_SESSION_TYPE = "timer_session_type"
    private val KEY_TIMER_IS_PAUSED = "timer_is_paused"

    fun loadSettings(): AppSettings {
        return AppSettings(
            displayName = prefs.getString(KEY_DISPLAY_NAME, "Grower") ?: "Grower",
            themeMode = prefs.getString(KEY_THEME_MODE, "System") ?: "System",
            accentColor = prefs.getString(KEY_ACCENT_COLOR, "Forest Green") ?: "Forest Green",
            focusDurationMinutes = prefs.getInt(KEY_FOCUS_DURATION, 25),
            shortBreakDurationMinutes = prefs.getInt(KEY_SHORT_BREAK, 5),
            longBreakDurationMinutes = prefs.getInt(KEY_LONG_BREAK, 15),
            autoStartBreak = prefs.getBoolean(KEY_AUTO_START_BREAK, false),
            autoStartFocus = prefs.getBoolean(KEY_AUTO_START_FOCUS, false),
            enableSounds = prefs.getBoolean(KEY_ENABLE_SOUNDS, true),
            enableVibration = prefs.getBoolean(KEY_ENABLE_VIBRATION, true),
            dailyFocusGoalMinutes = prefs.getInt(KEY_DAILY_FOCUS_GOAL, 100),
            speciesMode = prefs.getString(KEY_SPECIES_MODE, "Random") ?: "Random",
            selectedSpecies = TreeSpecies.fromString(prefs.getString(KEY_SELECTED_SPECIES, "OAK") ?: "OAK")
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, settings.displayName)
            .putString(KEY_THEME_MODE, settings.themeMode)
            .putString(KEY_ACCENT_COLOR, settings.accentColor)
            .putInt(KEY_FOCUS_DURATION, settings.focusDurationMinutes)
            .putInt(KEY_SHORT_BREAK, settings.shortBreakDurationMinutes)
            .putInt(KEY_LONG_BREAK, settings.longBreakDurationMinutes)
            .putBoolean(KEY_AUTO_START_BREAK, settings.autoStartBreak)
            .putBoolean(KEY_AUTO_START_FOCUS, settings.autoStartFocus)
            .putBoolean(KEY_ENABLE_SOUNDS, settings.enableSounds)
            .putBoolean(KEY_ENABLE_VIBRATION, settings.enableVibration)
            .putInt(KEY_DAILY_FOCUS_GOAL, settings.dailyFocusGoalMinutes)
            .putString(KEY_SPECIES_MODE, settings.speciesMode)
            .putString(KEY_SELECTED_SPECIES, settings.selectedSpecies.name)
            .apply()
    }

    // Garden & Completed Sessions Persistence
    fun loadGarden(): List<GardenTree> {
        val data = prefs.getString(KEY_GARDEN, "") ?: ""
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                GardenTree(
                    species = TreeSpecies.fromString(parts[0]),
                    completionDate = parts[1],
                    totalFocusTimeMinutes = parts[2].toIntOrNull() ?: 0,
                    pomodoroCycleIndex = parts[3].toIntOrNull() ?: 0
                )
            } else null
        }
    }

    fun saveGarden(garden: List<GardenTree>) {
        val serialized = garden.joinToString(";") {
            "${it.species.name}|${it.completionDate}|${it.totalFocusTimeMinutes}|${it.pomodoroCycleIndex}"
        }
        prefs.edit().putString(KEY_GARDEN, serialized).apply()
    }

    fun loadCompletedSessions(): List<String> {
        val data = prefs.getString(KEY_COMPLETED_SESSIONS, "") ?: ""
        return if (data.isBlank()) emptyList() else data.split(";")
    }

    fun saveCompletedSessions(sessions: List<String>) {
        prefs.edit().putString(KEY_COMPLETED_SESSIONS, sessions.joinToString(";")).apply()
    }

    // Active Tree & Cycle Progress
    fun getCycleCompletedSessions(): Int = prefs.getInt(KEY_CYCLE_COMPLETED_SESSIONS, 0)
    fun saveCycleCompletedSessions(count: Int) = prefs.edit().putInt(KEY_CYCLE_COMPLETED_SESSIONS, count).apply()

    fun getCycleTotalFocusMinutes(): Int = prefs.getInt(KEY_CYCLE_TOTAL_FOCUS_MINUTES, 0)
    fun saveCycleTotalFocusMinutes(minutes: Int) = prefs.edit().putInt(KEY_CYCLE_TOTAL_FOCUS_MINUTES, minutes).apply()

    fun getActiveTreeSpecies(): TreeSpecies? {
        val name = prefs.getString(KEY_ACTIVE_TREE_SPECIES, null) ?: return null
        return TreeSpecies.fromString(name)
    }
    fun saveActiveTreeSpecies(species: TreeSpecies?) {
        prefs.edit().putString(KEY_ACTIVE_TREE_SPECIES, species?.name).apply()
    }

    fun getPomodoroCyclesCount(): Int = prefs.getInt(KEY_POMODORO_CYCLES_COUNT, 0)
    fun savePomodoroCyclesCount(count: Int) = prefs.edit().putInt(KEY_POMODORO_CYCLES_COUNT, count).apply()

    // Timer state for resume
    data class SavedTimerState(
        val secondsRemaining: Long,
        val targetTimeMillis: Long,
        val sessionType: SessionType,
        val isPaused: Boolean
    )

    fun loadTimerState(): SavedTimerState? {
        if (!prefs.contains(KEY_TIMER_SECONDS_REMAINING)) return null
        val typeStr = prefs.getString(KEY_TIMER_SESSION_TYPE, SessionType.FOCUS.name) ?: SessionType.FOCUS.name
        return SavedTimerState(
            secondsRemaining = prefs.getLong(KEY_TIMER_SECONDS_REMAINING, 0L),
            targetTimeMillis = prefs.getLong(KEY_TIMER_TARGET_MILLIS, 0L),
            sessionType = SessionType.valueOf(typeStr),
            isPaused = prefs.getBoolean(KEY_TIMER_IS_PAUSED, true)
        )
    }

    fun saveTimerState(state: SavedTimerState?) {
        val editor = prefs.edit()
        if (state == null) {
            editor.remove(KEY_TIMER_SECONDS_REMAINING)
                .remove(KEY_TIMER_TARGET_MILLIS)
                .remove(KEY_TIMER_SESSION_TYPE)
                .remove(KEY_TIMER_IS_PAUSED)
        } else {
            editor.putLong(KEY_TIMER_SECONDS_REMAINING, state.secondsRemaining)
                .putLong(KEY_TIMER_TARGET_MILLIS, state.targetTimeMillis)
                .putString(KEY_TIMER_SESSION_TYPE, state.sessionType.name)
                .putBoolean(KEY_TIMER_IS_PAUSED, state.isPaused)
        }
        editor.apply()
    }
}
