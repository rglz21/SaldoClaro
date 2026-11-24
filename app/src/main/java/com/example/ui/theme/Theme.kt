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
        income = IncomeEmerald,
        incomeContainer = SoftMintContainer,
        expense = TerracottaExpense,
        expenseContainer = SoftCoralContainer,
        transfer = TransferBlue,
        transferContainer = SoftBlueContainer,
        warning = RoyalGold,
        warningContainer = SoftGoldContainer,
        heroStart = CharcoalSurface,
        heroEnd = ObsidianBlack,
        goldAccent = LuminousGold,
    )
}

private val LightSemanticColors = SemanticFinanceColors(
    income = IncomeEmerald,
    incomeContainer = SoftMintContainer,
    expense = TerracottaExpense,
    expenseContainer = SoftCoralContainer,
    transfer = TransferBlue,
    transferContainer = SoftBlueContainer,
    warning = RoyalGold,
    warningContainer = SoftGoldContainer,
    heroStart = CharcoalSurface,
    heroEnd = ObsidianBlack,
    goldAccent = LuminousGold,
)

private val DarkSemanticColors = SemanticFinanceColors(
    income = BrightEmerald,
    incomeContainer = DarkMintContainer,
    expense = SoftCoral,
    expenseContainer = DarkCoralContainer,
    transfer = SoftBlue,
    transferContainer = DarkBlueContainer,
    warning = LuminousGold,
    warningContainer = DarkGoldContainer,
    heroStart = Color(0xFF28261E),
    heroEnd = ObsidianBlack,
    goldAccent = LuminousGold,
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianBlack,
    primaryContainer = SoftGoldContainer,
    onPrimaryContainer = DeepGoldText,
    secondary = CharcoalSurface,
    onSecondary = WarmIvory,
    secondaryContainer = Color(0xFFE4E0D6),
    onSecondaryContainer = ObsidianBlack,
    tertiary = LuminousGold,
    onTertiary = ObsidianBlack,
    tertiaryContainer = Color(0xFFFFF5CE),
    onTertiaryContainer = DeepGoldText,
    error = TerracottaExpense,
    onError = Color.White,
    errorContainer = SoftCoralContainer,
    onErrorContainer = Color(0xFF69120A),
    background = WarmIvory,
    onBackground = ObsidianBlack,
    surface = PureCardSurface,
    onSurface = ObsidianBlack,
    surfaceVariant = LightSandVariant,
    onSurfaceVariant = InkMuted,
    outline = LightOutline,
    outlineVariant = LightOutlineSubtle,
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminousGold,
    onPrimary = ObsidianBlack,
    primaryContainer = DarkGoldContainer,
    onPrimaryContainer = LuminousGold,
    secondary = RoyalGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF2B2717),
    onSecondaryContainer = WarmIvory,
    tertiary = WarmIvory,
    onTertiary = ObsidianBlack,
    tertiaryContainer = DeepCharcoalVariant,
    onTertiaryContainer = WarmIvory,
    error = SoftCoral,
    onError = Color(0xFF3E0A06),
    errorContainer = DarkCoralContainer,
    onErrorContainer = SoftCoralContainer,
    background = ObsidianBlack,
    onBackground = WarmIvory,
    surface = CharcoalSurface,
    onSurface = WarmIvory,
    surfaceVariant = DeepCharcoalVariant,
    onSurfaceVariant = WarmIvoryMuted,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSubtle,
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
