package com.personx.hermatic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personx.hermatic.data.model.ChatMessageEntity
import com.personx.hermatic.security.SecurityManager
import net.sqlcipher.database.SupportFactory

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: HermesDatabase? = null

        fun getDatabase(context: Context, securityManager: SecurityManager): HermesDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
                
                val passphrase = securityManager.getDatabasePassphrase()
                val factory = SupportFactory(passphrase)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HermesDatabase::class.java,
                    "hermes_database"
                )
                .openHelperFactory(factory)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
