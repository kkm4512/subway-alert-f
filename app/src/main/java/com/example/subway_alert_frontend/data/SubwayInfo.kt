package com.example.subway_alert_frontend.data

data class SubwayInfoResponse(
    val code: Int,
    val message: String,
    val items: List<SubwayInfo>?
)

data class SubwayInfo(
    val statnCd: String,
    val statnNm: String,
    val lineNm: String,
    val extCd: String
)
