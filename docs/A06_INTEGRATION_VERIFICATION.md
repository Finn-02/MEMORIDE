# A06 통합 검증 진행 기록

2026-11-05 KST / Part A / Windows AMD64 / Python 3.10.4 / JBR 25.0.4 / Gradle wrapper 9.3.1.
기준 main `a04b3c5`(A05 PR #18 병합 `507b987`, 브리핑 PR #19 포함),
작업 브랜치 `part/a-a01-bootstrap`. **A06 진행 중이며 최종 인수 완료가 아니다.**

## 이번 검증

앱/저장 구현은 변경하지 않고 A 소유 통합시험 `PersistedArrivalBoundaryTest` 2개를 추가했다.
A 조정자 → 실제 BGameService → PreferencesGameSaveStore → 임시 파일 DataStore를 사용한다.
각 세션의 DataStore scope를 cancelAndJoin한 뒤 같은 파일로 서비스를 재생성한다.
가짜 저장 성공이나 파일 내용 직접 주입으로 복원을 대체하지 않는다.

1. 이용 가능한 상태에서 승인된 도착을 실제 파일에 저장한 직후 제한으로 바꾼다.
   B 공개 snapshot 갱신은 끝나고, 제한 중 브리핑 확인/이동은 거부된다.
   재생성 후 원본 영수증·null 측정·아이템이 복원되며, 같은 도착 반복은 무변경이다.
   이용 가능한 새 세션의 확인은 한 번만 이동하고 pending만 지운다.
   다시 재생성해도 원본 반복은 추가 지급/브리핑 재개를 만들지 않는다.
2. 브리핑 확인의 파일 commit과 snapshot 공개 사이에서 화면 coroutine을 취소하고
   UNKNOWN/배경으로 바꾼다. 저장과 공개 상태는 끝까지 일치하고 화면은 이동하지 않는다.
   재생성 후 확인 완료 상태와 원본 영수증이 유지되며, 반복 도착은 저장을 변경하지 않는다.

두 시험은 각각 15초 timeout으로 교착을 실패 처리한다. 임시 파일은 JUnit TemporaryFolder가 정리한다.
이것은 **JVM 파일/서비스 재생성 시험**이며 Android 프로세스 종료·APK 업데이트·실제 제한 신호 시험이 아니다.

## 현재 main의 검증 연결표

| 항목 | 이번 소프트웨어 회귀 근거 | 별도 기기/인수 범위 |
|---|---|---|
| I01, U15 | ArrivalBriefingIntegrationTest: 실제 B 저장·원본 재시도·브리핑 확인 실패/재시도 | 현재 APK의 전체 UI 인수는 후속 |
| I02 | 신규 파일 시험: 승인된 저장 중 제한 변경·영수증 복원·허용 후 1회 확인 | 실제 차량 제한 중 도착 UI는 미실행 |
| U09~11, U16 | GameCommandCoordinatorTest, VehicleGatePolicyTest: UNKNOWN/배경/오래된 여정·버튼 차단 | 이전 A03 실제 P/D/P 근거와 이번 JVM 시험 구분 |
| U14, U20 | GameSimulationRuntimeTest: reset 경쟁·늦은 명령/방문·실패 차단·같은 값의 새 세대 | 이전 A04 UI 근거 유지, 최종 APK 재확인은 후속 |
| I05 | GameSimulationRuntimeTest: 모드 변경 시 저장 보존·옛 토큰 거부 | 실제 SDK 늦은 최종도착은 미실행 |
| I06 | ArrivalBriefingIntegrationTest, ReceiptCloseCoordinatorTest, AdmittedGameWriteTest | 섬/월드 준비 이동은 A04 UI 근거, 최종 디자인 인수는 후속 |
| I03 | [A04-6 실제 UI 근거](A04_6_AVATAR_GALLERY.md) | 이번에 외형 UI를 재실행하지 않음 |
| S02, S10 | 파일 재생성은 복원 경계 보강. A05는 같은 서명 설치·appId 유지 확인 | **기기 재시작/업데이트 후 실제 진행 보존은 아직 미검증** |
| X03 | A05 서명 APK 존재, 일반 Android A/C 교차 설치 완료 | 서명 release의 기준 Pleos 설치/기본 시연은 미실행 |

## 실행 명령과 실제 결과

- `gradlew.bat --no-daemon :app:testDebugUnitTest --tests io.kailand.app.PersistedArrivalBoundaryTest`: exit 0, 신규 2개 실제 실행 PASS.
- `python scripts/check_all.py --android` (`JAVA_HOME`은 기록된 JBR): exit 0.
  Python 59개 PASS, JUnit **235개 실제 실행**(failure/error/skip 0), debug APK 빌드 PASS.
  `:app:testDebugUnitTest`는 UP-TO-DATE가 아니며 전체 suite를 실행했다. 변경 없는 일부 빌드 task는 캐시 재사용했다.
- 기존 SDK XML 버전과 DataStore Unsafe 경고는 남아 있다.
- `adb devices -l`, `adb -s emulator-5554 shell dumpsys package io.kailand`: 읽기 전용 환경 확인.
  현재 연결된 Pleos에는 debug versionCode 1이 설치되어 있다. 이번에 APK 설치·삭제·데이터 초기화·제한 변경은 하지 않았다.
  최초 sandbox 안 ADB 서버 접속은 실패했고, 권한 있는 읽기 전용 재실행은 성공했다. 앱 실패로 집계하지 않는다.

## 다음 단계와 미완료

1. 기존 Pleos debug 데이터와 분리된 시험 환경에서 서명 release X03을 수행한다.
   기존 debug를 팀 서명 APK로 덮을 수 있다고 가정하지 않으며, 기존 앱 삭제로 환경을 맞추지 않는다.
2. 실제 게임 진행을 만든 기기에서 재실행/업데이트 보존(S02/S10)을 확인한다.
   일반 Android 정식 UNKNOWN을 우회하지 않는다. 허용 가능한 실제 환경의 확보가 필요하다.
3. C 시연/방문 UI의 남은 행렬, 창 크기·글자 크기·복귀·3회 시연을 최종 대상 APK에서 확인한다.
4. USER_CONFIRMED는 B05 변경과 원본 복원/하향 호환 근거를 확인한 후 A 수동 여정에 연결한다.
   현재 main의 BService와 A 쓰기 경계는 아직 신규 출처 연결 준비 상태다.
5. 최종 UI 반영 후 전체 흐름과 release를 다시 확인하고 README·환경·최종 SHA를 맞춘다.

실차/주행 안전, macOS, 실제 PLEOS 최종도착·장애, 최종 UI/A06 인수는 NOT RUN이다.
현재 작업은 A06 첫 회귀 묶음으로 보존하며, A06 종료 전 새 PR을 반복 생성하지 않는다.
