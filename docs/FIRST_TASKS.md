# 파트 선택 후 첫 개발

**큰 경계는 확정, 내부 계획·설계·구현은 담당자 자율**이다. 아래 시작 요청을 자기 Codex·Claude에 주고 파일을 직접 읽게 한다. 전체 채팅·DOCX·ZIP을 매번 첨부할 필요는 없다. 한 번에 해당 첫 작업만 시작하고 이후는 [WBS](../tasks/backlog.md)를 따른다. 계획서 승인 대기를 새로 만들지 않는다.

먼저 [환경 설치](ONBOARDING.md), [AI 설정](LOCAL_AI_SETUP.md), [Git 흐름](GIT_DAILY.md)을 끝내고 작업 브랜치를 만든다. 역할은 PM이 [배정표](../config/team.json)에 한 번 기록한다. 공유 계약의 타입·의미는 보존하고, A01 이후 컴파일되는 계약을 기준으로 한다.

## part-1

**A: 공통 앱·차량 여정·Pleos·제한·연결·빌드.** [역할 카드](../roles/A.md), [첫 공용 빌드 기준](BOOTSTRAP_HANDOFF.md), [구조·소유권](02_ownership_architecture.md)을 읽는다.

첫 PR은 A01이다. 공용 앱·wrapper·컴파일 계약·최소 라우터·개발 fake·버전 기록을 전달한다. 실제 SDK 성공은 A03이며 첫 공유를 막지 않는다. A02 여정 fake → A03 SDK 관찰 → A04 저장·브리핑 연결 → A05 release → A06 검증 순서다.

```text
나는 MEMORIDE 파트 1(A) 담당이다. 먼저 git status와 AGENTS.md,
roles/A.md, tasks/backlog.md의 A01, docs/BOOTSTRAP_HANDOFF.md,
docs/15_cross_platform.md, contracts의 DTO·UI 계약을 읽어라.
A01의 변경 경로·최소 구조·버전 호환 확인·완료 검사를 짧게 계획하고 구현하라.
문서만 있는 현재 저장소를 보존하고 하나의 app 모듈을 루트에 만든다.
검증 전에는 toolchain verified를 true로 바꾸지 말고 실제 명령·결과를 기록하라.
다른 파트가 시작할 수 있는 공용 기반까지만 작은 PR로 나눈다.
내부 설계는 이 파트에서 결정하고 PM의 함수별 승인을 기다리지 않는다.
공유 경계·비용·제한·저장 호환·하루 이상 일정 영향만 지정된 절차로 요청하라.
```

## part-2

**B: 섬·캐릭터·아이템·보상·유일한 게임 저장.** [역할 카드](../roles/B.md), [상태 규칙](04_rules_states.md), [Catalog](../content/catalog.json), [데이터 계약](03_contracts_data.md)을 읽는다.

첫 PR은 B01의 기본 GameSnapshot·GameService·단일 저장 구성이다. 먼저 한 캐릭터·한 지역으로 B02 원자 지급·중복 방지까지 연결한다. 이후 B03 UI → B04 레이어 렌더러 → B05 복원·실패 검증이다. 캐릭터·음식·의상·장식의 그림과 렌더링은 B, 지역 주제·명칭·Catalog 원본은 PM이다. 제공 자산으로 시작하고 새 그림 완성을 기다리지 않는다.

```text
나는 MEMORIDE 파트 2(B) 담당이다. git status와 AGENTS.md, roles/B.md,
tasks/backlog.md의 B01~02, docs/03_contracts_data.md와 상태 규칙,
content/catalog.json, 관련 arrival·UI fixtures를 읽어라.
GameService가 게임 영구 상태를 쓰는 유일한 경계가 되도록 내부 설계를 정하라.
첫 작업 B01의 파일·입출력·최소 구현·시험을 짧게 계획하고 구현하라.
A01을 못 받았으면 계약·보상 예제부터 정리하고 별도 Android 앱은 만들지 마라.
A01 이후 실제 공통 계약과 빌드 버전을 사용한다. A의 SDK를 기다리지 말고
ArrivalCommand fake로 개발한다. 다른 feature와 AppRoot는 수정하지 않는다.
내부 계획은 내가 이 파트에서 자율 결정하며 일반 결함은 직접 해결한다.
계약 변경은 A와 해당 소비자, 큰 범위·비용·저장 비호환만 PM에게 요청하라.
```

## part-3

**C: 동행 모의 화면·동의 기반 가상 방문·월드맵·시연.** [역할 카드](../roles/C.md), [방문 시나리오](07_simulation_visit.md), [화면 계약](../contracts/ui_contracts.md), [UI 샘플](../fixtures/ui_samples.json)을 읽는다.

첫 PR은 C01 모의 내비·고정 캐릭터 슬롯이다. B 렌더러 대신 임시 슬롯으로 시작한다. C02 방문은 독립 로컬 상태로 구현하고 fake clock으로 시험한다. 이후 C03 월드 읽기 → C04 패널 → C05 타이머·백업 설치다. 월드맵은 PM의 지역·B의 발견 상태를 읽으며 지급·저장은 하지 않는다.

```text
나는 MEMORIDE 파트 3(C) 담당이다. git status와 AGENTS.md, roles/C.md,
tasks/backlog.md의 C01, contracts/ui_contracts.md, docs/07_simulation_visit.md,
fixtures/ui_samples.json을 읽어라. 내부 UI·상태 설계는 이 파트에서 정한다.
C01의 모의 화면·캐릭터 슬롯·DEMO 표시·제한 시 중단을 작게 계획하고 구현하라.
A01 전에는 스케치·상태·입력 예제부터 준비하고 별도 Android 앱은 만들지 마라.
캐릭터가 없어도 임시 슬롯으로 진행하고 feature끼리 직접 import하지 않는다.
실제 내비 오버레이·번호판 조회·V2V 서버·게임 저장은 구현하지 않는다.
일반 화면 결정마다 PM 승인을 기다리지 말고 자체 시험 후 작은 PR을 만든다.
공유 계약 변경만 A와 소비자에게 요청하고 큰 범위·비용·지연만 PM에게 알린다.
```

## pm

**PM: 브리핑 코드·문구·Catalog·fixtures + 통합 관리·인수·제출.** [역할 카드](../roles/PM.md), [UI 계약](../contracts/ui_contracts.md), [UI 샘플](../fixtures/ui_samples.json)을 읽는다.

첫 PR은 P01 브리핑 화면·순수 문구 함수다. 확정 영수증만 표시하고 보상·저장을 계산하지 않는다. A01 전에는 문구 함수·null·재방문 예제와 스케치를 정리하고, 이후 Compose로 만든다. P02 지역 콘텐츠·검증 → P03 실제 결과·Mac 확인으로 진행한다. 통합을 맡는다고 모든 파트 코드를 대신 구현하지 않는다.

```text
나는 MEMORIDE PM(P)이며 브리핑을 직접 개발한다. git status와 AGENTS.md,
roles/PM.md, tasks/backlog.md의 P01, contracts/ui_contracts.md,
fixtures/ui_samples.json과 관련 DTO를 읽어라.
P01의 결과 표시·null·재방문·DEMO 문구를 간단히 계획하고 구현하라.
A01 전에는 문구·예제를 준비하고 별도 Android 앱은 만들지 마라.
저장·보상 계산·SDK·AppRoot는 수정하지 않는다. 닫기·아이템 보기는
A의 비동기 콜백 결과를 따르고 실패 시 임의로 성공 이동하지 않는다.
작은 시험·Mac 빌드 결과를 구분해 기록하고 다른 담당자 기능을 대신 재설계하지 마라.
```

## 첫 PR에서 공통으로 남길 것

바뀐 동작, 공개 함수 호출 예제 또는 화면 진입 방법, 실제 검사 결과, NOT RUN, 다음 한 작업을 짧게 쓴다. [상태 카드](status/)는 현황 공유용이며 모든 내부 결정을 적는 일지가 아니다. 연결 오류는 생산자·소비자가 함께 확인하고, 작성자가 자기 기능을 수정한다. PM은 범위·통합 순서·인수에 집중한다.
