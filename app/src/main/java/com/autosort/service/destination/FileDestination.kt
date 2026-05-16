package com.autosort.service.destination

import java.io.File

/**
 * Pluggable destination for sorted files.
 *
 * Implement this interface to add new target types:
 *   - LocalFileDestination  (moves to a local folder)
 *   - GoogleDriveDestination (uploads to Drive — future)
 *   - DropboxDestination     (uploads to Dropbox — future)
 *   - S3Destination          (uploads to AWS S3 — future)
 *
 * Each implementation is registered in AutoSortService's destination
 * registry, keyed by [DestinationType]. The [FileSortEngine] routes
 * matched files to the correct implementation automatically.
 */
interface FileDestination {

    /**
     * Transfer [sourceFile] to the given [targetPath] with [fileName].
     *
     * @param sourceFile  The file to move/upload (guaranteed to exist).
     * @param targetPath  The destination path/identifier (local path, cloud folder ID, etc.).
     * @param fileName    The desired filename at the destination.
     * @return [Result] containing the final destination path/URL on success,
     *         or the exception on failure.
     */
    suspend fun send(sourceFile: File, targetPath: String, fileName: String): Result<String>
}
