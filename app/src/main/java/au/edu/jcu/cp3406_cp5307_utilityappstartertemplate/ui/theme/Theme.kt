package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ponytail: Static color scheme mapping for accent colors without using dynamic color APIs to maintain simplicity.

private fun getAccentColors(colorName: String, isDark: Boolean): Triple<Color, Color, Color> {
    return when (colorName) {
        "Emerald Green" -> Triple(
            Color(0xFF00875A), // Primary
            Color(0xFF36B37E), // Secondary
            Color(0xFFE3FCEF)  // Tertiary
        )
        "Cherry Pink" -> Triple(
            Color(0xFFD81B60),
            Color(0xFFEC407A),
            Color(0xFFFCE4EC)
        )
        "Maple Red" -> Triple(
            Color(0xFFC62828),
            Color(0xFFEF5350),
            Color(0xFFFFEBEE)
        )
        "Birch Amber" -> Triple(
            Color(0xFFE65100),
            Color(0xFFFB8C00),
            Color(0xFFFFF3E0)
        )
        "Ocean Blue" -> Triple(
            Color(0xFF0D47A1),
            Color(0xFF1E88E5),
            Color(0xFFE3F2FD)
        )
        else -> Triple( // "Forest Green" (Default)
            Color(0xFF1B5E20),
            Color(0xFF4CAF50),
            Color(0xFFE8F5E9)
        )
    }
}

@Composable
fun CP3406_CP5603UtilityAppStarterTemplateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColorName: String = "Forest Green",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}