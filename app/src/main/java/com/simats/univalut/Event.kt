package com.simats.univalut

import java.time.LocalDate

data class Event(
    val title: String,
    val type: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String
)
