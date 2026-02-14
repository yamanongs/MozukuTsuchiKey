package com.xaaav.mozukutsuchikey.clipboard

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClipboardHistory(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val dao = ClipboardDatabase.getInstance(context).clipboardDao()

    val items: StateFlow<List<ClipboardEntity>> = dao.observeAll()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
        val text = clip.getItemAt(0)?.text?.toString()
        if (text.isNullOrBlank()) return@OnPrimaryClipChangedListener

        scope.launch {
            // Clean up expired (1 hour TTL for unpinned)
            dao.deleteExpired(System.currentTimeMillis() - TTL_MS)
            // Remove duplicate then re-insert to update timestamp
            dao.deleteByText(text)
            dao.insert(ClipboardEntity(text = text))
        }
    }

    fun startListening() {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    fun stopListening() {
        clipboardManager.removePrimaryClipChangedListener(listener)
    }

    fun deleteItem(id: Long) {
        scope.launch { dao.delete(id) }
    }

    fun togglePin(item: ClipboardEntity) {
        scope.launch { dao.updatePin(item.id, !item.pinned) }
    }

    companion object {
        private const val TTL_MS = 60 * 60 * 1000L // 1 hour
    }
}
