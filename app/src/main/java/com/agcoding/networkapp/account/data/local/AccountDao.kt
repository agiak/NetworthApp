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

    // Leaves monthlySalary untouched so editing an account never resets the salary
    @Query("UPDATE accounts SET name = :name, startingBalance = :startingBalance, colorHex = :colorHex WHERE id = :id")
    suspend fun updateAccountDetails(id: Long, name: String, startingBalance: Double, colorHex: String)

    @Query("UPDATE accounts SET monthlySalary = :salary WHERE id = :id")
    suspend fun updateMonthlySalary(id: Long, salary: Double)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)
}
