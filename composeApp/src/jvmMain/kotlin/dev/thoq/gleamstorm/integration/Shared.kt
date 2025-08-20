@file:Suppress("unused")

package dev.thoq.gleamstorm.integration

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

object Shared {
    fun getTypeStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.tertiary,
        fontWeight = FontWeight.SemiBold
    )

    fun getBuiltinStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.secondary,
        fontWeight = FontWeight.Medium
    )

    fun getStringStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.secondary
    )

    fun getAtomStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.tertiary.copy(alpha = 0.9f)
    )

    fun getVariableStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.onSurface.copy(
            red = minOf(1.0f, colorScheme.onSurface.red + 0.1f),
            green = minOf(1.0f, colorScheme.onSurface.green + 0.1f),
            blue = maxOf(0.0f, colorScheme.onSurface.blue - 0.2f)
        )
    )

    fun getNumberStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.primaryContainer.copy(
            red = if(colorScheme.primaryContainer.red < 0.5f)
                minOf(1.0f, colorScheme.primaryContainer.red + 0.3f)
            else maxOf(0.0f, colorScheme.primaryContainer.red - 0.3f),
            green = if(colorScheme.primaryContainer.green < 0.5f)
                minOf(1.0f, colorScheme.primaryContainer.green + 0.3f)
            else maxOf(0.0f, colorScheme.primaryContainer.green - 0.3f),
            blue = if(colorScheme.primaryContainer.blue < 0.5f)
                minOf(1.0f, colorScheme.primaryContainer.blue + 0.3f)
            else maxOf(0.0f, colorScheme.primaryContainer.blue - 0.3f)
        )
    )

    fun getKeywordStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.primary,
        fontWeight = FontWeight.Bold
    )

    fun getConstructorStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.secondary.copy(
            red = minOf(1.0f, colorScheme.secondary.red + 0.2f),
            blue = minOf(1.0f, colorScheme.secondary.blue + 0.2f)
        )
    )

    fun getCommentStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.onSurface.copy(alpha = 0.7f)
    )

    fun getMacroStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.primary.copy(
            red = minOf(1.0f, colorScheme.primary.red + 0.2f),
            green = maxOf(0.0f, colorScheme.primary.green - 0.1f)
        ),
        fontWeight = FontWeight.Medium
    )

    fun getRecordStyle(colorScheme: ColorScheme) = SpanStyle(
        color = colorScheme.secondary.copy(
            red = minOf(1.0f, colorScheme.secondary.red + 0.2f),
            blue = minOf(1.0f, colorScheme.secondary.blue + 0.2f)
        ),
        fontWeight = FontWeight.Medium
    )
}