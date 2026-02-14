package com.xaaav.mozukutsuchikey.keyboard

import androidx.compose.ui.graphics.vector.ImageVector

sealed class Key {
    class Char(
        val normal: kotlin.Char,
        val shifted: kotlin.Char,
        val symbol: kotlin.Char? = null,
        val symbolLabel: String? = null,
        val symbolKeyCode: Int? = null,
    ) : Key()
    class Action(val label: String, val keyCode: Int) : Key()
    class Repeatable(
        val label: String,
        val icon: ImageVector? = null,
        val keyCode: Int,
    ) : Key()
    class Modifier(val type: ModifierType) : Key()
    class JpToggle : Key()
    class SymbolSwitch : Key()
    class VoiceInput : Key()
    class Clipboard : Key()
}

data class GridPosition(
    val col: Int,
    val row: Int,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
)

data class GridKey(
    val key: Key,
    val pos: GridPosition,
)
