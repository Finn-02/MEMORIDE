# A04 실제 저장·화면 연결 진행

최신 갱신: 2026-11-05 KST / Windows11 AMD64 / part/a-a01-bootstrap.
작업 전 HEAD d8c0d09, 최신 origin/main ab2281b 확인. 이전 B04 개발 호스트 로컬 변경과 개인 문서 2개를 보존했다. 아래 코드는 아직 커밋·push하지 않은 로컬 변경이다.

## A 전체 단계

| 단계 | 현재 상태 | 남은 범위 |
|---|---|---|
| A01 | 공용 앱 기반 main 반영 | 유지 |
| A02 | DEMO 여정 상태·명령 main 반영 | 실제 저장 연결은 A04 |
| A03 | 차량 gate·SDK 요청/조회/취소 검증 main 반영 | 자동 최종도착 식별 미해결, 사용자 확인 초안은 검증 완료·저장 미연결 |
| A04 | 저장 화면·DEMO/브리핑·C 동행/방문/Runner·모드/reset 연결 | 사용자 확인 저장 계약·실제 오류 UI/차량 인수 보완 |
| A05 | 미완료 | 서명 release와 다른 PC 설치 검증 |
| A06 | 미완료 | 제한·재시작·화면 이동·I03/I06/U20 최종 통합 인수 |

현재 A04-1 등의 세부 번호는 A 내부 작업 분할이다. 팀의 A01~A06 우선순위를 바꾸지 않는다.

## A04-1 당시 구현과 검사

아래 A04-1 기록은 당시 상태다. 최신 연결 결과는 [A04-3](A04_3_DEMO_ARRIVAL_BRIEFING.md)을 따른다. A04-1·2는 후속 작업 전 05bb2a9로 로컬 보존하고 main edea1fd를 f0c52c8로 merge했다.

`app/GameCommandCoordinator.kt`는 호출자가 주입한 단일 GameService를 감싸는 A 소유 명령 경계다. 보상·저장 로직을 다시 구현하지 않는다.

- apply / completeArrival / acknowledge / resetDemo가 하나의 Mutex를 공유한다.
- 실행 직전에 최신 gate·foreground·readiness와 콜백이 보관한 generation을 검사한다.
- 도착은 현재 공통 JourneySnapshot의 ID·source·kind·revision·ARRIVED·REGION 목적지 ID와 대조한다. 이 함수 자체가 SDK 도착을 인증하거나 사용자 확인 초안을 변환하지 않는다.
- 저장 실패 결과를 그대로 돌려주고 재전달된 원본 명령의 ID·시각·null 값을 변경하지 않는다. 재시도 명령 보관과 재전달은 JourneyPort/상위 연결 책임이다.
- acknowledge는 현재 pendingReceipt ID만 처리한다. await 중 백그라운드로 전환됐다면 저장 성공이어도 라우팅 허가로 사용할 Success를 반환하지 않는다. UI는 복귀 때 최신 pending을 다시 읽는다.
- reset은 기존 쓰기 종료 뒤 입력 차단·generation 증가 → 잠금 해제 후 stopOldWork → 잠금 안에서 B 초기화 순서다. 성공만 ACTIVE로 복귀한다. 실패·취소는 RESET_FAILED로 유지하고 최신 generation으로 명시적 재시도한다.
- reset은 현재 DEMO 모드에서만 허용한다. 정리 hook은 자기 호출 job을 join하면 안 된다.

## 연결할 때 반드시 지킬 조건

이 클래스는 아직 MainActivity/AppRoot나 B 서비스에 연결하지 않았다. 현재 APK UI의 실제 저장 동작이 바뀐 것은 아니다.

1. 앱 수명 소유자가 B 서비스를 한 번 생성하고 동일 인스턴스·coordinator를 재사용한다. B readiness READY를 별도로 확인한다.
2. context 공급자는 실제 최신 gate·foreground·mode·JourneySnapshot을 읽어야 한다. 고정된 초기 Compose 값이나 가짜 parking을 캡처하지 않는다.
3. UI/비동기 작업은 만들어진 세대 값을 보관한다. 실행 시 최신 generation으로 바꿔서 오래된 콜백을 새 세대로 통과시키지 않는다.
4. 동일 저장값 reset도 임시 UI 상태를 폐기하도록 화면을 generation별 Compose key로 구성한다. reset 중 Runner·방문·listener 정리는 stopOldWork에서 잠금 밖으로 연결한다.
5. mode 전환의 세션 무효화는 아직 미구현이다. 현재 coordinator에 모드 전환과 오래된 GameAction 차단까지 구현됐다고 해석하지 않는다. A04 후속에서 별도의 비파괴 세대 전환과 UI callback 범위를 연결·검증한다.
6. 실제 화면 이동은 acknowledge 이후 최신 화면/영수증/세대/제한을 다시 검사한다. 이 클래스는 라우터를 소유하지 않는다.
7. B가 확인할 사용자 확인 출처 계약은 아직 enum에 없다. USER_CONFIRMED가 반영되기 전 초안을 기존 PLEOS evidence로 보내지 않는다.

## 실제 검증

- JBR25.0.4 / Gradle9.3.1 / Python3.10.4.
- `gradlew.bat --no-daemon :app:testDebugUnitTest --tests io.kailand.app.GameCommandCoordinatorTest`: exit0, 신규11개 PASS. tmp/a04-coordinator-focused.log.
- `python scripts/check_all.py --android`: exit0. Python44개, JUnit203개(신규11개 포함), 실패0/오류0/skip0, debug 빌드 PASS. tmp/a04-coordinator-full.log.
- 단위 사례: UNKNOWN/배경/로딩 차단, 현재 도착 일치, 원본 재전달, 잠금 대기 뒤 gate 검사, ack 실패/오래된 ID, ack 중 배경 전환, 기존 쓰기와 reset 순서·잠금 밖 정리, reset 실패 후 차단·재시도, 정리 취소·제한 전환, DEMO 전용·동시 reset 차단.
- git diff --check PASS.

NOT RUN: 이번 coordinator의 실제 B/DataStore 연동·화면·기기·Runner/방문 U20·모드 전환·I03/I06·release 컴파일/설치. 이전 B04 호스트의 unsigned release 성공은 이번 새 main 코드의 release 검사 결과가 아니다. Coordinator 단위 시험을 전체 U20 통과로 표시하지 않는다.

## 다음 순서

- A04-2: 단일 B 서비스·Catalog와 실제 섬/인벤토리/월드·오류/로딩/제한 화면 연결 완료. AvatarSlot의 C 화면 연결은 후속. [결과](A04_2_SAVED_GAME_SCREENS.md).
- A04-3 완료: 기존 DEMO 여정→실제 저장→pendingReceipt·PM 브리핑→ack 이동 및 C 동행 AvatarSlot 연결. Python44/JUnit215/debug/unsigned release PASS, 기기9사례 확인. [결과](A04_3_DEMO_ARRIVAL_BRIEFING.md).
- A04-4 완료: Runner·방문 정리·mode 전환·reset generation 및 U20 소프트웨어 경쟁 검사. [실제 결과](A04_4_SIMULATION_AND_RESET.md). Python44/JUnit223/debug/unsigned release PASS, 기기9사례와 Pleos P 경계 확인. 다음은 A04 PR 검토와 A05/A06이다.
- 사용자 확인 출처 계약은 B 확인·공통 타입 반영 뒤 별도로 연결하고 원본 저장/복원 시험을 수행한다.
- B의 갤러리 Composable 도착 시 준비된 debug 호스트에 연결해 36조합을 시험한다.
