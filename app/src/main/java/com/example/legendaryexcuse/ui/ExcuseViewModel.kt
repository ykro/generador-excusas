package com.example.legendaryexcuse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.legendaryexcuse.R
import com.example.legendaryexcuse.brains.GeminiCloudBrain
import com.example.legendaryexcuse.brains.GemmaLocalBrain
import com.example.legendaryexcuse.model.ExcuseResult
import com.example.legendaryexcuse.model.ModelDownloader
import com.example.legendaryexcuse.model.ModelState
import com.example.legendaryexcuse.model.PipelineStep
import com.example.legendaryexcuse.model.RoutingMode
import com.example.legendaryexcuse.model.Tone
import com.example.legendaryexcuse.model.initialSteps
import com.example.legendaryexcuse.pipeline.Deanonymizer
import com.example.legendaryexcuse.pipeline.ExcusePipeline
import com.example.legendaryexcuse.pipeline.PromptLoader
import com.example.legendaryexcuse.util.Connectivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Settings(
  val defaultTone: Tone = Tone.AUTO,
  val routing: RoutingMode = RoutingMode.AUTO,
)

data class UiState(
  val model: ModelState = ModelState.Initializing,
  val backend: String? = null,
  val isOnline: Boolean = false,
  val input: String = "",
  val tone: Tone = Tone.AUTO,
  val steps: List<PipelineStep> = emptyList(),
  val result: ExcuseResult? = null,
  val isRunning: Boolean = false,
  val error: String? = null,
  val settings: Settings = Settings(),
)

/**
 * Holds the UI state and wires the pieces together (manual dependency container, no Hilt).
 * Settings live in memory on purpose to keep the demo small. Nothing here is written to disk or logs.
 */
class ExcuseViewModel(private val app: Application) : AndroidViewModel(app) {
  private val _state = MutableStateFlow(UiState())
  val state: StateFlow<UiState> = _state

  private val downloader = ModelDownloader(app.filesDir)
  private val connectivity = Connectivity(app)
  private val cloud by lazy { GeminiCloudBrain() }
  private val prompts = PromptLoader { name -> app.assets.open("prompts/$name.txt").bufferedReader().use { it.readText() } }
  private val deanonymizer = Deanonymizer(
    mapOf(
      "PERSON" to app.getString(R.string.generic_person),
      "PLACE" to app.getString(R.string.generic_place),
      "COMPANY" to app.getString(R.string.generic_company),
      "DATE" to app.getString(R.string.generic_date),
      "OTHER" to app.getString(R.string.generic_other),
    ),
  )
  private var local: GemmaLocalBrain? = null
  private var pipeline: ExcusePipeline? = null
  private var job: Job? = null

  init {
    viewModelScope.launch { connectivity.isOnline.collect { online -> _state.update { it.copy(isOnline = online) } } }
    prepareModel()
  }

  // ---- Local model lifecycle ----

  fun prepareModel() {
    if (downloader.isDownloaded()) initializeModel() else startDownload()
  }

  private fun startDownload() {
    val missing = downloader.missingSpaceBytes()
    if (missing > 0) return _state.update { it.copy(model = ModelState.NotEnoughSpace(missing)) }
    job = viewModelScope.launch {
      _state.update { it.copy(model = ModelState.Downloading(0, ModelDownloader.MODEL_SIZE_BYTES)) }
      try {
        var lastMb = -1L
        downloader.download { done, total ->
          if (done shr 20 != lastMb) {
            lastMb = done shr 20
            _state.update { it.copy(model = ModelState.Downloading(done, total)) }
          }
        }
        initializeModel()
      } catch (e: CancellationException) {
        _state.update { it.copy(model = ModelState.NotDownloaded) }
        throw e
      } catch (e: Exception) {
        _state.update { it.copy(model = ModelState.Failed(e.message ?: e.javaClass.simpleName)) }
      }
    }
  }

  fun cancelDownload() {
    job?.cancel()
  }

  private fun initializeModel() {
    _state.update { it.copy(model = ModelState.Initializing) }
    viewModelScope.launch {
      try {
        val brain = withContext(Dispatchers.IO) { GemmaLocalBrain.open(downloader.modelFile, app.cacheDir) }
        local = brain
        pipeline = ExcusePipeline(brain, cloud, prompts, deanonymizer)
        _state.update { it.copy(model = ModelState.Ready(brain.backend), backend = brain.backend) }
      } catch (e: Exception) {
        _state.update { it.copy(model = ModelState.Failed(e.message ?: e.javaClass.simpleName)) }
      }
    }
  }

  fun deleteModel() {
    job?.cancel()
    local?.close()
    local = null
    pipeline = null
    downloader.delete()
    _state.update { it.copy(model = ModelState.NotDownloaded, backend = null, steps = emptyList(), result = null) }
  }

  // ---- Main screen ----

  fun onInputChange(text: String) = _state.update { it.copy(input = text.take(MAX_INPUT_CHARS)) }

  fun onToneChange(tone: Tone) = _state.update { it.copy(tone = tone) }

  fun generate() {
    val pipeline = pipeline ?: return
    val current = _state.value
    if (current.input.isBlank() || current.isRunning) return
    _state.update { it.copy(isRunning = true, steps = initialSteps(), result = null, error = null) }
    job = viewModelScope.launch {
      try {
        val result = pipeline.run(current.input, current.tone, current.settings.routing, current.isOnline) { step ->
          _state.update { s -> s.copy(steps = s.steps.map { if (it.id == step.id) step else it }) }
        }
        _state.update { it.copy(result = result) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
      } finally {
        _state.update { it.copy(isRunning = false) }
      }
    }
  }

  /** Cancels the running pipeline; steps keep the state they reached. */
  fun cancel() {
    job?.cancel()
  }

  // ---- Settings ----

  fun setDefaultTone(tone: Tone) = _state.update { it.copy(settings = it.settings.copy(defaultTone = tone), tone = tone) }

  fun setRouting(routing: RoutingMode) = _state.update { it.copy(settings = it.settings.copy(routing = routing)) }

  override fun onCleared() {
    local?.close()
  }

  companion object {
    const val MAX_INPUT_CHARS = 1000
  }
}
