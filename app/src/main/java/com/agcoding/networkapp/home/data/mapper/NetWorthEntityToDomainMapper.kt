package com.agcoding.networkapp.home.data.mapper

import com.agcoding.networkapp.home.data.local.NetWorthEntity
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import java.time.LocalDate
import javax.inject.Inject

class NetWorthEntityToDomainMapper @Inject constructor() {
    fun map(entity: NetWorthEntity): NetWorthEntry = NetWorthEntry(
        id = entity.id,
        value = entity.value,
        date = LocalDate.ofEpochDay(entity.dateEpochDay),
        note = entity.note,
    )
}
