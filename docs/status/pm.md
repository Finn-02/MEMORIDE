# PM 진행 상태

## 2026-11-10 KST · UX-P01/P02

- S05 `RegionBriefingScreen`/`RegionBriefingSlot`, S06 `ArrivalSaveScreen`/`ArrivalSaveScreenSlot` 순수 화면과 fixture·JUnit을 추가했다. AppRoot 연결·ack·retry·닫기 뒤 복구는 A, 영수증·저장은 B 범위다.
- 결과: Debug 빌드, JUnit 283개 실패 0, `python3 scripts/check_all.py` PASS. 일반 Android 에뮬레이터(1280×720·720×1280·411dp, 글자 1.0/2.0)에서 임시 debug harness로 렌더링·ack 대기/실패/재시도·S06 연타 가드를 확인했다. harness는 커밋하지 않는다.
- NOT RUN: Android Studio Preview 렌더링, Pleos 확장/분할·제한 전환, 연결 앱의 네 시연, 실차·서명 release. UX-P03 문구·출처·시연표는 후속.

## 이전 기록 (2026-11-05)

상태: P01·P02 main 반영 완료. **P03의 Mac X02·DEMO 실제 저장→브리핑 인수 범위는 PASS**다. 팀원 교차 인수·Pleos 실제 제한 전환·서명 release 설치는 별도 미실행 항목으로 남긴다.

## 2026-11-05 KST 실제 검증 기준

- 기존 `part/pm-p01-briefing`의 시작 HEAD `cd4bfd9ed55a54a6549a3ef257a1a759f8c0c3e4`, 작업 트리 clean을 확인했다.
- `git fetch origin` 후 `origin/main`을 정상 fast-forward merge했다. 앱 검증 SHA는 `b1ce371449bc427d27904778f99b53df71398e7b`이며 요청된 A04 포함 ancestry 검사 exit 0이다.
- A04 실제 저장·브리핑 연결과 B04 갤러리가 반영된 상태를 사용했다. 이전 문서의 A04/B02 대기 설명은 현재 상태가 아니다.
- macOS 27.2 (26B123), arm64, Python 3.13.12. Android Studio `AI-262.10968.63.2622.16730706`, 기존 IDE Gradle JDK 설정과 같은 JBR 25.0.4, 저장소 wrapper Gradle 9.3.1, SDK 37을 확인했다. Kotlin 2.2.21은 wrapper 내장 도구 버전이며 앱 Kotlin 기준을 바꾸지 않았다.
- 일반 Android `Medium_Phone_API_37.0` / arm64-v8a / Google APIs Play Store 이미지의 별도 테스트 사용자에서 debug APK를 실행했다. `MEMORIDE 개발 DEMO → A04 실제 저장 화면 확인`과 `개발 화면 · 실제 로컬 게임 기록 사용` 문구를 확인했다. 저장 파일은 읽기만 대조했고 변경·초기화는 공개 UI와 B 서비스로만 실행했다.

## 실제 결과

| 항목 | 결과 | 관측 |
|---|---|---|
| `./gradlew --version` | PASS | Gradle 9.3.1, Launcher/Daemon JDK 25.0.4 |
| `python3 scripts/check_all.py --android` | PASS | exit 0. 정적 콘텐츠·샘플·패키지 검사, Python 44개, Android JUnit 230개 실패/오류/skip 0, debug APK 생성 |
| U13 검증기 회귀 | PASS | P02의 기존 Python 회귀 시험을 위 44개 실행에서 재검증 |
| D01 DAILY 실제 저장·브리핑 | PASS | 초기화 확인 후 요청→READY→출발→완료. `DEMO · 모의 여정`, 성장 `0 → 1단계`; 발견·아이템 없음. 확인 뒤 pending만 제거 |
| REGION·I06 정상 이동 | PASS | 깨끗한 상태의 섬→여정 준비, 월드맵 원주 선택→후보 원주 전달→REGION 완료. 원주 첫 발견·선물 3개가 영수증/화면과 일치. 시스템 Back으로 확인 저장 후 섬 복귀 |
| 새 여정 재방문·아이템 이동 | PASS | 서로 다른 여정 ID, 새 발견/선물 없음. 기존 원주 아이템 3개 보존. 아이템 보기 확인 저장 성공 뒤 섬 인벤토리 표시 |
| U12 D06 null | PASS | 원주 첫 발견·선물 3개와 DEMO 유지, 저장된 거리·시간 null, 이동 기록 행 생략. 실제 0은 기존 JUnit에서 `0m`·`0초`와 구분; 0 입력의 별도 기기 UI는 미실행 |
| 미확인 브리핑 복원 | PASS | D06 확인 전 프로세스 종료→개발 진입점→실제 저장 화면 재진입. 같은 원본 영수증·아이템 3개 복원, 저장 JSON 동일. 확인 후 pending만 null, 재시작/재진입 때 재노출 없음 |
| I06 저장·닫기 실패/재시도 모델 | PASS | `ArrivalBriefingIntegrationTest` 5개(ack 실패·재시도 포함), `ReceiptCloseCoordinatorTest` 4개, `AdmittedGameWriteTest` 6개 실제 JUnit 실행 통과 |
| I06 실제 저장 실패 주입 UI | NOT RUN | 지원되는 기기 실패 주입 경로 없음. 모델 시험 통과와 구분 |
| 일반 Android 정식 진입점 UNKNOWN | PASS | 이용 상태 미확인/실제 플랫폼 입력 미연결 표시. 브리핑·게임 조작 미노출; 제한 입력을 ADB로 바꾸지 않음 |
| 팀원 교차 인수 | NOT RUN | 이번 Mac 단독 실행. 상대/상대 SHA/결과를 임의 기록하지 않음 |
| Pleos 실제 제한 전환·최종 도착, USER_CONFIRMED 저장 | NOT RUN | 이번 DEMO 인수와 분리된 A/B 후속 범위 |
| 서명 release·실차·다른 기기 설치 | NOT RUN | debug 일반 Android 결과로 대체하지 않음 |

## 변경·보존·남은 작업

- PM 표시·문구·실패 재시도 UI에서 수정할 제품 결함은 재현되지 않았다. PM/A/B/C 소스·계약·보상·저장·라우팅을 변경하지 않고 이 상태 문서만 갱신했다.
- 최초 Gradle 실행은 sandbox의 로컬 잠금 소켓 제한으로 exit 1이었다. 같은 JDK/명령을 허용된 실행 환경에서 재실행해 exit 0을 확인했다. UI 하네스의 잠금 화면·이전 XML·런처/복원 Activity 가정은 보강한 뒤 실제 저장 화면 재진입으로 다시 확인했다. 초기 중단을 제품 시험 PASS로 계산하지 않았다.
- 초기화는 별도 테스트 사용자에게만 적용했다. 시험 후 테스트 사용자를 제거하고 원래 사용자 0으로 복귀했다. 원래 사용자의 게임 저장 파일은 시험 전후 모두 미생성이었다. 개인 SDK/JDK 경로와 검토 ZIP은 Git에 포함하지 않는다.
- 다음 작업은 별도 팀원 교차 인수 및 A의 Pleos·USER_CONFIRMED·서명 release 후속이다. A04 연결 대기나 PM의 저장/AppRoot 재구현은 필요하지 않다.
- 이번 인수 지점은 기존 PM 브랜치의 로컬 문서 커밋과 Mac 검토 ZIP이다. 원격 push/PR은 수행하지 않았다.
