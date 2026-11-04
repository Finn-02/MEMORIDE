# 사용자 확인 도착 — A 구현과 B·PM 계약 검토안

2026-11-04 KST. 사용자가 수동 도착 확인 방식으로 진행하도록 요청했다. 이 문서는 현재 공유 DTO를 변경한 결과가 아니라, 실제 저장 연결 전에 확인할 구체적인 계약안이다.

## 사용자 흐름

주차·플랫폼 허용 상태에서 선택했던 여정을 `여정 마무리`로 연다. 내비 종료/취소/도착/목적지 변경 callback은 확인 대기를 준비할 수 있지만 도착 근거로 쓰지 않는다. 제한·백그라운드에서는 질문을 표시하지 않는다. 다음 허용 진입 때 현재 목적지가 적힌 질문을 표시한다.

- `도착했어요`: 사용자가 해당 목적지 도착을 확인한 초안. SDK 자동 인증이나 실제 방문 인증이라고 표시하지 않는다.
- `목적지를 바꿀게요`: 이전 확인 무효화, 재선택 요구. 새 선택 전에는 이전 목적지 완료 금지.
- `여정을 취소할게요`: 취소, 도착/보상 없음.
- `계속 이동 중이에요` 또는 질문 닫기: 여정 유지, 도착/보상 없음.

내비 앱의 창을 닫거나 프로세스가 종료된 사실을 감시하는 기능은 추가하지 않았다. 기존 SDK에서 관측한 경로 이벤트만 힌트로 사용한다. 이벤트가 없더라도 주차 후 수동 버튼은 사용할 수 있다. 알림 권한·오버레이·DO 선언·새 SDK 의존성은 추가하지 않았다.

## 현재 A 구현 범위

- `ManualArrivalController`는 A 소유의 메모리 상태만 변경한다. `UserConfirmedArrivalDraft`는 공통 ArrivalCommand/ArrivalReceipt가 아니며 B로 전달하지 않는다.
- draft는 여정 ID, 목적지 버전, generation, 확인 nonce, 선택 목적지, 종류, 최초 확인 시각을 고정한다. 시간·거리는 null이다. 중복 클릭은 두 번째 초안을 만들지 않는다.
- 목적지 변경 시 같은 활성 ID/revision 증가. 취소 후 새 시작은 새 ID. 제한/화면 이탈 시 열린 질문의 토큰을 폐기한다. 복귀 때 필요한 질문은 새 토큰으로 만든다.
- reset은 검증 메모리만 지우고 generation을 증가시킨다. B 저장 초기화나 전체 U20 구현이 아니다. A04 쓰기 조정자는 저장 직전에도 최신 generation·gate·foreground를 검사해야 한다.
- SDK debug 진단의 목적지 선택과 연결했다. Compose 패널은 기존 MemorideTheme을 사용한다. 확인 완료 문구는 `실제 저장·보상 없음`을 표시한다. 확인 흐름 초기화는 실제 게임 진행을 지우지 않는다.
- ViewModel은 구성 변경 때 사용자 선택을 유지하지만, 프로세스 종료/진단 화면 finish 뒤 활성 여정의 영구 복원은 제공하지 않는다.

## 실제 저장에 필요한 합의안

추천은 명령부터 저장된 영수증까지 사용자 확인 출처를 명시하는 것이다. 예시로 `DataSource.USER_CONFIRMED`와 `ArrivalEvidence.USER_CONFIRMED_DESTINATION`을 추가하고 이 조합만 허용할 수 있다. 이는 제안명이며 아직 enum에 추가하지 않았다. 기존 PLEOS를 유지하는 대안을 선택한다면 별도의 확인 방식 필드가 영수증·원본 저장·복원·브리핑까지 보존돼야 한다. UI 문구만 바꾸거나 기존 PLEOS_FINAL_DESTINATION으로 보내는 방법은 사용하지 않는다.

### B 확인 사항

1. 사용자 확인에도 기존 DAILY/REGION 규칙을 적용하는 입력 조합을 확인한다. 성장 상한·날짜·지역 최초 지급·중복 방지는 그대로 유지한다. 실제 방문 인증 기능은 아니다.
2. source/evidence를 명시적 허용 조합으로 검증한다. 현재 `(source == DEMO) != (evidence == DEMO_FINAL_DESTINATION)` 검사는 enum 추가만으로 새 조합을 정확히 제한하지 못하므로 함께 보완해야 한다.
3. 최초 영수증과 사용자 확인 출처를 원자 저장하고, 중복/재시도/복원에서도 원본 출처·ID·확인 시각을 보존한다. acknowledge 이후 재지급하지 않는다.
4. 기존 저장을 새 버전이 읽는 경우와, 새 출처를 모르는 구버전으로 내려가는 경우의 동작을 확인한다. 알 수 없는 값을 PLEOS 자동 도착으로 해석하거나 초기 상태로 덮어쓰지 않는다. 저장 키/콘텐츠 ID를 바꾸지 않는다.

### PM 표시·범위 확인 사항

- 영수증은 `사용자 확인 완료 · 실제 방문 인증 아님`으로 구분한다. PLEOS 자동 인증/실제 도착 검증 성공 배지를 사용하지 않는다.
- 새 출처/확인 방식에 맞게 브리핑의 source 표시를 처리한다. 실제 저장 성공 전에는 완료 보상 연출을 확정하지 않는다.
- 사용자는 이 수동 확인 방식으로 진행하도록 요청했다. PM과는 브리핑·제출 설명의 출처 표시를 맞추고, 저장 호환 정책에 영향을 주는 변경만 해당 소비자와 확인한다. 일반 내부 구현 선택에 별도 승인 절차를 추가하지 않는다.

## 연결 후 시험 기준

- 네 가지 응답 중 도착 확인만 저장 경계에 전달. 취소/변경/계속은 보상 없음.
- 제한·백그라운드·옛 목적지 버전·옛 generation·옛 질문 토큰은 저장 직전에도 거부.
- 저장 실패 retry는 같은 원본 초안(ID/버전/확인 시각/null 측정값)을 재전달. 새 UUID·새 시각 생성 금지.
- 중복 클릭/재진입/재시도/재시작에서도 원본 영수증의 사용자 확인 표시 유지.
- A04의 단일 GameService/readiness/pendingReceipt와 acknowledge 성공 후 이동을 그대로 적용.

## 직접 실행과 검증 결과

1. 기존 debug APK의 `io.kailand.app.PleosProbeActivity`를 Pleos에서 실행한다. 주차·UX 허용 상태가 아니면 조작하지 못한다.
2. `사용자 확인 여정 · 개발 검증` 패널에서 대전/원주/속초와 종류 선택 → `수동 여정 시작` → `여정 마무리`. 이 선택은 내비 요청을 보내지 않는다.
3. 네 가지 응답을 확인한다. 도착 확인도 메모리 초안이며 실제 저장·보상 미연결을 표시한다. 확인 흐름 초기화는 게임 진행을 지우지 않는다.
4. 실제 SDK를 함께 확인하려면 위쪽 목적지 요청 버튼을 사용한다. 실제 내비 경로 취소/종료는 보상을 확정하지 않고 질문을 준비한다. 배경에서는 숨기고 P 허용 상태의 앱 복귀 때 현재 목적지를 묻는다.

최초 수동 확인 구현은 Windows11 AMD64/JBR25.0.4/Gradle9.3.1에서 `py -3 scripts/check_all.py --android` exit0: Python44개·JUnit181개(이번 상태 시험12개 포함), 실패/오류/skip0, debug 빌드 PASS. `:app:compileReleaseKotlin` PASS이며 서명 release 설치 시험은 아니다.

이후 A03 PR #12와 B04 PR #13이 병합된 main `ab2281b`를 A 소스 `a85aaa6`에 `6a14714`로 merge(충돌 없음)했다. 통합 후 전체 검사 exit0: Python44개·JUnit192개, 실패/오류/skip0, debug 빌드 PASS(45 tasks 중11 실행·34 up-to-date), release Kotlin 컴파일 PASS(8 tasks 중1 실행·7 up-to-date). 근거는 tmp/manual-arrival/latest-main-check.log 및 latest-main-release.log다. UI7개 사례는 동일한 A 수동 확인 코드의 앞선 결과이며 B04의 실제 앱 연결·기기 렌더링 인수까지 검증한 것은 아니다.

Pleos2.1.0 API34 UI7개 사례 PASS: 계속 이동 시 ID 유지·미완료, 변경 선택 후 재선택 요구 및 같은 ID/revision 증가, 취소 시 초안 없음, 새 ID와 사용자 확인 초안1개, 배경 질문 숨김/복귀 뒤 같은 ID 확인, 검증 흐름 초기화, 실제 SDK 경로 취소 후 수동 마무리 버튼 없이 질문 표시. onRouteEnded와 onRouteCancelled callback을 실제 관측했다. 두 신호 모두 도착 인증으로 해석하지 않는다.

UI 검사에서 다이얼로그가 호스트 XML을 가리는 점을 반영해 ID 확인 순서를 수정했다. 이후 에뮬레이터의 null-root 및 /sdcard I/O 오류가 발생했으나 저장 공간은 정상이었고, 기기 화면 재연결과 /data/local/tmp 사용 뒤 남은 시험을 통과했다. 재시험의 프로세스 PID도 동일함을 확인했다. 최초 도구/기기 오류를 통과한 UI 시험으로 계산하지 않았다.

로컬 근거: tmp/manual-arrival/full-check.log, focused-check.log, release-compile.log, ui-results.json, native-hint.log, manual-question.png, manual-confirmed.png, manual-native-hint.png. 로그·캡처는 커밋하지 않는다. 공통 계약·B 저장·PM 브리핑은 수정하지 않았다. 실제 저장/보상·실패 retry·영수증 복원·전체 I06/U20·프로세스 종료 복원·새 흐름의 실차/서명 release는 NOT RUN이다.

## 공유 계약을 아직 바꾸지 않은 이유

[AGENTS.md](../AGENTS.md)는 `A shared contract change needs A and one affected consumer; keep using the current fake while waiting.`라고 정한다. 현재 사용자 요청은 A 구현을 진행할 근거이며, B 소비자의 계약 확인까지 대신하지 않는다. 실제 저장 계약 확인 전에는 기존 계약·B 저장·PM 브리핑을 수정하지 않고 A 소유의 검증 흐름을 제공한다.
