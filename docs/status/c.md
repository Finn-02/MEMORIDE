# C 진행 상태

상태: IN PROGRESS — C01 독립 화면 1차 구현·Preview 렌더링·단위 시험 완료, AppRoot 연결·기기 UI 시험 대기
오늘 완료와 SHA: 2026-11-03, 준비 문서 보존 d6f5a93 → main 병합 29796f6(A01 a4a0010 포함) 이후 C01 코드·시험·Preview 구현. [C01 화면·연결 예제](../c01_companion.md), [C02 방문·시험 예제](../c02_visit.md), [C03 월드맵·읽기 규칙](../c03_world.md) 보존
막힌 작업과 우회: 기본 빌드 막힘 없음. 개발 DEMO 설치·Activity 시작 성공, Computer Use 앱 접근 승인 시간 초과로 화면 관찰 NOT RUN. C01 착수와 분리
다음 작업: A에게 C01 공개 함수·연결 예제 전달 후 AppRoot 연결 확인, C02 로컬 상태 머신·FakeClock 시험 구현
시험: C01 추가 후 py -3 scripts/check_all.py --android exit 0(Python 34개·Android 12개 PASS). 최종 표시 조정 후 Android 12개·debug 빌드 재실행 PASS(8개 task 실행/37 UP-TO-DATE). C01 Preview 4개 렌더링 관찰. 실제 UI 상호작용·방문 상태 머신·U07~08·I01·I03·I06·S03~04·S06·S08~09·Pleos·release NOT RUN
전달: 2026-11-03 사용자가 C01 commit·push·PR 절차 진행을 지시함. 실제 전송 결과는 PR과 검토 보고서에 기록. main 병합·C02 코드 전송은 이번 범위에 포함하지 않음

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
- [x] C03 월드맵·지역 상세·수집 표시와 검증 예제 10개 준비
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
- [ ] C02: VisitController·수락·거절·철회·닫기 구현
- [ ] C02: monotonic 만료·쿨다운·활성 1개·오래된 ID 차단, FakeClock 시험 구현·실행
- [ ] C03: Catalog·GameSnapshot 읽기 전용 월드맵·지역 상세·수집 목록 구현
- [ ] C03: 지역 선택 → A의 목적지 후보, 아이템 보기 → B 인벤토리 연결 확인
- [ ] C04: A02의 포트로 수동 출발·완료·취소·같은 도착 반복 구현
- [ ] C04: 저장 결과 await·실패 처리·전체 초기화 요청·늦은 입력 중단 확인
- [ ] C05: U07~08·S08~09, 화면 이탈·타이머 종료·초기화 반복 검증
- [ ] 연결: B 의상 → A 주입 슬롯 → C 동일 외형(I03), 월드맵 후보 이동(I06) 확인
- [ ] C 백업 PC에서 A의 release APK 설치·핵심 시연 확인

### 전달

- [ ] 변경 diff·검증 결과·NOT RUN·다음 작업 확인
- [ ] 사용자의 전달 지시 후 필요한 변경만 커밋·push·작은 PR 준비
- [ ] 기본 리뷰 짝 B와 검토하고 PM 통합 순서에 따라 전달

A01·C01 컴파일·단위 시험·debug APK 빌드는 PASS다. C01은 독립 화면을 구현했고 실제 앱 진입·UI 흐름은 미검증이다. VisitController·실제 Pleos·release 검증은 NOT RUN이다. 다음은 C01 연결과 C02 상태 머신 구현이다.
