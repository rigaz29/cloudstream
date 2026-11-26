package com.lagradost.cloudstream4.skia

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.generated.resources.Res
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_bold
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_extra_bold
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_italic
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_light
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_medium
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_regular
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_semi_bold
import com.lagradost.cloudstream4.theme.CloudStreamColorScheme
import com.lagradost.cloudstream4.theme.CloudStreamPrimaryColor
import com.lagradost.cloudstream4.theme.CloudStreamThemeMode
import org.jetbrains.compose.resources.Font

/**
 * CloudStream Skia look for the Compose screens. Mirrors app/src/skia/res/values/skia_theme.xml
 * so XML and Compose screens match. Called from upstream code through one-line hooks
 * (search for "Skia hook").
 */
object SkiaTheme {
    val ground = Color(0xFF09090B)
    val surface = Color(0xFF141418)
    val surfaceHigh = Color(0xFF1C1C22)
    val line = Color(0xFF26262E)
    val text = Color(0xFFF5F5F7)
    val textMuted = Color(0xFFA0A0AB)
    val accent = Color(0xFF8B7CFF)
    val onAccent = Color(0xFF0B0A14)

    val fontFamily: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.plus_jakarta_sans_light, weight = FontWeight.W300, style = FontStyle.Normal),
            Font(Res.font.plus_jakarta_sans_regular, weight = FontWeight.W400, style = FontStyle.Normal),
            Font(Res.font.plus_jakarta_sans_italic, weight = FontWeight.W400, style = FontStyle.Italic),
            Font(Res.font.plus_jakarta_sans_medium, weight = FontWeight.W500, style = FontStyle.Normal),
            Font(Res.font.plus_jakarta_sans_semi_bold, weight = FontWeight.W600, style = FontStyle.Normal),
            Font(Res.font.plus_jakarta_sans_bold, weight = FontWeight.W700, style = FontStyle.Normal),
            Font(Res.font.plus_jakarta_sans_extra_bold, weight = FontWeight.W800, style = FontStyle.Normal),
        )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

    /**
     * The upstream defaults (theme mode "AmoledLight", primary color "Normal") get the Skia
     * palette; any other choice the user makes keeps its upstream colors.
     */
    @Composable
    fun adjust(
        scheme: CloudStreamColorScheme,
        mode: CloudStreamThemeMode,
        primaryColor: CloudStreamPrimaryColor,
    ): CloudStreamColorScheme = remember(scheme, mode, primaryColor) {
        var out = scheme
        if (mode == CloudStreamThemeMode.AmoledLight) {
            out = out.copy(
                background = ground,
                surfaceVariant = surface,
                surface = surfaceHigh,
                surfaceContainer = surface,
                onBackground = text,
                onSurfaceVariant = textMuted,
                icon = textMuted,
            )
        }
        if (primaryColor == CloudStreamPrimaryColor.NORMAL) {
            out = out.copy(primary = accent)
        }
        out
    }

    /** Readable text on the primary color and Skia outlines for the Material 3 roles. */
    fun material(scheme: ColorScheme): ColorScheme {
        val onPrimary = if (scheme.primary.luminance() > 0.3f) onAccent else Color.White
        val isDark = scheme.background.luminance() < 0.5f
        return if (isDark) {
            scheme.copy(onPrimary = onPrimary, outline = line, outlineVariant = line)
        } else {
            scheme.copy(onPrimary = onPrimary)
        }
    }
}
