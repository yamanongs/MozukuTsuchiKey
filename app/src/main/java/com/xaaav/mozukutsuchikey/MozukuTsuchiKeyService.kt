package com.xaaav.mozukutsuchikey

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xaaav.mozukutsuchikey.clipboard.ClipboardHistory
import com.xaaav.mozukutsuchikey.keyboard.ImeKeyboard
import com.xaaav.mozukutsuchikey.mozc.MozcInputController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MozukuTsuchiKeyService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var mozcController: MozcInputController
    private lateinit var clipboardHistory: ClipboardHistory
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _inputActive = MutableStateFlow(true)
    val inputActive: StateFlow<Boolean> = _inputActive.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mozcController = MozcInputController(this)
        clipboardHistory = ClipboardHistory(this, serviceScope)
        clipboardHistory.startListening()
        serviceScope.launch(Dispatchers.IO) {
            mozcController.ensureInitialized()
        }
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        _inputActive.value = true
    }

    override fun onFinishInput() {
        _inputActive.value = false
        super.onFinishInput()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        _inputActive.value = true
    }

    override fun onWindowHidden() {
        _inputActive.value = false
        super.onWindowHidden()
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Set ViewTree owners on the IME window's decorView so all child views inherit them
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle)
            )
            setContent {
                ImeKeyboard(
                    inputConnection = { currentInputConnection },
                    mozcController = mozcController,
                    clipboardHistory = clipboardHistory,
                    inputActive = inputActive,
                )
            }
        }
        return composeView
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        clipboardHistory.stopListening()
        mozcController.destroy()
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }
}
