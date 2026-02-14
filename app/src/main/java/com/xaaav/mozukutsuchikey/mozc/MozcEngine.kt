package com.xaaav.mozukutsuchikey.mozc

import android.content.Context
import android.util.Log
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands

class MozcEngine(private val context: Context) {

    private var sessionId: Long = 0
    private var initialized = false

    fun initialize(): Boolean {
        if (initialized) return true

        val dataFile = copyAssetIfNeeded("mozc.data")
            ?: return false

        if (!MozcJNI.initialize()) {
            Log.e(TAG, "MozcJNI.initialize() failed")
            return false
        }

        val userProfileDir = context.filesDir.resolve("mozc_profile").also { it.mkdirs() }
        if (!MozcJNI.onPostLoad(userProfileDir.absolutePath, dataFile.absolutePath)) {
            Log.e(TAG, "MozcJNI.onPostLoad() failed")
            return false
        }

        sessionId = createSession()
        if (sessionId == 0L) {
            Log.e(TAG, "Failed to create Mozc session")
            return false
        }

        initialized = true
        Log.i(TAG, "Mozc initialized: session=$sessionId, dataVersion=${getDataVersion()}")
        return true
    }

    fun getDataVersion(): String = MozcJNI.getDataVersion()

    fun sendKey(keyCode: Int): MozcResult {
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_KEY)
            .setId(sessionId)
            .setKey(
                ProtoCommands.KeyEvent.newBuilder()
                    .setKeyCode(keyCode)
            )
            .build()
        return evalCommand(input)
    }

    fun sendSpecialKey(specialKey: ProtoCommands.KeyEvent.SpecialKey): MozcResult {
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_KEY)
            .setId(sessionId)
            .setKey(
                ProtoCommands.KeyEvent.newBuilder()
                    .setSpecialKey(specialKey)
            )
            .build()
        return evalCommand(input)
    }

    fun selectCandidate(candidateId: Int): MozcResult {
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(
                ProtoCommands.SessionCommand.newBuilder()
                    .setType(ProtoCommands.SessionCommand.CommandType.SELECT_CANDIDATE)
                    .setId(candidateId)
            )
            .build()
        return evalCommand(input)
    }

    fun reset(): MozcResult {
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(
                ProtoCommands.SessionCommand.newBuilder()
                    .setType(ProtoCommands.SessionCommand.CommandType.REVERT)
            )
            .build()
        return evalCommand(input)
    }

    fun destroy() {
        if (sessionId != 0L) {
            val input = ProtoCommands.Input.newBuilder()
                .setType(ProtoCommands.Input.CommandType.DELETE_SESSION)
                .setId(sessionId)
                .build()
            val command = ProtoCommands.Command.newBuilder().setInput(input).build()
            MozcJNI.evalCommand(command.toByteArray())
            sessionId = 0
        }
        initialized = false
    }

    private fun createSession(): Long {
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.CREATE_SESSION)
            .build()
        val command = ProtoCommands.Command.newBuilder().setInput(input).build()
        val resultBytes = MozcJNI.evalCommand(command.toByteArray())
        val result = ProtoCommands.Command.parseFrom(resultBytes)
        return if (result.hasOutput()) result.output.id else 0L
    }

    private fun evalCommand(input: ProtoCommands.Input): MozcResult {
        val command = ProtoCommands.Command.newBuilder().setInput(input).build()
        val resultBytes = MozcJNI.evalCommand(command.toByteArray())
        val result = ProtoCommands.Command.parseFrom(resultBytes)

        if (!result.hasOutput()) {
            return MozcResult(null, null, emptyList(), false)
        }

        val output = result.output
        val composingText = if (output.hasPreedit()) {
            output.preedit.segmentList.joinToString("") { it.value }
        } else null

        val committedText = if (output.hasResult() &&
            output.result.type == ProtoCommands.Result.ResultType.STRING
        ) {
            output.result.value
        } else null

        val candidates = if (output.hasAllCandidateWords()) {
            output.allCandidateWords.candidatesList.map {
                Candidate(it.id, it.value)
            }
        } else {
            emptyList()
        }

        return MozcResult(
            composingText = composingText,
            committedText = committedText,
            candidates = candidates,
            consumed = output.consumed
        )
    }

    private fun copyAssetIfNeeded(assetName: String): java.io.File? {
        val destFile = context.filesDir.resolve(assetName)
        if (destFile.exists()) return destFile

        return try {
            context.assets.open(assetName).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset: $assetName", e)
            null
        }
    }

    companion object {
        private const val TAG = "MozcEngine"
    }
}

data class MozcResult(
    val composingText: String?,
    val committedText: String?,
    val candidates: List<Candidate>,
    val consumed: Boolean
)
