package com.example.subway_alert_frontend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subway_alert_frontend.data.SubwayInfo
import com.example.subway_alert_frontend.ui.theme.SubwayalertfrontendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            SubwayalertfrontendTheme {
                SubwaySettingScreen()
            }
        }
    }
}

@Composable
fun SubwaySettingScreen(
    viewModel: SubwayViewModel = viewModel()
) {
    val context = LocalContext.current
    // 추가된 역 목록 상태 관리
    val addedStations = remember { 
        mutableStateListOf<SubwayInfo>().apply { 
            addAll(StationPreferences.getStations(context)) 
        } 
    }

    // 앱 시작 시 저장된 역이 있으면 서비스 시작
    LaunchedEffect(Unit) {
        if (addedStations.isNotEmpty()) {
            context.startForegroundService(Intent(context, SubwayService::class.java))
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "지하철 실시간 알림",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "역을 추가하면 알림창에 정보가 표시됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // 1. 현재 추가된 역 목록
            if (addedStations.isNotEmpty()) {
                Text(text = "알림 중인 역", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                addedStations.forEach { station ->
                    AddedStationItem(
                        info = station,
                        onRemove = {
                            StationPreferences.removeStation(context, station.statnCd)
                            addedStations.remove(station)
                            
                            // 서비스에 알림 제거 신호 전송
                            val stopIntent = Intent(context, SubwayService::class.java).apply {
                                action = "com.example.subway_alert_frontend.STOP_STATION"
                                putExtra("station_code", station.statnCd)
                            }
                            context.startService(stopIntent)
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // 2. 역 검색창
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("역 이름 (예: 돌ㄱ, 수서)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = viewModel::search) {
                        Icon(Icons.Default.Search, contentDescription = "검색")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 검색 결과
            when (val state = viewModel.uiState) {
                is SubwayViewModel.UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SubwayViewModel.UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is SubwayViewModel.UiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.results) { info ->
                            val isAlreadyAdded = addedStations.any { it.statnCd == info.statnCd }
                            StationSearchResultCard(
                                info = info,
                                isAdded = isAlreadyAdded,
                                onClick = {
                                    if (!isAlreadyAdded) {
                                        StationPreferences.addStation(context, info)
                                        addedStations.add(info)
                                        // 서비스 시작/갱신
                                        context.startForegroundService(Intent(context, SubwayService::class.java))
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AddedStationItem(info: SubwayInfo, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = info.statnNm, style = MaterialTheme.typography.bodyLarge)
                Text(text = info.lineNm, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StationSearchResultCard(info: SubwayInfo, isAdded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAdded, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = info.statnNm, style = MaterialTheme.typography.bodyLarge)
                Text(text = info.lineNm, style = MaterialTheme.typography.bodySmall)
            }
            if (isAdded) {
                Text(
                    text = "추가됨",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "추가",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
