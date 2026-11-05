# C04 수동 시연 제어

> 2026-11-05 현재: main a04b3c5를 C 브랜치에 통합했다. A04의 C 기능/AppRoot 연결과 A05 교차 설치 인수는 완료됐으며 아래 대기 문구는 과거 기록이다. 최신 C05 전달 범위·237개 회귀 및 실제 앱 미완료는 [C05 현재 판정](c05_acceptance.md)과 [A06 회신](C05_A06_REPLY.md)을 따른다. 자연색 그래픽은 프로토타입이다.


상태: 2026-11-04 독립 Runner·DemoPanel·가짜 A 포트 시험 구현. A02 실제 포트 및 AppRoot 연결·기기 인수는 대기한다. C02/C03 PR #5의 main 병합 `8c78b8e`를 같은 파트 브랜치에 반영했다.

## 범위와 공개 표면

- `feature/demo/DemoScenarios.kt`: PM의 `fixtures/scenarios.json`을 C 로컬 계획으로 읽는다. 시나리오 UUID·routeRevision·evidence는 A 입력으로 옮기지 않는다. 명시적 DEMO만 허용하고 완료 시각·null 측정값·기대 효과·선행 관계를 보존한다. 파싱 실패는 A의 오류 경계에서 보여 주며 Preview/빈 게임 상태로 대체하지 않는다.
- `DemoScenarioRunner.kt`: JourneyPort·DemoControlPort와 읽기 전용 GameSnapshot 공급자, A 소유 전체 초기화 콜백만 사용한다. GameService나 다른 feature 내부를 import하지 않는다. 한 번의 수동 입력마다 포트 응답을 await하고 다음 단계는 자동 실행하지 않는다.
- `DemoPanel.kt`: 초기화 확인 대화상자, 시나리오 선택, 목적지 요청 → READY 확인 → 출발 → 완료/저장 재시도, 같은 도착 반복, 모의 여정 취소를 제공한다. 자체 스크롤이 있으므로 A는 높이가 제한된 컨테이너에 배치한다. 실제 gate를 바꾸는 버튼·자동 재생·유료 서비스·저장 직접 접근은 없다.

## 연결 예제

아래는 A가 실제 owner에 맞게 연결할 문서 예제다. AppRoot·공통 계약·Manifest·테마·Gradle을 C에서 수정하지 않았다. 현재 main에는 A02 구현이 없어 실제 앱은 여전히 A01 임시 연결 자리이며, 가짜 포트는 단위 시험에만 있다.

```kotlin
// A의 IO/로딩 경계에서 한 번 읽는다. fixtures의 arrival UUID를 live ID로 쓰지 않는다.
val plans = DemoScenarios.fromJson(loadedScenarioJson)
// 실제 포트/카탈로그가 바뀌면 기존 Runner를 먼저 중단하고 새 owner를 연결한다.
val runner = DemoScenarioRunner(
    scenarios = plans,
    catalog = loadedCatalog,
    journey = journeyPortFromA,
    demo = demoControlPortFromA,
    readGame = { currentLoadedGameSnapshotOrNull },
    dailyDestination = { currentDailyDestinationFromAOrNull },
    resetGame = { /* A가 초기화 조정을 끝까지 await한 뒤 RequestResult 반환 */ },
)
DemoPanel(runner, currentGate, currentSource, Modifier.fillMaxSize())
```

실제 Runner는 ViewModel 등 유지되는 owner에 한 번 생성한다. 위 생성 코드를 매 재구성/화면 진입마다 실행하지 않는다. 공급자 람다는 최신의 로드된 snapshot·일상 목적지를 읽어야 한다. REGION은 전달된 Catalog 지역의 좌표를 사용하고 DAILY는 A가 고른 regionId=null 목적지를 사용한다. 일상 목적지가 없으면 요청하지 않고 안내하며 C가 좌표를 만들지 않는다. READY는 요청 시 캡처한 목적지·종류와 새로운 A 발급 여정 ID가 일치해야 한다.

## 수동 순서와 결과 확인

1. D01/D03/D06의 `resetBefore`는 실제 초기화를 실행하는 명령이 아니라 확인이 필요한 단계다. 사용자 확인 후에만 A의 전체 초기화를 호출한다. 실패 시 새 시연 입력은 계속 차단하고 별도 확인으로 재시도한다.
2. 목적지 Accepted는 접수다. 같은 버튼 호출에서 출발·완료·보상을 자동 실행하지 않는다. 새 요청과 맞는 READY를 수동 확인한 후에만 모의 출발을 요청한다.
3. Complete는 PM의 완료 시각·누적 측정값을 그대로 DemoInput.Complete에 넣는다. null은 0으로 대체하지 않는다. 저장 실패 뒤에는 JourneyPort.retryArrival로 원본 도착만 재시도하며 새 여정·새 UUID·새 Complete를 만들지 않는다.
4. Accepted가 돌아와도 같은 여정의 ARRIVED와 B의 현재 pendingReceipt, fixture의 새 성장/소유/발견 효과가 확인되어야 완료로 표시한다. 읽기 실패/null·잘못된 영수증·예상치 못한 효과는 성공 처리하지 않는다. 이 비교는 검증이며 C의 보상 계산·게임 저장이 아니다.
5. D04는 D03이 실제로 끝났고 그 영수증이 현재 마지막 도착일 때만 선택한다. A에 RepeatLastArrival를 실제 dispatch하여 await한다. phase가 이미 ARRIVED여도 생략하지 않는다. 원본 영수증 전체와 여정 ID 보존·새 지급 0을 확인한다. D02·D05도 PM의 선행 시나리오 완료가 필요하다.

## 중단·초기화 경계

gate UNKNOWN/RESTRICTED, 비foreground, 화면 이탈, mode 변경에서는 세대 증가·대기 중 시연 job cancel·진행 선택/선행 결과 폐기를 수행한다. 복귀 뒤 자동 재개하지 않는다. 취소를 무시하고 늦게 돌아온 포트 응답도 C 상태를 전진시키지 못한다. 별도 timer·자동 시나리오 job은 없다. A는 포트 안에서도 실제 gate·foreground·generation을 재검사해 늦은 쓰기를 차단해야 한다.

전체 초기화의 완료 토큰은 시연 세대와 분리되어 있다. 모드 변경이 진행 중 초기화의 정상 완료를 무효화하지 않는다. 새 reset만 이전 토큰을 무효화하며, 성공하더라도 현재 gate/모드/화면 조건을 우회하지 않는다.

- C 패널에서 확인한 초기화: `resetConfirmed()`가 이 Runner의 시연 job을 먼저 중단·차단하고 `resetGame`을 await한다. 이 콜백은 A의 확인 완료된 초기화 조정 경로다. A가 다른 Runner/방문/여정을 중단하고 B 초기화를 기다린 뒤 Accepted/Failed를 반환한다. **요청을 시작한 같은 Runner에 beginReset을 중복 호출하거나 그 요청 job을 join하지 않는다.** C는 다른 owner의 job을 잠금 안에서 기다리지 않는다.
- 다른 화면 등 외부 초기화: A가 `val token = runner.beginReset()`으로 중단·차단한 뒤 실제 조정을 실행하고 `runner.finishReset(token, success)`로 결과를 알린다. 해당 토큰만 사용하고 이전 요청의 완료 통지로 새 초기화를 풀지 않는다.
- 패널 내부의 reset await는 일반 시나리오 job과 분리한다. 시연 완료 응답을 기다리던 중에도 초기화가 그 job의 종료를 join하지 않고 진행할 수 있다. UI coroutine 취소로 초기화 결과를 받지 못하면 fail-closed를 유지한다. A는 유지되는 owner에서 실제 완료 통지를 연결해야 한다.
- C는 A의 조정 Mutex를 보유하지 않으며 C의 synchronized 구간에는 suspend 포트 호출·job join이 없다. 실제 교착·재지급 통합(U20)은 A 연결 후 확인한다.

## 검증과 남은 것

집중 시험은 실제 Catalog와 시나리오 원본을 읽고 가짜 A 포트·읽기 모델로 수동 순서, 새 ID/목적지, nullable 측정값, 저장 await/실패, 원본 replay, gate/PLEOS, 중복 입력, 이탈·늦은 응답, 초기화 대기/실패/세대/모드 조합을 확인한다. fake가 바꾼 snapshot은 저장 구현·실제 B 통합 성공의 근거가 아니다.

실행 결과: Windows / 공통 JBR 25.0.4·wrapper 9.3.1. 집중 `:app:testDebugUnitTest --tests io.kailand.feature.demo.DemoScenarioRunnerTest :app:assembleDebug`는 1차 19개·보강 후 22개 PASS, 두 빌드 성공. 마지막 fixture 체인 시험까지 포함한 전체 `py -3 scripts/check_all.py --android`는 exit 0, Python 44개·Android 106개(C04 23개 포함) PASS·debug 빌드 성공(2 tasks executed / 43 up-to-date, APK task UP-TO-DATE). 최종 production 코드는 직전 집중 빌드에서 실제 컴파일했다(8 tasks executed / 37 up-to-date). 새 B DataStore 시험도 전체 집계에 포함되지만 C04와 실제 B를 연결한 통합 시험은 아니다.

NOT RUN: A02/A04 포트와 AppRoot 연결, 실제 B 저장·브리핑, 일상 목적지 공급, reset 조정, 기기 클릭/뒤로/회전/background·모드 전환·접근성 및 S09/U20 통합, Pleos·release·운전 안전 시험. 이 작업에서는 Preview/기기 시연을 새로 수행하지 않는다.

다음: A가 위 owner·읽기 공급자·두 포트·전체 초기화 경로를 연결한 뒤 D01~D06과 D03 직후 D04를 반복한다. 독립 C04는 push하되 새 PR은 사용자 확인 후 생성한다. C05는 연결된 기기 흐름·백업 설치와 반복 인수를 이어간다.

## 저장 없는 UI 시연

`src/debug/.../demo/DemoPanelPreview.kt`는 별도 PreviewActivity에서 실행하는 격리 샘플이다. 실제 packaged Catalog/시나리오를 읽고 가짜 JourneyPort로 D03 목적지 요청·READY·출발을 보여 준다. 초기화 확인은 샘플 여정만 비우며 GameService나 파일을 사용하지 않는다. 완료·저장 재시도는 의도적으로 Failed를 반환하므로 완료/지급 성공을 꾸미지 않는다. DAILY 목적지는 공급하지 않는다. AppRoot, 실제 gate, A/B 구현에는 연결하지 않는다.

Android Studio의 Layoutlib에서는 `Asset repository is not set`으로 자산 로드가 실패할 수 있다. 이 경우 Run Preview 또는 기존 에뮬레이터의 PreviewActivity로 실행한다. 이 시연은 실제 A/B 통합 인수와 구분한다.

2026-11-04 Windows의 기존 API 37 에뮬레이터에서 debug APK 설치 및 PreviewActivity 실행 성공. D03 선택 → 초기화 확인 → 목적지 요청 → READY 수동 확인 → 모의 출발 → 완료 요청을 직접 클릭했다. `ui-sample-1`이 유지되며 완료 요청은 연결 대기 오류와 같은 도착 저장 재시도 단계로 표시됐다. 실제 게임 저장·보상은 호출하지 않았다. 실제 A/B 통합, 차량·Pleos·release·안전 시험은 여전히 NOT RUN이다.

## A02 main 병합 후 로컬 경계 검증

2026-11-04 A02/B03/C04 PR이 병합된 main `b8faf56`을 기존 C 브랜치에 fast-forward했다. C01~C03 디자인·패널 접기·C05 및 환경 문서 수정은 stash로 보존 후 충돌 없이 복원했다. 위의 A02 미구현/PR 대기 표시는 이전 구현 시점의 기록이며, 현재 C04 PR #8과 A02 PR #10은 main에 병합됐다. 새 commit/push는 하지 않았다.

앱 조립 경계의 **임시 JVM 검증 하네스**에서 실제 `DemoJourneyAdapter` + `DemoScenarioRunner` + `BGameService`/Preferences DataStore를 연결했다. 사용자 저장 대신 JUnit 임시 파일만 사용했다. C feature에는 A/B 구현 import를 추가하지 않았고 AppRoot·공통 계약·B 저장·빌드 파일은 수정하지 않았다. 하네스가 B의 기존 시험 주입 생성자로 실제 저장을 구성했으며, 이 시험용 조립은 production A04 구현이 아니다. 하네스·init script는 검토 ZIP에 포함하고 저장소에는 추가하지 않는다.

별도 5개 시험 PASS:

- D01→D02: A 발급 UUID로 저장, 동일 날짜 성장 1회, 원본 두 도착 보존.
- D03→D04→D05: 최초 기념품 3개, 실제 Repeat 명령 재전달·원본 command/receipt 유지·추가 효과 0·반복 전후 저장 파일 바이트 동일, 재방문 기념품 추가 없음.
- D06: 시간·거리 null과 원본 영수증을 실제 파일까지 보존.
- 실패 대역→재시도: 첫 호출의 StorageFailure를 명시적으로 주입한 뒤 원본 A 명령 그대로 재시도, 실제 B 저장 1개·기념품 3개. 실제 디스크 장애 주입 시험은 아님.
- 취소·UNKNOWN: 취소 후 도착 없음, C와 A의 제한 검사로 도착/보상 호출 차단.

임시 소스셋을 추가하는 로컬 init script로 `:app:testDebugUnitTest --tests io.kailand.app.integration.C04A02VerificationTest` exit 0(5 PASS, BUILD SUCCESSFUL in 2s). 최초 Java 소스셋 연결에서는 Kotlin 시험을 찾지 못했고, Kotlin 소스셋에 연결해 해결했다. 공통 Gradle 파일은 변경하지 않았다.

그 뒤 init script 없이 `py -3 -X utf8 scripts/check_all.py --android` exit 0: Python 44개·Android 165개 PASS, debug 빌드 성공(7 executed / 38 up-to-date). Android 165개는 새 main과 보존한 로컬 C05 시험을 포함하며 별도 5개 하네스 시험은 포함하지 않는다. 로컬 디자인을 포함한 working tree 결과로, 깨끗한 main만의 검증으로 주장하지 않는다.

남은 것: production AppRoot의 C04 패널·실제 B 상태/일상 목적지/전체 초기화 조립, 초기화와 저장/Runner 경쟁 U20, UI에서 D01~D06·S09 반복, B 외형 일치, release/물리 기기/Pleos/안전 검증은 여전히 NOT RUN이다. A02 기본 개발 화면은 메모리 도착 대역을 유지하므로 앱에서 실제 보상이 연결됐다고 설명하지 않는다.
