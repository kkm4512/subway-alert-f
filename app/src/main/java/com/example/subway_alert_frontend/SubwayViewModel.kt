package com.example.subway_alert_frontend

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subway_alert_frontend.data.SubwayApiClient
import com.example.subway_alert_frontend.data.SubwayInfo
import kotlinx.coroutines.*

class SubwayViewModel : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val results: List<SubwayInfo>) : UiState()
        data class Error(val message: String) : UiState()
    }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var query by mutableStateOf("")
        private set

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        query = value
        if (value.isBlank()) {
            uiState = UiState.Idle
            searchJob?.cancel()
            return
        }
        performSearch(value, true)
    }

    fun search() {
        if (query.isBlank()) return
        performSearch(query.trim(), false)
    }

    private fun performSearch(value: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(300) // 타이핑 중에는 0.3초 대기

            uiState = UiState.Loading

            try {
                // 1. 백엔드 API를 통해 검색어에 해당하는 지하철역 목록(이름)을 가져옴
                val searchResponse = SubwayApiClient.instance.searchStations(value)
                
                if (searchResponse.code != 200 || searchResponse.items.isNullOrEmpty()) {
                    uiState = UiState.Error("해당하는 역이 없습니다.")
                    return@launch
                }

                // 2. 검색 결과에서 중복되지 않은 역 이름 추출 (상위 10개)
                val uniqueStationNames = searchResponse.items
                    .map { it.statnNm }
                    .distinct()
                    .take(10)

                // 3. 추출된 각 역 이름에 대해 상세 정보(호선 정보 포함)를 병렬로 조회
                val deferredResults = uniqueStationNames.map { name ->
                    async {
                        try {
                            val response = SubwayApiClient.instance.getStationInfo(name)
                            if (response.code == 200) response.items ?: emptyList() else emptyList()
                        } catch (e: Exception) {
                            emptyList<SubwayInfo>()
                        }
                    }
                }

                val allResults = deferredResults.awaitAll().flatten()
                    .distinctBy { "${it.statnCd}-${it.lineNm}" } // 중복 제거

                if (allResults.isNotEmpty()) {
                    uiState = UiState.Success(allResults)
                } else {
                    uiState = UiState.Error("데이터를 불러올 수 없습니다.")
                }
            } catch (e: Exception) {
                uiState = UiState.Error("검색 중 오류가 발생했습니다.")
            }
        }
    }
}
