package com.rahulpandey.covid19

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.rahulpandey.covid19.databinding.ActivityMainBinding
import com.robinhood.ticker.TickerUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity-Truong"
private const val ALL_STATES = "All (Nationwide)"

class MainActivity : AppCompatActivity() {
    private lateinit var currentCovids: List<Covid>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var perStateDailyData: Map<String, List<Covid>>
    private lateinit var nationalDailyData: List<Covid>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.app_description)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit =
            Retrofit.Builder().baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        val service = retrofit.create(CovidService::class.java)
        service.getNationalData().enqueue(object : Callback<List<Covid>> {
            override fun onFailure(call: Call<List<Covid>>, t: Throwable) {
                Log.d(TAG, "onFailure $t")
            }

            override fun onResponse(call: Call<List<Covid>>, response: Response<List<Covid>>) {
                Log.d(TAG, "onResponse $response")
                val body = response.body()
                if (body == null) {
                    Log.w(TAG, "onResponse received null data")
                } else {
                    nationalDailyData = body.reversed()
                    Log.d(TAG, "Update graph with national data")
                    updateDisplayWithData(nationalDailyData)
                }
            }
        })

        service.getStatesData().enqueue(object : Callback<List<Covid>> {
            override fun onFailure(call: Call<List<Covid>>, t: Throwable) {
                Log.d(TAG, "onFailure $t")
            }

            override fun onResponse(call: Call<List<Covid>>, response: Response<List<Covid>>) {
                Log.d(TAG, "onResponse $response")
                val body = response.body()
                if (body == null) {
                    Log.w(TAG, "onResponse received null data")
                } else {
                    setupEventListeners()
                    perStateDailyData = body.reversed().groupBy { it.state }
                    Log.d(TAG, "Update spinner with state names")
                    // Update spinner with state names
                    updateSpinnerWithStates(perStateDailyData.keys)
                }
            }
        })
    }

    private fun updateSpinnerWithStates(states: Set<String>) {
        val abbreviations = states.sorted().toMutableList()
        abbreviations.add(0, ALL_STATES)

        // add state list as data source for the spinner
        binding.spinner.attachDataSource(abbreviations)
        binding.spinner.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val state = parent.getItemAtPosition(position) as String
            val data = perStateDailyData[state] ?: nationalDailyData
            updateDisplayWithData(data)
        }
    }

    private fun setupEventListeners() {
        Log.d(TAG, "setupEventListeners")

        // add a listener for the user scrubbing on the chart
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener {
            if (it is Covid) {
                updateInfoForDate(it)
            }
        }
        // respond to radio button selected events
        binding.radioGroupTime.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.week -> TimeScale.WEEK
                R.id.month -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }

        binding.radioGroupMetric.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.positive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.negative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.death -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        Log.d(TAG, "updateDisplayMetric")
        // update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.negative
            Metric.POSITIVE -> R.color.positive
            Metric.DEATH -> R.color.death
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        binding.sparkView.lineColor = colorInt
        binding.metric.textColor = colorInt

        // update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // reset number and date shown on bottom TextViews
        updateInfoForDate(currentCovids.last())
    }

    private fun updateDisplayWithData(dailyCovids: List<Covid>) {
        Log.d(TAG, "updateDisplayWithData")
        currentCovids = dailyCovids

        // create a SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyCovids)
        binding.sparkView.adapter = adapter

        // update radio buttons to select the positive cases and max time by default
        binding.positive.isChecked = true
        binding.max.isChecked = true

        // display metric for the most recent date
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covid: Covid) {
        Log.d(TAG, "updateInfoForDate")

        val cases = when (adapter.metric) {
            Metric.NEGATIVE -> covid.negativeIncrease
            Metric.POSITIVE -> covid.positiveIncrease
            Metric.DEATH -> covid.deathIncrease
        }
        // use Robinhood TickerView instead of a TextView
        binding.metric.setCharacterLists(TickerUtils.provideNumberList())
        binding.metric.text = NumberFormat.getInstance().format(cases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.date.text = outputDateFormat.format(covid.dateChecked)
    }
}