package io.charlag.sample.reduktsample.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

/**
 * Created by charlag on 02/09/17.
 */

@Database(
        entities = arrayOf(TodoEntity::class),
        version = 1,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDAO
}