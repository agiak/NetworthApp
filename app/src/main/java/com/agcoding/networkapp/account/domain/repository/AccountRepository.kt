package com.agcoding.networkapp.account.domain.repository

import com.agcoding.networkapp.account.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getAccountCount(): Flow<Int>
    suspend fun createAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun seedDefaultAccountIfNeeded()
}
