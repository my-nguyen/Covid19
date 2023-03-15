package com.rahulpandey.covid19

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val covids: List<Covid>): SparkAdapter() {
    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount() = covids.size

    override fun getItem(index: Int) = covids[index]

    override fun getY(index: Int): Float {
        val covid = covids[index]
        return when (metric) {
            Metric.NEGATIVE -> covid.negativeIncrease.toFloat()
            Metric.POSITIVE -> covid.positiveIncrease.toFloat()
            Metric.DEATH -> covid.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.MAX) {
            bounds.left = count - daysAgo.days.toFloat() // remove the last few days of data
        }
        return bounds
    }
}
