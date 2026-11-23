package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [ApiEndpointEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apiEndpointDao(): ApiEndpointDao

    companion object {
        private const val DATABASE_NAME = "api_manager_vault.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE api_endpoints ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''"
                )
                val cursor = db.query("SELECT id FROM api_endpoints")
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val rowId = c.getLong(0)
                        val uuid = UUID.randomUUID().toString()
                        db.execSQL(
                            "UPDATE api_endpoints SET remoteId = ? WHERE id = ?",
                            arrayOf<Any>(uuid, rowId)
                        )
                    }
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // null = no expiry. Room stores Long? as a nullable INTEGER column.
                db.execSQL("ALTER TABLE api_endpoints ADD COLUMN expiresAtMillis INTEGER")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
