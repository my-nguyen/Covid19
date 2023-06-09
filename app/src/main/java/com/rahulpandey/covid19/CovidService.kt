package com.rahulpandey.covid19

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {
    @GET("us/daily.json")
    fun getNationalData(): Call<List<Covid>>

    @GET("states/daily.json")
    fun getStatesData(): Call<List<Covid>>
}