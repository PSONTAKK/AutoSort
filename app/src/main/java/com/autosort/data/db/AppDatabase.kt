package com.autosort.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.autosort.data.model.DestinationType
import com.autosort.data.model.LogStatus
import com.autosort.data.model.Rule
import com.autosort.data.model.RuleType
import com.autosort.data.model.SortLog
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

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
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao

    companion object {
        private const val TAG = "AppDatabase"

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
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                passphraseStr = android.util.Base64.encodeToString(randomBytes, android.util.Base64.DEFAULT)
                prefs.edit().putString(KEY_DB_PASSPHRASE, passphraseStr).apply()
            }
            return android.util.Base64.decode(passphraseStr, android.util.Base64.DEFAULT)
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Tries SQLCipher encrypted DB first. If the native library crashes or
         * fails to load on certain devices (e.g. some MIUI/MediaTek devices),
         * falls back to a standard unencrypted Room database so the app still works.
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return try {
                // Test that SQLCipher native library can load
                System.loadLibrary("sqlcipher")

                val passphrase = getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)

                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosort_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        // Verify the DB actually opens (catches header mismatch)
                        it.openHelper.writableDatabase
                        Log.i(TAG, "Using SQLCipher encrypted database")
                    }
            } catch (e: Throwable) {
                // SQLCipher failed — native lib missing, UnsatisfiedLinkError,
                // or device-specific incompatibility. Fall back to plain Room.
                Log.w(TAG, "SQLCipher unavailable on this device, using standard database: ${e.message}")

                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autosort.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}
