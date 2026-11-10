# A06 중간 서명 APK Pleos 설치·진행 복원

2026-11-05 KST / Part A / Windows AMD64 / JBR 25.0.4 / Build Tools 36.0.0.
문서 작업 시작 HEAD `8d752b8`, 브랜치 `part/a-a01-bootstrap`.
**A05 중간 후보의 X03/S02/S10 실행 기록이며 A06 전체·최종 제출 빌드 인수가 아니다.**

## 대상과 분리

- 공식 `Pleos_Connect_v2_0` 프로필, 설치된 Pleos Connect 2.1.0 / Android14 API34 x86_64 이미지로
  새 AVD `MEMORIDE_A06_SIGNED_PLEOS`를 생성했다. 시험 serial은 `emulator-5562`, 사용자10이다.
- 기존 `MEMORIDE_Pleos_2_1_API34` / `emulator-5554`의 debug 앱·데이터·기어·제한은 변경하지 않았다.
- 새 AVD의 초기 주차 P를 화면으로 확인했고, Car 서비스의 display0 UXR는 DO=false / flags0이었다.
  주차·제한 주입, DO 선언, overlay, ADB 제한 해제 없이 정식 Main의 허용 경로를 사용했다.
- 먼저 PLEOS 입력 대기 화면을 확인한 다음 공개 DEMO 모드로 전환했다.
  도착은 **명시된 DEMO 입력**이며 실제 내비 최종도착이나 실제 방문 인증이 아니다.
- 개발 Activity·debug 서명·저장 파일 주입 없이 정식 release의 공개 UI와 B 서비스로 기록을 만들었다.

## 검증 APK

두 APK는 기존 A05 후보를 그대로 사용했고 새로 서명하지 않았다.
개인키/비밀번호를 읽지 않고 `verify_release.py`로 공개 인증서·서명·해시·release 경계를 다시 확인했다.

| 항목 | 값 |
|---|---|
| 실제 빌드 소스(담당자 선언) | `ee3ed0feda9e094211be2b275f1d68528b05d9c8` |
| 공통 인증서 SHA256 | `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb` |
| v2 APK SHA256 | `c6da26b671e5b2ee113fa31fdd73dd69e6ca52f22eb1b0718dd30563622e6165` |
| v3 APK SHA256 | `31267cc1d5fd58f78c41b7316d19c6feb40d3222cd2cb0f82492b3a24aead88e` |
| 버전 / 패키지 | versionCode2→3 / versionName0.1.0 / io.kailand |
| 설치 환경 ABI | x86_64; APK 검증에는 arm64-v8a/armeabi-v7a/x86/x86_64 포함 |

소스 SHA는 APK에서 역으로 증명한 값이 아니다. 최신 main으로 빌드한 APK로 표기하지 않는다.
APK 소스와 작업 시작 HEAD의 앱 구현 차이는 PR #19의 브리핑 거리 안내 조건과 debug 샘플이다.
이번 영수증은 거리 값이 있어 같은 안내가 표시되지만, 이것으로 최신 main APK 전체를 검증했다고 주장하지 않는다.

## 실제 수행 결과

| 순서 | 동작과 관측 | 판정 |
|---|---|---|
| 1 | 새 AVD 패키지 없음 확인 → v2 최초 설치 → 정식 Main·섬·캐릭터 자산 표시 | PASS |
| 2 | 모모 선택 → DEMO 대전 REGION 요청·도착 → 빵/빵모자/작은 빵 간판 브리핑 → 확인 | PASS |
| 3 | 공개 인벤토리에서 빵모자 장착 → DAILY 도착 → 성장 0→1, 미확인 브리핑 생성 | PASS |
| 4 | v2 `am force-stop` → 정식 Main cold start → 미확인 브리핑의 표시 문자열 전체 순서 비교 | 일치 PASS |
| 5 | 같은 인증서 v3 `install -r` → 앱 정상 종료·정식 Main cold start → 같은 브리핑 표시 비교 | 일치 PASS |
| 6 | v3 아이템 보기로 확인 완료 → 음식/의상/장식 보유 및 빵모자 장착 확인 | PASS |
| 7 | v3 재시작 → 브리핑 재개 없음, 모모·빵모자·성장1 유지 | PASS |
| 8 | 추가 장식 배치 후 재시작 사례 | BLOCKED: 첫 화면 조회의 ADB shell timeout 45초가 두 차례 반복되어 배치 입력 전 종료 |

패키지 appId는 10175로 유지됐고, 사용자별 firstInstallTime도 전후 같았다.
시간은 게스트 OS 표시값이므로 KST 작업 시각과 혼동하지 않는다.
데이터 유지 판정은 설치 정보만이 아니라 위 실제 진행 UI 복원에 근거한다.

관찰 범위는 공개 UI의 캐릭터·장착·보유·성장·미확인/확인 완료 브리핑이다.
비공개 저장 파일의 모든 필드/바이트·모든 과거 원본 ID를 기기에서 비교한 시험은 아니다.
이전 [파일 기반 통합시험](A06_INTEGRATION_VERIFICATION.md)의 전체 저장값/원본 비교와 구분한다.

## 명령·오류·재시험

- `avdmanager create avd --name MEMORIDE_A06_SIGNED_PLEOS --package "system-images;android-34;pleos_car_emulator;x86_64" --device Pleos_Connect_v2_0`: exit0.
- 새 AVD만 hidden/headless, cold boot, software GPU로 실행했다. 처음 RAM4096MB,
  호스트 여유 RAM 약252MB와 자동화 종료137을 확인한 뒤 새 AVD만 종료하고 RAM2048MB로 다시 실행했다.
- `verify_release.py <v2 또는 v3 APK> --build-tools <36.0.0> --expected-cert-sha256 <위 지문> --source-sha <위 소스>`: 각각 exit0.
- `adb -s emulator-5562 install <v2>`, `install -r <v3>`: Success.
- `am force-stop io.kailand`, `am start -W -n io.kailand/io.kailand.app.MainActivity`: 데이터 삭제 없는 실제 프로세스 종료/재실행.
- `uiautomator dump` / 공개 버튼 input / screenshot / `dumpsys package`로 근거를 수집했다.
  재시작 직후 SystemUI/차량 패널이 전경인 덤프, null root, idle 실패는 통과로 집계하지 않았다.
  정상 Main 재진입 후 새 XML의 대상 패키지와 기대 내용을 확인해 재시험했다.
- 초기 도구가 dump 실패 뒤 이전 XML을 읽을 수 있는 문제를 발견했다. 매 dump 전 자기 임시 XML만 삭제하고
  성공 메시지와 `io.kailand` 노드를 확인하도록 수정했다. 해당 이전 비교는 무효 처리하고 새 근거로 재실행했다.
- 새 이미지에는 `su`가 없어 읽기 권한 확인 시도는 실패했다. root 전환이나 원본 저장 파일 접근으로 우회하지 않았다.
  기록 변경은 공개 UI만 사용했다.
- 핵심 인수 뒤 장식의 비어 있지 않은 슬롯 재시작 사례를 추가 시도했으나, 첫 화면 조회부터 ADB shell timeout이 두 차례 발생했다.
  원인은 확정하지 않았으며 앱 배치/복원 실패로 단정하지 않는다. 이 사례는 BLOCKED로 남겼다.
  ADB `emu kill`도 응답하지 않아, 명령줄로 확인한 새 A06 launcher와 그 headless QEMU 자식만 강제 종료했다.
  강제 종료 뒤 새 AVD 재부팅/파일 무결성은 이번에 재검사하지 않았다.
  시험 데이터는 새 AVD에 보존하고, 사용자 홈이나 기존 AVD를 지우지 않았다.
- cold start 기록에는 약2.6~8.5초가 관찰됐다. 자원 압박이 있는 환경의 기능 인수이며 초기 화면5초 목표 달성·성능 인수로 집계하지 않는다.
- 앱 구현 변경이 없는 기기 인수 작업이므로 Python59/JUnit235/debug 검사는 이전 A06 실행 결과를 참조하며 이번에 재실행하지 않았다.
  이번 문서의 package/link/diff 정적 검사와 실제 APK 설치 인수는 구분한다.

## 남은 범위

X03의 중간 후보 Pleos 설치·ABI·서명·기본 DEMO 저장/표시, S02/S10의 위 관찰 사례는 PASS다.
실물 ARM/실차·주행 안전, 실제 PLEOS 최종도착/장애, 신규 USER_CONFIRMED 저장,
기존 사용자 전체 저장의 호환 행렬, 모든 과거 원본 영수증/손상 저장·장식의 업데이트 행렬,
최종 UI·최신 main의 최종 서명 빌드/A06 전체 인수는 NOT RUN이다.
최종 UI와 남은 B/C 연결 이후 최종 SHA 후보를 별도로 인수한다. 새 PR은 만들지 않는다.
