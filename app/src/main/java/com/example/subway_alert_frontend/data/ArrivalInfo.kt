package com.example.subway_alert_frontend.data

data class ArrivalResponse(
    val code: Int,
    val message: String,
    val items: List<ArrivalInfo>?
)

data class ArrivalInfo(
    val updnLine: Int,
    val start: String,
    val end: String,
    val arrivalMinute: Int,
    val btrainSttus: String,
    val arvlCd: String
)

data class SubwayTimeResponse(
    val code: Int,
    val message: String,
    val items: SubwayTime?
)

data class SubwayTime(
    val dayType: String,
    val firstTime: String,
    val lastTime: String
)
