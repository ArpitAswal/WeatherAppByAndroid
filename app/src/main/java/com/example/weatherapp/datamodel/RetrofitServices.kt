package com.example.weatherapp.datamodel

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitServices {

    @GET("weather")
    fun getWeatherData(
        @Query("q") city: String,
        @Query("appid") appid: String,
        @Query("units") units: String
    ) : Call<WeatherApp>
}