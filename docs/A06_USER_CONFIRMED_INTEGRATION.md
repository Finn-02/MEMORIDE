# A06 사용자 확인 도착 저장 연결

2026-11-06 KST / Part A / Windows AMD64 / Python3.10.4 / JBR25.0.4 / wrapper9.3.1.
기존 `part/a-a01-bootstrap` 유지. 시작 HEAD `d4e3555`, B PR #20(`38605d7`)와
C PR #21(`1e66647`)을 포함한 main `9a19c77`을 `953dadc`로 병합했다.
기존 미추적 A01 문서 두 개는 보존하고 이번 전달에서 제외한다.

## 동작과 진입

정식 앱에서 실제 주차·이용 허용 상태를 확인한 뒤 **사용자 확인 모드 → 섬의 여정 준비**로 들어간다.
일반 Android의 debug 개발 진입은 공개 DevelopmentActivity의 **A04 실제 저장 화면 확인** 버튼이다.
Automotive 개발 제한과 정식 UNKNOWN 차단은 그대로다.

- 목적지 후보와 DAILY/REGION을 고르고 여정을 시작한다. 월드맵 선택은 후보만 채우며 자동 저장하지 않는다.
- **여정 마무리** 질문에서 **도착했어요**를 선택해야 기존 단일 B GameService에 저장한다.
- 목적지 변경·취소·계속 이동·질문 닫기는 도착 저장을 호출하지 않는다.
- 사용자 확인은 실제 방문 인증이나 SDK 자동 도착이 아니다. 이 흐름은 내비 요청을 실행하지 않는다.
- 시간·거리는 측정하지 않으며 명령·영수증에 둘 다 null을 유지한다.
- 저장 실패/거부 후 재시도는 최초 ID·목적지 버전·확인 시각·출처·null의 원래 명령을 사용한다.
- 실제 저장된 pendingReceipt가 PM 브리핑을 연다. acknowledge 성공 후에만 섬/아이템으로 이동한다.
- 확인 완료 후 다음 여정은 새 ID로 시작한다. 저장한 영수증은 B의 기존 원본 보존 정책을 따른다.

## 연결 경계

ManualJourneyRuntime은 앱 세션에 속하며 Main scope에서 동작한다.
ManualArrivalController의 질문 토큰을 검증한 초안만 ArrivalCommand로 바꾼다.
GameCommandCoordinator의 write Mutex를 얻은 뒤에도 실제 gate·foreground·readiness·owner·generation·
화면 lease·현재 초안/질문 identity·journeyId·revision·kind·source·phase·region을 재검사한다.
화면에 남은 이전 callback은 새 화면/소유자의 허용 상태를 빌려 쓰지 못한다.

일반 completeArrival의 USER_CONFIRMED 차단은 보존하고 전용 내부 진입에서만 허용한다.
이는 B 대기로 인한 전체 차단을 유효한 사용자 확인 경로로 한정해 해제한 것이다.
모드 전환은 기존 coordinator의 세대 증가/잠금 밖 정리로 이전 수동 여정을 무효화한다.
DEMO 초기화는 계속 DEMO에서만 가능하고 이전 세대 입력은 거부한다.
배경/이용 제한/화면 이탈은 질문을 닫고 이전 클릭을 거부한다. 명시적 재시도용 최초 확인 초안은 메모리에 유지한다.
저장 진입이 이미 승인됐으면 B의 원자 commit과 snapshot 발행은 끝내고 표시/이동은 현재 허용 상태에 따른다.

B 저장 소스·공통 DTO·PM/C 화면 내부·Manifest·의존성·서명 설정은 변경하지 않았다.
기존 진단용 ManualJourneyConfirmationPanel은 여전히 저장 없는 메모리 초안 검사 화면이다.

## 직접 실행 결과

- 집중: 새 통합9개 + coordinator14개 + manual controller12개 = **35개 PASS**, debug 빌드 PASS.
  첫 집중 실행은 시험 fixture의 순환 타입 추론 컴파일 오류로 실패했고 명시적 타입으로 수정 후 통과했다.
- 이후 중복 클릭/ack 후 재시도 차단, 저장 승인 뒤 UI coroutine 취소 사례2개를 더해 새 통합 시험은 **11개**다.
- 전체 `python -X utf8 scripts/check_all.py --android`: exit0, Python **59**, JUnit **261**,
  failures/errors/skips 모두0, debug BUILD SUCCESSFUL(50s,2 executed/43 UP-TO-DATE).
  production Kotlin은 첫 집중 실행에서 실제 컴파일했고 전체 회귀에서 신규 시험과 전체 JUnit을 실제 실행했다.
- 신규 통합 시험은 실제 Preferences DataStore 파일과 BGameService를 사용한다.
  저장 실패 재시도·늦은 질문/목적지 변경·비도착 응답·UNKNOWN/RESTRICTED/BG/owner·화면 이탈·
  write lock 대기 중 변경·모드/reset·ack 실패·파일 재생성 복원·중복/취소 경계를 확인했다.
- `git diff --check` PASS. SDK XML/Unsafe 경고는 기존 경고이며 Android SDK/안전 인증을 뜻하지 않는다.

일반 API37 x86_64 `Medium_Phone_API_37.0` / emulator-5580에서 기존 데이터 삭제 없이 debug를 설치했다.
초기 부팅 전 설치는 `device is still booting`으로 실패했고 부팅 완료 확인 후 install Success/공개 Activity Status ok.
RAM 권장량 안내를 닫은 뒤 computer-use로 아래 흐름을 직접 조작했다.

| 직접 조작/관찰 | 결과 |
|---|---|
| 사용자 확인 모드 → 여정 준비 → 원주 지역 여정 시작 | PASS, 출처/미인증/측정 없음 문구 표시 |
| 마무리 질문 → 원주에 도착했어요 | PASS, 저장 후 사용자 확인 원주 브리핑 |
| 브리핑 내용 | PASS, 원주 발견/성장0/기념품3개 |
| 앱 전용 DataStore 읽기 확인 | PASS, source USER_CONFIRMED·두 null·원본 ID/확인 시각·pending 보존 |
| 브리핑 닫기 → 섬 | PASS, pending null/원본 영수증1개와 아이템3개 보존 |

기기 저장 확인은 UI로 만든 파일의 읽기만 수행했다. 저장 주입·비공개 Activity 직접 시작·
실제 주차 위조·UNKNOWN 해제·제한 우회·기존 서명 release/Pleos AVD 변경은 하지 않았다.

## NOT RUN / 다음 인수

2026-11-06 후속: [공식 Pleos debug 인수](A06_USER_CONFIRMED_PLEOS_ACCEPTANCE.md)에서
정식 Main의 P 허용·사용자 확인 저장·브리핑·미확인/확인 완료 재시작 복원을 확인했다.
D의 실제 제한 신호/저장 불변도 확인했으나 QEMU 반복 종료로 D 화면/P 복귀는 BLOCKED다.
아래 NOT RUN은 최초 일반 Android 검증 당시 기록이며 후속 문서의 실제 범위를 우선한다.

이번 신규 경로의 실제 Pleos/차량 gate 조작·실차·서명 release·기기 업데이트/다운그레이드,
UI 저장/ack 실패 주입·기기 프로세스 재시작·큰 글자/회전 전수는 NOT RUN.
자동 시험의 실제 파일 재생성/경쟁 검증과 기기 UI 시험은 구분한다.
활성 미저장 여정/초안은 메모리 전용이며 프로세스 종료 뒤 자동 복원하지 않는다.
저장된 진행/원본/pending은 B가 복원한다. UC 저장의 구버전 정상 사용은 보장하지 않는다.

C05 전체 D01~D06 3회/활성 방문 reset UI/접근성, A06 최종 SHA 서명 후보 인수는 여전히 진행 중이다.
다음은 B와 이번 연결의 retry/pending/복원/ack 교차 인수, 실제 Pleos 주차 상태의 새 화면 확인이다.
이 작업을 최종 A06·실제 SDK 자동 도착·운전 안전 검증 완료로 표시하지 않는다.
