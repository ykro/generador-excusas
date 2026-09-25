package com.example.legendaryexcuse.model

import android.os.StatFs
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Downloads the Gemma file into filesDir/models/. A file copied with `adb push` skips the download. */
class ModelDownloader(filesDir: File) {
  val modelFile = File(File(filesDir, "models"), MODEL_FILE_NAME)
  private val partialFile = File(modelFile.parentFile, "$MODEL_FILE_NAME.part")

  fun isDownloaded(): Boolean = modelFile.exists() && modelFile.length() == MODEL_SIZE_BYTES

  /** Bytes missing on disk to fit the model (0 if it fits). */
  fun missingSpaceBytes(): Long {
    modelFile.parentFile!!.mkdirs()
    val free = StatFs(modelFile.parentFile!!.path).availableBytes
    return (MODEL_SIZE_BYTES - partialFile.length() + SPACE_MARGIN_BYTES - free).coerceAtLeast(0)
  }

  /** Resumable download; cancel the calling coroutine to stop it. */
  suspend fun download(onProgress: (downloaded: Long, total: Long) -> Unit) = withContext(Dispatchers.IO) {
    modelFile.parentFile!!.mkdirs()
    val alreadyHave = partialFile.length()
    val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
    if (alreadyHave > 0) connection.setRequestProperty("Range", "bytes=$alreadyHave-")
    connection.connect()
    val append = connection.responseCode == HttpURLConnection.HTTP_PARTIAL
    var downloaded = if (append) alreadyHave else 0L
    connection.inputStream.use { input ->
      java.io.FileOutputStream(partialFile, append).use { output ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
          ensureActive()
          val read = input.read(buffer)
          if (read < 0) break
          output.write(buffer, 0, read)
          downloaded += read
          onProgress(downloaded, MODEL_SIZE_BYTES)
        }
      }
    }
    check(partialFile.renameTo(modelFile)) { "Could not move the downloaded model" }
  }

  fun delete() {
    modelFile.delete()
    partialFile.delete()
  }

  companion object {
    const val MODEL_NAME = "Gemma 4 E2B"
    const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val MODEL_SIZE_BYTES = 2_588_147_712L
    const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/$MODEL_FILE_NAME"
    private const val SPACE_MARGIN_BYTES = 200L * 1024 * 1024
  }
}
