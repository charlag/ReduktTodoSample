package io.charlag.sample.reduktsample.data.db

import android.arch.persistence.room.*
import io.reactivex.Flowable

/**
 * Created by charlag on 02/09/17.
 */

@Dao
interface TodoDAO {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun add(todo: TodoEntity)

    @Query("UPDATE todo SET completed = :completed WHERE id == :id")
    fun markCompleted(id: Long, completed: Boolean)

    @Query("SELECT * FROM todo")
    fun getAll(): Flowable<List<TodoEntity>>
}