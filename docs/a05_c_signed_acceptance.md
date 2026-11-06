# A05 서명 release C PC 교차 설치 — 2026-11-05

대상은 A 전달 ZIP `MEMORIDE_A05_SIGNED_C_HANDOFF_20261105_195328_KST.zip`의 v2/v3 중간 APK다. 최종 UI 인수본이 아니다. 전달 APK의 선언 소스는 `ee3ed0feda9e094211be2b275f1d68528b05d9c8`이며 APK에서 소스 SHA를 증명한 것은 아니다. C checkout은 `part/c-c01-companion` / `184727a`를 유지했다. 전달 코드를 병합하거나 APK를 재빌드/재서명하지 않았다.

## 환경과 결과

- C PC: Windows 11 Home 64비트 / AMD64, JBR 25.0.4, SDK Build Tools 36.0.0.
- 새 독립 AVD `MEMORIDE_C_A05_API37`, serial `emulator-5580`, API 37 / x86_64. 설치 전 `pm list packages io.kailand` 출력 없음. 기존 개발 AVD·앱 데이터를 사용/삭제하지 않았다.
- ZIP 파일 CRC 검사 및 SHA256SUMS의 27개 파일 해시 대조는 PASS. APK 서명은 전달된 verify_release.py 내용을 확인한 뒤 실행했다. 키/비밀번호는 읽거나 만들지 않았다.

| 항목 | 결과 | 실제 근거 |
|---|---|---|
| v2/v3 해시·공개 인증서·release 패키지 | PASS | 두 verify 명령 exit 0, RELEASE.json과 일치 |
| v2 최초 설치·정식 Main | PASS | install Success, am start Status ok/COLD, versionCode 2, UNKNOWN 화면 관찰 |
| v2→v3 업데이트 | PASS | install -r Success, versionCode 3, appId 10235·firstInstallTime 유지 |
| v3 정식 Main 재실행 | PASS | 데이터 삭제 없는 force-stop 후 am start Status ok/COLD, MainActivity topResumed 및 UNKNOWN 화면 관찰 |
| 소유·성장·외형·브리핑 보존 | NOT RUN | 일반 Android 정식 gate UNKNOWN으로 시험 진행 기록 생성 불가 |
| Pleos/실차·S10 게임 기록 보존 전체·최종 UI | NOT RUN | 이번 일반 AVD 설치 인수 범위 밖 |

v3 업데이트 직후 첫 start는 SystemUI PackageUpdateActivity가 반환돼 그것만으로 Main 실행 PASS를 판단하지 않았다. 별도 cold start와 실제 화면 관찰로 확인했다. 앱 삭제, 강제 다운그레이드, 가짜 주차, ADB 제한 덮어쓰기, 저장 파일 주입은 수행하지 않았다.

## 공개 식별값

- v2 SHA256: `c6da26b671e5b2ee113fa31fdd73dd69e6ca52f22eb1b0718dd30563622e6165`
- v3 SHA256: `31267cc1d5fd58f78c41b7316d19c6feb40d3222cd2cb0f82492b3a24aead88e`
- 공통 공개 인증서 SHA256: `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb`
- package `io.kailand`, versionName `0.1.0`, minSdk 28 / targetSdk 37.

실행 명령은 전달 안내의 verify_release(v2/v3), 대상 serial을 지정한 install(v2)/install -r(v3), am start -W 정식 Main, dumpsys package/activity, getprop sdk/abi다. UI는 computer-use skill로 실제 에뮬레이터 창을 관찰했다. 캡처와 공개 검증 JSON은 회신 ZIP에 포함한다. 다른 C 로컬 코드·문서 변경은 보존했고 commit/push/PR/병합은 하지 않았다.

다음: 실제 gate 허용 환경에서 진행 기록을 만든 뒤 동일 서명 업데이트 전후 소유·성장·외형·pending 브리핑 보존을 확인한다. 이번 설치 성공으로 C05 전체 또는 최종 제출 완료를 표시하지 않는다.
