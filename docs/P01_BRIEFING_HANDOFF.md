# P01 브리핑 연결 안내

공개 함수는 `io.kailand.feature.briefing.BriefingScreen`이다. 확정된 영수증을 표시하며 보상 계산·저장·화면 이동은 하지 않는다. 공통 계약·AppRoot·MainActivity·빌드 버전·Catalog 원본은 변경하지 않았다.

## A의 연결 예제

```kotlin
val receipt = snapshot.pendingReceipt
if (receipt != null) {
    BriefingScreen(
        receipt = receipt,
        catalog = catalog,
        avatar = snapshot.avatar,
        gate = gate,
        onDismiss = { acknowledgeAndRouteIfCurrent(receipt.journeyId, openItems = false) },
        onOpenItems = { acknowledgeAndRouteIfCurrent(receipt.journeyId, openItems = true) },
    )
}
```

`acknowledgeAndRouteIfCurrent`는 예제의 A 소유 연결 함수 이름이다. A가 최신 gate·foreground·표시 중 journeyId를 검사하고 `GameService.acknowledgeReceipt`를 await한 뒤, 여전히 같은 결과인 경우에만 성공 라우팅을 연결해야 한다. 실제 공유 콜백 타입은 두 개 모두 `suspend () -> MutationResult`로 유지했다. PM 화면은 `GameService`를 받지 않는다. `pendingReceipt == null`이면 A가 빈 상태를 선택하며 가짜 영수증을 만들지 않는다.

## 표시와 상호작용

- DEMO 배지를 유지한다. PLEOS는 입력 source를 표시할 뿐 SDK 검증 성공을 뜻하지 않는다.
- `1200초 / 5000m`는 `20분 / 5km`다. null은 해당 행을 생략하고 실제 0은 `0초 / 0m`로 표시한다. 음수·비유한 값은 생략하고 안내한다. 거리는 1km 미만에서 정수 m, 그 이상에서 소수 한 자리 km로 내림하며 1m 미만은 별도로 표시한다. 시간은 초 단위를 보존한다.
- 성장 전후·발견 지역·기념품은 영수증 값만 표시한다. `grantedItemIds`는 원본 기념품 기록이며 재표시 시 삭제하거나 새 지급처럼 설명하지 않는다. 목록이 비면 추가 기념품 없음이다.
- 알 수 없는 character/region/item ID는 일반 이름과 누락 안내로 표시한다. Catalog를 수정하거나 ID로 이미지 경로를 합성하지 않는다. 캐릭터는 이름·착용 정보로 임시 표현하며 B feature를 import하지 않는다.
- RESTRICTED/UNKNOWN에서는 상세·버튼을 그리지 않고 시스템 Back도 소비한다. 허용 상태에서는 두 버튼과 Back이 같은 비동기 처리 경로를 사용한다. 처리 중에는 모든 동작을 차단한다.
- Rejected/StorageFailure/예외는 화면에 오류와 다시 시도를 남긴다. 내부 오류 문자열은 그대로 노출하지 않는다. Success는 확인 완료 상태로 남기며 PM이 라우팅하지 않는다. 같은 세션의 성공 콜백은 다시 실행하지 않는다.
- 영수증 또는 gate가 바뀌면 이전 Compose scope와 처리 상태를 폐기한다. 취소는 오류로 바꾸지 않는다. 취소를 무시한 콜백의 늦은 결과도 새 화면 상태에 쓰지 않는다. 이미 시작된 저장·라우팅의 최신 상태 검사는 A의 책임이다.

## 독립 샘플

`app/src/debug/java/io/kailand/feature/briefing/BriefingSamples.kt`에서 Preview를 연다. 실행 가능한 Preview는 APK에 패키징된 원본 `content/catalog.json`과 `fixtures/ui_samples.json`을 IO coroutine에서 읽는다. 최초 렌더는 로딩일 수 있으므로 Interactive 또는 Run Preview로 확인한다. 별도 앱·Manifest·AppRoot 연결은 필요 없다.

regional, daily, missing_metrics, restricted_pending, fresh에 각각 Preview가 있다. fresh는 도착 기록 없음이다. 같은 fixture에서 UNKNOWN, 재방문, ID 누락, 첫 StorageFailure 후 성공 재시도 변형을 구성했다. 재시도 샘플은 닫기와 아이템 보기 모두 첫 동작 실패 후 다음 동작에 성공한다. 성공 표시도 실제 저장·이동이 없는 샘플임을 명시한다. 샘플은 debug에만 있고 공용 developmentGate로 Automotive/car mode/background를 차단한다.

## 로컬 검증 (2026-11-03, PM Mac)

- macOS 27.2 arm64, JBR 25.0.4 / Gradle 9.3.1. 공용 버전 업그레이드 없음.
- P01 집중 JUnit 시험 13개 PASS. 이후 샘플의 플랫폼·foreground 제한 보완을 포함해 `python3 scripts/check_all.py --android` PASS.
- 최종 Python 34개, Android 17개(P01 13개 + 기존 gate 4개), 실패·오류·skip 0. `:app:testDebugUnitTest` 실제 실행. debug APK 빌드 성공, 45 actionable tasks 중 10 실행·35 UP-TO-DATE. 캐시 결과를 신규 시험 개수에 더하지 않았다.
- 단위 시험은 fixture 원본의 표시·누락·0·재방문·ID 오류·수치 경계와 비동기 중복·실패·취소·세션 폐기를 검증한다. 실차·저장 통합 시험을 대체하지 않는다.
- 기존 AVD가 이번에는 ADB `device`로 연결됐고 일반 Android 홈을 관찰했다. 초기 offline 원인은 확정하지 않았다. Wipe Data·AVD 삭제·전체 ADB 서버 중단은 하지 않았다.
- Compose Preview에서 DAILY, 측정값 누락, RESTRICTED 화면을 직접 관찰했다. 지역/기타 일부 Preview는 로딩 상태로 보였다. Preview를 기기에 실행하는 UI 조작은 반영을 확인하지 못해 P01 에뮬레이터 실행·실패/재시도 버튼·Back·창 크기/글자 크기 시험은 NOT RUN이다. 화면 인수 완료로 기록하지 않는다.
- A01 개발 홈→여정→복귀·정식 MainActivity 실행·A04 실제 연결·실제 Pleos/SDK/차량 gate/release는 이번에 NOT RUN. 다음은 일반 에뮬레이터의 독립 Preview 실행과 P01 화면/실패·재시도 수동 확인이다.


## 독립 기기 인수 후속 (2026-11-03)

[기기 인수표](P01_UI_ACCEPTANCE.md)의 14개 사례를 일반 ARM64 API37 에뮬레이터에서 확인했다. 정적 Preview 로딩 관찰과 별도로, 실제 PreviewActivity foreground·화면/UI dump·실패/재시도·Back·중복 입력·작은 창/큰 글자를 기록했다. A04 실제 저장/라우팅·Pleos/차량 gate/release는 NOT RUN이다.

기존 debug APK에는 exported `androidx.compose.ui.tooling.PreviewActivity`와 `BriefingSamplesKt` 메서드가 들어 있다. IDE Run Preview 반영이 어려우면 대상 일반 에뮬레이터에서 아래처럼 실행한다. `-f 0x10008000`은 사용자 저장 데이터 삭제 없이 새 Activity task로 샘플 세션을 시작한다.

```bash
adb -s emulator-5554 shell am start -W -f 0x10008000 \
  -n io.kailand/androidx.compose.ui.tooling.PreviewActivity \
  --es composable io.kailand.feature.briefing.BriefingSamplesKt.RetryBriefingPreview
```

이번 샘플 보완은 상태 표시줄 겹침을 없애는 안전 영역 여백과 수동 동시 입력 관찰을 위한 모의 대기 2.5초다. 성공·실패 정책과 공개 화면 계약은 바뀌지 않았다. 설치 직후 첫 Preview가 흰 화면이면 같은 Activity 새 task 실행을 재시도한다. 이는 관측한 도구 현상의 복구 방법이며 원인이나 정식 앱 시작 성공을 확정한 것은 아니다.
