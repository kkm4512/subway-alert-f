package com.example.subway_alert_frontend.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface SubwayApiService {
    @GET("subway-info")
    suspend fun getStationInfo(
        @Query("statnNm") stationName: String
    ): SubwayInfoResponse

    @GET("subway-realtime-arrival")
    suspend fun getArrival(
        @Query("statnCd") stationCode: String
    ): ArrivalResponse

    @GET("subway-first-last-time")
    suspend fun getFirstLastTime(
        @Query("statnCd") stationCode: String,
        @Query("updnLine") updnLine: Int
    ): SubwayTimeResponse
}

object SubwayApiClient {
    val instance: SubwayApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://subway-alert-b.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SubwayApiService::class.java)
    }
}
