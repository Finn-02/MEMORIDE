# 실제 개발 환경 기록

2026-11-02 A01에서 Windows 11 AMD64의 debug 단위 시험·APK 빌드를 확인했다. 기준 소스는 `be98b401106d8da7ca6445d750d0e9ad62f838b3` + `part/a-a01-bootstrap`의 미커밋 A01 변경이다. 실제 SDK·에뮬레이터 화면·서명 release·다른 PC의 결과와 구분한다. 자세한 시작 방법은 [A01 실행 안내](A01_QUICKSTART.md)를 따른다.

| 항목 | 팀 공통 실제 값 | 확인 방법 |
|---|---|---|
| Android Studio | AI-262.10968.63.2622.16730706 | 설치 product-info 확인, IDE Sync PASS (23:13 KST) |
| Gradle 실행 JDK | JetBrains JBR 25.0.4 | wrapper --version의 Launcher/Daemon JVM |
| AGP와 Gradle wrapper | 9.1.1 / 9.3.1 | 공통 빌드 파일·wrapper 실제 실행 |
| Kotlin과 Compose | 내장 Kotlin 2.2.10 / BOM 2026.01.00 | 버전 카탈로그·실제 컴파일. Gradle embedded Kotlin 2.2.21과 별개 |
| DataStore / serialization | 1.2.0 / 1.9.0 | 공통 정의에 고정, B의 실제 저장 구현은 아직 없음 |
| Activity / coroutines | 1.12.3 / 1.10.2 | 공통 정의·의존성 해결 |
| minSdk | 28 | app 빌드 파일 |
| compileSdk와 targetSdk | 37.0 / 37 | compileSdk release(37), minorApiLevel=0 / Build-Tools 36.0.0 |
| NaviHelper | 계획 3.0.5, 미연결 | A03에서 실제 해결·호출 검증 |
| Pleos Emulator | 계획 2.1.0 API 34, NOT RUN | AVD와 앱 설치 별도 |
| applicationId | io.kailand | A01 빌드 |
| JVM criteria 사용 여부 | false | wrapper는 이번 명령의 JAVA_HOME을 사용 |
| 주차·제한 입력의 실제 근거 | 미확인 | SDK 또는 플랫폼 API와 상태 매핑 |

## 공통 버전과 개인 설정의 관계

첫 공용 빌드 전달은 [A01 기준](BOOTSTRAP_HANDOFF.md), 팀원 설치는 [온보딩](ONBOARDING.md)을 따른다. 빌드 파일·wrapper·버전 카탈로그가 실제 dependency 버전을 결정하며 toolchain.json은 그 기록이다. 개인 경로·캐시·AVD·debug 키는 공유하지 않는다. 새로운 의존성은 A와 호환성을 확인하고 같은 정의에 고정한다. Python은 3.10 이상 표준 라이브러리만 필요하며 pip·venv·Node는 공통 필수가 아니다.

## PC별 확인

| 담당 | OS와 CPU | 정적 검사 | debug 빌드 | Pleos 설치·SDK | release 설치 |
|---|---|---|---|---|---|
| A 기준 PC | Windows 11 AMD64 | PASS, Python 34개 시험 포함 | PASS, 단위 시험 4개 PASS | NOT RUN | NOT RUN |
| B | Windows CPU 미확인 | NOT RUN | NOT RUN | 필요 시 | 필요 시 |
| C 백업 PC | Windows CPU 미확인 | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| PM | macOS CPU 미확인 | NOT RUN | NOT RUN | 가능하면 검증 | 필요 시 |

## 로컬 명령

아래 wrapper·`--android` 명령은 **A01 공통 프로젝트를 받은 뒤**, 각 PC에서 JDK 25와 SDK를 맞추고 사용한다. 이 Windows PC는 `py -3` 대신 설치된 `python` 3.10.4를 사용했다.

macOS:
```bash
python3 scripts/doctor.py
./gradlew --version
python3 scripts/check_all.py --android
```

Windows PowerShell:
```powershell
py -3 scripts/doctor.py
.\gradlew.bat --version
py -3 scripts/check_all.py --android
```

doctor는 진단만 하며 설치·설정 변경·요금제 변경을 하지 않는다. Java major를 간단히 확인하지만 IDE의 실제 실행 JVM과 모든 AGP 호환성을 자동 검증하지는 않는다. wrapper의 Daemon JVM 출력이 다르면 기록부터 맞춘다. 개별 결과는 templates/environment_report.md를 사용한다.

## A01 실제 명령과 범위

- 현재 터미널 프로세스의 JAVA_HOME을 Android Studio 내장 JBR로 지정하고 `gradlew.bat --version`: Gradle 9.3.1 / JBR 25.0.4, exit 0. 전역 환경 변수 변경 없음.
- `gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug`: BUILD SUCCESSFUL, 4개 단위 시험. 최초 자산 경로 설정 실패 두 건을 수정한 뒤 성공.
- Gradle SDK XML 버전 경고와 native library 두 개의 strip 생략 경고가 있었다. APK 빌드는 성공했지만 실제 기기 ABI·실행은 아직 미검증이다.
- `python scripts/validate_content.py`, `validate_ui_samples.py`, `check_package.py`: PASS. 로컬 properties 줄바꿈을 LF로 맞춘 뒤 패키지 검사도 통과.
- `python scripts/check_all.py --android`: exit 0. Python 34개 시험·doctor의 JDK25 확인·Android 단위 시험·debug 빌드 통합 경로 PASS. 직전 실제 실행한 Android 4개 시험의 재호출은 UP-TO-DATE.
- debug APK의 원본 Catalog·fixtures 3개 및 PNG 15개를 ZIP에서 꺼내 원본 바이트와 대조: 18개 일치. applicationId `io.kailand`, minSdk 28, targetSdk 37 확인.
- UI 실행·실제 gate·SDK·release·macOS 시험은 NOT RUN. 실제 주차 확인을 대체하는 개발 fake를 넣지 않았다.

## Android Studio 연결 확인 (2026-11-02 23:13 KST)

루트 settings.gradle.kts를 File > Open으로 현재 창에서 다시 열어 Android 프로젝트로 인식시켰다. 프로젝트 Gradle JDK는 내장 JBR 25로 선택했다. IDE 로그의 onSuccess(RESOLVE_PROJECT:1), onImportFinished 및 Gradle sync finished in 46 s 61 ms를 확인했고 Android 뷰의 app과 상단 app 실행 구성이 표시됐다. 개인 IDE 설정과 JBR 경로는 Git에 포함하지 않는다. 에뮬레이터 설치·앱 UI 실행은 아직 NOT RUN이다.

## 일반 에뮬레이터 첫 화면 확인

2026-11-02 Windows Android Studio의 Medium Phone API 37.0에서 실행된 앱 화면을 직접 관찰했다. MEMORIDE 제목, DEMO · A01 개발 샘플 · 게임 저장/보상 없음, 임시 tori 외형과 다섯 이동 버튼 표시 PASS. 첫 화면 표시만 확인했으며 화면 이동·재실행·저장·실제 차량 제한·Pleos·release 검증은 하지 않았다.

추가 확인: 홈에서 여정 준비로 이동한 화면을 직접 관찰했다. 사용자가 메인 섬 버튼으로 처음 화면에 복귀했다고 확인했다. 시스템 뒤로 가기 버튼·제스처는 별도로 검증하지 않았다.
