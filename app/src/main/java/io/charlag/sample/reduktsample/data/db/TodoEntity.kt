package io.charlag.sample.reduktsample.data.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by charlag on 02/09/17.
 */

@Entity(tableName = "todo")
data class TodoEntity(
        @PrimaryKey(autoGenerate = true) var id: Long,
        var text: String,
        var completed: Boolean
)