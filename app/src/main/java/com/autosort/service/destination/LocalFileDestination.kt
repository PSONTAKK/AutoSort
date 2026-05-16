package com.autosort.service.destination

import android.util.Log
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Moves files to a local directory on the device.
 * Handles directory creation, name collision, and atomic-move fallback.
 */
class LocalFileDestination : FileDestination {

    companion object {
        private const val TAG = "LocalFileDestination"
    }

    override suspend fun send(
        sourceFile: File,
        targetPath: String,
        fileName: String
    ): Result<String> = runCatching {

        val targetDir = File(targetPath)

        // Create target directory if it doesn't exist
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // Resolve destination, handle name collision
        val destFile = resolveDestination(targetDir, fileName)
        val srcPath  = sourceFile.toPath()
        val destPath = destFile.toPath()

        try {
            Files.move(srcPath, destPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Log.w(TAG, "Atomic move not supported, falling back to copy+delete: ${e.message}")
            Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING)
            Files.delete(srcPath)
        }

        Log.i(TAG, "Moved '${sourceFile.name}' -> '${destFile.absolutePath}'")
        destFile.absolutePath
    }

    /**
     * If [targetDir]/[fileName] already exists, appends _<timestamp> before
     * the file extension to avoid overwriting.
     *
     * Example: report.pdf -> report_1715870400000.pdf
     */
    private fun resolveDestination(targetDir: File, fileName: String): File {
        val candidate = File(targetDir, fileName)
        if (!candidate.exists()) return candidate

        val timestamp = System.currentTimeMillis()
        val dotIndex  = fileName.lastIndexOf('.')
        val newName   = if (dotIndex >= 0) {
            "${fileName.substring(0, dotIndex)}_$timestamp${fileName.substring(dotIndex)}"
        } else {
            "${fileName}_$timestamp"
        }
        return File(targetDir, newName)
    }
}
