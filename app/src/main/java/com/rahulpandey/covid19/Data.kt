package com.rahulpandey.covid19

import java.util.Date

data class Covid(
    val dateChecked: Date,
    val positiveIncrease: Int,
    val negativeIncrease: Int,
    val deathIncrease: Int,
    val state: String
)