package com.xaaav.mozukutsuchikey.keyboard

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ===================== Grid Constants =====================
const val GRID_COLS = 720
const val GRID_ROWS = 252    // 6(top) + 5*RH + 4*RG + 6(bottom)

const val RH = 40     // key row height
const val RG = 10     // gap between rows
const val CG = 12     // gap between columns
const val R1 = 6
const val R2 = R1 + RH + RG
const val R3 = R2 + RH + RG
const val R4 = R3 + RH + RG
const val R5 = R4 + RH + RG

// ===================== Split Grid Constants =====================
const val RH_S = 42    // split key row height (taller)
const val RG_S = 7     // split gap between rows
const val R1_S = 7
const val R2_S = R1_S + RH_S + RG_S   // 56
const val R3_S = R2_S + RH_S + RG_S   // 105
const val R4_S = R3_S + RH_S + RG_S   // 154
const val R5_S = R4_S + RH_S + RG_S   // 203
// Total: 7 + 5*42 + 4*7 + 7 = 252 ✓

val GRID_KEYS: List<GridKey> = listOf(
    // Row 1: ESC ` 1 2 3 4 5 6 7 8 9 0 - / Del
    GridKey(Key.Action("ESC", AndroidKeyEvent.KEYCODE_ESCAPE), GridPosition(4, R1, 36, RH)),
    GridKey(Key.Char('`', '~', symbolLabel = "F1", symbolKeyCode = AndroidKeyEvent.KEYCODE_F1), GridPosition(52, R1, 36, RH)),
    GridKey(Key.Char('1', '!', symbolLabel = "F2", symbolKeyCode = AndroidKeyEvent.KEYCODE_F2), GridPosition(100, R1, 36, RH)),
    GridKey(Key.Char('2', '@', symbolLabel = "F3", symbolKeyCode = AndroidKeyEvent.KEYCODE_F3), GridPosition(148, R1, 36, RH)),
    GridKey(Key.Char('3', '#', symbolLabel = "F4", symbolKeyCode = AndroidKeyEvent.KEYCODE_F4), GridPosition(196, R1, 36, RH)),
    GridKey(Key.Char('4', '$', symbolLabel = "F5", symbolKeyCode = AndroidKeyEvent.KEYCODE_F5), GridPosition(244, R1, 36, RH)),
    GridKey(Key.Char('5', '%', symbolLabel = "F6", symbolKeyCode = AndroidKeyEvent.KEYCODE_F6), GridPosition(292, R1, 36, RH)),
    GridKey(Key.Char('6', '^', symbolLabel = "F7", symbolKeyCode = AndroidKeyEvent.KEYCODE_F7), GridPosition(340, R1, 36, RH)),
    GridKey(Key.Char('7', '&', symbolLabel = "F8", symbolKeyCode = AndroidKeyEvent.KEYCODE_F8), GridPosition(388, R1, 36, RH)),
    GridKey(Key.Char('8', '*', symbolLabel = "F9", symbolKeyCode = AndroidKeyEvent.KEYCODE_F9), GridPosition(436, R1, 36, RH)),
    GridKey(Key.Char('9', '(', symbolLabel = "F10", symbolKeyCode = AndroidKeyEvent.KEYCODE_F10), GridPosition(484, R1, 36, RH)),
    GridKey(Key.Char('0', ')', symbolLabel = "F11", symbolKeyCode = AndroidKeyEvent.KEYCODE_F11), GridPosition(532, R1, 36, RH)),
    GridKey(Key.Char('-', '_', symbolLabel = "F12", symbolKeyCode = AndroidKeyEvent.KEYCODE_F12), GridPosition(580, R1, 36, RH)),
    GridKey(Key.Char('/', '?', symbolLabel = "Ins", symbolKeyCode = AndroidKeyEvent.KEYCODE_INSERT), GridPosition(628, R1, 36, RH)),
    GridKey(Key.Repeatable("Del", keyCode = AndroidKeyEvent.KEYCODE_FORWARD_DEL), GridPosition(676, R1, 40, RH)),

    // Row 2: Tab Q W E R T Y U I O P BS
    GridKey(Key.Action("Tab", AndroidKeyEvent.KEYCODE_TAB), GridPosition(4, R2, 60, RH)),
    GridKey(Key.Char('q', 'Q', symbol = '!'), GridPosition(76, R2, 47, RH)),
    GridKey(Key.Char('w', 'W', symbol = '@'), GridPosition(135, R2, 47, RH)),
    GridKey(Key.Char('e', 'E', symbol = '='), GridPosition(194, R2, 47, RH)),
    GridKey(Key.Char('r', 'R', symbol = '+'), GridPosition(253, R2, 47, RH)),
    GridKey(Key.Char('t', 'T', symbol = '~'), GridPosition(312, R2, 47, RH)),
    GridKey(Key.Char('y', 'Y', symbol = '|'), GridPosition(371, R2, 47, RH)),
    GridKey(Key.Char('u', 'U', symbol = '\\'), GridPosition(430, R2, 47, RH)),
    GridKey(Key.Char('i', 'I', symbol = '['), GridPosition(489, R2, 47, RH)),
    GridKey(Key.Char('o', 'O', symbol = ']'), GridPosition(548, R2, 47, RH)),
    GridKey(Key.Char('p', 'P', symbol = '_'), GridPosition(607, R2, 47, RH)),
    GridKey(Key.Repeatable("\u232B", icon = Icons.AutoMirrored.Filled.Backspace, keyCode = AndroidKeyEvent.KEYCODE_DEL), GridPosition(666, R2, 50, RH)),

    // Row 3: JP A S D F G H J K L ; Enter(spans rows 3-4)
    GridKey(Key.JpToggle(), GridPosition(4, R3, 72, RH)),
    GridKey(Key.Char('a', 'A', symbol = '{'), GridPosition(88, R3, 47, RH)),
    GridKey(Key.Char('s', 'S', symbol = '}'), GridPosition(147, R3, 47, RH)),
    GridKey(Key.Char('d', 'D', symbol = '\''), GridPosition(206, R3, 47, RH)),
    GridKey(Key.Char('f', 'F', symbol = '"'), GridPosition(265, R3, 47, RH)),
    GridKey(Key.Char('g', 'G', symbol = '`'), GridPosition(324, R3, 47, RH)),
    GridKey(Key.Char('h', 'H', symbolLabel = "Home", symbolKeyCode = AndroidKeyEvent.KEYCODE_MOVE_HOME), GridPosition(383, R3, 47, RH)),
    GridKey(Key.Char('j', 'J', symbolLabel = "PgDn", symbolKeyCode = AndroidKeyEvent.KEYCODE_PAGE_DOWN), GridPosition(442, R3, 47, RH)),
    GridKey(Key.Char('k', 'K', symbolLabel = "PgUp", symbolKeyCode = AndroidKeyEvent.KEYCODE_PAGE_UP), GridPosition(501, R3, 47, RH)),
    GridKey(Key.Char('l', 'L', symbolLabel = "End", symbolKeyCode = AndroidKeyEvent.KEYCODE_MOVE_END), GridPosition(560, R3, 47, RH)),
    GridKey(Key.Char(';', ':', symbol = ':'), GridPosition(619, R3, 47, RH)),
    GridKey(Key.Action("\u21B5", AndroidKeyEvent.KEYCODE_ENTER), GridPosition(676, R3, 40, RH * 2 + RG)),

    // Row 4: Shift Z X C V B N M , . ↑
    GridKey(Key.Modifier(ModifierType.SHIFT), GridPosition(4, R4, 78, RH)),
    GridKey(Key.Char('z', 'Z', symbol = '<'), GridPosition(94, R4, 47, RH)),
    GridKey(Key.Char('x', 'X', symbol = '>'), GridPosition(153, R4, 47, RH)),
    GridKey(Key.Char('c', 'C', symbol = '('), GridPosition(212, R4, 47, RH)),
    GridKey(Key.Char('v', 'V', symbol = ')'), GridPosition(271, R4, 47, RH)),
    GridKey(Key.Char('b', 'B', symbol = '&'), GridPosition(330, R4, 47, RH)),
    GridKey(Key.Char('n', 'N', symbol = '#'), GridPosition(389, R4, 47, RH)),
    GridKey(Key.Char('m', 'M', symbol = '$'), GridPosition(448, R4, 47, RH)),
    GridKey(Key.Char(',', '<', symbol = '^'), GridPosition(507, R4, 47, RH)),
    GridKey(Key.Char('.', '>', symbol = '%'), GridPosition(566, R4, 47, RH)),
    GridKey(Key.Repeatable("\u2191", icon = Icons.Default.KeyboardArrowUp, keyCode = AndroidKeyEvent.KEYCODE_DPAD_UP), GridPosition(628, R4, 36, RH)),

    // Row 5: Ctrl Alt #+= Space Voice Hide ← ↓ →
    GridKey(Key.Modifier(ModifierType.CTRL), GridPosition(4, R5, 60, RH)),
    GridKey(Key.Modifier(ModifierType.ALT), GridPosition(76, R5, 60, RH)),
    GridKey(Key.SymbolSwitch(), GridPosition(148, R5, 60, RH)),
    GridKey(Key.Char(' ', ' '), GridPosition(220, R5, 228, RH)),
    GridKey(Key.VoiceInput(), GridPosition(460, R5, 60, RH)),
    GridKey(Key.Clipboard(), GridPosition(532, R5, 36, RH)),
    GridKey(Key.Repeatable("\u2190", icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, keyCode = AndroidKeyEvent.KEYCODE_DPAD_LEFT), GridPosition(580, R5, 36, RH)),
    GridKey(Key.Repeatable("\u2193", icon = Icons.Default.KeyboardArrowDown, keyCode = AndroidKeyEvent.KEYCODE_DPAD_DOWN), GridPosition(628, R5, 36, RH)),
    GridKey(Key.Repeatable("\u2192", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, keyCode = AndroidKeyEvent.KEYCODE_DPAD_RIGHT), GridPosition(676, R5, 36, RH)),
)

val GRID_KEYS_SPLIT: List<GridKey> = listOf(
    // Row 1 — Left: ESC ` 1 2 3 4 5
    GridKey(Key.Action("ESC", AndroidKeyEvent.KEYCODE_ESCAPE), GridPosition(4, R1_S, 36, RH_S)),
    GridKey(Key.Char('`', '~', symbolLabel = "F1", symbolKeyCode = AndroidKeyEvent.KEYCODE_F1), GridPosition(46, R1_S, 36, RH_S)),
    GridKey(Key.Char('1', '!', symbolLabel = "F2", symbolKeyCode = AndroidKeyEvent.KEYCODE_F2), GridPosition(88, R1_S, 36, RH_S)),
    GridKey(Key.Char('2', '@', symbolLabel = "F3", symbolKeyCode = AndroidKeyEvent.KEYCODE_F3), GridPosition(130, R1_S, 36, RH_S)),
    GridKey(Key.Char('3', '#', symbolLabel = "F4", symbolKeyCode = AndroidKeyEvent.KEYCODE_F4), GridPosition(172, R1_S, 36, RH_S)),
    GridKey(Key.Char('4', '$', symbolLabel = "F5", symbolKeyCode = AndroidKeyEvent.KEYCODE_F5), GridPosition(214, R1_S, 36, RH_S)),
    GridKey(Key.Char('5', '%', symbolLabel = "F6", symbolKeyCode = AndroidKeyEvent.KEYCODE_F6), GridPosition(256, R1_S, 36, RH_S)),
    // Row 1 — Right: 6 7 8 9 0 - / Del
    GridKey(Key.Char('6', '^', symbolLabel = "F7", symbolKeyCode = AndroidKeyEvent.KEYCODE_F7), GridPosition(382, R1_S, 36, RH_S)),
    GridKey(Key.Char('7', '&', symbolLabel = "F8", symbolKeyCode = AndroidKeyEvent.KEYCODE_F8), GridPosition(424, R1_S, 36, RH_S)),
    GridKey(Key.Char('8', '*', symbolLabel = "F9", symbolKeyCode = AndroidKeyEvent.KEYCODE_F9), GridPosition(466, R1_S, 36, RH_S)),
    GridKey(Key.Char('9', '(', symbolLabel = "F10", symbolKeyCode = AndroidKeyEvent.KEYCODE_F10), GridPosition(508, R1_S, 36, RH_S)),
    GridKey(Key.Char('0', ')', symbolLabel = "F11", symbolKeyCode = AndroidKeyEvent.KEYCODE_F11), GridPosition(550, R1_S, 36, RH_S)),
    GridKey(Key.Char('-', '_', symbolLabel = "F12", symbolKeyCode = AndroidKeyEvent.KEYCODE_F12), GridPosition(592, R1_S, 36, RH_S)),
    GridKey(Key.Char('/', '?', symbolLabel = "Ins", symbolKeyCode = AndroidKeyEvent.KEYCODE_INSERT), GridPosition(634, R1_S, 36, RH_S)),
    GridKey(Key.Repeatable("Del", keyCode = AndroidKeyEvent.KEYCODE_FORWARD_DEL), GridPosition(676, R1_S, 40, RH_S)),

    // Row 2 — Left: Tab Q W E R T
    GridKey(Key.Action("Tab", AndroidKeyEvent.KEYCODE_TAB), GridPosition(4, R2_S, 36, RH_S)),
    GridKey(Key.Char('q', 'Q', symbol = '!'), GridPosition(46, R2_S, 36, RH_S)),
    GridKey(Key.Char('w', 'W', symbol = '@'), GridPosition(88, R2_S, 36, RH_S)),
    GridKey(Key.Char('e', 'E', symbol = '='), GridPosition(130, R2_S, 36, RH_S)),
    GridKey(Key.Char('r', 'R', symbol = '+'), GridPosition(172, R2_S, 36, RH_S)),
    GridKey(Key.Char('t', 'T', symbol = '~'), GridPosition(214, R2_S, 36, RH_S)),
    GridKey(Key.Action("Tab", AndroidKeyEvent.KEYCODE_TAB), GridPosition(256, R2_S, 36, RH_S)),
    // Row 2 — Right: BS Y U I O P BS
    GridKey(Key.Repeatable("\u232B", icon = Icons.AutoMirrored.Filled.Backspace, keyCode = AndroidKeyEvent.KEYCODE_DEL), GridPosition(416, R2_S, 36, RH_S)),
    GridKey(Key.Char('y', 'Y', symbol = '|'), GridPosition(458, R2_S, 36, RH_S)),
    GridKey(Key.Char('u', 'U', symbol = '\\'), GridPosition(500, R2_S, 36, RH_S)),
    GridKey(Key.Char('i', 'I', symbol = '['), GridPosition(542, R2_S, 36, RH_S)),
    GridKey(Key.Char('o', 'O', symbol = ']'), GridPosition(584, R2_S, 36, RH_S)),
    GridKey(Key.Char('p', 'P', symbol = '_'), GridPosition(626, R2_S, 36, RH_S)),
    GridKey(Key.Repeatable("\u232B", icon = Icons.AutoMirrored.Filled.Backspace, keyCode = AndroidKeyEvent.KEYCODE_DEL), GridPosition(668, R2_S, 48, RH_S)),

    // Row 3 — Left: JP A S D F G
    GridKey(Key.JpToggle(), GridPosition(8, R3_S, 36, RH_S)),
    GridKey(Key.Char('a', 'A', symbol = '{'), GridPosition(50, R3_S, 36, RH_S)),
    GridKey(Key.Char('s', 'S', symbol = '}'), GridPosition(92, R3_S, 36, RH_S)),
    GridKey(Key.Char('d', 'D', symbol = '\''), GridPosition(134, R3_S, 36, RH_S)),
    GridKey(Key.Char('f', 'F', symbol = '"'), GridPosition(176, R3_S, 36, RH_S)),
    GridKey(Key.Char('g', 'G', symbol = '`'), GridPosition(218, R3_S, 36, RH_S)),
    GridKey(Key.JpToggle(), GridPosition(260, R3_S, 36, RH_S)),
    // Row 3 — Right: Enter H J K L ; Enter(spans rows 3-4)
    GridKey(Key.Action("\u21B5", AndroidKeyEvent.KEYCODE_ENTER), GridPosition(424, R3_S, 36, RH_S)),
    GridKey(Key.Char('h', 'H', symbolLabel = "Home", symbolKeyCode = AndroidKeyEvent.KEYCODE_MOVE_HOME), GridPosition(466, R3_S, 36, RH_S)),
    GridKey(Key.Char('j', 'J', symbolLabel = "PgDn", symbolKeyCode = AndroidKeyEvent.KEYCODE_PAGE_DOWN), GridPosition(508, R3_S, 36, RH_S)),
    GridKey(Key.Char('k', 'K', symbolLabel = "PgUp", symbolKeyCode = AndroidKeyEvent.KEYCODE_PAGE_UP), GridPosition(550, R3_S, 36, RH_S)),
    GridKey(Key.Char('l', 'L', symbolLabel = "End", symbolKeyCode = AndroidKeyEvent.KEYCODE_MOVE_END), GridPosition(592, R3_S, 36, RH_S)),
    GridKey(Key.Char(';', ':', symbol = ':'), GridPosition(634, R3_S, 36, RH_S)),
    GridKey(Key.Action("\u21B5", AndroidKeyEvent.KEYCODE_ENTER), GridPosition(676, R3_S, 40, RH_S * 2 + RG_S)),

    // Row 4 — Left: Shift Z X C V B
    GridKey(Key.Modifier(ModifierType.SHIFT), GridPosition(16, R4_S, 36, RH_S)),
    GridKey(Key.Char('z', 'Z', symbol = '<'), GridPosition(58, R4_S, 36, RH_S)),
    GridKey(Key.Char('x', 'X', symbol = '>'), GridPosition(100, R4_S, 36, RH_S)),
    GridKey(Key.Char('c', 'C', symbol = '('), GridPosition(142, R4_S, 36, RH_S)),
    GridKey(Key.Char('v', 'V', symbol = ')'), GridPosition(184, R4_S, 36, RH_S)),
    GridKey(Key.Char('b', 'B', symbol = '&'), GridPosition(226, R4_S, 36, RH_S)),
    // Row 4 — Right: N M , . ↑
    GridKey(Key.Char('n', 'N', symbol = '#'), GridPosition(470, R4_S, 36, RH_S)),
    GridKey(Key.Char('m', 'M', symbol = '$'), GridPosition(512, R4_S, 36, RH_S)),
    GridKey(Key.Char(',', '<', symbol = '^'), GridPosition(554, R4_S, 36, RH_S)),
    GridKey(Key.Char('.', '>', symbol = '%'), GridPosition(596, R4_S, 36, RH_S)),
    GridKey(Key.Repeatable("\u2191", icon = Icons.Default.KeyboardArrowUp, keyCode = AndroidKeyEvent.KEYCODE_DPAD_UP), GridPosition(638, R4_S, 36, RH_S)),

    // Row 5 — Left: Ctrl Alt Sym Dock
    GridKey(Key.Modifier(ModifierType.CTRL), GridPosition(4, R5_S, 54, RH_S)),
    GridKey(Key.Modifier(ModifierType.ALT), GridPosition(64, R5_S, 54, RH_S)),
    GridKey(Key.SymbolSwitch(), GridPosition(124, R5_S, 54, RH_S)),
    GridKey(Key.DockToggle(), GridPosition(184, R5_S, 36, RH_S)),
    // Row 5 — Right: Space Voice Clip ← ↓ →
    GridKey(Key.Char(' ', ' '), GridPosition(419, R5_S, 69, RH_S)),
    GridKey(Key.VoiceInput(), GridPosition(494, R5_S, 54, RH_S)),
    GridKey(Key.Clipboard(), GridPosition(554, R5_S, 36, RH_S)),
    GridKey(Key.Repeatable("\u2190", icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, keyCode = AndroidKeyEvent.KEYCODE_DPAD_LEFT), GridPosition(596, R5_S, 36, RH_S)),
    GridKey(Key.Repeatable("\u2193", icon = Icons.Default.KeyboardArrowDown, keyCode = AndroidKeyEvent.KEYCODE_DPAD_DOWN), GridPosition(638, R5_S, 36, RH_S)),
    GridKey(Key.Repeatable("\u2192", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, keyCode = AndroidKeyEvent.KEYCODE_DPAD_RIGHT), GridPosition(680, R5_S, 36, RH_S)),
)

// ===================== Dimensions =====================

data class QwertyDimensions(
    val fontSize: Int,
    val cornerRadius: Dp,
    val totalHeight: Dp,
)

@Composable
fun getQwertyDimensions(): QwertyDimensions {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp) {
        if (screenWidthDp <= 600) {
            QwertyDimensions(
                fontSize = 11,
                cornerRadius = 4.dp,
                totalHeight = 220.dp,
            )
        } else {
            QwertyDimensions(
                fontSize = 13,
                cornerRadius = 5.dp,
                totalHeight = 240.dp,
            )
        }
    }
}

// ===================== Grid Layout Composable =====================

@Composable
fun KeyboardGrid(
    gridKeys: List<GridKey>,
    cols: Int,
    rows: Int,
    modifier: Modifier = Modifier,
    keyContent: @Composable (GridKey) -> Unit,
) {
    Layout(
        content = {
            gridKeys.forEach { gridKey ->
                Box(modifier = Modifier.layoutId(gridKey.pos)) {
                    keyContent(gridKey)
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val totalHeight = constraints.maxHeight

        val placeables = measurables.map { measurable ->
            val pos = measurable.layoutId as GridPosition
            val x = totalWidth * pos.col / cols
            val x2 = totalWidth * (pos.col + pos.colSpan) / cols
            val y = totalHeight * pos.row / rows
            val y2 = totalHeight * (pos.row + pos.rowSpan) / rows
            val w = (x2 - x).coerceAtLeast(0)
            val h = (y2 - y).coerceAtLeast(0)
            measurable.measure(Constraints.fixed(w, h)) to pos
        }

        layout(totalWidth, totalHeight) {
            placeables.forEach { (placeable, pos) ->
                val x = totalWidth * pos.col / cols
                val y = totalHeight * pos.row / rows
                placeable.place(x, y)
            }
        }
    }
}
