# C 진행 상태

상태: IN PROGRESS — C01·C02·C03 A01 이전 준비 완료, Android 구현 대기
오늘 완료와 SHA: 2026-11-02, 기준 be98b40, 미커밋 — [C01 화면](../c01_companion.md), [C02 방문·시험 예제](../c02_visit.md), [C03 월드맵·읽기 규칙](../c03_world.md) 작성
막힌 작업과 우회: origin/main에 app·wrapper·검증된 toolchain 없음. 별도 앱 없이 스케치·상태·입력 예제로 준비
다음 작업: A01 공유 후 실제 계약·공통 빌드 환경을 받아 feature/companion 구현, 이후 feature/visit 상태 머신·시험 구현
시험: C03 준비 문서 추가 후 Windows py -3 scripts/check_all.py PASS(34개), git diff --check PASS. Android·렌더링·방문 상태 머신·U07~08·I01·I03·I06·S03~04·S06·S08~09 NOT RUN
전달: 사용자 요청 전 push·원격 PR 생성하지 않음

## 파트 C 체크리스트

체크는 해당 항목만 완료했다는 뜻이다. 준비 문서 완료와 실제 기능 구현·시험 통과를 구분한다. A01은 회의에서 착수가 확인됐으며, 현재 로컬에는 app·wrapper가 아직 없다.

### 지금까지 완료

- [x] 작업 폴더와 파트 C 브랜치 확인: part/c-c01-companion
- [x] PM 공통 지침·역할·계약·Catalog·MVP 범위 검토
- [x] GitHub main과 로컬 기준 커밋 일치 확인: 마지막 원격 확인 be98b40
- [x] 설치 환경 진단과 Git 로컬 설정 확인
- [x] 공통 정적 검사와 기존 Python 테스트 34개 PASS 확인
- [x] C01 화면 스케치·고정 슬롯·숨김·제한 시 중단 기준 준비
- [x] C02 방문 상태·동의·만료·쿨다운과 시험 예제 18개 준비
- [x] C03 월드맵·지역 상세·수집 표시와 검증 예제 10개 준비
- [x] 각 준비 작업의 검토 ZIP 생성·무결성 확인

### A01 공유 직후

- [ ] A01의 main 반영과 전달 커밋·실제 계약 경로 확인
- [ ] 현재 미커밋 준비 문서를 보존하고 공통 기반 반영
- [ ] Android Studio Rabbit 2 2026.2.2 설치 버전 확인·맞춤
- [ ] A가 기록한 JDK·SDK와 공통 wrapper·빌드 버전 맞춤
- [ ] wrapper --version 및 py -3 scripts/check_all.py --android 실행·결과 기록

### 실제 구현과 검증

- [ ] C01: 모의 내비·회색 임시 슬롯·DEMO 표시·숨김 구현
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

현재 Android·Kotlin·VisitController·APK 검증은 NOT RUN이다. 다음 우선순위는 A01 수령과 공통 빌드 확인 → C01 실제 구현이다.
