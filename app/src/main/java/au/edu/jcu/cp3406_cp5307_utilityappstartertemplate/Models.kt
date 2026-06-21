package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

// ponytail: Standard Kotlin models with simple string mappings. Zero external dependencies.

enum class TreeSpecies(val displayName: String) {
    OAK("Oak"),
    PINE("Pine"),
    CHERRY_BLOSSOM("Cherry Blossom"),
    MAPLE("Maple"),
    BIRCH("Birch");

    companion object {
        fun fromString(value: String): TreeSpecies {
            return entries.find { it.name == value || it.displayName == value } ?: OAK
        }
    }
}

enum class TreeStage(val displayName: String) {
    SEED("Seed"),
    SPROUT("Sprout"),
    YOUNG_TREE("Young Tree"),
    NEARLY_MATURE("Nearly Mature"),
    MATURE("Mature Tree");

    companion object {
        fun fromInt(value: Int): TreeStage {
            return when (value) {
                0 -> SEED
                1 -> SPROUT
                2 -> YOUNG_TREE
                3 -> NEARLY_MATURE
                else -> MATURE
            }
        }
    }
}

enum class SessionType(val displayName: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class AppSettings(
    val displayName: String = "Grower",
    val themeMode: String = "System", // System, Light, Dark
    val accentColor: String = "Forest Green", // Forest Green, Emerald Green, Cherry Pink, Maple Red, Birch Amber, Ocean Blue
    val focusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val autoStartBreak: Boolean = false,
    val autoStartFocus: Boolean = false,
    val enableSounds: Boolean = true,
    val enableVibration: Boolean = true,
    val dailyFocusGoalMinutes: Int = 100,
    val speciesMode: String = "Random", // Random, Seasonal, Manual
    val selectedSpecies: TreeSpecies = TreeSpecies.OAK,
    val selectedWeather: WeatherCondition = WeatherCondition.SUNNY,
    val weatherMode: String = "Manual" // Manual or Real-time
)

data class GardenTree(
    val species: TreeSpecies,
    val completionDate: String, // e.g., "2026-06-21"
    val totalFocusTimeMinutes: Int,
    val pomodoroCycleIndex: Int // running total of completed cycles
)

enum class WeatherCondition(val displayName: String) {
    SUNNY("Sunny"),
    CLOUDY("Cloudy"),
    RAINY("Rainy"),
    WINDY("Windy"),
    STORM("Storm")
}
