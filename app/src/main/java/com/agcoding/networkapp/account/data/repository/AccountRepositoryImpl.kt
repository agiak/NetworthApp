package com.agcoding.networkapp.account.data.repository

import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.local.AccountEntity
import com.agcoding.networkapp.account.data.mapper.AccountEntityToDomainMapper
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.shared.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao,
    private val mapper: AccountEntityToDomainMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {

    override fun getAccounts(): Flow<List<Account>> =
        dao.getAllAccounts().map { it.map(mapper::map) }

    override fun getAccountCount(): Flow<Int> = dao.getAccountCount()

    override suspend fun createAccount(account: Account): Long = withContext(ioDispatcher) {
        dao.insertAccount(AccountEntity(name = account.name, startingBalance = account.startingBalance, colorHex = account.colorHex))
    }

    override suspend fun updateAccount(account: Account) = withContext(ioDispatcher) {
        dao.updateAccount(AccountEntity(id = account.id, name = account.name, startingBalance = account.startingBalance, colorHex = account.colorHex))
    }

    override suspend fun deleteAccount(id: Long) = withContext(ioDispatcher) {
        dao.deleteAccount(id)
    }

    override suspend fun seedDefaultAccountIfNeeded() = withContext(ioDispatcher) {
        if (dao.getAccountCountOnce() == 0) {
            dao.insertAccount(AccountEntity(name = "Main", startingBalance = 0.0, colorHex = "#76C893"))
        }
    }
}
