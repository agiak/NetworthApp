package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import java.time.YearMonth
import javax.inject.Inject

class GenerateDummyDataUseCaseImpl @Inject constructor(
    private val netWorthRepository: NetWorthRepository
) : GenerateDummyDataUseCase {

    override suspend operator fun invoke(): Result<Unit> {
        netWorthRepository.deleteAllEntries().getOrElse { return Result.failure(it) }

        val now = YearMonth.now()
        dummyMonthlyValues.forEachIndexed { index, value ->
            val month = now.minusMonths((dummyMonthlyValues.size - 1 - index).toLong())
            val entry = NetWorthEntry(value = value, date = month.atDay(15))
            netWorthRepository.addEntry(entry).getOrElse { return Result.failure(it) }
        }

        return Result.success(Unit)
    }

    companion object {
        private val dummyMonthlyValues = listOf(
            10_500.0, 11_200.0, 10_800.0, 11_500.0, 12_100.0, 12_800.0,
            13_200.0, 13_900.0, 14_200.0, 15_100.0, 15_800.0, 16_500.0
        )
    }
}
