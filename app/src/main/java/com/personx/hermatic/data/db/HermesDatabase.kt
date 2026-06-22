package com.personx.hermatic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personx.hermatic.data.model.ChatMessageEntity
import com.personx.hermatic.security.SecurityManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [ChatMessageEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: HermesDatabase? = null

        fun getDatabase(context: Context, securityManager: SecurityManager): HermesDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher
                System.loadLibrary("sqlcipher")
                
                val passphrase = securityManager.getDatabasePassphrase()
                val factory = SupportOpenHelperFactory(passphrase)
                
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
