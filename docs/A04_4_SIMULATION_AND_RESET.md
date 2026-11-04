# A04-4 시연·방문·모드 전환·초기화 연결

2026-11-05 KST / Windows11 AMD64 / part/a-a01-bootstrap.
작업 기준은 A04-3 로컬 8ff3911 및 확인한 origin/main edea1fd다. 개인 A01 문서2개는 보존·커밋 제외했다. 최종 커밋/PR은 Git 기록을 따른다.

## 변경 동작과 경계

- A의 GameSimulationRuntime이 기존 C DemoScenarioRunner·VisitController와 단일 B GameService를 연결한다. C/B/PM 내부 코드·공통 DTO·저장 스키마·의존성은 변경하지 않았다.
- 안정된 JourneyPort/DemoControlPort가 매 작업의 실제 adapter/generation을 고정한다. 초기화/모드 전환 뒤 이전 작업은 쓰기 경계에서 거부하고 새 adapter로 관찰한다.
- C의 resetConfirmed가 이미 Runner beginReset/finishReset을 담당하므로 A는 Runner 토큰을 중복 생성하지 않는다. A는 세대 차단→잠금 해제→방문 취소/adapter drain→잠금 안 B 초기화 순서로 처리한다. reset 호출 job을 자기 자신과 join하지 않는다.
- DemoPanel은 reset 세대 변경 중에도 유지해 D01/D03/D06의 선택과 reset acknowledgement가 사라지지 않게 한다. B/방문 화면·인벤토리와 임시 UI는 generation key로 폐기한다. 같은 저장값으로 재초기화해도 세대는 증가한다.
- 실패·취소된 reset은 입력 차단을 유지하며 명시적 재확인으로 재시도한다. 실패 상태에서 재진입한 화면도 DEMO 초기화 복구 화면으로 시작한다.
- 시연에서는 D04 원본 반복을 브리핑 확인 전에 수행한다. B의 acknowledge 이후 AlreadyApplied가 pending을 다시 열지 않는 정책을 유지하기 위해서다. 시연 진행 중 자동 브리핑으로 패널을 폐기하지 않고 `저장된 브리핑 보기`로 연다. Runner가 저장 결과 검증을 마친 뒤에만 확인 버튼을 허용한다.
- 브리핑은 시연 위 모달로 표시해 Runner 검증을 끊지 않는다. 닫기/아이템 보기/물리 Back은 동일 acknowledge 성공 뒤 섬/인벤토리로 이동한다. 일반 여정 준비 흐름은 기존 자동 브리핑을 유지한다.
- `DEMO 모드 / PLEOS 모드`는 실제 gate·foreground·저장 readiness가 허용할 때만 전환한다. 먼저 새 세대로 입력을 차단하고 이전 시연/방문을 정리한 뒤 출처를 바꾼다. 게임 진행/영수증은 초기화하지 않는다. 실패 시 MODE_FAILED로 차단하고 모드 재선택으로 복구한다.
- 실제 플랫폼 제한은 모드와 독립적이다. UNKNOWN/RESTRICTED에서 모드 버튼으로 게임을 열 수 없다. PLEOS는 실제 여정 입력 대기를 표시하며 SDK 성공으로 설명하지 않는다.
- 가상 방문은 기존 C의 목적·동의·15초 만료·30초 쿨다운과 B의 AvatarSlot을 사용한다. 시간은 SystemClock.elapsedRealtime, 예약 취소는 join 없이 수행한다. 제한·배경·화면 이탈·모드 변경·reset은 방문/옛 토큰을 중단한다. 방문은 GameService를 호출하지 않는다.
- `방문만 다시 준비`는 C의 개발 시연 재시작이며 게임 기록을 지우지 않는다. 전체 게임 초기화는 C의 별도 확인 모달을 거친다.
- debug DevelopmentActivity/DevelopmentGameActivity는 io.kailand.development taskAffinity로 분리했다. 정식 진입점을 실행했을 때 공유 작업 스택의 개발 화면이 다시 보이던 문제를 해결한다. applicationId io.kailand·저장 키·release Activity 구성은 유지한다.

## 실제 검사

Python3.10.4 / JBR25.0.4 / Gradle9.3.1, 기존 wrapper와 프로세스 JAVA_HOME 사용.

- 초기 compileDebugKotlin PASS. 집중 검사 최초20개 중2개 실패는 시험 하네스가 D03의 resetBefore를 누락한 것이 원인이었다. 하네스를 수정하고 Runtime8개를 재실행해 PASS. 초기 실패를 통과로 계산하지 않았다.
- 최종 `python scripts/check_all.py --android` exit0: Python44개·JUnit223개, 실패0/오류0/skip0, debug 빌드 PASS. tmp/a04-4-full.log.
- 신규 Runtime8개는 실제 BGameService/C Runner·방문과 격리 메모리 Store를 사용한다. D01~D06, reset 자기 토큰 보존, 기존 쓰기와 reset/옛 입력 경쟁, 실패 차단·재시도, 모드 전환의 비파괴성, 제한/배경/만료/철회/늦은 타이머를 확인했다.
- U20 소프트웨어 경계: Runner 저장 대기 중 reset이 자기 join 없이 완료되고 최종 진행이 초기화됨, 늦은 Arrival/visit token 거부, 초기화 뒤 대기하던 옛 GameAction 거부, 같은 저장값 reset의 새 generation 확인. Android 실제 디스크 장애/실차 시험과 구분한다.
- `gradlew.bat --no-daemon :app:assembleRelease` PASS, unsigned release. debug manifest 분리 후 최종 확인은 50 tasks 중1 실행/49 up-to-date. 서명 release 설치가 아니다. tmp/a04-4-release.log.

일반 Android API37의 임시 사용자 MEMORIDE_A04_4_TEST(ID11)에서 실제 DataStore UI9개 사례를 확인했다.

1. D01 초기화 후 선택 시나리오 유지·최초 DAILY 성장1 저장.
2. D02 같은 KST 날짜 성장0·브리핑 조기 전환 없이 시연 진행.
3. D03 초기화→REGION 아이템3개, D04 동일 원본/새 지급0 확인.
4. 검증 후 브리핑 모달과 물리 Back acknowledge→섬 이동.
5. 동의한 방문 표시, 게임 저장/보상 변화 없음.
6. 모드 변경 시 방문 종료·기존 게임 기록/영수증 보존.
7. DEMO 복귀·방문 철회에서 방문/보상 생성 없음.
8. 동일 기본 저장값으로 전체 초기화를 반복해도 시연기 조작 가능.
9. 배경/복귀에서 옛 방문 자동 재개 없음·저장 불변.

근거: tmp/a04-4/results.json, D01-done.xml, D02-done.xml, D03-done.xml, replay-done.xml, runner-briefing.png, visitor.png, visit-mode-cancelled.xml, reset-finished.xml, visit-background-return.xml.

초기 임시 사용자 전환 직후 Activity 등록 대기·UiAutomation 연결 timeout·Updating 화면으로 하네스가 중단됐다. 앱 전경과 자동화 프로세스 예외를 구분해 확인했고, 초기 설정 미완료(0)였던 임시 사용자11만 user_setup_complete=1로 처리한 뒤 UI 연결을 복구했다. 원래 사용자·차량/주차/UX 제한은 변경하지 않았다. 모드 칩과 메뉴의 중복 DEMO 이름도 구분한 뒤 재시험했다.

UI9개는 taskAffinity 보강 전 동일 핵심 코드에서 통과했다. 이후 최종 manifest로 전체 검사와 진입점 경계를 재시험했다. 과거 공유 작업 스택은 한 번 clear-task로 정리하고 Pleos의 명시적 P 모드 전환·게임 저장 불변, Automotive 개발 진입 차단, 정식 Main 복귀를 확인했다. 기존 task 레코드 정리와 게임 데이터 초기화는 다르며 저장을 지우지 않았다.

임시 사용자11 종료/제거·원래 사용자0 복귀 및 원래 게임 저장 SHA256 동일을 확인했다. tmp/a04-4/cleanup-result.txt. DataStore 파일은 읽기만 관찰했고 변경은 B 서비스/공개 UI로만 실행했다.

## 실행 방법

- 일반 Android: 개발 DEMO → A04 실제 저장 화면 확인 → 섬의 DEMO 메뉴. 모드 선택 칩과 메뉴를 구분한다.
- D01/D03/D06은 전체 초기화 확인이 필요하다. 한 단계씩 직접 누른다. D04는 브리핑 확인 전에 실행한다. 끝나면 저장된 브리핑 보기를 연다.
- 가상 방문에서 내 차 요청→가상 앞차 시점→수락/거절을 체험한다. 방문 다시 준비와 전체 게임 초기화는 별도다.
- Pleos: 정식 Main의 실제 P/UX 허용이 필요하다. 개발 DEMO 진입점은 Automotive에서 차단된다. DEMO를 선택해도 실제 제한은 계속 우선한다.

## 남은 범위 / NOT RUN

- USER_CONFIRMED 출처/원본 저장은 B 계약 확인 대기. 실제 사용자 확인 저장·PLEOS 최종도착 port는 미연결이다.
- 실제 기기 디스크 장애로 reset/ack 실패 UI를 강제 주입한 시험은 NOT RUN. 실패 결과의 보존/재시도는 단위·통합 모델에서 확인했다.
- D05/D06은 이번 Runtime 단위 통합으로 확인했으며 기기 UI 전수 실행은 아니다. 방문 거절/실제15초 대기 전체 UI 행렬, B04 전체36조합도 미완료다.
- 최종 APK의 실제 D/N/R·서비스 장애·실차, 서명 release 설치·다른 PC 인수는 NOT RUN. 이전 A03 P/D/P 결과를 이번 실차 인증으로 사용하지 않는다.
- 다음은 A04 통합 PR 검토/병합 후 A05 서명 release/교차 설치, A06 제한·오류 UI·전체 인수 보완이다. 일반 내부 구현에 별도 PM 승인 대기 절차를 추가하지 않는다.
