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
             8_200.0,  8_700.0,  8_400.0,  9_100.0,  9_600.0, 10_200.0,
            10_500.0, 10_100.0, 10_800.0, 11_400.0, 11_900.0, 12_500.0,
            13_100.0, 12_800.0, 13_500.0, 14_200.0, 14_600.0, 15_300.0,
            16_000.0, 15_700.0, 16_500.0, 17_300.0, 18_100.0, 19_000.0
        )
    }
}
