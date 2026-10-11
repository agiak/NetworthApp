package com.agcoding.networkapp.fixedexpenses.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpenseDao {

    @Query("SELECT * FROM fixed_expenses ORDER BY id DESC")
    fun getAll(): Flow<List<FixedExpenseEntity>>

    @Query("SELECT * FROM fixed_expenses ORDER BY id DESC")
    suspend fun getAllOnce(): List<FixedExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FixedExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FixedExpenseEntity>)

    /** Insert that returns the new id (used by cloud sync). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturningId(entity: FixedExpenseEntity): Long

    @Update
    suspend fun update(entity: FixedExpenseEntity)

    @Query("DELETE FROM fixed_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fixed_expenses")
    suspend fun deleteAll()
}
