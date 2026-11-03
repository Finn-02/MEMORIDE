# C02 가상 방문 상태·동의·시간 경계 준비

상태: C02 독립 VisitController·Compose 화면·FakeClock 시험 구현. 실제 검사 결과는 아래 기록을 따른다. AppRoot 연결·기기 UI 흐름은 NOT RUN.
기준: A01과 C01을 포함한 C 브랜치 `97fbd98` 이후 C02 로컬 구현, [방문 시나리오](07_simulation_visit.md), [C 역할](../roles/C.md), [UI 계약](../contracts/ui_contracts.md).
아래 타입은 C 소유 로컬 API이며 공통 DTO·게임 저장 계약을 변경하지 않는다.

## 구현과 A 연결

- `feature/visit/VisitController.kt`: 한 세션의 요청·동의·종료, UUID/generation, 15초 만료·30초 쿨다운, reset 잠금. 모든 변경 이벤트를 짧은 synchronized 구간에서 처리하며 외부 job join·비동기 저장·네트워크 호출은 없다.
- `feature/visit/VisitRuntime.kt`: Android `SystemClock.elapsedRealtime()`과 main Handler를 주입한다. 응답 대기 중 예약 콜백은 최대 1개다. 최대 1초마다 절대 만료 시각에서 남은 시간을 다시 계산하고, 수락·종료·이탈에서는 제거한다. Handler의 tick 횟수나 epoch가 만료 근거가 아니다.
- `feature/visit/VisitScreen.kt`: `VisitRoute`가 lifecycle·최신 gate·mode·컨트롤러를 연결하고 `VisitScreen`이 순수 상태·콜백으로 그린다. 내 차/가상 앞차 시점 버튼은 같은 세션의 보기만 바꾼다. 수락·거절은 같은 크기이며 동의한 VISITING에서만 주입 슬롯을 표시한다. 캐릭터 숨김 선택은 rememberSaveable로 유지하도록 구성했다. 외형은 최신 AvatarPreview를 그대로 전달하고 슬롯은 160dp 정사각 영역이다.
- debug 전용 `VisitPreview.kt`: 대기·방문 가로·큰 글자·UNKNOWN의 네 정적 Preview. 이 작업에서 렌더링 관찰·클릭 시험은 NOT RUN이며 컴파일과 구분한다.
- `VisitControllerTest.kt`: FakeClock/수동 scheduler로 만료·쿨다운·늦은 이벤트·동시 요청·제한/이탈/모드·초기화·표시 정책을 검증한다. 실시간 sleep은 사용하지 않는다.

A는 AppRoot 등 유지되는 소유 범위에서 controller를 한 번 만들고 실제 입력을 연결한다. 아래는 문서 예제이며 AppRoot 코드를 수정하지 않았다. ViewModel 등으로 controller를 보존하면 화면 왕복 중 쿨다운도 보존할 수 있다. 단순 remember는 해당 composition 수명만 보존하므로 Activity 재생성까지 영속 보존하는 것으로 설명하지 않는다. 프로세스 재시작 후 방문은 복원하지 않는다.

```kotlin
// A의 유지되는 owner에서 한 번 생성. 화면 분기/재구성마다 생성하지 않는다.
val visitController = remember { createVisitController() }
// A의 방문 화면 분기에서, 높이가 제한된 컨테이너에 배치한다.
VisitRoute(
    controller = visitController,
    gate = currentGate,
    mode = currentSource,
    avatar = currentAvatarFromB,
    avatarSlot = avatarSlotFromA,
    onBack = { /* A의 이전 화면 이동 */ },
    modifier = Modifier.fillMaxSize(),
)
```

VisitRoute는 클릭 처리 직전에 lifecycle·gate·mode를 controller에 갱신한다. 모드가 바뀐 첫 프레임은 이전 모드의 슬롯을 표시하지 않는다. lifecycle 비활성·화면 제거에서는 세션과 타이머를 취소하고 observer를 해제한다. 동일 컨텍스트의 재구성은 카운트다운 갱신을 반복 발행하지 않는다. A가 화면보다 먼저 제한/모드 전환을 처리할 때도 같은 controller의 `updateContext`/`leaveScreen`을 호출해 취소한다. 단순 화면 재진입으로 쿨다운을 비우지 않는다.

전체 초기화 연결은 A가 `val resetGeneration = visitController.beginReset()`으로 방문을 먼저 차단한 뒤 자기 초기화 절차를 수행하고 `finishReset(resetGeneration, success)`로 결과를 통지하는 방식이다. 실패·오래된 세대의 성공 통지는 입력을 열지 않는다. 새 전체 초기화가 시작되면 새 토큰을 사용한다. C가 B 저장소를 직접 지우거나 A의 초기화 잠금을 보유한 채 취소를 기다리지 않는다. 이 연결은 아직 NOT RUN이다. `restartDemo()`는 진행자 C04의 명시적 방문 재시연 연결용이며 일반 VisitScreen에는 버튼을 넣지 않았다.

## 실제 검사 기록

- 1차 `gradlew.bat --no-daemon :app:testDebugUnitTest --tests io.kailand.feature.visit.VisitControllerTest :app:assembleDebug`: exit 0, C02 단위 시험 20개·debug 빌드 PASS(16 tasks executed / 29 up-to-date).
- 동일 컨텍스트 재발행 방지·숨김 정책과 관련 시험을 추가한 전체 `py -3 scripts/check_all.py --android`: exit 0, Python 34개·Android 33개(A01 4+C01 8+C02 21) PASS, debug 빌드 성공(9 tasks executed / 36 up-to-date). 제한 화면 문구의 마지막 조정 후 `:app:testDebugUnitTest :app:assembleDebug`도 exit 0, Android 33개 PASS·debug 빌드 성공(7 tasks executed / 38 up-to-date).
- 단위 시험은 Android lifecycle·Compose 클릭/회전/복원·실제 Handler 계측 시험을 대신하지 않는다. AppRoot와 B 렌더러 연결, 실제 기기 S08~09 및 전체 게임 초기화 U20 통합은 NOT RUN이다.

## 첫 구현 단위

한 프로세스의 같은 VisitController를 발신·수신 화면에서 읽는다. 화면에는 개발 파트 A/B 대신 내 차·가상 앞차를 표시한다. DEMO 가상 방문이며 실제 두 차량·번호판·통신·마이크·위치 권한을 사용하지 않는다. SDK 여정의 출처를 가상 방문의 실제 통신 증거로 쓰지 않는다.

첫 범위는 요청 목적, 수락·거절·철회·만료·닫기, 활성 요청 1개, 같은 대상 재요청 제한이다. 캐릭터는 A가 주입한 슬롯 또는 회색 임시 슬롯으로 표시한다. 방문 횟수·수락·종료는 GameAction·보상·GameService·저장을 호출하지 않는다.

2026-11-03 팀 회의 A안에 따라 B가 사전 렌더링 PNG의 공통 렌더러를 만들고 A가 슬롯으로 연결한다. C는 별도 3D 모델·캐릭터 렌더러·PNG 세트를 만들지 않는다. 동일 AvatarPreview·avatarSlot으로 섬·동행·방문의 외형과 의상을 확인하되, 방문 캐릭터는 동의 전·거절·만료·철회 뒤 표시하지 않는다. 첫 렌더러 인수 때 작은 영역의 정렬·gate 중단과 함께 검증한다. 계약·일정 변경은 없으며 해당 화면 연결 검증은 아직 NOT RUN이다.

## 화면과 문구

```text
DEMO 가상 방문 · 주차 상태의 모의 체험
[내 차 시점] [가상 앞차 시점]

발신: 대상 가상 앞차 / 목적 주행 관련 메시지
      [방문 요청] → 요청을 보냈어요 [요청 철회]

수신: 뒤 차량의 캐릭터가 주행 관련 메시지를
      전하고 싶어 해요. 받아보시겠어요?
      남은 시간 표시
      [수락] [거절]  ← 동등한 크기와 접근성

수락 후: [고정 캐릭터 슬롯]
      혹시 추월 중이 아니라면, 안전할 때
      주행차로를 확인해 주실 수 있을까요?
      [대화 닫기]
```

내 차·가상 앞차 시점 선택은 같은 가상 장면 안의 보기 전환이다. 별도 요청이나 동의를 만들지 않는다. S07 자체 이탈·background에서는 방문을 취소한다. 무응답은 동의가 아니며 거절에 불이익을 주지 않는다. 메시지 열람 수락은 운전 행동 약속이 아니다. 자유 입력·STT·LLM·위반 판정은 넣지 않는다.

종료 문구는 거절됨·시간 만료·요청 철회·이용 제한으로 종료·화면 이탈로 종료·대화 종료를 구분한다. 거절·만료 뒤 자동 재요청하지 않는다. 색상 외에 텍스트로 상태를 알리고, 수락 전·종료 후 방문 캐릭터는 보이지 않는다.

## C 내부 상태와 시간

| 요소 | 내부 설계안 |
|---|---|
| 요청 | requestId, senderId, receiverId, purpose, presetMessageId, createdAtMonotonicMs, expiresAtMonotonicMs |
| 상태 | IDLE, REQUESTED, ACCEPTED, VISITING, REJECTED, EXPIRED, WITHDRAWN, CANCELLED, COMPLETED |
| 취소 이유 | 제한·상태 미확인·화면 이탈·background·모드 전환·초기화 |
| 활성 요청 | REQUESTED / ACCEPTED / VISITING 중 하나인 세션 최대 1개 |
| 만료 | 생성 시각 + 15,000ms. 수락 가능한 구간은 now < expiresAt. 정확히 만료 시각이면 거부 |
| 쿨다운 | 성공적으로 생성된 요청부터 같은 대상에게 30,000ms. 거절·철회·만료·닫기로 단축하지 않음 |
| 시간 입력 | production은 Android monotonic clock, 시험은 수동 FakeClock. epoch·기기 날짜를 사용하지 않음 |
| 무효화 | 세션 generation과 매번 새 requestId. 초기화 후에도 예전 ID를 재사용하지 않음 |

쿨다운 시작 기준과 정확한 경계는 C 내부 구현 선택이다. 실패·쿨다운 거부·중복 클릭은 새 요청 ID나 새 쿨다운을 만들지 않는다. 단일 발신·수신으로 시작하며 추후 가상 역할을 바꿔도 활성 요청 제한은 controller 전체에 적용한다. 재요청 가능 여부와 활성 세션 여부를 별도로 검사한다.

수락 순간 만료 조건을 통과하면 요청 만료 타이머를 해제한다. 15초는 답변 대기 제한이며 이미 시작된 대화를 15초에 만료시키지 않는다. 첫 구현에는 별도의 방문 지속시간·자동 보행·자동 닫기 타이머를 추가하지 않는다.

## 이벤트 처리와 전이

입력은 하나의 직렬 처리 경로에서 검사하고 상태를 교체한다. A가 제공하는 현재 gate·화면 활성 상태를 사용하며 UI 버튼이 활성이라는 이유만으로 명령을 허용하지 않는다. 공통 DTO에 foreground나 방문 필드를 추가하지 않는다.

| 현재 상태 / 입력 | 검사 | 결과 |
|---|---|---|
| IDLE 또는 종료 상태 / 요청 | PARKED_ALLOWED, foreground, S07 활성, 초기화 중 아님, 대상 쿨다운 종료 | 새 ID·generation 캡처, REQUESTED, 만료 예약 |
| 활성 요청 존재 / 새 요청 | controller 전체 활성 1개 | 거부, 기존 세션·만료·쿨다운 보존 |
| REQUESTED / 수락 | 현재 requestId·generation 일치, 최신 gate 허용, now < expiresAt | ACCEPTED → VISITING, 요청 만료 job 해제, 캐릭터 표시 |
| REQUESTED / 거절 | 현재 ID·generation 일치 | REJECTED, 타이머 해제, 캐릭터 없음 |
| REQUESTED / 철회 | 현재 ID·generation 일치 | WITHDRAWN, 타이머 해제, 캐릭터 없음 |
| REQUESTED / 시간 확인 | now >= expiresAt | EXPIRED, 캐릭터 없음 |
| VISITING / 대화 닫기 | 현재 ID·generation 일치 | COMPLETED, 캐릭터 제거 |
| 활성 상태 / 제한·이탈·모드 전환 | 현재 컨텍스트 무효화 | CANCELLED, 타이머 해제, 캐릭터 제거 |
| 임의 상태 / 오래된 ID·generation의 이벤트 | 현재 세션과 불일치 | 무시, 새 세션·쿨다운·타이머 보존 |
| 종료 상태 / 동일 ID 수락·거절·닫기 반복 | terminal 상태 | 무시, 방문 재생성 없음 |

유효한 요청 이벤트 처리 때도 시간을 확인한다. 정확히 만료된 요청의 수락은 EXPIRED로 끝낸다. 만료된 요청에 대한 거절·철회도 먼저 EXPIRED로 정규화한다. 오래된 ID의 이벤트는 현재 요청의 상태를 바꾸지 않는다. 수락 뒤 오래된 만료 알림은 현재가 REQUESTED가 아니므로 무시한다.

ACCEPTED는 동의를 기록하는 짧은 내부 전이이며 별도 대기 연출을 두지 않는다. VISITING 진입과 캐릭터 표시 전에 유효 컨텍스트를 유지한다. 대기하는 연출을 나중에 넣으면 시작 콜백에서도 ID·generation·gate를 재검사해야 한다.

취소·거절·만료가 먼저 직렬 처리되면 이후 수락은 무효다. 수락이 유효하게 먼저 처리되었어도 이후 제한·이탈·철회 등 취소 조건에서는 방문을 종료한다. VISITING 중 발신 철회는 WITHDRAWN으로 종료하고 캐릭터를 제거한다. 이벤트 순서와 관계없이 종료 후 캐릭터가 다시 나타나지 않게 한다.

## 타이머와 초기화

만료 알림은 캡처한 ID·generation이 현재 REQUESTED와 같은지, monotonic now가 만료 시각에 도달했는지 확인한다. 일찍 실행된 알림은 만료시키지 않고 남은 시간을 다시 예약한다. 카운트다운 표시는 시간 차이로 계산하며 타이머 tick 횟수를 진실로 사용하지 않는다.

세션 종료·S07 이탈·background·실제 제한·모드 전환에서 C의 예약 job을 취소한다. 복귀하거나 gate가 다시 열려도 이전 요청·방문을 자동 복원하지 않는다. 새 요청은 새 ID와 유효한 쿨다운 조건을 요구한다. 모드 전환이나 이탈로 쿨다운을 초기화하지 않는다. 프로세스 재시작에서는 임시 방문 세션을 복원하지 않는다.

방문 분기 재시연용 버튼 이름은 '가상 방문 다시 준비'로 한다. 진행자 시연에서만 명시적으로 사용하고, generation 증가·현재 세션 취소·타이머 취소 후 방문 로컬 상태와 쿨다운을 비운다. gate와 화면 활성 조건이 충족될 때만 새 시연 요청을 받는다. 실제 제한을 해제하거나 B의 게임 상태를 지우지 않는다.

전체 게임 초기화는 A의 확인·조정 경로에 요청한다. C는 A의 초기화 시작 통지를 받으면 새 입력 차단·로컬 generation 증가·방문 취소를 수행하고, A가 완료를 알리기 전에는 다시 열지 않는다. 실패 시 자동 재개하지 않는다. A의 조정 잠금을 잡은 채 C의 job 종료를 기다리지 않으며, 자기 job을 join하지 않는다. 구체적인 연결 표면은 A01 이후 현재 계약으로 맞춘다. [초기화 순서](18_final_review.md)의 의미를 유지하고 C가 게임 저장 파일을 직접 지우지 않는다.

## FakeClock 시험 예제

아래 r1/r2와 g1/g2는 설명용 기호다. 실제 ID·게임 데이터·fixture를 추가하는 것이 아니다. 각 행은 별도 초기 상태로 시작하되 한 행 안의 사건은 순서대로 수행한다. 기본 컨텍스트는 이용 허용·foreground·S07 활성이다. 실제 sleep 없이 FakeClock과 수동 scheduler로 시간을 바꾼다.

| 예제 | 사건 순서 | 기대 결과 | 관련 시험 |
|---|---|---|---|
| V01 | t=0 요청 r1, 수락 전 관찰 | REQUESTED, 캐릭터 없음 | U07, S08 |
| V02 | r1 거절 → r1 늦은 수락 | REJECTED 유지, 캐릭터 없음 | U07 |
| V03 | r1 철회 → r1 늦은 수락 | WITHDRAWN 유지, 캐릭터 없음 | U07 |
| V04 | r1 생성 t=0 → t=14,999 수락 | VISITING, 슬롯 1개, 방문 보상·저장 없음 | S08 |
| V05 | r1 생성 t=0 → t=15,000 수락, 만료 job은 아직 실행 안 됨 | EXPIRED, 캐릭터 없음. 타이머 실행 지연과 무관 | U07 |
| V06 | r1 생성 → t=15,001 만료 → 늦은 수락 | EXPIRED 유지, 캐릭터 없음 | U07 |
| V07 | r1 생성 → r2 동시 요청, 순서를 바꿔 재현 | 직렬 처리된 첫 요청만 활성, 나머지는 거부 | U08 |
| V08 | t=0 r1 생성·거절 → 같은 대상 t=29,999 요청 → t=30,000 요청 | 첫 재요청 거부, 경계에서 새 r2 허용 | U08 |
| V09 | r1 수락 후 t=30,000 새 요청 | 쿨다운이 끝나도 활성 방문 때문에 거부 | U08 |
| V10 | r1 종료 → t=30,000 r2 → r1 수락·만료·닫기 | r2 상태·타이머 그대로, r1 캐릭터 없음 | U07~08 |
| V11 | REQUESTED 또는 VISITING에서 UNKNOWN/RESTRICTED → 이용 허용 | 취소 후 캐릭터 없음, 자동 재개 없음 | U16, S06 |
| V12 | REQUESTED 또는 VISITING에서 background·이탈·모드 전환 각각 재현 | 취소·타이머 종료, 복귀해도 수락 없이 방문 없음 | S04, S09 |
| V13 | g1/r1 요청 → 방문 다시 준비 → g2/r2 → g1/r1 콜백 | r2 보존, 이전 세대 이벤트 무시 | S09 |
| V14 | 전체 초기화 시작 → 수락·새 요청 → 초기화 실패 | 방문 취소, 새 입력 차단 유지, 게임 저장 직접 접근 없음 | U20 입력 보조 |
| V15 | r1 수락 → 닫기 → 수락·닫기 중복, 오래된 만료 job | COMPLETED 유지, 슬롯 없음, 저장·보상 없음 | U07, S08 |
| V16 | 기기 epoch 날짜 앞뒤 변경, monotonic은 동일 | 만료·쿨다운 불변 | U07~08 |
| V17 | 만료 job을 t=14,000에 조기 실행 → t=15,000 실행 | 조기 실행은 REQUESTED 유지, 만료 경계에 EXPIRED | U07 |
| V18 | 수락 뒤 t=15,000 요청 만료 job 실행 → 발신 철회 | 만료 job은 방문 유지, 철회로 WITHDRAWN·슬롯 제거 | U07, S08 |

위 V01~18은 VisitControllerTest의 단위 시나리오에 대응한다. V16은 주입한 FakeClock의 epoch만 바꿔 monotonic 판단의 불변성을 확인하며 실제 단말 날짜 변경 시험은 아니다. V04/V11 등의 캐릭터 표시는 순수 표시 정책을 확인하며 실제 슬롯 호출 횟수·lifecycle·UI 클릭 계측은 NOT RUN이다. 방문 코드에는 저장·보상 의존성이 없지만 B와 연결한 저장 부작용 계측 시험으로 주장하지 않는다. C05의 화면·시연 검증에서는 [품질 기준](09_quality_tests.md)의 S08~09로 연결한다.

## 다음 한 작업

A가 VisitRoute에 실제 gate·mode·외형·슬롯을 연결하고, 기기에서 내 차 요청 → 가상 앞차 수락/거절/만료 → 닫기/철회와 이탈·초기화를 확인한다. B renderer를 받을 때 섬·동행·방문의 외형·의상 일치도 확인한다. 그동안 C03 읽기 전용 월드맵은 독립 진행할 수 있다. 공통 계약·PM fixtures·Catalog·빌드 버전은 유지한다.
