package com.agcoding.networkapp.account.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    fun getAccountCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCountOnce(): Int

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)
}
