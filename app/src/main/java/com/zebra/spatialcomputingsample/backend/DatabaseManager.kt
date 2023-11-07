package com.zebra.spatialcomputingsample.backend

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Product::class],
    version = 1,
    exportSchema = false
)

abstract class DatabaseManager : RoomDatabase() {

    abstract fun getProductDao(): ProductDao

    companion object {
        private var mDatabaseManager: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager? {
            if (mDatabaseManager == null) {
                synchronized(DatabaseManager::class) {
                    mDatabaseManager = Room.databaseBuilder(
                        context.applicationContext,
                        DatabaseManager::class.java,
                        "zebraspatialcomputing.db"
                    )
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return mDatabaseManager
        }

        fun destroyInstance() {
            mDatabaseManager = null
        }
    }
}