package com.example.subway_alert_frontend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.IBinder
import com.example.subway_alert_frontend.data.ArrivalInfo
import com.example.subway_alert_frontend.data.SubwayApiClient
import com.example.subway_alert_frontend.data.SubwayTime
import kotlinx.coroutines.*

class SubwayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "subway_alert_channel"
    private val ACTION_STOP = "com.example.subway_alert_frontend.STOP_STATION"
    private val EXTRA_STATION_CODE = "station_code"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val code = intent.getStringExtra(EXTRA_STATION_CODE)
            if (code != null) {
                removeStationAndCancel(code)
            }
        } else {
            startPolling()
        }
        return START_STICKY
    }

    private fun removeStationAndCancel(stationCode: String) {
        StationPreferences.removeStation(this, stationCode)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(stationCode.hashCode())
        manager.cancel(999)
        
        if (StationPreferences.getStations(this).isEmpty()) {
            stopSelf()
        }
    }

    private fun startPolling() {
        serviceScope.coroutineContext.cancelChildren()
        
        serviceScope.launch {
            while (true) {
                val stations = StationPreferences.getStations(applicationContext)
                if (stations.isEmpty()) {
                    stopSelf()
                    break
                }

                stations.forEach { station ->
                    launch {
                        try {
                            val arrivalResponse = SubwayApiClient.instance.getArrival(station.statnCd)
                            if (arrivalResponse.code == 200 && arrivalResponse.items != null) {
                                val firstLastUp = try {
                                    SubwayApiClient.instance.getFirstLastTime(station.statnCd, 1).items
                                } catch (e: Exception) { null }
                                val firstLastDown = try {
                                    SubwayApiClient.instance.getFirstLastTime(station.statnCd, 2).items
                                } catch (e: Exception) { null }

                                val text = buildNotificationText(arrivalResponse.items, firstLastUp, firstLastDown)
                                updateNotification(station.statnNm, station.lineNm, station.statnCd, text)
                            } else {
                                updateNotification(station.statnNm, station.lineNm, station.statnCd, arrivalResponse.message)
                            }
                        } catch (e: Exception) {
                            updateNotification(station.statnNm, station.lineNm, station.statnCd, "데이터를 불러올 수 없습니다.")
                        }
                    }
                }
                delay(30_000L)
            }
        }
    }

    private fun buildNotificationText(
        arrivals: List<ArrivalInfo>,
        upTime: SubwayTime?,
        downTime: SubwayTime?
    ): String {
        if (arrivals.isEmpty()) return "현재 운행 중인 열차가 없습니다."
        val sb = StringBuilder()
        
        // 상선 첫차/막차 및 도착 정보 2개
        if (upTime != null) {
            sb.appendLine("첫 ${formatTime(upTime.firstTime)} | 막 ${formatTime(upTime.lastTime)}")
        }
        arrivals.filter { it.updnLine == 1 }.take(2).forEach {
            val timeText = if (it.arrivalMinute == 0) "곧도착" else "${it.arrivalMinute}분"
            sb.appendLine(" • ${it.end}: $timeText (${it.arvlCd})")
        }

        if (sb.isNotEmpty()) sb.appendLine() // 가독성을 위한 한 줄 띄움

        // 하선 첫차/막차 및 도착 정보 2개
        if (downTime != null) {
            sb.appendLine("첫 ${formatTime(downTime.firstTime)} | 막 ${formatTime(downTime.lastTime)}")
        }
        arrivals.filter { it.updnLine == 2 }.take(2).forEach {
            val timeText = if (it.arrivalMinute == 0) "곧도착" else "${it.arrivalMinute}분"
            sb.appendLine(" • ${it.end}: $timeText (${it.arvlCd})")
        }

        return sb.toString().trimEnd()
    }

    private fun formatTime(timeStr: String): String {
        return try {
            val t = timeStr.padStart(6, '0')
            "${t.substring(0, 2)}:${t.substring(2, 4)}"
        } catch (e: Exception) { timeStr }
    }

    private fun updateNotification(stationName: String, lineName: String, stationCode: String, text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val stopIntent = Intent(this, SubwayService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_STATION_CODE, stationCode)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, stationCode.hashCode(), stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 컬러 아이콘 (Large Icon) 생성
        val largeIcon = Icon.createWithResource(this, R.mipmap.ic_launcher)

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("$stationName ($lineName)")
            .setContentText(text.split("\n").firstOrNull())
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeIcon)
            .setColor(Color.parseColor("#1A237E"))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "끄기", stopPendingIntent)
            .setOngoing(true)
            .build()

        val stations = StationPreferences.getStations(this)
        if (stationCode == stations.firstOrNull()?.statnCd) {
            manager.cancel(stationCode.hashCode())
            startForeground(999, notification)
        } else {
            manager.notify(stationCode.hashCode(), notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "지하철 도착 알림", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
