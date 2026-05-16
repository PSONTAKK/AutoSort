package com.autosort.service.destination

import android.util.Log
import android.webkit.MimeTypeMap
import com.autosort.data.auth.GoogleAuthManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import java.io.File

/**
 * Uploads files to Google Drive. Implements FileDestination so it
 * plugs into the engine with zero coupling.
 *
 * targetPath is treated as a slash-separated folder path in Drive,
 * e.g. "AutoSort/PDFs" → creates/finds "AutoSort" then "PDFs" inside it.
 */
class GoogleDriveDestination(
    private val authManager: GoogleAuthManager
) : FileDestination {

    companion object {
        private const val TAG = "GoogleDriveDest"
        private const val APP_NAME = "AutoSort"
    }

    override suspend fun send(
        sourceFile: File,
        targetPath: String,
        fileName: String
    ): Result<String> = runCatching {

        Log.i(TAG, "send() called: file='$fileName' target='$targetPath' signed_in=${authManager.isSignedIn}")

        val credential = authManager.getCredential()
            ?: throw IllegalStateException("Not signed in to Google. Please sign in from Settings.")

        val driveService = buildDriveService(credential)

        // Find or create the target folder chain
        val folderId = findOrCreateFolderChain(driveService, targetPath)

        // Detect MIME type
        val extension = fileName.substringAfterLast('.', "")
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        // Upload file
        val fileMetadata = DriveFile().apply {
            name = fileName
            parents = listOf(folderId)
        }

        val mediaContent = FileContent(mimeType, sourceFile)

        val uploadedFile = driveService.files()
            .create(fileMetadata, mediaContent)
            .setFields("id, webViewLink")
            .execute()

        // Delete local source file after successful upload
        if (sourceFile.exists()) {
            sourceFile.delete()
        }

        val link = uploadedFile.webViewLink ?: "drive://file/${uploadedFile.id}"
        Log.i(TAG, "Uploaded '$fileName' to Drive: $link")
        link
    }

    /**
     * Builds a Drive service instance from the signed-in credential.
     */
    private fun buildDriveService(credential: GoogleAccountCredential): Drive {
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }

    /**
     * Takes a path like "AutoSort/PDFs/2024" and creates or finds each
     * folder in the chain, returning the final folder's ID.
     *
     * If targetPath is empty, files go to root.
     */
    private fun findOrCreateFolderChain(driveService: Drive, path: String): String {
        if (path.isBlank()) return "root"

        val segments = path.trim('/').split('/')
        var parentId = "root"

        for (segment in segments) {
            parentId = findOrCreateFolder(driveService, segment, parentId)
        }

        return parentId
    }

    /**
     * Finds a folder by name under the given parent, or creates it.
     */
    private fun findOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentId: String
    ): String {
        // Search for existing folder
        val query = "name = '$folderName' and " +
                "'$parentId' in parents and " +
                "mimeType = 'application/vnd.google-apps.folder' and " +
                "trashed = false"

        val result = driveService.files()
            .list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .setPageSize(1)
            .execute()

        val existing = result.files
        if (existing != null && existing.isNotEmpty()) {
            return existing[0].id
        }

        // Create new folder
        val folderMetadata = DriveFile().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(parentId)
        }

        val folder = driveService.files()
            .create(folderMetadata)
            .setFields("id")
            .execute()

        Log.i(TAG, "Created Drive folder: $folderName (${folder.id})")
        return folder.id
    }
}
