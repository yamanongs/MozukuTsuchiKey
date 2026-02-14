package com.xaaav.mozukutsuchikey.mozc

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent.SpecialKey

class MozcInputController(private val context: Context) {

    private var engine: MozcEngine? = null
    private var initAttempted = false

    var composingText by mutableStateOf("")
        private set

    var candidates by mutableStateOf<List<Candidate>>(emptyList())
        private set

    val isComposing: Boolean get() = composingText.isNotEmpty()

    var onCommit: ((String) -> Unit)? = null

    fun ensureInitialized(): Boolean {
        if (engine != null) return true
        if (initAttempted) return false

        initAttempted = true
        val e = MozcEngine(context)
        return if (e.initialize()) {
            engine = e
            Log.i(TAG, "MozcInputController initialized")
            true
        } else {
            Log.e(TAG, "MozcEngine initialization failed")
            false
        }
    }

    fun handleCodePoint(codePoint: Int): Boolean {
        val e = engine ?: return false
        val result = e.sendKey(codePoint)
        applyResult(result)
        return result.consumed
    }

    fun handleSpecialKey(specialKey: SpecialKey): Boolean {
        val e = engine ?: return false
        val result = e.sendSpecialKey(specialKey)
        applyResult(result)
        return result.consumed
    }

    fun selectCandidate(candidateId: Int): Boolean {
        val e = engine ?: return false
        val result = e.selectCandidate(candidateId)
        applyResult(result)
        return result.consumed
    }

    fun reset() {
        val e = engine ?: return
        val result = e.reset()
        applyResult(result)
    }

    fun destroy() {
        engine?.destroy()
        engine = null
        initAttempted = false
        composingText = ""
        candidates = emptyList()
    }

    private fun applyResult(result: MozcResult) {
        composingText = result.composingText ?: ""
        candidates = result.candidates
        if (result.committedText != null) {
            onCommit?.invoke(result.committedText)
        }
    }

    companion object {
        private const val TAG = "MozcInputController"
    }
}
