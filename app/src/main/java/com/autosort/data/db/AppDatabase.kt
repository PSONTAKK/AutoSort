package com.autosort.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autosort.data.model.DestinationType
import com.autosort.data.model.LogStatus
import com.autosort.data.model.Rule
import com.autosort.data.model.RuleType
import com.autosort.data.model.SortLog
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)

    @TypeConverter
    fun fromLogStatus(value: LogStatus): String = value.name

    @TypeConverter
    fun toLogStatus(value: String): LogStatus = LogStatus.valueOf(value)

    @TypeConverter
    fun fromDestinationType(value: DestinationType): String = value.name

    @TypeConverter
    fun toDestinationType(value: String): DestinationType = DestinationType.valueOf(value)
}

@Database(
    entities = [Rule::class, SortLog::class],
    version = 3, // Bumped version to trigger recreation if needed
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_KEY_PREFS = "db_encryption_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"

        /**
         * Generates or retrieves a 256-bit random passphrase stored securely in
         * EncryptedSharedPreferences (backed by Android Keystore).
         */
        private fun getDatabasePassphrase(context: Context): ByteArray {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                DB_KEY_PREFS,
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            var passphraseStr = prefs.getString(KEY_DB_PASSPHRASE, null)
            if (passphraseStr == null) {
                // Generate a random 256-bit passphrase
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                passphraseStr = android.util.Base64.encodeToString(randomBytes, android.util.Base64.DEFAULT)
                prefs.edit().putString(KEY_DB_PASSPHRASE, passphraseStr).apply()
            }
            return android.util.Base64.decode(passphraseStr, android.util.Base64.DEFAULT)
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Fix 5: Use SQLCipher SupportFactory with a secure random key
                val passphrase = getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)

                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosort_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration() // Existing plain-text DB will be destroyed and recreated as encrypted
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
