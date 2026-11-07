# USER_CONFIRMED 공통 계약 공급 — A → B05

현재 적용 상태(2026-11-06): B PR #20 저장 보강을 수용하고 A의 전용 사용자 확인 저장 경로를 연결했다.
일반 completeArrival의 신규 출처 차단은 유지하며 토큰을 검증한 전용 진입만 허용한다.
[연결 동작·실행 결과·남은 인수](A06_USER_CONFIRMED_INTEGRATION.md)를 따른다.
아래 공급/대기 설명은 2026-11-05 당시 기록이다.

2026-11-05 KST. B가 전달한 사전 감사에서 기존 보상·원본 영수증·단일 DataStore·schemaVersion 1
재사용과 아래 enum 이름을 확인했다. A와 영향받는 저장 소비자 B의 이 확인을 근거로 공통 타입을 제공한다.
감사 메시지에는 실행 로그·branch/SHA가 없으므로 신규 출처 저장 시험 완료로 해석하지 않는다.

## 이번 제공 범위

실행 타입의 기준은 `app/src/main/java/io/kailand/core/contract/KailandContracts.kt`다.

- `DataSource.USER_CONFIRMED`
- `ArrivalEvidence.USER_CONFIRMED_DESTINATION`

기존 enum 이름·순서와 DTO 필드, GameService API, 저장 키·콘텐츠 ID·schemaVersion은 유지한다.
새 값은 enum 끝에 추가한다. PM 브리핑의 exhaustive source 분기에
`사용자 확인 완료 · 실제 방문 인증 아님` 한 항목과 문구 회귀 시험을 추가했다.
기존 DEMO/PLEOS 문구와 레이아웃은 유지한다. C 코드는 변경하지 않았으며 기존 source 전체 순회 시험이 새 값도 검사한다.

**타입 공급과 실제 저장 활성화는 별개다.** A의 GameCommandCoordinator는
새 source 또는 새 evidence를 포함한 도착 요청을 B에 전달하지 않고 Rejected로 응답한다.
사용자 확인 초안은 여전히 메모리 전용이다. 모드 버튼은 기존 DEMO/PLEOS만 유지한다.
새 타입을 사용한 모델·시험은 개발할 수 있지만, 앱의 사용자 확인 저장 기능을 완료했다고 표시하지 않는다.

[기존 공통 기준서 v1.0 원문](MEMORIDE_USER_CONFIRMED_COMMON_STANDARD_v1.0.md)을 함께 커밋했다.
문구는 유지하고 저장소 검사에 맞춰 두 줄의 Markdown 줄끝 공백과 최종 개행만 정리했다.
검토 ZIP에는 바이트 그대로의 원본도 포함한다. 원문의 과거 구현 상태와 시험 숫자는 당시 기록이다.
현재 공급 상태는 이 문서와 해당 PR의 검증 HEAD를 따른다.

## B가 이어서 구현할 범위

현재 BGameService의 DEMO 여부 비교식은 새 enum을 추가한 뒤의 완전한 조합 검증이 아니다.
아래 검증·저장 복원 보강은 B 소유 파일에서 B가 진행한다. A는 이를 대신 구현하지 않았다.

| source | 허용 evidence |
|---|---|
| DEMO | DEMO_FINAL_DESTINATION |
| PLEOS | PLEOS_FINAL_DESTINATION |
| USER_CONFIRMED | USER_CONFIRMED_DESTINATION |

- 전체 9조합 중 위 3조합만 허용하고 나머지 6조합은 Rejected, 저장 불변을 시험한다.
- USER_CONFIRMED의 elapsedSeconds 또는 distanceMeters가 non-null이면 입력을 거부한다.
  0도 허용하지 않으며 null로 조용히 바꿔 잘못된 입력을 성공 처리하지 않는다.
- 원본 영수증의 source·ID·최초 확인 시각·두 null을 저장/재시도/중복/재생성 복원에서 보존한다.
  DAILY KST 날짜·성장 상한, REGION 최초 보상·재방문, 원주 D06, acknowledge/pending 규칙은 유지한다.
- 저장 복원 시 pending만 보지 않고 과거 영수증 전체의 신규 source와 null 불변조건도 검증한다.
- 기존 DEMO/PLEOS 저장을 새 코드에서 읽는 시험과 신규 출처의 실제 DataStore 왕복·실패 시험을 추가한다.
- 새 USER_CONFIRMED 저장을 구버전에서 정상 사용한다고 보장하지 않는다.
  알 수 없는 source 읽기 실패는 원본을 보존하고 PLEOS로 바꾸거나 조용히 초기화하지 않는다.
  schemaVersion 1 유지와 다운그레이드 지원은 별개다. 저장 비호환 정책의 추가 변경은 별도 확인한다.

## 공급과 후속 통합 순서

1. 이 A 계약 공급 PR의 커밋을 받아 B05 구현·시험을 진행한다. B는 기존 파트 브랜치를 유지한다.
   main 반영 전 시험이 필요하면 이 A 브랜치를 받아 로컬 통합하되 공통 DTO 사본은 만들지 않는다.
2. B가 최소 저장 검증·복원 변경과 실제 시험 결과를 별도 PR로 공유한다.
   갤러리 PR #16은 이미 병합됐으며 이번 저장 변경을 그 이력에 섞지 않는다.
3. B 저장 보강 후 A가 현재 gate·foreground·owner·generation·목적지 버전·질문 토큰과
   원본 명령 유지 조건을 확인하는 실제 수동 여정 경로를 연결하고, 위 임시 진입 차단을 제거한다.
4. PM/C의 표시와 실제 저장된 pendingReceipt, 비동기 acknowledge, reset/I06/U20을 함께 검증한다.
   제안된 PM 문구의 컴파일 분기는 이번에 공급했지만 신규 저장·기기 인수를 끝냈다는 뜻은 아니다.

이번 공급은 A05 서명 release·다른 PC 설치나 실제 SDK 자동 도착 구현을 포함하지 않는다.
기존 gate·UNKNOWN 차단·실제 제한 우선 규칙은 유지한다.
