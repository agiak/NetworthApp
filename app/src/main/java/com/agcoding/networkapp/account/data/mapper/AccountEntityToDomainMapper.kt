package com.agcoding.networkapp.account.data.mapper

import com.agcoding.networkapp.account.data.local.AccountEntity
import com.agcoding.networkapp.account.domain.model.Account
import javax.inject.Inject

class AccountEntityToDomainMapper @Inject constructor() {
    fun map(entity: AccountEntity): Account = Account(
        id             = entity.id,
        name           = entity.name,
        startingBalance = entity.startingBalance,
        colorHex       = entity.colorHex,
    )
}
