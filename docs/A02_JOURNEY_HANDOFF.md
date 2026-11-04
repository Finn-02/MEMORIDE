# A02 DEMO 여정 연결

`io.kailand.app.journey.DemoJourneyAdapter`는 기존 `JourneyPort`와 `DemoControlPort`를 구현한다. 공통 DTO 변경은 없다. 목적지 요청은 접수만 반환하고, 준비 완료는 `state.phase == READY`로 관찰한다. `Complete`, `RepeatLastArrival`, `retryArrival`은 주입된 도착 처리 결과를 기다린다.

## A가 생성하고 C에는 포트만 전달

```kotlin
val adapter = DemoJourneyAdapter(
    scope = appScope,
    regionIds = catalog.regions.map { it.id }.toSet(),
    interactionAllowed = { latestGate.canInteract && foreground },
    completeArrival = arrivalHandler,
)
val journey: JourneyPort = adapter
val demo: DemoControlPort = adapter

// C의 클릭 코루틴에서 호출. 목적지는 Catalog에서 선택한다.
val requested = journey.requestDestination(destination, JourneyKind.REGION)
// Accepted가 READY나 지급 성공을 의미하지 않는다. READY를 관찰한 뒤 출발한다.
val started = demo.dispatch(DemoInput.StartTravel)
val completed = demo.dispatch(DemoInput.Complete(completedAtEpochMs, elapsedSeconds, distanceMeters))
// 완료·반복은 반환 결과와 B의 확정된 저장 상태를 함께 확인한다.
val replayed = demo.dispatch(DemoInput.RepeatLastArrival)
```

실제 구현에서 A가 adapter를 생성해 C 화면/시연 패널에 두 포트를 전달한다. C가 A의 구현 클래스를 import하거나 저장소를 생성하지 않는다. 위 호출은 각 상태를 확인한 사용자 단계의 예시이며 자동 연속 실행 예제가 아니다.

## 동작 규칙

- 최초 요청: 새 UUID/revision 0. 같은 종류의 활성 목적지 변경: ID 유지/revision 증가. 활성 종류 변경은 취소 후 새 요청.
- 준비 완료 후에만 모의 출발. READY/TRAVELLING에서만 유효한 모의 도착을 허용한다.
- 도착 확인은 ARRIVED, 저장 실패는 Failed와 오류를 별도로 남긴다. 같은 원본 명령으로 retry하며 새 ID/시각/측정값을 만들지 않는다.
- 같은 세션의 마지막 성공 도착을 Repeat로 재전달한다. phase가 이미 ARRIVED여도 저장 경계를 실제 호출한다. 새 여정·취소·invalidate 뒤 원본 재사용은 거부한다.
- 시간/거리 null은 유지한다. DAILY는 regionId를 null로 정규화한다. 보상·성장 계산은 도착 처리 함수의 소유자 B가 맡는다.
- 최신 gate/foreground가 허용되지 않으면 새 조작을 거부한다. 취소 정리는 가능하다. 이미 시작한 유효 도착 저장은 gate 변화로 버리지 않는다.
- 지연된 준비 결과는 내부 세대로 무효화한다. 공개 Complete에는 세션 토큰이 없으므로 현재 여정의 수동 입력으로 사용한다. A04에서 Runner 취소·세션별 콜백 바인딩과 reset generation 검사를 연결해야 한다.
- `invalidate()`는 진행 중 저장 종료를 기다리고 여정 캐시를 비운다. B 저장 초기화나 C Runner 취소 자체는 수행하지 않는다. 전체 U20 완료를 뜻하지 않는다.

## 실행과 검증 범위

일반 Android debug의 `io.kailand.app.DevelopmentActivity` → 여정 준비. 목적지/종류 선택 → 요청 → 모의 출발/도착/취소. 동행 모의 화면에 같은 상태를 전달하며 Back은 여정 준비로 돌아온다.

개발 화면은 Catalog와 D03 fixture의 모의 완료 입력을 읽는다. `다음 모의 저장 실패`, `시간·거리 측정 없음`으로 실패 재시도와 null 표시를 확인한다. 도착 경계는 debug 전용 메모리 대역이며 실제 GameService·디스크 저장·보상을 수행하지 않는다. 정식 MainActivity는 UNKNOWN을 유지하며 release에 이 대역을 연결하지 않는다.

개발 진입점은 Activity ViewModel에 adapter와 메모리 대역을 보관해 창 크기/방향/글자 배율 변경 때 ID·상태를 유지한다. 화면별 선택 상태는 rememberSaveable로 보관한다. 화면의 `직전 모의 결과`는 마지막 처리 이력이며, 현재 여정의 성공 여부는 명령 반환과 현재 상태를 함께 확인한다. 실제 B 연결 시에는 pendingReceipt가 표시 기준이다.

단위 시험: `:app:testDebugUnitTest --tests io.kailand.app.journey.DemoJourneyAdapterTest`.

실제 B 저장·pendingReceipt·브리핑·reset/Runner 통합은 A04, Pleos는 A03, 서명 release 설치는 A05의 별도 검증이다. 현재 앱은 active journey를 영구 저장하지 않으며 프로세스 재시작 시 새 여정이 필요하다.
