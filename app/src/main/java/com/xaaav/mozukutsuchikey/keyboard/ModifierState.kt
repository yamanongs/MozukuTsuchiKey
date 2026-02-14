package com.xaaav.mozukutsuchikey.keyboard

enum class ModifierLevel {
    OFF,
    TRANSIENT,
    LOCKED
}

enum class ModifierType(val label: String) {
    CTRL("Ctrl"),
    ALT("Alt"),
    SHIFT("\u21E7"),  // ⇧
}
