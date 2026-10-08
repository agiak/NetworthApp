package com.agcoding.networkapp.expenseanalysis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryFeedbackDao {

    @Query("SELECT * FROM expense_category_feedback")
    fun getAll(): Flow<List<CategoryFeedbackEntity>>

    @Query("SELECT * FROM expense_category_feedback")
    suspend fun getAllOnce(): List<CategoryFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryFeedbackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CategoryFeedbackEntity>)

    @Query("DELETE FROM expense_category_feedback WHERE nameKey = :nameKey")
    suspend fun delete(nameKey: String)

    @Query("DELETE FROM expense_category_feedback")
    suspend fun deleteAll()
}
