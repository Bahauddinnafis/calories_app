package com.nafis.calories.data.local.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlin.time.Clock

/** JPEG files under `<baseDir>/history_images`, referenced by the relative path stored in Room. */
class ImageStore(
    private val baseDir: String,
    private val clock: Clock,
    private val fileSystem: FileSystem = SystemFileSystem
) {

    suspend fun save(jpeg: ByteArray): String? = io {
        val directory = Path(baseDir, DIRECTORY)
        fileSystem.createDirectories(directory)
        val fileName = "${clock.now().toEpochMilliseconds()}.jpg"
        fileSystem.sink(Path(directory, fileName)).buffered().use { it.write(jpeg) }
        "$DIRECTORY/$fileName"
    }

    suspend fun load(relativePath: String): ByteArray? = io {
        val file = Path(baseDir, relativePath)
        if (!fileSystem.exists(file)) return@io null
        fileSystem.source(file).buffered().use { it.readByteArray() }
    }

    suspend fun delete(relativePath: String): Boolean = io {
        fileSystem.delete(Path(baseDir, relativePath), mustExist = false)
        true
    } ?: false

    suspend fun deleteAll(): Int = io {
        val directory = Path(baseDir, DIRECTORY)
        if (!fileSystem.exists(directory)) return@io 0
        fileSystem.list(directory).count { file -> runCatching { fileSystem.delete(file) }.isSuccess }
    } ?: 0

    private suspend fun <T> io(block: () -> T?): T? = withContext(Dispatchers.IO) {
        runCatching(block).getOrNull()
    }

    private companion object {
        const val DIRECTORY = "history_images"
    }
}
