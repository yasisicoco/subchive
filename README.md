# Subchive

구독 서비스와 결제일을 관리하는 Kotlin Android 앱입니다. Android Studio에서 이 폴더를 열면 됩니다. 원래 구독비서의 비공개 코드·브랜드 자산을 복제하지 않고, 유사 앱 분석에서 확인한 기술과 공개 Android API로 새로 구현했습니다.

## 실행

1. Android Studio에서 `subchive` 폴더를 엽니다.
2. Gradle JDK를 17 또는 21로 지정합니다.
3. SDK Manager에서 Android SDK Platform 36과 Build Tools를 설치합니다.
4. Gradle Sync 후 Android 8.0(API 26) 이상 기기 또는 에뮬레이터에서 `app`을 실행합니다.
5. 첫 구독을 등록하거나 홈의 `샘플 구독 4개로 둘러보기`를 누릅니다.

프로젝트에 PC 전용 `local.properties`나 서명 키를 포함하지 않았습니다. SDK 경로는 Android Studio가 생성하거나 `ANDROID_HOME`으로 지정합니다. Gradle Wrapper가 포함되어 있어 Gradle 전역 설치는 필요 없습니다. 첫 빌드에는 Maven 의존성 다운로드가 필요합니다.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

macOS/Linux에서는 `chmod +x gradlew` 후 `./gradlew`를 사용합니다. 전체 검증은 `tools/verify.ps1` 또는 연결된 기기를 포함하는 `tools/verify.ps1 -DeviceTests`로 재사용할 수 있습니다.

## 구현 기능

- 구독 등록·수정·삭제·일시정지·다시 활성화
- 서비스 아이콘 검색·선택, 서비스명과 카테고리 자동 입력
- KRW, USD, JPY, EUR, HKD, CNY와 주간·월간·분기·연간 결제
- 다음 결제일, 월말 기준일 유지, 윤년 처리, 자동갱신 예정일 계산
- 구독별 당일·1·3·7·14일 전 알림, 동일 날짜·알림 중복 방지
- 알림에서 구독 상세로 이동
- 월별 결제 캘린더와 날짜별 구독 목록
- 통화별 월·연 환산액, 카테고리 도넛 차트, 향후 6개월 예상 결제액
- 한국어·영문 ML Kit OCR: 갤러리/파일 또는 카메라 앱 촬영 → 추출 후보 검토 → 폼에 적용
- 해지 웹 링크와 메모
- 캘린더 앱에 한 번 추가 또는 앱 전용 로컬 캘린더에 다음 3개월 자동 동기화
- JSON 백업·복원, 입력 검증, 가져오기 트랜잭션
- 시스템/밝게/어둡게 테마
- 선택형 AdMob 배너·UMP 동의·광고 제거 일회성 결제·구매 복원 모듈

서비스 18개의 실제 PNG 로고를 앱에 번들했습니다. Dashboard Icons와 각 개발자의 Google Play 앱 페이지에서 원본을 내려받았으며 출처·SHA-256은 `app/src/main/assets/logo_sources.json`에 있습니다. 네이버플러스·쿠팡 와우에는 각각 NAVER·쿠팡 앱의 대표 아이콘을 사용합니다. 직접 입력한 서비스만 글자 아이콘을 사용합니다. 은행 계좌, 카드 결제 내역, Google Play의 다른 구독을 자동으로 읽거나 실제 결제를 대행하지 않습니다.

### UI 1.1

화면 제목은 22sp, 본문은 13~14sp, 보조 설명은 11~12sp로 정리했습니다. 카드·로고·차트·여백을 줄이고 회색과 블루그레이로 통일했습니다. 주요 금액과 추가·저장 버튼, 임박한 결제, 현재 선택만 강조합니다. 브랜드 색은 작은 로고 이미지 안에만 남겼습니다. 확대된 시스템 글꼴 설정은 계속 따릅니다.

홈의 구독 추가는 상단 버튼에 배치해 목록 위를 가리는 큰 플로팅 버튼을 없앴습니다. 로고 이미지는 앱 리소스로 포함하므로 실행 중 이미지를 다운로드하지 않습니다. 로고를 갱신하려면 표준 Python으로 `python tools/fetch_service_logos.py`를 실행합니다. 다운로드 위치와 매핑은 해당 스크립트의 `CATALOG`와 `ui/ServiceLogos.kt`에서 관리합니다.

## 사용 기술

| 영역 | 구성 |
|---|---|
| 언어·빌드 | Kotlin 2.1.20, AGP 8.13.1, Gradle 8.14.3, KSP 2.1.20-2.0.1 |
| UI | Compose BOM 2025.04.01, Material 3, Navigation Compose 2.8.9 |
| 상태 | AndroidViewModel, StateFlow, lifecycle-runtime-compose 2.9.0 |
| 저장 | Room 2.7.2, SQLite, SharedPreferences |
| 예약 | WorkManager 2.10.2, CoroutineWorker |
| 비동기 | Kotlin Coroutines 1.10.2 |
| OCR | ML Kit 한국어·라틴 번들 모델 16.0.1 |
| 광고 | Google Mobile Ads 24.2.0, UMP 3.2.0 |
| 결제 | Google Play Billing 8.3.0 |
| 테스트 | JUnit, Room in-memory DB, AndroidJUnitRunner, Compose UI Test |

## 프로젝트로 옮기기

독립 앱으로 시작하려면 전체 폴더를 새 저장소로 옮기면 됩니다. 기존 Android 앱에 넣으려면 아래 순서로 옮깁니다.

1. `app/build.gradle.kts`의 의존성과 Compose/KSP 플러그인을 기존 모듈에 병합합니다. 기존 앱의 버전을 우선하고 호환성을 확인합니다.
2. `app/src/main/java/com/example/submate`를 원하는 패키지로 이동합니다. Android Studio의 Refactor → Rename으로 패키지를 바꾸고 `namespace`와 `applicationId`를 지정합니다.
3. 기존 Application이 있다면 `SubMateApplication`의 repository/settings/calendar/worker 초기화를 기존 Application 또는 DI 구성으로 옮깁니다. 단순 복사 시 Manifest의 `android:name`도 확인합니다.
4. `SubMateApp`을 기존 Activity의 Compose 진입점 또는 Navigation 목적지에서 호출하도록 연결합니다. 알림의 `MainActivity` 진입점과 `subscription_id` 처리를 함께 옮깁니다.
5. `AndroidManifest.xml`의 권한·FileProvider·TimeChangeReceiver·AdMob 메타데이터와 `res` 파일을 병합합니다. FileProvider authority는 applicationId를 사용합니다.
6. `app/schemas`의 Room 스키마와 테스트도 함께 이동합니다. 기존 Room DB와 합칠 경우 Entity/DAO를 추가하고 해당 DB의 버전·Migration을 작성합니다. 현재 앱에는 destructive migration을 사용하지 않습니다.
7. 앱 이름과 아이콘을 교체하고 개인정보처리방침을 실제 운영 조건에 맞게 작성합니다.
8. `tools/verify.ps1` 또는 Gradle 명령으로 검증합니다.

### 소스 위치

| 경로 | 역할 |
|---|---|
| `domain/Billing.kt` | 반복 결제일·정수 최소 단위 금액 계산 |
| `domain/ReceiptParser.kt` | OCR 텍스트에서 서비스·금액·날짜 후보 추출 |
| `domain/ReminderPolicy.kt` | 사전 알림·중복 방지 판단 |
| `domain/ServiceCatalog.kt` | 서비스·카테고리·통화 목록 |
| `data/` | Room, 설정, 검증, 백업·복원 |
| `worker/` | 일일 알림·캘린더 유지 작업 |
| `integration/` | OCR, 캘린더, 선택형 광고·결제 |
| `ui/` | 화면, 테마, ViewModel |

## 광고·결제 설정

기본값은 `adsEnabled=false`, `billingEnabled=false`입니다. 기본 사용 흐름에는 광고 요청이나 결제가 없습니다. 라이브러리는 포함되므로 합쳐진 Manifest에 `INTERNET`, `ACCESS_NETWORK_STATE`, 광고 식별자, 결제 권한 등이 나타날 수 있습니다. 따라서 이 APK를 `네트워크 권한 없는 앱`으로 표시하면 안 됩니다.

테스트 광고는 다음과 같이 켭니다. 기본 광고 ID는 Google 공식 테스트 ID입니다.

```powershell
.\gradlew.bat assembleDebug -PadsEnabled=true
```

실제 광고와 광고 제거 구매를 활성화할 때는 다음 값을 본인의 등록 정보로 지정합니다.

```properties
adsEnabled=true
admobAppId=본인의_AdMob_앱_ID
adBannerId=본인의_배너_광고단위_ID
billingEnabled=true
removeAdsProduct=remove_ads
```

- 값을 개인 Gradle 설정 또는 CI에서 전달합니다. 현재 프로젝트에는 운영용 ID가 없습니다.
- AdMob의 개인정보 메시지를 구성하면 UMP 동의 흐름을 거친 후 광고를 요청합니다. 필요한 지역에서는 설정에 개인정보 선택 메뉴가 나타납니다.
- Play Console에 실제 applicationId와 서명 키로 앱을 등록하고 `remove_ads` 일회성 상품을 활성화해야 합니다.
- 내부 테스트 트랙에 배포하고 라이선스 테스터로 결제·취소·보류·환불·복원을 검증합니다. 로컬 설치만으로 실제 상품 결제를 완성할 수 없습니다.
- 구매 완료 상태와 승인(acknowledge)을 처리하며 소비하지 않는 상품으로 구성했습니다. 구매 검증은 클라이언트 구현이며, 상용 서비스에서 부정 사용 방지가 필요하면 서버 검증·환불 통지 처리를 추가합니다.
- 운영 광고·실제 유료 결제는 이번 로컬 검증의 대상이 아닙니다.

## 알림·캘린더 동작

알림은 설정한 시간부터 24시간 주기의 WorkManager 작업으로 확인합니다. 정확한 시각 알람이 아니므로 절전·제조사 정책·강제 종료에 따라 지연되거나 실행되지 않을 수 있습니다. Android 13 이상은 사용자의 알림 권한 허용이 필요합니다. 앱이 강제 종료된 경우 다시 열어 주세요. 시간대 변경 시 작업을 다시 예약합니다.

구독의 원래 기준일을 저장하므로 1월 31일 → 2월 말일 → 3월 31일처럼 계산합니다. `자동갱신`은 예정일 계산 옵션으로, 외부 서비스의 구독 상태를 바꾸지 않습니다. 과거 결제 이력은 수집·생성하지 않습니다.

캘린더 자동 연결은 `Subchive 결제일`이라는 LOCAL 계정 캘린더를 만듭니다. 이 앱이 소유한 캘린더의 일정만 재생성합니다. 기존 개인 캘린더 일정은 수정하지 않습니다. 연결을 끄면 앱 전용 캘린더의 생성 일정을 지웁니다. 일부 기기에서 LOCAL 캘린더 생성이 지원되지 않으면 오류를 표시하며, 상세 화면의 수동 추가를 이용할 수 있습니다. 수동 추가한 일정은 캘린더 앱에서 관리합니다.

## 데이터·OCR

- 금액은 `Long` 최소 통화 단위로 저장합니다. 예: KRW 17,000원 → 17000, USD 19.99 → 1999. 다른 통화끼리는 합치지 않습니다.
- 월·연 환산은 추정치이고, 6개월 예상은 오늘 이후 예정 일정입니다. 실제 청구 금액·환율과 차이가 있을 수 있습니다.
- OCR 모델은 앱에 포함됩니다. 추출은 간단한 규칙 기반 후보이며 자동 저장하지 않습니다. 복수 금액·날짜가 있는 영수증은 사용자가 확인해야 합니다.
- 구독 DB는 Android 앱 전용 저장소에 보관하지만 별도의 DB 암호화는 적용하지 않았습니다. OS 자동 백업은 꺼져 있습니다.
- JSON 백업은 암호화하지 않습니다. 사용자가 파일을 직접 선택해 저장·가져옵니다. 복원은 기존 구독을 유지하고 추가하므로 반복 가져오기는 중복을 만들 수 있습니다.
- 백업은 2MB/1,000개 한도와 입력 검증을 적용하며, 하나라도 유효하지 않으면 전체 가져오기를 취소합니다.
- 카메라 기능은 외부 카메라 앱과 FileProvider를 사용하므로 CAMERA 권한을 직접 요구하지 않습니다.

## 검증

`VALIDATION.md`에 실제 실행한 빌드·단위·기기 테스트 결과와 남은 한계를 기록합니다. Android 기기 제조사별 절전 동작, 실제 Play 결제, 운영 광고, 여러 개인 캘린더 앱은 별도로 검증해야 합니다.

## 공개 기술 문서

- https://developer.android.com/develop/ui/compose
- https://developer.android.com/training/data-storage/room
- https://developer.android.com/develop/background-work/background-tasks/persistent
- https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- https://developer.android.com/identity/providers/calendar-provider
- https://developers.google.com/admob/android/privacy
- https://developer.android.com/google/play/billing/integrate

SubGuard APK 분석은 기능·구성 참고에 사용했으며, 해당 앱의 코드를 프로젝트에 복사하지 않았습니다. 구독비서·SubGuard의 앱 이름과 스토어 설명을 복제하지 않았습니다. 구독 서비스의 이름과 로고는 서비스 식별용이며 관련 출처는 `THIRD_PARTY_LOGOS.md`에 정리했습니다.

## 이름 변경 호환성

앱 표시 이름, 홈·설정·백업 파일명과 캘린더 표시 이름은 Subchive로 통일했습니다. 기존 설치 데이터와 백업 호환성을 위해 applicationId, Kotlin 클래스명, DB·설정 파일명과 백업 format 식별자는 유지합니다. 홈·설정·다크 모드 캡처는 이름 변경 후 다시 저장했습니다.
