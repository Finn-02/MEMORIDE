# 로컬 개발 환경 설치와 첫 실행

이 문서는 전원이 한 번 수행한다. 파트 선택 전에도 1~4단계는 가능하다. PM은 macOS, 팀원 세 명은 Windows PowerShell을 기준으로 한다. 앱의 공통 버전 기록은 [toolchain](../config/toolchain.json), 실제 PC별 결과는 [ENVIRONMENT](ENVIRONMENT.md) 한 곳에서 관리한다.

## 1. 설치할 도구

| 도구 | 설치와 확인 | 팀 규칙 |
|---|---|---|
| Git | [공식 설치](https://git-scm.com/downloads), `git --version` | `git switch`를 지원하는 Git 2.23 이상. Windows는 Git for Windows 사용 |
| Python | [공식 설치](https://www.python.org/downloads/), Mac `python3 --version`, Windows `py -3 --version` | 3.10 이상. 검증 도구는 표준 라이브러리만 사용 |
| Android Studio | [공식 설치](https://developer.android.com/studio/install) | A01에서 공유한 동일 배포 버전의 OS별 설치본 사용. 이미 설치했으면 버전을 기록하고 무조건 재설치하지 않음 |
| 코드 편집기·AI | [로컬 AI 설정](LOCAL_AI_SETUP.md) | Android Studio는 빌드·Preview·실행, AI 편집기는 같은 저장소의 코드 편집 |
| JDK·Android SDK | Android Studio의 SDK Manager·Gradle 설정 | A01의 검증된 값에 맞춤. 독립 시스템 Gradle·Kotlin·Compose 설치 불필요 |

설치 후 새 터미널을 연다. Windows에서 `py`가 없다면 `python --version`으로 3.10 이상인지 확인하고 문서의 `py -3`를 `python`으로 대체한다. Mac에서 Python이 없으면 공식 설치본을 사용한다. 기존 회사·학교 장비의 전역 설정을 무작정 바꾸지 않는다.

Python venv·pip 설치는 **현재 필요 없다**. Python은 앱 서버가 아니라 저장소 검사 도구다. 선택적으로 격리하려면 Mac `python3 -m venv .venv`, Windows `py -3 -m venv .venv`로 만들고 활성화 없이 Mac `.venv/bin/python scripts/check_all.py`, Windows `.\.venv\Scripts\python.exe scripts/check_all.py`를 실행할 수 있다. `.venv`는 Git에서 제외된다. 설치할 `requirements.txt`는 없다.

Node.js·npm·Docker·WSL·Conda·데이터베이스는 앱 개발 필수가 아니다. AI 도구를 npm 방식으로 설치하기로 한 사람만 그 도구의 공식 Node 요구 버전을 따른다. CLI를 쓰지 않는 팀원에게 Node를 강제하지 않는다.

## 2. private 저장소 접근과 clone

PM이 보낸 초대를 **실제로 수락한 GitHub 계정**으로 로그인하고 [저장소](https://github.com/Finn-02/MEMORIDE)가 열리는지 확인한다. 로그인과 Git 커밋 작성자 설정은 다르다. HTTPS 인증은 Git Credential Manager의 브라우저 로그인 또는 GitHub CLI의 `gh auth login`을 사용한다. 이미 되는 인증은 유지한다. [GitHub 공식 인증 안내](https://docs.github.com/en/get-started/git-basics/caching-your-github-credentials-in-git)를 따른다. 토큰을 clone URL·문서·AI 프롬프트에 넣지 않는다.

처음 clone하는 사람만 아래를 실행한다. 이미 clone했다면 같은 저장소를 다시 만들지 말고 3단계와 [일상 Git 흐름](GIT_DAILY.md)을 따른다. 브라우저의 Download ZIP은 브랜치·pull 개발의 시작 방법이 아니다.

macOS Terminal:

```bash
mkdir -p ~/src
cd ~/src
git clone https://github.com/Finn-02/MEMORIDE.git
cd MEMORIDE
```

Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force C:\src | Out-Null
Set-Location C:\src
git clone https://github.com/Finn-02/MEMORIDE.git
Set-Location MEMORIDE
```

폴더는 각자 바꿔도 된다. 권한이 없는 위치는 사용자 소유의 짧은 영문 폴더를 사용한다. OneDrive·iCloud 동기화 폴더를 피하고 Android Studio·AI 모두 **같은 MEMORIDE 루트**를 연다.

## 3. clone마다 한 번 적용할 설정

두 OS 공통으로 저장소 루트에서 실행한다. 아래 이름·이메일은 본인의 값으로 바꾼다. 이메일은 GitHub에서 제공하는 본인 noreply 주소를 사용할 수 있다.

```text
git config --local user.name "본인 이름"
git config --local user.email "본인 GitHub 커밋 이메일"
git config --local core.autocrlf false
git config --local pull.ff only
git config --local fetch.prune true
git status
```

전역 설정은 바꾸지 않는다. `.gitattributes`가 소스 LF·bat/cmd CRLF를 관리하고 `.editorconfig`가 UTF-8·들여쓰기·마지막 줄을 안내한다. IDE에서 EditorConfig 지원을 켜고, 기존 파일 전체 포맷 변경을 자기 기능 수정과 섞지 않는다. 상세 OS 규칙은 [cross-platform](15_cross_platform.md)을 따른다.

## 4. 앱이 없어도 가능한 첫 검사

macOS:

```bash
python3 scripts/doctor.py
python3 scripts/check_all.py
```

Windows PowerShell:

```powershell
py -3 scripts/doctor.py
py -3 scripts/check_all.py
```

완료 조건은 데이터·샘플·파일 검사와 Python 테스트가 모두 PASS인 것이다. `doctor.py`는 정보를 출력하는 진단 도구이며, 기본 실행의 종료 코드 0은 모든 도구가 설치되었다는 보증이 아니다. Git 버전·Python 버전과 진단 출력을 함께 확인한다.

현재의 `Android ... NOT RUN` 문구는 정상적인 사실 표시다. 이 단계 성공은 APK 빌드·Pleos 성공을 의미하지 않는다. 실패하면 [오류 분류](15_cross_platform.md)와 [환경 기록 템플릿](../templates/environment_report.md)을 사용한다. 토큰·개인 절대 경로는 가려서 공유한다.

## 5. A01 공통 앱을 받은 뒤 Android 맞추기

**각자 New Project로 별도 앱을 만들지 않는다.** 파트 1만 [첫 공용 빌드 전달 기준](BOOTSTRAP_HANDOFF.md)에 따라 저장소 루트에 하나의 Android 프로젝트를 추가한다. 다른 파트는 그동안 자기 첫 작업의 입력·완료 조건·화면 스케치를 정리할 수 있다. Compose 코드의 실제 컴파일과 Preview는 공통 프로젝트를 받은 뒤 수행한다.

1. [일상 Git 흐름](GIT_DAILY.md)에 따라 A01이 반영된 main을 받는다. 진행 중인 자기 브랜치에는 `origin/main`을 병합한다.
2. Android Studio에서 이 저장소 루트를 연다. 기록된 Android Studio·JDK major·SDK 플랫폼을 맞춘다. 자동 AGP·Gradle·Kotlin 업그레이드 제안은 적용하지 않는다.
3. SDK Manager에서 [실제 환경 기록](ENVIRONMENT.md)의 compileSdk 플랫폼과 필요한 도구를 설치한다. `local.properties`의 `sdk.dir`는 Android Studio가 자신의 경로로 만들게 한다. 팀원 파일을 복사하지 않는다.
4. Gradle JDK와 터미널의 JDK를 맞춘다. 아래의 실행 결과가 기록과 일치하는지 확인한다. `JAVA_HOME`이 필요하면 자신의 실제 JDK 폴더를 OS별 환경 설정에 넣고 새 터미널을 연다. 저장소에는 절대 경로를 쓰지 않는다.
5. 실제 `--android` 검사를 수행한다. 최초 실행에는 Gradle·Maven·SDK 다운로드와 네트워크가 필요하다. 이후 캐시를 유지한다.

macOS:

```bash
./gradlew --version
python3 scripts/check_all.py --android
```

Windows PowerShell:

```powershell
.\gradlew.bat --version
py -3 scripts/check_all.py --android
```

`Permission denied`면 먼저 wrapper가 올바른 커밋인지 확인하고 Mac에서 `chmod +x gradlew`를 적용한다. 실행 비트의 원격 수정은 A가 관리한다. 앱 검사가 버전 미확인·wrapper 없음으로 종료 코드 2를 내면 거짓 성공으로 바꾸지 않는다. A01 미완료인지 확인한다.

JDK는 Android Studio 자체를 실행하는 JBR과 Gradle 실행 JVM을 구분한다. 일반 프로젝트에서는 IDE의 Gradle JDK와 터미널 `JAVA_HOME`을 같은 설치본으로 맞춘다. Daemon JVM criteria가 있으면 그 설정이 실행 JVM 선택에 영향을 주므로 A가 사용 여부를 기록하고 `--version`의 실제 JVM을 확인한다. 체크 스크립트는 터미널 Java major도 비교하므로 그 major도 맞춘다. [Android 공식 JDK 설명](https://developer.android.com/build/jdks)을 참고한다.

공통 버전의 기준은 **커밋된 빌드 파일·wrapper·버전 카탈로그**다. `config/toolchain.json`은 이를 설명하는 기록이며 Gradle 버전을 자동 적용하는 파일이 아니다. 두 값이 다르면 A에게 차이를 전달하고 임의 업그레이드로 해결하지 않는다.

## 6. 화면 실행과 Pleos 실행을 구분하기

파트 2·3·PM은 A가 분리한 개발 전용 fake 진입점과 Compose Preview로 화면을 개발한다. 실제 Pleos SDK 검증은 A의 Windows 기준 PC와 C의 백업 PC에서 진행한다. 공통 프로젝트가 없으면 fake 화면도 실행되지 않는다.

Pleos 이미지 설치 feed·CPU·가상화 확인은 [cross-platform의 에뮬레이터 절](15_cross_platform.md#15-2-에뮬레이터와-cpu-확인)을 따른다. 모든 PC에서 같은 ABI를 강제하지 않는다. Apple Silicon·Windows x64·Intel Mac 지원을 같은 것으로 취급하지 않는다. 지원하지 않는 CPU이면 기준 PC 시연을 사용하고 자기 PC 결과를 NOT RUN으로 남긴다.

실제 제한·UNKNOWN 차단을 fake로 우회하지 않는다. 개발 전용 fake는 실제 플랫폼에서 자동으로 활성화되면 안 된다. `assembleDebug` 성공과 에뮬레이터 설치·SDK 동작 성공은 별도로 기록한다.

## 7. 개인 준비 완료 기준

- 저장소 초대 수락·clone·Git 작성자 설정 완료.
- 정적 검사 PASS, 자기 OS·CPU·Python·선택한 AI 도구 확인.
- [파트 선택](PART_GUIDE.md) 후 PM이 [배정표](../config/team.json)를 한 번 갱신.
- 공통 앱을 받으면 debug 검사 결과를 [ENVIRONMENT](ENVIRONMENT.md)에 기록. 환경이 같다는 말 대신 실제 OS별 결과를 남김.
- [역할별 첫 작업](FIRST_TASKS.md)의 프롬프트로 자기 AI와 작게 설계·구현 시작.

상세 내부 설계 문서를 PM에게 일괄 승인받는 절차는 없다. 매일 [Git 흐름](GIT_DAILY.md)으로 최신 main을 받고 작은 PR을 만든다.
