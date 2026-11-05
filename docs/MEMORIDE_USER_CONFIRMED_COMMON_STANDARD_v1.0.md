# MEMORIDE 사용자 확인 도착 — A→B 공통 구현 기준서 v1.0

작성일: 2026-11-04 KST
상태: **A가 정한 단일 적용 기준 · B의 호환성 확인 후 공통 계약 반영**
관련 A 구현: https://github.com/Finn-02/MEMORIDE/pull/14

## 1. 이번 요청

사용자가 주차·이용 허용·앱 전경 상태에서 선택 목적지 도착을 확인하고, 그 확인을 기존 GameService의 저장·보상 처리에 연결한다.

여러 대안 중 하나를 골라 달라는 요청이 아니다. 아래 기준으로 진행하되, B는 기존 저장 및 소비자 코드와 충돌하는 부분을 실제 코드로 확인하고 구체적으로 회신한다. B가 확인하지 않은 상태를 팀 전체 합의 완료로 표시하지 않는다.

현재 A의 구현은 debug 진단 화면의 메모리 초안까지다. 공통 DTO 변경, B 저장 호출, 실제 보상, 일반 AppRoot 연결은 아직 하지 않았다. PR 상태와 최신 main은 작업 시작 시 직접 확인한다.

## 2. 고정할 공통 기준

### 출처와 근거

공통 enum에 다음 값을 추가하는 기준으로 통일한다. 기존 값은 삭제·이름 변경하지 않는다.

- `DataSource.USER_CONFIRMED`
- `ArrivalEvidence.USER_CONFIRMED_DESTINATION`

허용 조합은 다음 세 가지뿐이다.

| source | evidence |
|---|---|
| DEMO | DEMO_FINAL_DESTINATION |
| PLEOS | PLEOS_FINAL_DESTINATION |
| USER_CONFIRMED | USER_CONFIRMED_DESTINATION |

다른 조합은 `ArrivalResult.Rejected`로 거부하고 저장을 변경하지 않는다. 현재 DEMO 여부만 비교하는 검증식은 명시적 조합 검증으로 교체한다.

`USER_CONFIRMED`는 사용자의 도착 선언을 뜻한다. SDK 자동 도착이나 실제 방문 인증을 뜻하지 않는다. 이 출처는 명령·영수증·원본 저장·복원·브리핑까지 유지한다. 기존 `ArrivalReceipt.source`와 `SavedReceipt.source`로 보존하며, 이번 작업에서 별도 provenance 필드나 새로운 저장 스키마를 임의로 늘리지 않는다. 코드 확인 결과 이것만으로 요구사항을 충족할 수 없으면 이유와 최소 수정안을 보고한다.

### 입력 값

| 값 | 의미와 처리 |
|---|---|
| journeyId | A가 최초 발급한 여정 ID. 중복·실패 재시도에서도 유지 |
| routeRevision | 확인한 목적지 버전. 활성 여정에서 목적지 변경 시 증가 |
| kind | 기존 DAILY 또는 REGION |
| regionId | REGION은 기존 유효 지역 ID. DAILY는 A가 null로 전달하며 B의 기존 정규화 유지 |
| completedAtEpochMs | 최초 사용자 확인 시각. 재시도 시 새 시각으로 바꾸지 않음 |
| elapsedSeconds / distanceMeters | 현재 사용자 확인 흐름에서는 둘 다 null. 0이나 미확정 SDK 값으로 대체하지 않음 |

A의 generation·질문 nonce는 A의 오래된 입력 차단용이다. B의 영수증 필드에 추가하거나 B가 플랫폼 상태를 판단하도록 만들지 않는다. B는 기존 ID 기반 멱등성·원자 저장을 유지한다.

### 완료·취소의 의미

- ‘도착했어요’만 완료 초안을 생성하고 실제 연결 후 저장 명령으로 전달한다.
- 목적지 변경·취소·계속 이동·질문 닫기는 완료 명령을 보내지 않는다.
- 내비 종료·취소·도착 이벤트는 확인 질문의 힌트일 뿐 자동 저장 근거가 아니다.
- `RequestResult.Accepted`와 A 내부 `CONFIRMED`는 저장·보상 성공이 아니다.
- `Applied` 또는 `AlreadyApplied`의 원본 영수증과 저장된 `pendingReceipt`를 실제 UI 판단에 사용한다.

## 3. 기존 보상 규칙 유지

- DAILY: `completedAtEpochMs`의 Asia/Seoul 날짜 기준 최초 완료 +1, 같은 날짜 추가 완료 +0, 성장 상한 3.
- 사용자 확인이 자정을 넘기면 사용자 확인 날짜에 해당한다. 실제 도착 시각을 추정·소급하지 않는다.
- REGION: 기존 지역 최초 보상·발견 규칙 유지. 원주 D06의 아이템 3개 및 null 측정값 보존 유지.
- 기존 원본 영수증·처리 이력·중복 방지·pendingReceipt 규칙 유지.
- 저장 실패 후 재시도는 동일 ID·목적지 버전·최초 확인 시각·출처·null 측정값을 사용한다.
- 중복 호출은 원본 영수증 반환. 이미 acknowledge한 영수증을 다시 pending으로 만들거나 최신 pending을 덮어쓰지 않는다.
- 사용자 확인 전용 별도 보상 계산기·저장 파일·GameService 인스턴스를 만들지 않는다.

## 4. 소유권과 적용 순서

### A 담당

- 공통 enum 및 그 변경으로 필요한 컴파일 소비자 정렬을 조정한다.
- 공통 타입 반영은 A 소유 변경으로 진행한다. B가 독자적인 공통 DTO 사본을 만들거나 공통 타입을 먼저 무단 변경하지 않는다.
- 실제 gate·foreground·generation·목적지 버전을 저장 직전에 재검사한다.
- 게임 명령과 reset을 조정하고 늦은 입력을 차단한다.
- 단일 GameService/readiness, 공통 JourneySnapshot, AppRoot·라우팅을 연결한다.
- 실제 저장된 pendingReceipt로 브리핑을 열고 acknowledge 성공 뒤에만 이동한다.

### B 담당

- 현재 코드와 본 기준의 호환성을 검토한다.
- 합의된 공통 enum이 제공되면 BGameService의 허용 조합 검증을 변경한다.
- 사용자 확인 출처의 저장·원본 영수증·중복·실패·복원 시험을 구현한다.
- B의 기존 보상·저장 책임과 공개 GameService API를 유지한다.
- 다른 파트의 AppRoot·Pleos·브리핑·C UI·Gradle을 부수적으로 수정하지 않는다.

### PM / C

- PM: 브리핑에 ‘사용자 확인 완료’를 표시하고 실제 방문 인증으로 설명하지 않는다. enum 추가로 영향을 받는 source 분기도 함께 정렬한다.
- C: A가 전달한 공통 여정 상태를 표시한다. 기존 DEMO 입력·성공 판정과 사용자 확인 흐름을 섞지 않는다.
- 이 문서는 B에게 PM/C 소유 코드를 대신 수정하도록 요청하지 않는다.

### 순서

1. B가 최신 코드와 저장 호환성을 확인하고 구체적인 충돌 유무를 회신한다.
2. A와 B가 계약 확인을 마치고 A가 공통 타입 변경을 소비자 수정과 함께 반영한다. 중간 main이 컴파일되지 않는 방식으로 enum만 단독 병합하지 않는다.
3. B는 공통 타입을 받은 뒤 B 소유 구현·시험을 완료하고 작은 PR로 공유한다.
4. PM 표시와 A04 실제 연결을 반영하고 A/B/C/PM 통합 시험을 진행한다.

공통 타입이 아직 없으면 기존 fake와 기존 계약을 유지한다. 가능한 코드 검토·시험 설계는 진행하되, 새 enum이 이미 제공된 것처럼 컴파일 PASS를 주장하지 않는다. 구현 가능한 단계에서 끝내지 말고 검증과 결과 보고까지 수행한다. 타입 공급 대기라면 남은 의존성을 정확히 보고한다.

## 5. 저장 호환 기준

- 기존 DEMO/PLEOS 저장을 새 버전에서 정상 복원한다.
- 기존 저장 키·콘텐츠 ID·원본 영수증 ID를 바꾸지 않는다.
- 현재 SavedReceipt는 문자열 source를 DataSource.valueOf로 복원한다. 구버전은 USER_CONFIRMED를 알지 못하므로 새 저장의 구버전 열기는 정상 지원으로 간주하지 않는다.
- **이번 기준의 다운그레이드 정책:** 사용자 확인 영수증이 저장된 데이터는 이를 지원하는 버전에서만 연다. 구버전 호환을 위해 출처를 PLEOS로 바꾸지 않는다.
- 알 수 없는 source/손상된 저장을 자동 초기화하거나 덮어쓰지 않는다. 현재 B의 READ_ERROR 경로 및 원본 데이터 보존을 검증한다. 별도 명시적 초기화는 기존 정책을 따른다.
- 과거 배포된 구버전을 이번 PR이 수정할 수 있다고 주장하지 않는다. 구버전 동작은 확인 범위를 적고 사용자 안내가 필요한 점을 회신한다.
- schemaVersion 변경이나 손실성 마이그레이션이 필요하다는 결론이면 임의로 적용하지 말고 근거를 보고한다. 저장 비호환 정책의 추가 변경은 관련 담당자와 확인한다.

## 6. B의 필수 검증

1. 세 허용 조합은 통과하고 나머지 조합은 거부되며 저장이 변하지 않는다.
2. USER_CONFIRMED DAILY의 최초/추가 완료·성장 상한·KST 자정 경계가 기존 규칙대로다.
3. USER_CONFIRMED REGION 최초·재방문 보상 및 원주 D06·null 값이 유지된다.
4. 중복 호출·재생성 후 재호출에서 출처·ID·확인 시각·원본 영수증이 유지되고 한 번만 지급된다.
5. 저장 실패 시 보상·원본·pending이 부분 반영되지 않고 동일 명령 재시도가 가능하다.
6. acknowledge 후 중복 입력이 pending을 다시 열지 않으며, 오래된 입력/ack가 최신 pending을 지우지 않는다.
7. 기존 저장 복원과 새 사용자 확인 영수증 왕복 저장·복원을 통과한다.
8. 알 수 없는 source 저장은 읽기 오류로 드러나고 원본 파일/값을 덮어쓰지 않는다.

기능 시험 뒤 PR 전 공통 검사: Windows `py -3 scripts/check_all.py --android`, macOS `python3 scripts/check_all.py --android`. 저장소 wrapper와 기록된 JDK를 사용한다. 환경에 맞는 실제 명령·결과를 적는다. SDK·실차·release 설치·A04 통합을 단위 시험 성공으로 표시하지 않는다.

## 7. 현재 근거와 검증 한계

A의 앞선 기록: Python 44개·JUnit 192개 PASS, debug 빌드 PASS, release Kotlin 컴파일 PASS. 수동 흐름 단위 시험 12개 및 Pleos UI 7개 사례 PASS. 이는 새 저장 계약의 검증 결과가 아니다.

실제 사용자 확인 저장·보상·실패 재시도·영수증 복원·전체 I06/U20·실차·서명 release는 NOT RUN. 활성 여정과 미저장 초안은 메모리 상태이며 프로세스 종료 뒤 복원은 미구현이다.

이 문서 작성 시 앱 코드 수정이나 시험 재실행은 하지 않았다.

## 8. B가 회신할 결과

- 기준 확인: 적용 가능 / 구체적인 충돌 및 근거 파일·행
- 현재 branch/SHA 및 확인한 main SHA
- 공통 타입 반영 여부와 A에 필요한 최소 변경
- B 수정 파일·동작 변화
- 실제 검사 명령·결과 및 NOT RUN
- 저장 호환·다운그레이드 한계
- PR 링크 또는 남은 의존성
- A04 호출 조건과 다음 단계

관련 코드: `core/contract/KailandContracts.kt`, `feature/island/BGameService.kt`, `feature/island/GameSave.kt`, `feature/island/GameSaveStore.kt`.

일반 내부 구현은 B가 자율적으로 결정한다. 핵심 범위·추가 비용·제한 완화·저장 비호환·하루 이상 지연에 해당하는 경우만 기존 절차로 알린다. 자동 병합·force push·불필요한 새 프로젝트·유료 서비스는 추가하지 않는다.
