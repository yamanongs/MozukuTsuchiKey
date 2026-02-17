package com.xaaav.mozukutsuchikey.flick

/**
 * Flick keyboard data model — character mappings for JP/EN/Number modes.
 * Ported from Sumire (KazumaProject/JapaneseKeyboard) MIT License.
 */

/** A single flick key: tap + 4 directional flick characters */
data class FlickChar(
    val tap: Char?,
    val left: Char? = null,
    val top: Char? = null,
    val right: Char? = null,
    val bottom: Char? = null,
) {
    fun charForDirection(direction: FlickDirection): Char? = when (direction) {
        FlickDirection.TAP -> tap
        FlickDirection.LEFT -> left
        FlickDirection.TOP -> top
        FlickDirection.RIGHT -> right
        FlickDirection.BOTTOM -> bottom
    }
}

enum class FlickDirection {
    TAP, LEFT, TOP, RIGHT, BOTTOM
}

enum class FlickInputMode {
    JAPANESE, ENGLISH, NUMBER;

    fun next(): FlickInputMode = when (this) {
        JAPANESE -> ENGLISH
        ENGLISH -> NUMBER
        NUMBER -> JAPANESE
    }

    val label: String get() = when (this) {
        JAPANESE -> "あ"
        ENGLISH -> "A"
        NUMBER -> "1"
    }
}

/** Tenkey position index (0-11) for the 12 main keys */
enum class TenKeyIndex(val index: Int) {
    KEY_1(0), KEY_2(1), KEY_3(2),
    KEY_4(3), KEY_5(4), KEY_6(5),
    KEY_7(6), KEY_8(7), KEY_9(8),
    KEY_DAKUTEN(9), KEY_11(10), KEY_12(11),
}

// ==================== Japanese Mode ====================

val FLICK_JP = listOf(
    // KEY_1: あ行
    FlickChar(tap = 'あ', left = 'い', top = 'う', right = 'え', bottom = 'お'),
    // KEY_2: か行
    FlickChar(tap = 'か', left = 'き', top = 'く', right = 'け', bottom = 'こ'),
    // KEY_3: さ行
    FlickChar(tap = 'さ', left = 'し', top = 'す', right = 'せ', bottom = 'そ'),
    // KEY_4: た行
    FlickChar(tap = 'た', left = 'ち', top = 'つ', right = 'て', bottom = 'と'),
    // KEY_5: な行
    FlickChar(tap = 'な', left = 'に', top = 'ぬ', right = 'ね', bottom = 'の'),
    // KEY_6: は行
    FlickChar(tap = 'は', left = 'ひ', top = 'ふ', right = 'へ', bottom = 'ほ'),
    // KEY_7: ま行
    FlickChar(tap = 'ま', left = 'み', top = 'む', right = 'め', bottom = 'も'),
    // KEY_8: や行
    FlickChar(tap = 'や', left = '（', top = 'ゆ', right = '）', bottom = 'よ'),
    // KEY_9: ら行
    FlickChar(tap = 'ら', left = 'り', top = 'る', right = 'れ', bottom = 'ろ'),
    // KEY_DAKUTEN: (handled specially — dakuten/handakuten/small)
    FlickChar(tap = null),
    // KEY_11: わ行
    FlickChar(tap = 'わ', left = 'を', top = 'ん', right = 'ー', bottom = '〜'),
    // KEY_12: 記号
    FlickChar(tap = '、', left = '。', top = '？', right = '！', bottom = '…'),
)

// ==================== English Mode ====================

val FLICK_EN = listOf(
    // KEY_1: @#&_
    FlickChar(tap = '@', left = '#', top = '&', right = '_', bottom = '1'),
    // KEY_2: abc
    FlickChar(tap = 'a', left = 'b', top = 'c', right = null, bottom = '2'),
    // KEY_3: def
    FlickChar(tap = 'd', left = 'e', top = 'f', right = null, bottom = '3'),
    // KEY_4: ghi
    FlickChar(tap = 'g', left = 'h', top = 'i', right = null, bottom = '4'),
    // KEY_5: jkl
    FlickChar(tap = 'j', left = 'k', top = 'l', right = null, bottom = '5'),
    // KEY_6: mno
    FlickChar(tap = 'm', left = 'n', top = 'o', right = null, bottom = '6'),
    // KEY_7: pqrs
    FlickChar(tap = 'p', left = 'q', top = 'r', right = 's', bottom = '7'),
    // KEY_8: tuv
    FlickChar(tap = 't', left = 'u', top = 'v', right = null, bottom = '8'),
    // KEY_9: wxyz
    FlickChar(tap = 'w', left = 'x', top = 'y', right = 'z', bottom = '9'),
    // KEY_DAKUTEN: (caps toggle in EN mode)
    FlickChar(tap = null),
    // KEY_11: quotes
    FlickChar(tap = '\'', left = '"', top = ':', right = ';', bottom = '0'),
    // KEY_12: punctuation
    FlickChar(tap = '.', left = ',', top = '?', right = '!', bottom = '-'),
)

// ==================== Number Mode ====================

val FLICK_NUM = listOf(
    FlickChar(tap = '1', left = '☆', top = '♪', right = '→', bottom = null),
    FlickChar(tap = '2', left = '￥', top = '$', right = '€', bottom = null),
    FlickChar(tap = '3', left = '%', top = '°', right = '#', bottom = null),
    FlickChar(tap = '4', left = '○', top = '*', right = '・', bottom = null),
    FlickChar(tap = '5', left = '+', top = '×', right = '÷', bottom = null),
    FlickChar(tap = '6', left = '<', top = '=', right = '>', bottom = null),
    FlickChar(tap = '7', left = '「', top = '」', right = ':', bottom = null),
    FlickChar(tap = '8', left = '〒', top = '々', right = '〆', bottom = null),
    FlickChar(tap = '9', left = '^', top = '|', right = '\\', bottom = null),
    FlickChar(tap = '(', left = ')', top = '[', right = ']', bottom = null),
    FlickChar(tap = '0', left = '~', top = '…', right = '@', bottom = null),
    FlickChar(tap = '.', left = ',', top = '-', right = '/', bottom = null),
)

fun flickKeysForMode(mode: FlickInputMode): List<FlickChar> = when (mode) {
    FlickInputMode.JAPANESE -> FLICK_JP
    FlickInputMode.ENGLISH -> FLICK_EN
    FlickInputMode.NUMBER -> FLICK_NUM
}

/** Label shown on each of the 12 keys */
fun flickKeyLabel(mode: FlickInputMode, index: Int): String {
    if (index == TenKeyIndex.KEY_DAKUTEN.index) {
        return when (mode) {
            FlickInputMode.JAPANESE -> "小゛゜"
            FlickInputMode.ENGLISH -> "A/a"
            FlickInputMode.NUMBER -> "()[]"
        }
    }
    val keys = flickKeysForMode(mode)
    return keys.getOrNull(index)?.tap?.toString() ?: ""
}

// ==================== Toggle cycling (tap same key repeatedly) ====================

/** Japanese toggle: あ→い→う→え→お→ぁ→ぃ→ぅ→ぇ→ぉ→あ */
fun Char.getNextToggleChar(tapChar: Char): Char? = when {
    // あ行
    this == 'あ' && tapChar == 'あ' -> 'い'
    this == 'い' && tapChar == 'あ' -> 'う'
    this == 'う' && tapChar == 'あ' -> 'え'
    this == 'え' && tapChar == 'あ' -> 'お'
    this == 'お' && tapChar == 'あ' -> 'ぁ'
    this == 'ぁ' && tapChar == 'あ' -> 'ぃ'
    this == 'ぃ' && tapChar == 'あ' -> 'ぅ'
    this == 'ぅ' && tapChar == 'あ' -> 'ぇ'
    this == 'ぇ' && tapChar == 'あ' -> 'ぉ'
    this == 'ぉ' && tapChar == 'あ' -> 'あ'
    // か行
    this == 'か' && tapChar == 'か' -> 'き'
    this == 'き' && tapChar == 'か' -> 'く'
    this == 'く' && tapChar == 'か' -> 'け'
    this == 'け' && tapChar == 'か' -> 'こ'
    this == 'こ' && tapChar == 'か' -> 'か'
    // さ行
    this == 'さ' && tapChar == 'さ' -> 'し'
    this == 'し' && tapChar == 'さ' -> 'す'
    this == 'す' && tapChar == 'さ' -> 'せ'
    this == 'せ' && tapChar == 'さ' -> 'そ'
    this == 'そ' && tapChar == 'さ' -> 'さ'
    // た行
    this == 'た' && tapChar == 'た' -> 'ち'
    this == 'ち' && tapChar == 'た' -> 'つ'
    this == 'つ' && tapChar == 'た' -> 'て'
    this == 'て' && tapChar == 'た' -> 'と'
    this == 'と' && tapChar == 'た' -> 'っ'
    this == 'っ' && tapChar == 'た' -> 'た'
    // な行
    this == 'な' && tapChar == 'な' -> 'に'
    this == 'に' && tapChar == 'な' -> 'ぬ'
    this == 'ぬ' && tapChar == 'な' -> 'ね'
    this == 'ね' && tapChar == 'な' -> 'の'
    this == 'の' && tapChar == 'な' -> 'な'
    // は行
    this == 'は' && tapChar == 'は' -> 'ひ'
    this == 'ひ' && tapChar == 'は' -> 'ふ'
    this == 'ふ' && tapChar == 'は' -> 'へ'
    this == 'へ' && tapChar == 'は' -> 'ほ'
    this == 'ほ' && tapChar == 'は' -> 'は'
    // ま行
    this == 'ま' && tapChar == 'ま' -> 'み'
    this == 'み' && tapChar == 'ま' -> 'む'
    this == 'む' && tapChar == 'ま' -> 'め'
    this == 'め' && tapChar == 'ま' -> 'も'
    this == 'も' && tapChar == 'ま' -> 'ま'
    // や行
    this == 'や' && tapChar == 'や' -> 'ゆ'
    this == 'ゆ' && tapChar == 'や' -> 'よ'
    this == 'よ' && tapChar == 'や' -> 'ゃ'
    this == 'ゃ' && tapChar == 'や' -> 'ゅ'
    this == 'ゅ' && tapChar == 'や' -> 'ょ'
    this == 'ょ' && tapChar == 'や' -> 'や'
    // ら行
    this == 'ら' && tapChar == 'ら' -> 'り'
    this == 'り' && tapChar == 'ら' -> 'る'
    this == 'る' && tapChar == 'ら' -> 'れ'
    this == 'れ' && tapChar == 'ら' -> 'ろ'
    this == 'ろ' && tapChar == 'ら' -> 'ら'
    // わ行
    this == 'わ' && tapChar == 'わ' -> 'を'
    this == 'を' && tapChar == 'わ' -> 'ん'
    this == 'ん' && tapChar == 'わ' -> 'ゎ'
    this == 'ゎ' && tapChar == 'わ' -> 'ー'
    this == 'ー' && tapChar == 'わ' -> '〜'
    this == '〜' && tapChar == 'わ' -> 'わ'
    // 記号
    this == '、' && tapChar == '、' -> '。'
    this == '。' && tapChar == '、' -> '？'
    this == '？' && tapChar == '、' -> '！'
    this == '！' && tapChar == '、' -> '…'
    this == '…' && tapChar == '、' -> '・'
    this == '・' && tapChar == '、' -> '、'
    // English toggle
    this == '@' && tapChar == '@' -> '#'
    this == '#' && tapChar == '@' -> '&'
    this == '&' && tapChar == '@' -> '_'
    this == '_' && tapChar == '@' -> '1'
    this == '1' && tapChar == '@' -> '@'

    this == 'a' && tapChar == 'a' -> 'b'
    this == 'b' && tapChar == 'a' -> 'c'
    this == 'c' && tapChar == 'a' -> 'A'
    this == 'A' && tapChar == 'a' -> 'B'
    this == 'B' && tapChar == 'a' -> 'C'
    this == 'C' && tapChar == 'a' -> '2'
    this == '2' && tapChar == 'a' -> 'a'

    this == 'd' && tapChar == 'd' -> 'e'
    this == 'e' && tapChar == 'd' -> 'f'
    this == 'f' && tapChar == 'd' -> 'D'
    this == 'D' && tapChar == 'd' -> 'E'
    this == 'E' && tapChar == 'd' -> 'F'
    this == 'F' && tapChar == 'd' -> '3'
    this == '3' && tapChar == 'd' -> 'd'

    this == 'g' && tapChar == 'g' -> 'h'
    this == 'h' && tapChar == 'g' -> 'i'
    this == 'i' && tapChar == 'g' -> 'G'
    this == 'G' && tapChar == 'g' -> 'H'
    this == 'H' && tapChar == 'g' -> 'I'
    this == 'I' && tapChar == 'g' -> '4'
    this == '4' && tapChar == 'g' -> 'g'

    this == 'j' && tapChar == 'j' -> 'k'
    this == 'k' && tapChar == 'j' -> 'l'
    this == 'l' && tapChar == 'j' -> 'J'
    this == 'J' && tapChar == 'j' -> 'K'
    this == 'K' && tapChar == 'j' -> 'L'
    this == 'L' && tapChar == 'j' -> '5'
    this == '5' && tapChar == 'j' -> 'j'

    this == 'm' && tapChar == 'm' -> 'n'
    this == 'n' && tapChar == 'm' -> 'o'
    this == 'o' && tapChar == 'm' -> 'M'
    this == 'M' && tapChar == 'm' -> 'N'
    this == 'N' && tapChar == 'm' -> 'O'
    this == 'O' && tapChar == 'm' -> '6'
    this == '6' && tapChar == 'm' -> 'm'

    this == 'p' && tapChar == 'p' -> 'q'
    this == 'q' && tapChar == 'p' -> 'r'
    this == 'r' && tapChar == 'p' -> 's'
    this == 's' && tapChar == 'p' -> 'P'
    this == 'P' && tapChar == 'p' -> 'Q'
    this == 'Q' && tapChar == 'p' -> 'R'
    this == 'R' && tapChar == 'p' -> 'S'
    this == 'S' && tapChar == 'p' -> '7'
    this == '7' && tapChar == 'p' -> 'p'

    this == 't' && tapChar == 't' -> 'u'
    this == 'u' && tapChar == 't' -> 'v'
    this == 'v' && tapChar == 't' -> 'T'
    this == 'T' && tapChar == 't' -> 'U'
    this == 'U' && tapChar == 't' -> 'V'
    this == 'V' && tapChar == 't' -> '8'
    this == '8' && tapChar == 't' -> 't'

    this == 'w' && tapChar == 'w' -> 'x'
    this == 'x' && tapChar == 'w' -> 'y'
    this == 'y' && tapChar == 'w' -> 'z'
    this == 'z' && tapChar == 'w' -> 'W'
    this == 'W' && tapChar == 'w' -> 'X'
    this == 'X' && tapChar == 'w' -> 'Y'
    this == 'Y' && tapChar == 'w' -> 'Z'
    this == 'Z' && tapChar == 'w' -> '9'
    this == '9' && tapChar == 'w' -> 'w'

    this == '\'' && tapChar == '\'' -> '"'
    this == '"' && tapChar == '\'' -> ':'
    this == ':' && tapChar == '\'' -> ';'
    this == ';' && tapChar == '\'' -> '0'
    this == '0' && tapChar == '\'' -> '\''

    this == '.' && tapChar == '.' -> ','
    this == ',' && tapChar == '.' -> '?'
    this == '?' && tapChar == '.' -> '!'
    this == '!' && tapChar == '.' -> '-'
    this == '-' && tapChar == '.' -> '.'

    else -> null
}

// ==================== Dakuten / Handakuten / Small ====================

/** 濁点変換 (tap) */
fun Char.getDakutenSmall(): Char? = when (this) {
    'あ' -> 'ぁ'; 'ぁ' -> 'あ'
    'い' -> 'ぃ'; 'ぃ' -> 'い'
    'う' -> 'ぅ'; 'ぅ' -> 'ゔ'; 'ゔ' -> 'う'
    'え' -> 'ぇ'; 'ぇ' -> 'え'
    'お' -> 'ぉ'; 'ぉ' -> 'お'
    'か' -> 'が'; 'が' -> 'か'
    'き' -> 'ぎ'; 'ぎ' -> 'き'
    'く' -> 'ぐ'; 'ぐ' -> 'く'
    'け' -> 'げ'; 'げ' -> 'け'
    'こ' -> 'ご'; 'ご' -> 'こ'
    'さ' -> 'ざ'; 'ざ' -> 'さ'
    'し' -> 'じ'; 'じ' -> 'し'
    'す' -> 'ず'; 'ず' -> 'す'
    'せ' -> 'ぜ'; 'ぜ' -> 'せ'
    'そ' -> 'ぞ'; 'ぞ' -> 'そ'
    'た' -> 'だ'; 'だ' -> 'た'
    'ち' -> 'ぢ'; 'ぢ' -> 'ち'
    'つ' -> 'っ'; 'っ' -> 'づ'; 'づ' -> 'つ'
    'て' -> 'で'; 'で' -> 'て'
    'と' -> 'ど'; 'ど' -> 'と'
    'は' -> 'ば'; 'ば' -> 'ぱ'; 'ぱ' -> 'は'
    'ひ' -> 'び'; 'び' -> 'ぴ'; 'ぴ' -> 'ひ'
    'ふ' -> 'ぶ'; 'ぶ' -> 'ぷ'; 'ぷ' -> 'ふ'
    'へ' -> 'べ'; 'べ' -> 'ぺ'; 'ぺ' -> 'へ'
    'ほ' -> 'ぼ'; 'ぼ' -> 'ぽ'; 'ぽ' -> 'ほ'
    'や' -> 'ゃ'; 'ゃ' -> 'や'
    'ゆ' -> 'ゅ'; 'ゅ' -> 'ゆ'
    'よ' -> 'ょ'; 'ょ' -> 'よ'
    'わ' -> 'ゎ'; 'ゎ' -> 'わ'
    // English: toggle case
    in 'a'..'z' -> this.uppercaseChar()
    in 'A'..'Z' -> this.lowercaseChar()
    else -> null
}

/** 濁点 (flick left on dakuten key) */
fun Char.getDakuten(): Char? = when (this) {
    'う', 'ぅ' -> 'ゔ'
    'か' -> 'が'; 'き' -> 'ぎ'; 'く' -> 'ぐ'; 'け' -> 'げ'; 'こ' -> 'ご'
    'さ' -> 'ざ'; 'し' -> 'じ'; 'す' -> 'ず'; 'せ' -> 'ぜ'; 'そ' -> 'ぞ'
    'た' -> 'だ'; 'ち' -> 'ぢ'; 'つ', 'っ' -> 'づ'; 'て' -> 'で'; 'と' -> 'ど'
    'は', 'ぱ' -> 'ば'; 'ひ', 'ぴ' -> 'び'; 'ふ', 'ぷ' -> 'ぶ'
    'へ', 'ぺ' -> 'べ'; 'ほ', 'ぽ' -> 'ぼ'
    else -> null
}

/** 半濁点 (flick right on dakuten key) */
fun Char.getHandakuten(): Char? = when (this) {
    'は', 'ば' -> 'ぱ'; 'ひ', 'び' -> 'ぴ'; 'ふ', 'ぶ' -> 'ぷ'
    'へ', 'べ' -> 'ぺ'; 'ほ', 'ぼ' -> 'ぽ'
    else -> null
}

/** 小文字 (flick top on dakuten key) */
fun Char.getSmallChar(): Char? = when (this) {
    'あ' -> 'ぁ'; 'い' -> 'ぃ'; 'う', 'ゔ' -> 'ぅ'; 'え' -> 'ぇ'; 'お' -> 'ぉ'
    'つ', 'づ' -> 'っ'; 'や' -> 'ゃ'; 'ゆ' -> 'ゅ'; 'よ' -> 'ょ'; 'わ' -> 'ゎ'
    else -> null
}

fun Char.isHiragana(): Boolean = this in '\u3040'..'\u309F' || this == 'ゔ'
