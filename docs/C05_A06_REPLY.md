# C05 → A06 전달 회신

2026-11-06 KST / Windows11 Home AMD64 / Python3.14.7 / JBR25.0.4 / wrapper9.3.1 / SDK API37.

## 브랜치와 전달 범위

기존 `part/c-c01-companion` 유지. 시작 C HEAD `184727a`, 반영 main `a04b3c5`, 통합/실제 검사 코드 `854bfe6`. 최종 문서 커밋·PR SHA는 Git/전달 REPORT.md에 기록한다. 이전 C03 수정은 `3015d8f`, 이전 검증 문서 보존은 `6a9dd02`로 분리했다.

요청된 미병합9파일 전부를 전달한다: CompanionScreen 자연색 모의 지도/패널, VisitScreen 편지 그림, WorldScreen 지역 카드, VisitControllerTest 신규3개, src/debug VisitAcceptancePreview, c01/c04/c05/status 문서. 자연색 그림은 프로토타입이며 최종 디자인 확정이 아니다. 추가 범위는 C03 공백 ID 검증/회귀 시험과 기존 C 서명 인수 기록이다. AppRoot·공통 DTO·B 저장·PM 화면·빌드 파일은 직접 수정하지 않았다. 개인 환경 문서 수정은 보존/전달 제외. Actions는 실행하지 않았다.

## 실행 결과

- `py -3 -X utf8 scripts/check_all.py --android` exit0: Python59개, Android237개, 실패/오류/skip0. debug BUILD SUCCESSFUL in55s, 15 executed / 30 UP-TO-DATE. Kotlin/단위 시험 task 실제 실행. 기존 DataStore Unsafe 경고 유지.
- VisitController27개 = main24개 + 수락/거절 경쟁·수락/reset 경쟁·부분 reset3회 신규3개. 이 부분 시험을 S09 전체 게임 초기화 UI로 세지 않는다.
- APK SHA256 `23c0d8d6dffdab014e28d78035478c41846118d98dd6ec623c6036c4efeebb0e` (debug, release 아님).
- 새 독립 `MEMORIDE_C05_UI` / emulator-5582 / 일반 Android API37 x86_64. 초기 offline/창 소실 뒤 software GPU로 부팅 성공. 기존 A05 AVD/서명 앱은 건드리지 않았다.
- 공개 DevelopmentActivity의 A04 실제 저장 화면 버튼으로 진입. 비공개 Activity 직접 시작, 저장 주입, UNKNOWN 해제, 가짜 실차 주차, 제한 덮어쓰기 없음. 실제 화면 조작/관찰은 computer-use skill 사용.

| 항목 | 판정·관찰 범위 |
|---|---|
| S08 수락/닫기 | PASS: 정식 VisitRoute 수신 시점에서 15초 내 수락→토리 표시→대화 닫기→완료/캐릭터 없음 |
| 거절 | PASS: 수신 거절→거절 문구/캐릭터 없음. 첫 시도는 시간 초과라 만료로 기록하고 재시도 성공만 거절로 판정 |
| 만료 | PASS: 요청→실제15초 경과→만료/캐릭터 없음 |
| 철회 | PASS: 요청 철회→철회 문구/캐릭터 없음 |
| 미동의/방문 보상 없음 | 요청·만료·거절·수락 당시 저장 파일 없음 유지(find는 profileInstalled만). 방문으로 게임 저장/보상이 생성되지 않음 |
| 모드 중단 | PASS: REQUESTED에서 PLEOS 칩→모드 전환 종료, DEMO 복귀 후 이전 요청/타이머 비재개. 실제 PLEOS SDK 시험 아님 |
| 화면 이탈 | 섬 이탈/재진입에서 캐릭터·타이머 비재개 관찰. 해당 요청은 만료 문구여서 SCREEN_LEFT 원인 단독 판정은 보류 |
| background/foreground | PASS: 요청 중 Android HOME intent→공개 개발 진입점으로 복귀, 일시 UNKNOWN→섬, 방문 재진입 시 화면 이탈 종료/타이머 비재개 |
| reset | 전체 저장 초기화→D03 시연 3회 완료. 활성 방문과 reset 동시 경쟁의 UI 강제 재현은 NOT RUN; 자동 시험만 PASS |
| S09 핵심 반복 | 전체 게임 초기화 확인→D03 목적지 요청→READY→출발→완료/실제 저장을3회. 여정ID는 각 회차 다름. D01~D06 전수3회/각 회차 의상·방문까지는 NOT RUN이므로 S09 전체는 부분 완료 |
| D03→D04 | 1회차 원본 반복 성공/새 지급0 문구. 전후 파일 SHA256 `31c7de5563fe8cd635e73b425fb92cfb88d1584725aa0fb7524c5dbf8e77edaa` 동일 |
| 브리핑 | 3회차 저장 브리핑에 대전/성장0/기념품3종 표시, 닫기 후 섬 이동 |
| 큰 글자/창 | 1280x800·1280x1200 기본 표시, 800x1000/font1.3에서 시연 D06·브리핑 닫기·방문 요청에 도달(스크롤 포함). 설정 후 Runner 자동 재개 없음. 전 화면·극소 창·회전·2배 글자·OS 다중창은 NOT RUN |
| I03 | A04_6의 모모+빵모자 실제 저장 근거 재사용. 이번 토리 방문 슬롯 표시는 관찰했으나 변경 의상 전체 I03 재시험은 NOT RUN |
| A05 | A의 A05_C_ACCEPTANCE_REVIEW에 수용된 C v2/v3 서명 인수 재사용. 재설치/재서명하지 않음 |

3회차 여정ID: `04fa0629-16b0-482d-bc24-8d3364839729`, `99a7c31e-c8e1-43dc-8371-1cd6b273a180`, `29543990-ccaa-4040-91d9-76fb971c0b8c`.
방문 재준비는 공개 진행자 버튼이며 일부 독립 사례 사이 사용했다. 이 조작으로 쿨다운 시험을 PASS 처리하지 않는다. 늦은 token 재전달/경쟁은 기존 자동 시험으로만 판정한다.

## 남은 범위와 다음 행동

C05 전체는 IN PROGRESS. S09 전체 시나리오/옛 입력 기기 재전달/활성 방문 reset UI 경쟁/전 화면 접근성 전수/최종 release·Pleos·실차는 NOT RUN이다. 일반 AVD 결과를 실제 주차/SDK/안전 인증으로 확대하지 않는다. A05/I03의 예전 대기 문구를 현재 C 상태에서 해소했다. 앱 연결 결함은 이번 관찰에서 확정하지 않았으므로 A 코드 변경 요청을 만들지 않는다. 다음은 본 PR 리뷰와 남은 A06 시나리오 인수다.

스크린샷·최소 검사 로그·patch·최종 commit/PR과 위 판정은 검토 ZIP으로 전달한다. 11/5~11/6 실제 실행 결과이며 중간 Esc/정지 구간에는 시험하지 않았다. 사용자 전용 데이터·키·캐시·APK는 ZIP에 포함하지 않는다.
