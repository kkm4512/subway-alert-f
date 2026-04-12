package com.example.subway_alert_frontend

import android.content.Context
import androidx.core.content.edit
import com.example.subway_alert_frontend.data.SubwayInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StationPreferences {
    private const val PREF_NAME = "subway_settings_v2"
    private const val KEY_STATIONS = "added_stations"
    private val gson = Gson()

    fun addStation(context: Context, info: SubwayInfo) {
        val current = getStations(context).toMutableList()
        if (current.none { it.statnCd == info.statnCd }) {
            current.add(info)
            saveStations(context, current)
        }
    }

    fun removeStation(context: Context, stationCode: String) {
        val current = getStations(context).filter { it.statnCd != stationCode }
        saveStations(context, current)
    }

    fun getStations(context: Context): List<SubwayInfo> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_STATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<SubwayInfo>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveStations(context: Context, stations: List<SubwayInfo>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_STATIONS, gson.toJson(stations))
        }
    }
}
