# UX-C03 — 네 시연 제어와 A 연결 요청

2026-11-10 KST / Part C. 이 문서는 **독립 구현·콜백 제안**이며 실제 네 시연 완료 보고가 아니다.
기준: 팀 ZIP C 카드·통합 배정·UX_04_DEMO_SCENARIOS. 과거 D01~D06 Runner는 변경하거나 새 제품에 노출하지 않는다.

Figma 프로토타입에서 내비 실패 흐름4(`58:1622`→섬→`23:668`)의 재시도/섬 복귀 선택지를 직접 확인했다.
저장 실패 흐름3(`58:1596`→내비 연결용 `31:1007`→목적지 `25:787`→확인 `25:982`→실패 안내/재시도→결과 `12:482`)도 조작해 확인했다.
이는 프로토타입 대조이며 실제 앱/SDK/저장 실행 결과가 아니다. Figma의 연결용 화면이나 시간 경과로 결과로 넘어가는 동작은 앱에 복제하지 않는다.

## 구현한 범위

- `UxDemoPanel`: 네 프리셋만 표시하는 개발/리허설 전용 제어 화면. 준비 요청→준비 결과 수동 확인→A 앱 흐름 열기 요청→실제 결과 수동 확인.
- `UxDemoPreset`: 원주 새 지역, 같은 날 원주 재방문, 확정 저장 실패 후 원본 재시도, 내비 실행 실패.
- `checkUxDemoPreparation`/`checkUxDemoResult`: A가 전달한 불변 증거를 읽는 순수 검사. 요청 접수·타이머·화면 탭으로 PASS를 만들지 않는다.
- debug Preview: 기존 packaged Catalog를 읽고 A 연결 미완료를 표시한다. 가짜 성공/영수증/주차를 공급하지 않는다.
- AppRoot/기존 DemoPanel/공유 계약/B 저장/PM fixture는 변경하지 않는다. C가 여정 명령·reset·저장소·SDK를 직접 호출하지 않는다.

## A에 필요한 세 콜백 — 소비자 합의 전 실제 연결 금지

| C 입력/출력 | A 구현 책임 |
| --- | --- |
| `onPrepare: suspend (UxDemoRequest) -> RequestResult` | 별도 시험 프로필/저장임을 검사하고 B 소유 준비 경계로 프리셋을 구성. 기존 사용자 저장을 몰래 초기화하지 않음. 오류 주입은 개발/리허설에만 설정 |
| `onOpenApp: suspend (UxDemoRequest, sessionId: String) -> RequestResult` | 동일 시험 환경·현재 gate/foreground/generation 재검사 후 새 S01부터 실제 새 flow 열기. 완료된 시연을 재실행하지 않음 |
| `readEvidence: () -> UxDemoEvidence?` | 준비 상태와 실제 수행 결과를 메모리의 불변 읽기 모델로 제공. 아직 미확인/null은 NOT RUN. C 화면이 사라져도 A가 원본 증거를 보관 |

`requestToken`은 C 제어 요청 상관관계용으로 생성하며 **런타임 journeyId가 아니다.** A가 생성한 여정 ID·routeRevision·시각·원본 명령을 덮어쓰지 않는다.
`sessionId`는 A가 식별한 별도 시험 환경이다. UI는 요청 token/preset과 session의 일치를 확인하며, A도 모든 콜백 진입 시 이를 검증해야 한다.
`developmentEnvironment`/`isolatedEnvironment`는 A가 사실대로 제공한다. 사용자 체크박스나 C의 가짜 실제 주차 입력으로 참값을 만들지 않는다. 실제 platform gate와 UNKNOWN 차단은 유지한다.
외부 앱 사용/화면 이탈 때 진행 중 C 요청은 취소하고 자동 재개하지 않는다. 프리셋/요청은 보존하되, 복귀 후 사용자가 준비 상태를 다시 확인해야 제어를 연다. 공유 Mutex를 잡은 채 C job을 join하지 않는다.

## 준비 요청 목록

| 프리셋 | 별도 시험 초기 상태 | A의 오류/입력 준비 | 확인할 결과 |
| --- | --- | --- | --- |
| 새 지역 | 빈 지도·모든 기념품 잠김·성장0·방문0·보류 결과 없음 | 등록 원주 대상 새 flow, 그날 최초 성장 가능한 조건 | 확정 REGION 영수증·원주 최초 선물3종·성장+1·방문+1 |
| 재방문 | 원주만 발견/선물 보유, 방문1이상, 같은 KST 날짜 성장 이미 사용 | 새 여정 ID로 원주 재방문 | 성장0·추가 선물0·방문+1 |
| 저장 재시도 | 새 지역과 동일 | 실제 저장 확정 실패1회, `ArrivalCommand` 원본과 재시도본·실패 시점 snapshot 보관 | 같은 ID뿐 아니라 시각/revision/source/내용 동일, 실패 시점 불변, 성공 뒤 한 번 반영 |
| 내비 실패 | 독립 초기 상태 | 내비 실행 실패1회, 실패 시점 snapshot·출발/도착/영수증 여부·복구 분기 보관 | 실패 중 진행/보상 없음. 섬 복귀는 상태 불변, 정상 재시도 분기는 확정 결과 별도 확인 |

재방문 프리셋은 **같은 날** 시연이다. 다른 날 등록 지역 최초 완료 +1/상한3·재방문 선물0의 B 규칙을 바꾸지 않는다. 해당 다른 날/자정 회귀는 별도 인수 항목이다.
내비 호출의 RequestResult.Accepted만으로 외부 화면 실행을 성공 처리하지 않는다. 실제 내비 실패 여부·재시도 성공/섬 route 복귀는 A가 관측해 전달한다.

## 증거 필드 의미

- `preparedGame`: B 경계가 준비한 별도 상태. `visitsBefore`는 B/A의 실제 집계이며 null을0으로 바꾸지 않는다.
- `beforeAttempt`: 캐릭터 선택 등 정상 사전 조작 이후, 내비/도착 시도 직전 snapshot. 준비 상태와 진행/소유/발견이 달라졌으면 검사 중단.
- `afterAttempt`: 선택한 분기 뒤의 확정 snapshot. `receipt`는 저장에서 반환된 원본으로, ack 후 pending이 없어져도 따로 보존한다.
- `source`: A가 실제 경로에 맞춰 제공한다. fixture를 PLEOS 성공으로 표시하지 않는다. USER_CONFIRMED 측정은 null이어야 한다.
- 저장 실패 증거는 확정 실패일 때만 제공한다. 지연/timeout만으로 실패나 미지급을 단정하지 않는다. 재시도는 A의 원본 명령 경계다.
- 내비 실패 중 `travelStarted/arrivalSubmitted`는 false, 실패 영수증은 null이어야 한다. 한 분기 성공을 양쪽 복구 분기 전수로 확대하지 않는다.

## PM에 확인할 fixture·대본

원주 Catalog ID/기념품 명칭은 그대로 쓴다. 날짜·횟수 예시는 제품 데이터로 넣지 않는다.
준비용 KST 날짜/시계 제어와 발표 순서(내비 실패→새 지역→재방문→저장 retry)는 PM/A가 실제 별도 환경에 맞춰 확인한다.
최초/재방문 대본과 실제 최초 지역 선물·하루 성장 조건을 분리한다. 거리/시간 null·연비/누적거리 미지원 값을0이나 예시 수치로 채우지 않는다.
위 요청은 문서로만 준비했으며 A/PM에게 메시지를 전송하지 않았다.

## 검증과 NOT RUN

소프트웨어 검사 결과는 이번 검토 ZIP REPORT.md의 실제 집계를 따른다. 테스트는 합성 불변 증거의 검사이며 실제 저장·SDK·전체 flow 실행이 아니다.
Windows 공통 `check_all.py --android` exit0: Python59/JUnit294(신규 시연 증거 검사9개 포함), 실패/오류/skip0, debug 빌드15초(11 executed/34 up-to-date) PASS.
실제 A 준비/오류 주입/route 연결, 네 시연 앱 전체 실행, Preview 렌더링·터치·Pleos 창별/큰 글자·접근성·release/실차는 NOT RUN이다.
PR27(UX-C01~02)의 공개 head에는 이 UX-C03 작업을 push하지 않는다. 먼저 A와 콜백 제안을 맞추고 실제 연결 뒤 별도 PR로 정리한다.
