# 🚇 지하철 도착 시간 알리미 (Subway Arrival Alerter)

사용자가 설정한 지하철역의 실시간 도착 정보 및 첫차/막차 시간을 안드로이드 알림창(Notification)을 통해 실시간으로 제공하는 앱입니다.

## 🌟 주요 기능
- **실시간 도착 정보**: 상선/하선별 최대 2개씩, 총 4개의 실시간 도착 정보를 알림창에 표시합니다.
- **첫차/막차 시간**: 각 역의 상/하선별 평일 기준 첫차와 막차 시간을 함께 안내합니다.
- **다중 역 모니터링**: 여러 개의 역을 동시에 추가하여 각각 별도의 알림 카드로 관리할 수 있습니다.
- **지능형 검색**: 한글 초성 검색(예: `돌ㄱ` -> `돌곶이`) 및 자동완성 기능을 지원합니다.
- **백그라운드 서비스**: 앱을 닫아도 포그라운드 서비스를 통해 30초마다 정보를 자동 갱신합니다.
- **알림 제어**: 알림창 내 '끄기' 버튼을 통해 개별 역의 알림을 즉시 종료할 수 있습니다.

---

## 📡 API Specification

본 앱은 아래 백엔드 서버와 통신하여 데이터를 가져옵니다.
- **Base URL**: `https://subway-alert-b.onrender.com/`

### 1. 지하철역 정보 검색
역 이름을 통해 해당 역의 고유 코드 및 호선 정보를 가져옵니다.

- **Endpoint**: `/subway-info`
- **Method**: `GET`
- **Query Parameters**: 
  - `statnNm`: 검색할 역 이름 (예: `돌곶이`)
- **Success Response**:
```json
{
    "code": 200,
    "message": "SUCCESS",
    "items": [
        {
            "statnCd": "2644",
            "statnNm": "돌곶이",
            "lineNm": "6호선",
            "extCd": "643"
        }
    ]
}
```

### 2. 실시간 도착 정보 조회
특정 역의 현재 열차 도착 예정 정보를 가져옵니다.

- **Endpoint**: `/subway-realtime-arrival`
- **Method**: `GET`
- **Query Parameters**: 
  - `statnCd`: 역 고유 코드 (예: `2644`)
- **Success Response**:
```json
{
    "code": 200,
    "message": "SUCCESS",
    "items": [
        {
            "updnLine": 1,
            "start": "응암순환(상선)행",
            "end": "상월곡방면",
            "arrivalMinute": 5,
            "btrainSttus": "일반",
            "arvlCd": "운행중"
        }
    ]
}
```

### 3. 첫차/막차 시간 조회
해당 역의 운행 시작 및 종료 시간을 조회합니다.

- **Endpoint**: `/subway-first-last-time`
- **Method**: `GET`
- **Query Parameters**: 
  - `statnCd`: 역 고유 코드
  - `updnLine`: 방향 (1: 상선/내선, 2: 하선/외선)
- **Success Response**:
```json
{
    "code": 200,
    "message": "SUCCESS",
    "items": {
        "dayType": "평일",
        "firstTime": "053720",
        "lastTime": "235830"
    }
}
```

---

## 🛠 Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Network**: Retrofit2, Gson
- **Async**: Kotlin Coroutines
- **Architecture**: MVVM
- **Component**: Android Foreground Service, Notification Manager

---

## 📸 Screenshots & Icons
- **App Name**: 지하철 도착 시간 알리미
- **Icons**: 맞춤형 지하철 전동차 및 알림 벨 벡터 아이콘 적용
- **Notification Color**: `#1A237E` (Deep Blue)
