package com.agcoding.networkapp.shared.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDate.formatForDisplay(): String =
    format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
