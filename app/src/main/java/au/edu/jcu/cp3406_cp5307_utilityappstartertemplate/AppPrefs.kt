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
