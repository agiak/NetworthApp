package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import java.time.LocalDate
import javax.inject.Inject

class GenerateSpecificDataUseCase @Inject constructor(
    private val repository: NetWorthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val entries = listOf(
            NetWorthEntry(value = 36029.0, date = LocalDate.of(2024, 10, 25)),
            NetWorthEntry(value = 38329.0, date = LocalDate.of(2024, 11, 25)),
            NetWorthEntry(value = 39149.0, date = LocalDate.of(2024, 12, 25)),
            NetWorthEntry(value = 42265.0, date = LocalDate.of(2025, 1, 25)),
            NetWorthEntry(value = 43103.0, date = LocalDate.of(2025, 2, 25)),
            NetWorthEntry(value = 43510.0, date = LocalDate.of(2025, 3, 25)),
            NetWorthEntry(value = 41989.0, date = LocalDate.of(2025, 4, 25)),
            NetWorthEntry(value = 42855.0, date = LocalDate.of(2025, 5, 25)),
            NetWorthEntry(value = 44743.0, date = LocalDate.of(2025, 6, 25)),
            NetWorthEntry(value = 37992.0, date = LocalDate.of(2025, 7, 25)),
            NetWorthEntry(value = 39981.0, date = LocalDate.of(2025, 8, 25)),
            NetWorthEntry(value = 41570.0, date = LocalDate.of(2025, 9, 25)),
            // Skip October 2025
            NetWorthEntry(value = 46237.0, date = LocalDate.of(2025, 11, 25)),
            NetWorthEntry(value = 53541.0, date = LocalDate.of(2025, 12, 25)),
            NetWorthEntry(value = 57749.0, date = LocalDate.of(2026, 1, 25)),
            NetWorthEntry(value = 62246.0, date = LocalDate.of(2026, 2, 25)),
            NetWorthEntry(value = 60982.0, date = LocalDate.of(2026, 3, 25)),
            NetWorthEntry(value = 65855.0, date = LocalDate.of(2026, 4, 25))
        )

        return try {
            entries.forEach { repository.addEntry(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
