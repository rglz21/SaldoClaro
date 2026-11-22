package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SemanticFinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val goldAccent: Color,
)

val LocalFinanceColors = staticCompositionLocalOf {
    SemanticFinanceColors(
        income = MintEmerald,
        incomeContainer = SoftMintContainer,
        expense = TerracottaExpense,
        expenseContainer = SoftCoralContainer,
        transfer = TransferBlue,
        transferContainer = SoftBlueContainer,
        warning = WarmGold,
        warningContainer = SoftGoldContainer,
        heroStart = ForestEmerald,
        heroEnd = DeepPine,
        goldAccent = LuminousGold,
    )
}

private val LightSemanticColors = SemanticFinanceColors(
    income = MintEmerald,
    incomeContainer = SoftMintContainer,
    expense = TerracottaExpense,
    expenseContainer = SoftCoralContainer,
    transfer = TransferBlue,
    transferContainer = SoftBlueContainer,
    warning = WarmGold,
    warningContainer = SoftGoldContainer,
    heroStart = ForestEmerald,
    heroEnd = DeepPine,
    goldAccent = LuminousGold,
)

private val DarkSemanticColors = SemanticFinanceColors(
    income = BrightMint,
    incomeContainer = DarkMintContainer,
    expense = SoftCoral,
    expenseContainer = DarkCoralContainer,
    transfer = SoftBlue,
    transferContainer = DarkBlueContainer,
    warning = LuminousGold,
    warningContainer = DarkGoldContainer,
    heroStart = Color(0xFF103F30),
    heroEnd = Color(0xFF071E16),
    goldAccent = LuminousGold,
)

private val LightColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftMintContainer,
    onPrimaryContainer = DeepPine,
    secondary = MintEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EFE9),
    onSecondaryContainer = ForestEmerald,
    tertiary = WarmGold,
    onTertiary = Color.White,
    tertiaryContainer = SoftGoldContainer,
    onTertiaryContainer = Color(0xFF472C00),
    error = TerracottaExpense,
    onError = Color.White,
    errorContainer = SoftCoralContainer,
    onErrorContainer = Color(0xFF69120A),
    background = AlabasterBg,
    onBackground = InkDark,
    surface = PureCardSurface,
    onSurface = InkDark,
    surfaceVariant = MutedSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = SubtleOutline,
    outlineVariant = Color(0xFFE3E7DF),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightMint,
    onPrimary = Color(0xFF032116),
    primaryContainer = DarkMintContainer,
    onPrimaryContainer = SoftMintContainer,
    secondary = LuminousGold,
    onSecondary = Color(0xFF2B1C00),
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = SoftGoldContainer,
    tertiary = SoftBlue,
    onTertiary = Color(0xFF081F44),
    tertiaryContainer = DarkBlueContainer,
    onTertiaryContainer = SoftBlueContainer,
    error = SoftCoral,
    onError = Color(0xFF3E0A06),
    errorContainer = DarkCoralContainer,
    onErrorContainer = SoftCoralContainer,
    background = ObsidianBg,
    onBackground = InkLight,
    surface = ObsidianSurface,
    onSurface = InkLight,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = InkLightMuted,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF23322D),
)

val SaldoClaroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SaldoClaroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalFinanceColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaldoClaroTypography,
            shapes = SaldoClaroShapes,
            content = content,
        )
    }
}
