package com.example.legendaryexcuse.brains

import com.example.legendaryexcuse.agents.ExcuseAgents
import com.example.legendaryexcuse.agents.ask
import com.example.legendaryexcuse.agents.runText
import com.example.legendaryexcuse.model.StepId
import com.google.adk.kt.litertlm.LiteRtLmModel
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import android.os.Build
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Gemma 4 E2B running on the phone through LiteRT-LM, wrapped as an ADK model.
 * Owns the ONE engine of the app (two engines would exhaust memory on most phones);
 * every local agent shares it.
 */
class GemmaLocalBrain private constructor(private val engine: Engine, val backend: String) : LocalBrain, AutoCloseable {
  private val model = LiteRtLmModel.create(engine, name = "gemma")
  private val agents = ExcuseAgents.local(model)

  // LiteRT-LM generation blocks the thread, so it runs on the IO dispatcher.
  override suspend fun runJson(step: StepId, input: String): String =
    withContext(Dispatchers.IO) { agents.getValue(step).ask(input) }

  override fun stream(input: String): Flow<String> =
    agents.getValue(StepId.GENERATE).runText(input, streaming = true).flowOn(Dispatchers.IO)

  override fun close() {
    model.close()
    engine.close()
  }

  companion object {
    /**
     * Loads the model, trying the GPU first and falling back to the CPU. Slow: call it off the main thread.
     * A broken GPU driver can kill the process natively (no exception to catch), so we leave a marker
     * file while trying: if it is still there on the next launch, the GPU crashed and we go straight
     * to the CPU. Emulators have no usable GPU delegate, so they always use the CPU.
     */
    fun open(modelFile: File, cacheDir: File): GemmaLocalBrain {
      val gpuMarker = File(cacheDir, "gpu_init_in_progress")
      if (!isEmulator() && !gpuMarker.exists()) {
        gpuMarker.createNewFile()
        val gpu = runCatching { startEngine(modelFile, cacheDir, Backend.GPU()) }.getOrNull()
        gpuMarker.delete()
        if (gpu != null) return GemmaLocalBrain(gpu, "GPU")
      }
      return GemmaLocalBrain(startEngine(modelFile, cacheDir, Backend.CPU()), "CPU")
    }

    private fun isEmulator() = Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish")

    private fun startEngine(modelFile: File, cacheDir: File, backend: Backend): Engine {
      val engine = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = backend, cacheDir = cacheDir.absolutePath))
      try {
        engine.initialize()
      } catch (e: Exception) {
        engine.close()
        throw e
      }
      return engine
    }
  }
}
