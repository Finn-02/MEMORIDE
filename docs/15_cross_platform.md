# 15 macOS와 Windows 공통 개발 환경

처음 설치·clone·검사는 [온보딩](ONBOARDING.md), 실제 매일 명령은 [Git 안내](GIT_DAILY.md)를 먼저 따른다. 이 문서는 OS별 세부 차이의 기준이다.

## 15 1 같은 코드와 다른 로컬 경로

PM은 macOS, A·B·C는 Windows를 사용한다. 공통 언어는 Kotlin·Compose이고 Python 도구는 3.10 이상 표준 라이브러리만 쓴다. WSL·Docker·유료 클라우드 PC를 필수로 두지 않는다. A의 Windows를 기준 Pleos 실행 PC, C의 Windows를 백업으로 지정한다. 이는 실행 책임 분담이며 PM의 Mac 개발을 제외한다는 뜻이 아니다.

같게 유지할 것은 Git 커밋·공통 계약·Gradle wrapper·AGP·Kotlin·Compose·JDK major·min/compile/target SDK·콘텐츠 버전이다. 다르게 두어야 할 것은 SDK 설치 경로·JDK 설치 경로·AVD 파일·캐시·debug 서명키다. 경로를 맞추려고 개인 파일을 저장소에 올리지 않는다. [S26, S27]

| 항목 | macOS PM | Windows A B C |
|---|---|---|
| 권장 작업 폴더 | `~/src/MEMORIDE` | `C:\src\MEMORIDE` |
| 기본 터미널 | zsh 또는 IDE Terminal | PowerShell 또는 IDE Terminal |
| Python 명령 | `python3` | `py -3`, 없으면 설치된 `python` |
| Gradle 명령 | `./gradlew` | `.\gradlew.bat` |
| 공통 검사 | `python3 scripts/check_all.py` | `py -3 scripts/check_all.py` |
| 앱 포함 검사 | 위 명령 끝에 `--android` | 위 명령 끝에 `--android` |
| JDK | A가 기록한 major와 일치 | 같은 major와 Gradle 실행 JDK |
| SDK 경로 | 각자 `local.properties` | 각자 `local.properties` |

짧은 영문 로컬 경로를 권장해 Windows 경로 길이와 도구별 공백 해석 문제를 줄인다. OneDrive·iCloud 동기화 폴더의 같은 작업 트리를 공유하지 않는다. 개인정보가 있는 절대 경로는 공통 예제로 고정하지 않는다. Python 검사에 서드파티 패키지 설치는 필요 없다. Python 실행 명령이 없으면 공식 Python 3을 설치한 뒤 터미널을 다시 연다.

## 15 2 에뮬레이터와 CPU 확인

Pleos 공식 설치 문서는 다음 두 feed를 구분한다. SDK Manager의 SDK Update Sites에 OS에 맞는 주소를 넣고 API 34의 Pleos Connect system image와 Automotive AVD를 선택한다. 문서가 안내하는 emulator 버전은 2.1.0이다. Mac 주소의 arm64는 CPU 확인이 필요한 이유다. Intel Mac에서 그대로 성공한다고 가정하지 않는다. [S09]

- Mac: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
- Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`

macOS는 `uname -m`, Windows PowerShell은 `$env:PROCESSOR_ARCHITECTURE`로 CPU 계열을 확인할 수 있다. Windows 가상화와 Android Emulator 요구사항을 확인한다. 공식 Android Studio 설치 안내는 Windows ARM을 현재 지원 대상으로 안내하지 않는다. 해당 장비라면 D1에 A/C의 지원 PC로 기준 시연을 옮기고 PM에게 장비 제약을 알린다. BIOS 변경은 장비 담당자가 공식 안내를 확인하고 수행한다. [S26]

모든 UI 개발자가 Pleos 전체 환경을 먼저 완성할 필요는 없다. Compose Preview·계약 단위 시험·표시된 Generic Android fake로 화면을 개발하고 실제 SDK·제한 검증은 기준 Pleos에서 수행한다. 일반 에뮬레이터에서 fake gate를 허용하는 빌드는 개발 전용으로 분리하며, Pleos 실행에서는 실제 gate를 기본으로 사용한다. fake gate를 real gate의 대체 입력으로 자동 전환하지 않는다.

## 15 3 JDK와 Gradle을 한 번 확정하기

A는 첫 정상 debug 빌드 후 `config/toolchain.json`과 `docs/ENVIRONMENT.md`를 함께 채운다. 알려지지 않은 버전은 null로 남기고 성공으로 표기하지 않는다. 각자 Android Studio의 Gradle JDK와 터미널 JAVA_HOME이 같은 JDK를 가리키게 한다. IDE 자체를 실행하는 JBR과 프로젝트 Gradle 실행 JDK는 역할이 다르다. 최신 Android Studio의 Daemon JVM criteria를 쓰는 프로젝트라면 A가 그 사용 여부와 실제 wrapper `--version` 결과도 기록한다. [S27]

프로젝트 생성 도구가 선택한 호환 조합을 우선하고 팀원이 각자 최신 AGP·Gradle·Kotlin을 조합하지 않는다. AGP 호환표를 확인한 뒤 같은 조합으로 잠근다. 문서의 Gradle 8 이상은 호환 조합 전체를 보증하는 문장이 아니다. `org.gradle.java.home` 같은 개인 절대 경로는 공통 gradle.properties에 커밋하지 않는다. [S09, S27, S28]

Android wrapper는 `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle-wrapper.jar`를 함께 커밋한다. wrapper jar는 빌드에 필요한 파일이므로 단순히 모든 jar를 ignore하지 않는다. A는 Windows에서도 `git update-index --chmod=+x gradlew`로 실행 비트를 기록할 수 있다. 이후 전원은 wrapper를 쓰고 시스템 Gradle을 따로 설치하지 않는다.

## 15 4 줄바꿈과 파일명

`.gitattributes`가 텍스트 LF, Windows bat의 CRLF, 바이너리 비변환을 정한다. 각 clone에서 `git config --local core.autocrlf false`를 적용할 수 있으며 전역 Git 설정은 바꾸지 않는다. 첫 커밋 이후 광범위한 줄바꿈 변환은 코드 수정과 섞지 않는다. [S29]

소스·자산·폴더명은 짧은 영문으로 만들고 대소문자만 다른 파일을 만들지 않는다. Android resource는 소문자·숫자·밑줄을 사용한다. 대소문자만 이름을 바꿀 때는 임시 이름을 거치는 `git mv` 두 단계로 처리한다. 파일명 끝 공백·점, Windows 예약 이름, 경로에 역슬래시를 박은 코드, 대소문자가 틀린 자산 참조를 피한다.

JSON·MD·Python은 UTF-8로 저장한다. 제공 Python 도구는 읽기 인코딩을 명시한다. PowerShell 5의 기본 출력 인코딩에 기대어 JSON을 생성하지 말고 제공 Python 도구 또는 IDE의 UTF-8 저장을 쓴다. 스크립트는 프로젝트 루트에서 실행하며 루트 탐색에 개인 사용자 경로를 하드코딩하지 않는다.

## 15 5 오류를 분류하는 순서

| 증상 | 먼저 확인할 것 | 담당자 자율 해결 범위 |
|---|---|---|
| py 또는 python3 없음 | Python 설치와 새 터미널 | 각자 설치, 제품 계약 변경 불필요 |
| IDE 성공 터미널 실패 | JDK major·JAVA_HOME·wrapper 결과 | 각자 로컬 설정, A는 공통 버전 확인 |
| Pleos 부팅 실패 | CPU·이미지 feed·가상화·RAM | A 기준 PC, C 백업 PC로 범위 확인 |
| Windows만 자산 표시 | 경로 대소문자·파일명·assets 패키징 | 자산 작성자와 A 연결 확인 |
| SDK 다운로드 실패 | 공식 Maven·버전·네트워크·서비스 조건 | A가 오류 원인 기록, UI는 fake로 진행 |
| APK 업데이트 불가 | 기존 앱과 새 APK의 서명·applicationId | release 담당 A, 삭제 설치는 데이터 손실 설명 |

매 실패마다 캐시 전체 삭제·재설치부터 하지 않는다. 첫 실패의 핵심 오류, 명령, OS·JDK·SHA를 기록하고 원인 한 가지씩 바꾼다. 환경 문제가 2시간 이상 이어져 다른 작업을 막으면 PM에 장비·일정 영향만 짧게 알린다. 실제 양 OS 빌드 성공은 X01·X02를 수행한 뒤에만 기록한다.

## 15 6 문서 열람

기존 배포 ZIP의 Word 문서는 NanumGothic을 사용한다. Mac과 Windows의 대체 글꼴에 따른 페이지 차이를 줄이려면 배포 ZIP의 assets/fonts에 있는 Regular·Bold를 설치한다. 현재 Git 저장소에는 TTF 대신 OFL.txt만 있으며 공식 Google Fonts 원본은 ZIP에서 제공한다. 글꼴은 문서 열람용이다. 앱에는 자동 포함하지 않는다. [S41]
