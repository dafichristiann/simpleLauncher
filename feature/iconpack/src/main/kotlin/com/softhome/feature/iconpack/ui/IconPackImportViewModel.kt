package com.softhome.feature.iconpack.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softhome.core.model.DiscoveredIconPack
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.feature.iconpack.data.ImportProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the icon-pack import sheet. */
data class IconPackImportUiState(
    val activePack: IconPack? = null,
    val installed: List<DiscoveredIconPack> = emptyList(),
    val scanning: Boolean = false,
    val progress: ImportProgress? = null,
    /** Transient user-facing message (success/failure) shown in the sheet. */
    val message: String? = null,
) {
    val importing: Boolean
        get() = progress is ImportProgress.Validating || progress is ImportProgress.Indexing
}

@HiltViewModel
class IconPackImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IconPackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IconPackImportUiState())
    val state: StateFlow<IconPackImportUiState> = _state.asStateFlow()

    init {
        refreshInstalled()
        viewModelScope.launch {
            repository.activePack.collect { pack ->
                _state.value = _state.value.copy(activePack = pack)
            }
        }
    }

    fun refreshInstalled() {
        viewModelScope.launch {
            _state.value = _state.value.copy(scanning = true)
            val packs = repository.scanInstalled()
            _state.value = _state.value.copy(installed = packs, scanning = false)
        }
    }

    /** Import a `.zip` the user picked; [uri] is read through the content resolver. */
    fun importZip(uri: Uri) {
        val name = queryDisplayName(uri) ?: "icon_pack.zip"
        viewModelScope.launch {
            val stream = try {
                context.contentResolver.openInputStream(uri)
            } catch (_: Throwable) {
                null
            }
            if (stream == null) {
                _state.value = _state.value.copy(message = "Could not open that file.")
                return@launch
            }
            repository.importZipStream(stream, name).collect { emitProgress(it) }
        }
    }

    fun applyInstalled(pack: DiscoveredIconPack) {
        viewModelScope.launch {
            _state.value = _state.value.copy(message = "Reading ${pack.label}...")
            val result = repository.applyInstalled(pack)
            _state.value = when (result) {
                is IconPackParseResult.Success ->
                    _state.value.copy(message = "Applied ${result.pack.name} (${result.pack.iconCount} icons)")
                is IconPackParseResult.NoAppFilter ->
                    _state.value.copy(message = "${pack.label} has no appfilter; icons will be masked.")
                is IconPackParseResult.Invalid ->
                    _state.value.copy(message = "Could not read ${pack.label}: ${result.reason}")
            }
        }
    }

    fun clearPack() {
        viewModelScope.launch {
            repository.clearActive()
            _state.value = _state.value.copy(message = "Icon pack cleared. Icons are masked.")
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun emitProgress(progress: ImportProgress) {
        val current = _state.value
        _state.value = when (progress) {
            is ImportProgress.Done ->
                current.copy(progress = progress, message = "Imported ${progress.pack.name} (${progress.pack.drawableCount} icons)")
            is ImportProgress.Failed ->
                current.copy(progress = null, message = "Import failed: ${progress.reason}")
            else -> current.copy(progress = progress)
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Throwable) {
        null
    }
}
