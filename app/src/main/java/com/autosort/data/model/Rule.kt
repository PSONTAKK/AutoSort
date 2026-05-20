package com.autosort.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RuleType {
    CONTAINS,
    ENDS,
    STARTS,
    REGEX
}

/**
 * Where matched files should be sent.
 * Add new entries here as you implement new FileDestination providers.
 */
enum class DestinationType {
    LOCAL,           // Local folder on device
    CLOUD_GDRIVE,    // Google Drive (future)
    CLOUD_DROPBOX,   // Dropbox (future)
    CLOUD_S3         // AWS S3 (future)
}

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: RuleType,
    val value: String,
    val target: String,
    val destinationType: DestinationType = DestinationType.LOCAL,
    val active: Boolean = true,
    val sourceFolder: String? = null,
    // V2: If true, keep local file after uploading to Drive (only for CLOUD_GDRIVE)
    val keepLocalAfterUpload: Boolean = false,
    // V2: Support multiple Google accounts (which account to use for Drive)
    val targetAccount: String? = null
)
