# C 진행 상태

상태: IN PROGRESS — C01~C03 main 병합 확인, C04 독립 수동 시연 패널·Runner·시험 구현 완료. AppRoot 연결·기기 UI 시험 대기

2026-11-04 C04: PR #5 병합 main 8c78b8e(B 저장 기반 포함)를 기존 파트 브랜치에 반영했다. DemoScenarioRunner·DemoPanel·PM fixtures 읽기 구현, C04 23개·전체 Android 106개·Python 44개 PASS, debug 빌드 성공(exit 0). C04를 같은 브랜치에 commit·push하고 새 PR은 사용자 확인 후 생성한다. 실제 A02/A04 포트·일상 목적지·전체 초기화 연결과 기기 인수는 NOT RUN. [C04 연결·검증](../c04_demo.md)

2026-11-04 PR #5 R1 보완: 초기화 완료 토큰을 방문 generation과 분리해 reset 진행 중 모드 전환 후에도 해당 성공 통지를 처리한다. 새 reset은 이전 토큰을 무효화하고 실패·오래된 완료·제한 gate는 계속 차단한다. 수정 전 회귀 1개 실패를 재현한 뒤 수정 후 C02 24개·전체 Android 59개·Python 44개 PASS, debug 빌드 성공(exit 0). 기존 PR #5에 일반 push로 반영하며 main 병합과 AppRoot/기기 통합은 미수행
오늘 완료와 SHA: 2026-11-03, C01 main 병합 d83f472 확인. C02 15c4269·C03 9b240e2로 보존 후 최신 main b95e0e4를 c863644에 병합. [C01 화면·연결 예제](../c01_companion.md), [C02 구현·연결·시험](../c02_visit.md), [C03 월드맵·읽기 규칙](../c03_world.md) 보존
막힌 작업과 우회: 기본 빌드 막힘 없음. 기존 일반 Android AVD에서 A01 DevelopmentActivity 시작·메인 섬 화면 관찰 완료. 이 화면은 A01 임시 연결 자리이며 C02/C03 실제 앱 진입 완료가 아님
다음 작업: A의 C01~C04 AppRoot·포트·초기화 연결 및 B 실제 상태/renderer 연결 후 C05 기기 반복·백업 설치 인수. C04 독립 구현 PR은 사용자 확인 후 생성
시험: C02 전체 py -3 scripts/check_all.py --android exit 0(Python 34개·Android 33개 PASS, C02 21개 포함), debug 빌드 PASS(9개 task 실행/36 UP-TO-DATE). 마지막 제한 문구 조정 후 Android 33개·debug 빌드 재확인 PASS(7개 task 실행/38 UP-TO-DATE). C01 Preview 4개 관찰은 앞선 결과이며 C02 Preview는 컴파일만 확인. U07~08의 C 로컬 단위 시나리오는 PASS, 실제 UI 상호작용·I01·I03·I06·S03~04·S06·S08~09·U20 전체 통합·Pleos·release NOT RUN
전달: C01 PR #3·C02/C03 PR #5 MERGED 확인. C04 일반 push까지 진행하고 새 PR/병합은 별도 사용자 지시를 따른다. 실제 SHA와 전송 결과는 작업 보고서에 기록

전달 전 최신 main 재검사: `py -3 scripts/check_all.py --android` exit 0, Python 44개·Android 56개(A01 4 + PM 13 + C01 8 + C02 21 + C03 10) PASS, debug 빌드 성공(17 tasks executed / 28 up-to-date). 앱 연결·기기 통합 인수 완료를 뜻하지 않음

C03 추가 검사: main 동기화 전 py -3 scripts/check_all.py --android exit 0, Python 34개·Android 43개 PASS(C03 10개 포함), debug 빌드 성공(11 tasks executed / 34 up-to-date). fresh 대화형 Preview에서 수집 0/9·미발견 3지역·상세 잠금·후보 버튼을 관찰. 이미지 디코딩·가로/큰 글자·콜백 라우팅·실제 기기 I01/I06/S03은 NOT RUN

시연 추가 확인: C01 PreviewActivity에서 숨김·재표시 및 별도 UNKNOWN 샘플의 차단 안내를 관찰. C02 InteractivePreview에서 요청·카운트다운·무응답 만료·수신 시점 버튼 표시를 관찰했고 수락/거절 클릭 완료는 미확인. 실제 CompanionScreen/VisitRoute의 lifecycle 및 AppRoot 통합 검증은 별도

## 파트 C 체크리스트

체크는 해당 항목만 완료했다는 뜻이다. 준비 문서 완료와 실제 기능 구현·시험 통과를 구분한다. A01은 main 병합 후 현재 파트 C 브랜치에 반영했고, 이 PC에서 기본 빌드를 확인했다.

### 지금까지 완료

- [x] 작업 폴더와 파트 C 브랜치 확인: part/c-c01-companion
- [x] PM 공통 지침·역할·계약·Catalog·MVP 범위 검토
- [x] GitHub main 수령 확인: a4a0010이 현재 C 브랜치 29796f6의 조상에 포함
- [x] 설치 환경 진단과 Git 로컬 설정 확인
- [x] 공통 정적 검사와 기존 Python 테스트 34개 PASS 확인
- [x] C01 화면 스케치·고정 슬롯·숨김·제한 시 중단 기준 준비
- [x] C01 독립 Compose 화면·debug Preview·표시 정책 시험 8개 구현, 네 Preview 렌더링 확인
- [x] C02 방문 상태·동의·만료·쿨다운과 시험 예제 18개 준비
- [x] C02 독립 로컬 controller·시점 전환·카운트다운·숨김·동의 UI와 FakeClock 시험 21개 구현, 컴파일·시험·빌드 PASS
- [x] C03 월드맵·지역 상세·수집 표시와 검증 예제 10개 준비
- [x] C03 독립 월드맵·상세·수집·PNG 로딩/fallback·후보 콜백 구현, 단위 시험 10개·debug 빌드 PASS
- [x] 2026-11-03 팀 회의 A안 확인: B의 오프라인 3D → PNG 공통 렌더러를 A 주입 슬롯으로 재사용, C의 별도 모델·렌더러 없음
- [x] 각 준비 작업의 검토 ZIP 생성·무결성 확인

### A01 공유 직후

- [x] A01의 main 반영과 전달 커밋·실제 계약 경로 확인
- [x] 현재 미커밋 준비 문서를 보존하고 공통 기반 반영
- [x] Android Studio Rabbit 2 2026.2.2 설치 버전 확인·맞춤
- [x] A가 기록한 JDK·SDK와 공통 wrapper·빌드 버전 맞춤
- [x] wrapper --version 및 py -3 scripts/check_all.py --android 실행·결과 기록

### 실제 구현과 검증

- [x] C01: 독립 모의 내비·주입 슬롯·DEMO 표시·숨김 구현(앱 메뉴 연결은 A 대기)
- [ ] C01: A의 AppRoot에 실제 입력·콜백 연결하고 숨김·재표시 동작 확인
- [ ] C01: 제한·background·화면 이탈 중단, 창 크기·글자 확대 확인
- [x] C02: 독립 VisitController·수락·거절·철회·닫기 구현
- [x] C02: monotonic 만료·쿨다운·활성 1개·오래된 ID 차단, FakeClock 시험 구현·실행
- [ ] C02: A의 AppRoot·초기화 연결, 기기 시점 전환·동의·숨김·이탈/복귀·실제 Handler 동작 확인
- [x] C03: Catalog·GameSnapshot 읽기 전용 월드맵·지역 상세·수집 목록 독립 구현
- [ ] C03: 지역 선택 → A의 목적지 후보, 아이템 보기 → B 인벤토리 연결 확인
- [x] C04: 공통 JourneyPort/DemoControlPort 기반 수동 요청·READY 확인·출발·완료·취소·원본 반복 독립 구현
- [x] C04: 가짜 포트로 저장 결과 await·실패/원본 재시도·전체 초기화·늦은 입력 중단 시험
- [ ] C04: A02/A04 실제 포트·일상 목적지·GameSnapshot 공급자·초기화 콜백 연결, 기기 수동 시연 확인
- [ ] C05: U07~08·S08~09, 화면 이탈·타이머 종료·초기화 반복 검증
- [ ] 연결: B 의상 → A 주입 슬롯 → C 동일 외형(I03), 월드맵 후보 이동(I06) 확인
- [ ] C 백업 PC에서 A의 release APK 설치·핵심 시연 확인

### 전달

- [ ] 변경 diff·검증 결과·NOT RUN·다음 작업 확인
- [x] C01: 선별 커밋·push·PR #3 생성 및 main 병합 확인
- [x] C02·C03: 기능별 커밋·전달 문서·기존 시험 결과 준비(최신 main 재검사·전송 결과는 전달 보고서 확인)
- [x] C04: 독립 구현·시험·연결 문서 준비(검사·push SHA는 작업 보고서 확인)
- [ ] C04: 사용자 확인 후 새 PR 생성
- [ ] 기본 리뷰 짝 B와 검토하고 PM 통합 순서에 따라 전달

A01·C01~C04 컴파일·단위 시험·debug 빌드는 PASS다. C 기능의 AppRoot 진입·실제 UI 흐름, B 외형 일치, 실제 Pleos·release는 미검증이다. 다음은 A/B 연결과 C05 기기·반복 인수다.
