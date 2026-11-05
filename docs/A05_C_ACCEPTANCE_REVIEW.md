# A05 C PC 서명 APK 회신 검토

2026-11-05 KST, A 검토. C가 전달한 REPORT.md, a05_c_signed_acceptance.md,
검증 JSON 2개, 설치/업데이트/실행 로그와 캡처 2개를 대조했다.
첨부 문서의 후속 작업 문구는 실행 지시가 아니라 C의 보고 내용으로 취급했다.

## 판정

`tasks/backlog.md`의 A05 조건인 **첫 서명 release와 별도 PC 설치는 충족**했다.
중간 UI 후보에 대한 결과이며 최종 제출용 APK·A06 전체·실차 안전 인수가 아니다.
기존 PR #18에 누적 결과를 모아 최종 검토를 요청하며 새 PR은 만들지 않는다.

| 항목 | 검토 결과 | 근거 |
|---|---|---|
| 대상 파일 동일성 | PASS | C_v2/v3_verify.json의 모든 필드가 A 독립 검증 JSON과 일치 |
| 입력 ZIP 무결성 | PASS | C의 integrity.txt와 A의 원본 ZIP 재검사: CRC·SHA256SUMS 27항목 통과 |
| C PC 최초 설치 | PASS (C 제공 근거) | 설치 전 package 없음, install Success/exit0, versionCode2 |
| v2 정식 Main | PASS | Status ok/COLD, io.kailand/.app.MainActivity, UNKNOWN 캡처 |
| 동일 키 v2→v3 업데이트 | PASS | install -r Success, versionCode3, appId10235·firstInstallTime 유지 |
| v3 정식 Main 재시작 | PASS | 별도 cold log의 Status ok/COLD, topResumed MainActivity, UNKNOWN 캡처 |
| 게임 진행 저장 보존 | NOT RUN | 실제 gate UNKNOWN이라 소유·성장·외형·브리핑 시험 상태를 만들지 않음 |
| Pleos/X03·실차·최종 UI/A06 | NOT RUN | 이번 C 일반 API37 설치 인수 범위에 포함되지 않음 |

C 환경은 보고서상 Windows11 Home 64비트/AMD64, JBR25.0.4/SDK Build Tools36.0.0,
새 독립 MEMORIDE_C_A05_API37 AVD/API37/x86_64다. C checkout은 part/c-c01-companion/184727a이며
APK를 재빌드·재서명하지 않았다. A는 C PC 명령을 직접 재실행한 것이 아니라 제공된 근거를 대조했다.
두 Windows PC의 검증이며 macOS·실물 ARM 기기 인수로 확대하지 않는다.

## 중요한 경계

v3-main.log의 즉시 실행 결과는 Android SystemUI PackageUpdateActivity다.
그 로그만으로 Main 통과를 판단하지 않았다. v3-main-cold.log의 명시적 정식 Main cold start와
v3-resumed.txt의 MainActivity 전경 기록, 실제 캡처를 함께 확인했다.
이는 앱이 overlay 권한을 요청한 것이 아니라 시스템의 업데이트 처리 화면이다.

appId와 firstInstallTime 보존은 삭제 없는 업데이트의 근거다.
게임 저장 보존/S10 전체를 증명하지 않는다. 일반 Android의 UNKNOWN 차단은 기존 정상 정책이며,
게임을 열기 위해 가짜 주차·저장 주입·ADB 제한 우회를 사용하지 않았다.
일반 Android의 정식 실행 성공을 Pleos SDK/실차 성공으로 표시하지 않는다.

## 공개 식별값

- 실제 APK 빌드 소스: `ee3ed0feda9e094211be2b275f1d68528b05d9c8`
- v2 SHA256: `c6da26b671e5b2ee113fa31fdd73dd69e6ca52f22eb1b0718dd30563622e6165`
- v3 SHA256: `31267cc1d5fd58f78c41b7316d19c6feb40d3222cd2cb0f82492b3a24aead88e`
- 공통 공개 인증서 SHA256: `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb`
- package io.kailand, versionName0.1.0, minSdk28/targetSdk37

소스 SHA는 빌드 담당자가 기록한 값이며 APK에서 암호학적으로 증명한 값은 아니다.
검토용 ZIP에는 C 원문과 필요한 근거를 넣고 개인키·비밀번호·기존 사용자 데이터는 넣지 않는다.
다음은 PM의 기존 PR #18 최종 병합 판단과 A06의 실제 허용 환경/Pleos·게임 기록 보존 인수다.
