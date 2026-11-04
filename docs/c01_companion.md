# C01 동행 모의 화면 준비

상태: 2026-11-03 C01 독립 화면 1차 구현·컴파일·단위 시험 완료. A의 AppRoot 연결과 기기 UI 흐름은 NOT RUN.
기준: A01 포함 C 브랜치 `29796f6` 이후 C01 구현. 아래 초기 설계는 유지하며 공통 계약을 추가하거나 변경하지 않는다.

## 구현과 A 연결 예제

- 실행 코드: [CompanionScreen](../app/src/main/java/io/kailand/feature/companion/CompanionScreen.kt), [C 내부 표시 변환](../app/src/main/java/io/kailand/feature/companion/CompanionPresentation.kt).
- Debug 전용 [CompanionPreview](../app/src/debug/java/io/kailand/feature/companion/CompanionPreview.kt): 세로 360×800, 가로 800×480, 320×800·fontScale 1.3, UNKNOWN 차단. IDE에서 네 Preview 렌더링 완료와 세로 표시를 관찰했다. 실제 클릭·회전·background 흐름 검증은 아니다.
- [CompanionPresentationTest](../app/src/test/java/io/kailand/feature/companion/CompanionPresentationTest.kt) 8개 PASS: gate·source·phase 조합, background, 숨김 입력 유지, null·0·비정상 측정값, 누적값 출처, 도착과 보상 구분. 실제 UI 상태 복원·lifecycle observer 계측 시험으로 표현하지 않는다.
- 전체 `py -3 scripts/check_all.py --android` exit 0: Python 34개와 Android 12개(A01 4+C01 8) PASS. 이 전체 호출의 APK 빌드 작업은 UP-TO-DATE였다. 이후 작은 창용 숨김 버튼을 지도 앞에 배치한 최종 코드를 `:app:testDebugUnitTest :app:assembleDebug`로 재확인했다: Android 12개 실제 실행 PASS, BUILD SUCCESSFUL(8개 task 실행·37개 UP-TO-DATE).
- push 전 재검사: C 브랜치 HEAD와 C01 선별 파일만 복사한 임시 검증 사본에서 `py -3 scripts/check_all.py --android` exit 0. Python 34개·Android 12개 PASS, debug 빌드 성공(45개 task 모두 실행). 진행 중인 C02 코드와 다른 미커밋 문서는 이 사본에 포함하지 않았다.

A는 기존 AppRoot의 C01 진입 자리에 아래 호출을 연결한다. `journeyFromA`, `avatarFromB`, `currentGate`, `avatarSlotFromA`는 실제 런타임 입력을 뜻하며 아래는 연결용 문서 예제다. C는 AppRoot·Manifest·공통 DTO·테마·의존성·PM fixtures를 수정하지 않았다.

```kotlin
CompanionScreen(
    journey = journeyFromA,
    avatar = avatarFromB,
    gate = currentGate,
    avatarSlot = avatarSlotFromA,
    onBack = { /* A의 이전 화면 이동 */ },
    onHideChanged = { hidden -> /* 필요한 C 화면 가시성 알림만 수신, 게임 저장 없음 */ },
    modifier = Modifier.fillMaxSize(),
)
```

이 화면은 자체 세로 스크롤을 가지므로 A는 높이가 제한된 화면 컨테이너에 배치한다. 숨김은 rememberSaveable로 창 크기 변경에 유지한다. lifecycle observer는 RESUMED 여부를 관찰하고 화면 제거 시 해제하며, background·gate 제한에서는 슬롯을 구성하지 않는다. 타이머·이동 애니메이션·저장 명령은 없다. 현재 앱 메뉴는 A01 임시 화면 그대로이며, 이 구현을 앱 통합 완료로 기록하지 않는다.

## 범위와 연결

- S04는 실제 주차·이용 허용 상태에서 보는 자체 도형 지도다. 실제 내비 오버레이·길 안내·GPS·지도 API를 구현하지 않는다.
- [UI 계약](../contracts/ui_contracts.md)의 CompanionScreen 입력인 JourneySnapshot, AvatarPreview, gate, avatarSlot과 출력 onBack, onHideChanged를 그대로 따른다. 공개 함수의 이름·패키지 확정은 A01 이후 A와 맞춘다.
- AppRoot가 B의 캐릭터를 슬롯으로 전달한다. C는 B 패키지·저장소·GameService를 참조하지 않는다. 처음에는 같은 정사각 영역의 회색 임시 슬롯을 사용한다.
- 2026-11-03 팀 회의 A안 반영: B가 오프라인 3D 제작물을 공통 256×256 투명 PNG 레이어로 출력하고 공통 렌더러를 만든다. C는 모델·별도 캐릭터 PNG·렌더러를 제작하지 않고 기존 AvatarPreview와 A 주입 avatarSlot을 유지한다. 런타임 3D 의존성·계약·C01~C05 범위·일정은 바꾸지 않는다. B의 첫 렌더러 전달 시 섬과 동행의 캐릭터·의상 일치, 작은 슬롯 정렬·숨김·제한 중단을 함께 확인한다. 아직 이 연결 시험은 NOT RUN이다.
- 첫 범위는 모의 지도, 여정 문구, 고정 슬롯, 숨김·다시 표시, 제한·화면 이탈 시 중단이다. 방문은 C02, 월드맵은 C03, 출발·완료·반복·초기화 패널은 C04다.

## 화면 스케치

가로 화면은 지도와 동행 영역을 나란히, 작은 세로 화면은 안내·지도 다음에 동행 영역을 놓는다. 아래 배치는 영역 관계를 설명하며 px·dp 고정값이나 새 공통 테마가 아니다.

```text
가로
+---------------------------------------------------------+
| 뒤로 | DEMO 동행 모의 화면                               |
| 실제 주차 상태에서 체험하는 자체 모의 지도               |
+------------------------------------+--------------------+
| 목적지: 대전                       | 내 캐릭터          |
| 여정: 모의 이동 중                 |                    |
|                                    | [회색 정사각 슬롯] |
| [단순 도형 경로 · 시작점 · 목적점]  |                    |
|                                    | 캐릭터 숨기기      |
| 모의 누적 거리 / 모의 경과 시간    |                    |
+------------------------------------+--------------------+

세로 / 작은 창
+----------------------------------+
| 뒤로 | DEMO 동행 모의 화면        |
| 주차 상태의 모의 체험             |
| 목적지 · 여정 단계                |
| [단순 도형 경로]                  |
| 모의 누적 거리 / 모의 경과 시간   |
+----------------------------------+
| [고정 슬롯]  캐릭터 숨기기        |
+----------------------------------+
```

중요 문구와 캐릭터 영역은 레이아웃으로 분리한다. 슬롯은 지도 위를 이동하거나 안내를 덮지 않는다. 숨김 상태에서도 다시 표시 버튼의 위치를 유지한다. 창이 작아지면 지도 장식을 먼저 줄이고 안내와 버튼을 보존한다. 텍스트 확대 시 세로 재배치·스크롤로 잘림을 피한다.

아이콘에는 짧은 한국어 텍스트를 함께 둔다. 터치 영역은 공통 가이드의 초기값 56dp 이상을 따르되 실제 플랫폼 요구가 우선이다. 색만으로 상태를 구분하지 않는다. B 아트 가이드의 따뜻한 톤은 참고하되 A 공통 테마를 바꾸거나 C에서 새 캐릭터를 제작하지 않는다.

## 입력과 표시 원칙

| 입력 | 표시 또는 처리 |
|---|---|
| source=DEMO | DEMO 배지와 모의 지도 설명을 항상 표시. 값은 모의 누적 거리·모의 경과 시간으로 표시 |
| source=PLEOS | 여정 출처 PLEOS와 별개로 지도는 모의 지도라고 표시. DEMO 전용 문구·숫자를 실제 입력에 섞지 않음 |
| distanceMeters / elapsedSeconds=null | 해당 행 생략 또는 미확인 표시. 0으로 대체하지 않음 |
| distanceMeters / elapsedSeconds=0 | 명시적으로 제공된 0은 표시 가능. 0으로 실제 주차를 판정하지 않음 |
| destination=null | 목적지 미선택 표시. 지역명이나 좌표를 임의 생성하지 않음 |
| avatar | 전달받은 AvatarPreview 그대로 슬롯에 전달. 알려지지 않은 ID의 fallback은 렌더러 경계에서 처리 |
| expression | 첫 구현은 idle 고정. 임의 감정 모델·자동 연출·타이머 없음 |

JourneySnapshot에는 현재 속도·다음 회전·남은 거리·남은 시간이 없다. 첫 화면은 도형 경로와 목적지·단계·제공된 누적 거리·경과 시간으로 구성한다. 누적값을 잔여값으로 바꾸거나 평균속도를 계산하지 않는다. 와이어프레임의 모의 속도나 회전 숫자를 위해 공통 DTO를 확장하지 않는다. 이후 필요하면 C 내부의 명시적 Preview 장식으로만 검토한다.

## 상태와 중단 기준

제한과 화면 수명주기를 먼저 판단하고 그다음 여정 단계를 표시한다. 모의 TRAVELLING은 실제 주행·주차 상태가 아니다.

| 조건 | 화면·연출 | 부작용 |
|---|---|---|
| gate=UNKNOWN | 이용 상태 미확인, 동행 표시·조작 중단. A의 제한 화면 연결을 따름 | 실제 주차를 추정하거나 gate 변경 금지 |
| gate=RESTRICTED | 이용 제한 이유 표시, 동행 표시·조작 중단 | DEMO 단계와 무관하게 차단 |
| background / 화면 이탈 | 렌더링 연출·C 소유 job이 있으면 취소 | 새 listener·방문·보상·자동 진행 없음 |
| PARKED_ALLOWED + IDLE | 시작된 여정 없음 표시 | 목적지 요청을 자동 실행하지 않음 |
| PARKED_ALLOWED + PREPARING | 준비 중, 확정되지 않은 안내임을 표시 | 무한 로컬 타이머 대신 A 상태를 표시 |
| PARKED_ALLOWED + READY | 준비됨, 모의 이동 전 표시 | C01에서 출발 명령을 만들지 않음 |
| PARKED_ALLOWED + TRAVELLING | 출처를 붙인 여정 문구와 정적 캐릭터 | 시간 경과만으로 도착 처리하지 않음 |
| PARKED_ALLOWED + ARRIVED | 도착 단계 표시, 보상·브리핑은 A가 저장 결과로 연결 | 슬롯이나 화면 진입에서 보상 계산 금지 |
| PARKED_ALLOWED + CANCELLED | 취소됨 표시 | 도착·방문·보상 성공 연출 없음 |
| PARKED_ALLOWED + ERROR | 오류와 확인 가능한 상태 표시 | DEMO 자동 전환·임의 도착 처리 없음 |

숨김은 C의 화면 UI 상태다. 숨기기 시 onHideChanged(true), 다시 표시 시 false를 전달하고 GameAction이나 게임 저장은 호출하지 않는다. 창 크기 변경은 새 여정으로 취급하지 않고 숨김 선택을 유지한다. 제한 복귀 후에도 사용자가 숨긴 캐릭터를 강제로 표시하지 않는다. 화면 재진입에서는 최신 입력을 읽고, 프로세스 재시작 후 숨김의 영구 복원은 요구하지 않는다. 재시작 시 실제 게임 외형은 B의 로드된 상태가 기준이다.

AppRoot가 현재 foreground·gate를 관리한다. C01은 자체 lifecycle에서 화면 연출을 중단하고 gate 입력을 읽으며, foreground를 이유로 새 공통 DTO 필드를 추가하지 않는다. 첫 구현에는 자동 움직임이나 예약 타이머가 없어 수명주기 동작을 단순하게 유지한다.

## A01 이후 연결용 입력 예제

아래는 [기존 계약 초안](../contracts/KailandContracts.kt)의 필드만 사용하는 문서 예제다. 컴파일된 코드가 아니며, A01의 실행 타입과 대조한 뒤 C의 Preview·시험에만 옮긴다. AppRoot·빌드·PM fixtures는 수정하지 않는다.

```kotlin
// 값의 출처: Catalog의 daejeon, UI sample regional의 avatar.
val previewAvatar = AvatarPreview("tori", "daejeon_hat")
val previewJourney = JourneySnapshot(
    journeyId = "00000000-0000-4000-8000-000000000003",
    source = DataSource.DEMO,
    kind = JourneyKind.REGION,
    phase = JourneyPhase.TRAVELLING,
    routeRevision = 0,
    destination = DestinationSpec(
        regionId = "daejeon",
        label = "대전",
        latitude = 36.35,
        longitude = 127.38,
    ),
    elapsedSeconds = 1200,
    distanceMeters = 5000.0,
)
val previewGate = InteractionGate(GateState.PARKED_ALLOWED)
```

이 고정 UUID와 주차 허용 입력은 표시가 분리된 Preview·시험 전용이다. 실제 Runner는 A의 런타임 ID를 사용한다. 실제 앱에서 A의 gate를 위 값으로 덮어쓰지 않고 샘플을 live 상태로 저장하지 않는다. 앱에서는 목적지 이름·좌표를 Catalog에서 읽는다. 위 시간·거리는 누적값이며 경로 위의 현재 위치나 잔여 시간을 계산할 근거가 아니다.

## 구현 후 검증할 목록

표시 정책 단위 시험 8개와 네 Preview 렌더링은 위 구현 기록처럼 확인했다. 아래 항목의 앱 통합·상호작용·실제 기기 시험은 아직 NOT RUN이며 단위 시험이나 Preview만으로 PASS 처리하지 않는다.

| 확인 | 입력과 기대 결과 | 관련 기준 |
|---|---|---|
| 기본 표시 | 위 Preview 입력 → DEMO·대전·모의 이동·5km 누적·20분 경과·정적 회색 슬롯 | C01 |
| 누락·빈 상태 | null 수치·목적지 → 0·임의 지역 없음, 빈 상태 설명 | C01 |
| 숨김 | 숨김·재표시·창 회전 → 슬롯 가시성만 변경, 외형·저장 변화 없음 | C01, S03 |
| 실제 제한 우선 | DEMO TRAVELLING 중 UNKNOWN/RESTRICTED → 표시·조작·연출 중단 | U09, U16, S06 |
| 수명주기 | background·이탈·복귀 → 연출 중단, 최신 입력 표시, 중복 listener 없음 | S04 |
| 외형 연결 | B 의상 저장 성공 → AppRoot 슬롯 → 섬과 동행 동일 외형 | I03 |
| 크기·접근성 | 작은 창·가로·세로·글자 1.3배 → 중요 안내·버튼 잘림 및 지도 겹침 없음 | S03 |
| 출처·완료 구분 | PLEOS 입력에도 모의 지도 명시, ARRIVED만으로 보상 성공·자동 브리핑 없음 | INV07~08 |

## 다음 한 작업

A에게 위 공개 화면과 연결 예제를 전달하고 AppRoot에서 실제 입력으로 연결한 뒤 숨김·화면 이탈·제한·창 크기 흐름을 확인한다. B의 첫 renderer 인수 시 외형 일치 I03을 함께 확인한다. 그동안 C02 로컬 방문 상태 구현은 독립 진행할 수 있다. C01 전체 완료는 남은 연결·UI 검증 후 판정한다.

## 로컬 미리보기: 캐릭터 패널 접기

2026-11-04 사용자 요청으로 숨김 시 캐릭터 그림뿐 아니라 카드 전체를 composition에서 제거한다. 가로 화면에서는 지도가 기존 캐릭터 열의 너비까지 사용하고, 세로 화면에서도 빈 슬롯을 남기지 않는다. 숨김 상태의 지도 높이는 현재 창 높이의 절반을 기준으로 240~480dp 안에서 조절한다. 작은 창·큰 글자는 기존 스크롤을 유지한다. 상단의 56dp 이상 `캐릭터 다시 표시` 버튼으로 기존 분할/세로 배치를 복원한다.

`rememberSaveable` 숨김 상태·onHideChanged·실제 gate/foreground 재확인·AvatarSlot/AvatarPreview 계약은 유지한다. UNKNOWN/RESTRICTED/비활성에서는 지도 확장으로 표시를 우회하지 않는다. 게임 상태·캐릭터 외형·저장은 변경하지 않는다. 모의 지도이며 실제 내비 화면 확대나 차량 SDK 연동을 뜻하지 않는다.

초기에는 로컬 디자인 미리보기로 보존했고, 이후 사용자 push 지시에 따라 C01~C03 그래픽 보완과 함께 기존 C 브랜치 전달 대상으로 정리했다. 기존 C01 표시 정책 시험 8개·debug 빌드 PASS, 가로 샘플에서 패널 접기·전체 너비 지도·다시 표시 복원을 관찰했다. 최신 main 반영 후 전체 Android 165개·Python 44개 및 debug 빌드도 PASS. 실제 AppRoot/B 렌더러·회전 복원·큰 글자 인수는 별도 확인이 필요하다.
