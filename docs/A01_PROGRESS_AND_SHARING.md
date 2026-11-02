# MEMORIDE 파트 1: 지금까지 한 일과 팀 공유 방법

이 문서의 미업로드 표시는 최초 작성 시점의 기록입니다. 이후 공유 여부는 GitHub 브랜치·PR 상태로 확인합니다.

작성일: 2026-11-02 · 담당: 파트 1(A) · 작업: A01 공용 앱 기반

**지금은 함께 개발할 Android 앱의 기본 틀을 만들고, 내 Windows PC에서 빌드와 첫 화면 이동을 확인한 상태입니다. 아직 GitHub에 업로드하거나 main에 합치지 않았습니다.**

이 문서는 진행 상황을 쉽게 설명하는 안내입니다. 기존 AGENTS.md, 데이터·UI 계약과 역할별 개발 지침을 바꾸지 않습니다.

## 1. 우리가 지금 만들고 있는 것은?

MEMORIDE의 게임 전체를 완성한 것이 아니라, 여러 사람이 같은 앱에서 자기 기능을 개발할 수 있는 출발점을 만들었습니다. 집에 비유하면 공동으로 사용할 기초와 출입구를 만든 셈입니다.

| 담당 | 맡은 일 | 공용 기반을 받은 뒤 첫 작업 |
|---|---|---|
| 파트 1(A), 나 | 공통 앱, 화면 연결, 차량 여정, Pleos 연동, 이용 제한, 빌드 | A02: 가짜 입력으로 여정 흐름 구현 |
| 파트 2(B) | 게임 상태·저장·보상, 섬·아이템·캐릭터 | B01: 게임 상태와 유일한 저장 경계 구현 |
| 파트 3(C) | 동행 모의 화면, 가상 방문, 월드맵, 시연 화면 | C01: 동행 모의 화면과 임시 캐릭터 슬롯 |
| PM(P) | 브리핑 화면·문구, Catalog·fixtures, 통합 관리 | P01: 확정된 결과를 표시하는 브리핑 |

## 2. 지금까지 완료한 일

| 한 일 | 쉽게 설명하면 | 확인 결과 |
|---|---|---|
| 루트의 단일 app 모듈 생성 | 모두 같은 Android 앱에서 개발할 기반 | debug APK 빌드 성공 |
| Gradle wrapper와 공통 버전 고정 | 팀원들이 같은 빌드 도구·라이브러리 기준 사용 | Gradle 9.3.1 / JBR 25.0.4 실행 확인 |
| 공통 계약을 실행 코드로 이동 | 파트끼리 주고받을 데이터 형식을 실제 컴파일 | 기존 공개 필드·의미 유지 |
| AppRoot·테마·임시 화면 추가 | 각 파트 화면을 나중에 연결할 자리 마련 | 홈과 여정 준비 표시 확인 |
| 개발 DEMO 진입점 분리 | 일반 에뮬레이터에서 기본 틀을 확인하는 debug 전용 화면 | 실제 차량 주차 확인과 별개 |
| 원본 자산 포함 방식 추가 | Catalog·예제 데이터·PNG를 앱 안에서 읽을 준비 | APK의 자산 18개가 원본과 일치 |
| 자동 검사 실행 | 기본 코드·데이터 오류와 빌드 문제 확인 | Python 시험 34개, Android 단위 시험 4개 통과 |
| Android Studio 연결 | IDE가 프로젝트와 app을 정상 인식 | Gradle Sync 성공 |
| 에뮬레이터 실행 | 가상 휴대폰에서 앱이 켜지는지 확인 | API 37.0에서 개발 DEMO 홈 표시 |
| 화면 이동 확인 | 버튼으로 다른 화면에 갔다가 돌아오기 | 홈→여정 준비는 AI 화면 관찰, 메인 섬 복귀는 사용자 확인 |

실제 실행 기록: `docs/ENVIRONMENT.md`, `docs/status/a.md`.

## 3. 지금 화면이 단순한 이유

현재 보이는 임시 tori와 보라색 버튼은 화면 연결을 확인하는 샘플입니다. “실제 기능은 아직 없습니다”라는 문구는 현재 단계에서 정상입니다.

아직 구현하거나 검증하지 않은 항목은 다음과 같습니다.

- 실제 게임 저장·보상·섬 성장·캐릭터 렌더링
- 실제 차량 입력·Pleos 연동·주차 및 이용 제한의 현장 검증
- 전체 화면의 이동, 시스템 뒤로 가기, 재시작·복원 시험
- 서명 release 설치, macOS와 다른 팀원 PC의 빌드

정식 진입점은 실제 이용 상태를 확인할 수 없어 UNKNOWN으로 차단합니다. 일반 에뮬레이터의 개발 DEMO가 열렸다는 사실은 실제 차량에서 사용 가능하다는 증거가 아닙니다.

## 4. 파트 2·3은 언제 시작할 수 있나?

**공통 앱 안에서의 본격적인 개발 기준은 A01을 PR로 공유하고 main에 반영한 뒤, 각자 PC에서 받아 기본 빌드를 확인하는 것입니다.** 내 차량 연동이나 전체 게임 완성을 기다릴 필요는 없습니다.

현재 위치는 아래의 ②입니다.

1. A01 공용 앱 기반 만들기 — 완료
2. 내 PC의 빌드·동기화·기본 화면 확인 — 완료, 확인 범위는 위 기록 참고
3. A01 커밋·푸시·PR — 아직 안 함
4. 검토 후 main 반영 — 아직 안 함
5. B·C·PM이 받아 각자 기능 개발 — 다음 단계

팀원들은 지금도 준비할 수 있습니다. B는 상태 모델·보상 예제, C는 동행 화면 스케치·방문 상태 전이, PM은 브리핑 문구와 누락값 처리를 준비하면 됩니다. 별도 Android 프로젝트를 새로 만들지는 않습니다.

## 5. Git 용어 네 가지만 알기

| 용어 | 뜻 |
|---|---|
| 브랜치 | 작은 작업을 따로 진행하는 작업 줄기. 지금은 part/a-a01-bootstrap |
| 커밋(commit) | 변경 파일을 설명과 함께 Git에 기록. 아직 인터넷 공유는 아님 |
| 푸시(push) | 내 커밋을 GitHub에 업로드 |
| PR(Pull Request) | 작업 브랜치를 팀 공용 main에 합치기 위한 검토 요청. 생성만으로 합쳐지지 않음 |

GitHub 저장소: https://github.com/Finn-02/MEMORIDE

## 6. 내가 팀에 공유하는 순서

아래는 안내용입니다. 이 문서를 작성하면서 commit·push·PR·merge를 실행하지 않았습니다.

### ① 문서만 먼저 공유하기

이 Markdown 파일을 카카오톡·Discord 등 팀 대화방에 직접 첨부할 수 있습니다. 파일 공유만으로 팀원 컴퓨터에 공통 앱 코드가 설치되는 것은 아닙니다. 코드 전달은 아래 GitHub 절차로 진행합니다.

### ② 현재 브랜치와 변경 파일 확인

Android Studio 아래 Terminal을 열고 저장소 루트에서 실행합니다.

```powershell
git status
git diff --check HEAD
git diff HEAD
```

브랜치가 `part/a-a01-bootstrap`인지 확인합니다. 다른 사람이 만든 변경이 섞여 있으면 내용을 먼저 확인합니다. `git diff`에는 새 파일 내용이 나오지 않으므로 Android Studio에서도 새 app·Gradle 파일을 확인합니다.

### ③ 이번 작업 파일만 커밋 대상으로 선택

아래 경로들은 이 A01 작업 기준입니다. 실행 직전에 git status와 대조합니다. `app/src`에는 기본 화면·계약·시험이 포함됩니다.

```powershell
git add -- README.md REPOSITORY_STATUS.md config/project.json config/toolchain.json contracts/KailandContracts.kt docs/ENVIRONMENT.md docs/status/a.md docs/A01_QUICKSTART.md docs/A01_PROGRESS_AND_SHARING.md app/build.gradle.kts app/src build.gradle.kts settings.gradle.kts gradle.properties gradle/libs.versions.toml gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties gradlew gradlew.bat
git diff --cached --stat
git diff --cached
```

`--cached`는 “이번 커밋에 넣기로 선택한 변경”을 보여줍니다. `local.properties`, `.idea`, `.gradle`, build 폴더, ZIP, 키·자격 증명은 넣지 않습니다. 현재 PC에서 gradlew의 Mac 실행 비트는 이미 설정했으며 Git은 100755로 기록해야 합니다.

### ④ 커밋하고 GitHub에 올리기

선택한 파일과 검사 결과를 확인한 뒤 실행합니다.

```powershell
git commit -m "feat(A01): add shared Android bootstrap"
git push -u origin HEAD
```

첫 줄은 로컬 기록, 두 번째 줄은 GitHub 업로드입니다. 로그인 요청이 나오면 본인 계정으로 직접 처리합니다. 명령이 실패하면 다음 단계로 넘어가지 말고 오류를 확인합니다.

### ⑤ GitHub에서 PR 만들기

저장소의 Pull requests에서 새 PR을 만듭니다. **base는 main, compare는 part/a-a01-bootstrap**으로 선택합니다. 업로드 직후 Compare & pull request 버튼이 나타나면 그것을 사용해도 됩니다.

제목 예시:

```text
feat(A01): 공용 Android 앱 기반과 개발 DEMO 추가
```

본문은 저장소 PR 템플릿에 맞춰 아래 내용을 옮기면 됩니다. 실제 커밋 SHA와 확인 결과는 제출할 때 다시 확인합니다.

```text
## 변경
- A01: 단일 app, wrapper·버전 고정, 컴파일 계약, 최소 AppRoot·테마·개발 DEMO, 원본 자산 매핑 추가.
- 기존 계약의 공개 필드·의미 유지. 섬·저장·보상·Pleos 실제 기능은 후속 작업.

## 검증
- Windows 11 AMD64 / 작업 브랜치의 실제 커밋 SHA 기재.
- Gradle 9.3.1 / JBR25.0.4, testDebugUnitTest 4개와 assembleDebug PASS.
- python scripts/check_all.py --android exit 0, Python 시험 34개 포함.
- APK 원본 자산 18개 바이트 대조 PASS. Android Studio Sync PASS.
- 일반 API37 에뮬레이터 개발 DEMO 홈·여정 이동 확인, 메인 섬 복귀는 사용자 확인.
- NOT RUN: 시스템 Back·전체 화면·실제 Pleos/gate·release·macOS/다른 PC.
- 실행 방법과 연결 표면: docs/A01_QUICKSTART.md.

## 남은 것
- PM 교차 검토 후 main 반영. B/C/PM은 자기 PC에서 기본 빌드 확인 후 각 feature 시작.
- A는 A02로 진행. A01 공유에 A03 실제 SDK 성공을 기다리지 않음.
```

### ⑥ PR 링크를 팀원에게 보내고 검토받기

PR 링크와 이 문서를 팀 대화방에 공유합니다. 기본 교차 검토는 A↔PM이며, 공유 계약 변경이 생기면 A와 영향을 받는 담당자가 함께 확인합니다. PR은 프로젝트 담당자가 검토 후 병합합니다. 자동 병합이나 force push는 사용하지 않습니다.

전송 문구 예시 — 대괄호는 실제 상황으로 바꿉니다.

```text
[A01 PR 생성 후 보내기]
공용 Android 앱 기반을 만들었습니다.
제 Windows에서 빌드·단위 시험·IDE Sync와 에뮬레이터 홈/여정 왕복을 확인했습니다.
아직 저장·보상·실제 차량 연동은 없습니다.
PR: [실제 PR 링크]
PM 검토 후 main에 반영되면 B는 B01, C는 C01, PM은 P01을 시작할 수 있습니다.
실행 안내: docs/A01_QUICKSTART.md
진행 설명: docs/A01_PROGRESS_AND_SHARING.md
```

GitHub Actions는 별도 실행하지 않습니다. 로컬 검사 결과를 PR에 기록하며 Actions 설정·비용 통제가 적용됐다고 추정하지 않습니다.

## 7. 팀원이 코드를 받는 방법

**A01이 main에 반영됐다는 확인을 받은 뒤** 진행합니다. 기존 작업이 있으면 먼저 자기 작업 브랜치에 커밋해 보존합니다. 변경을 버리는 명령으로 시작하지 않습니다.

이미 자기 작업 브랜치가 있는 팀원은 그 브랜치에서:

```powershell
git status
# 기존 변경을 확인하고 보존한 뒤
git fetch origin
git merge origin/main
```

아직 작업 브랜치가 없고 현재 작업이 깨끗한 팀원은:

```powershell
git switch main
git pull --ff-only origin main
# B 예시. C는 part/c-c01-companion 등 자기 작업 이름 사용
git switch -c part/b-b01-game-state
```

충돌이 나면 파일 소유자와 확인합니다. 다른 파트의 브랜치를 차례로 직접 합치는 방식은 사용하지 않습니다.

그다음 저장소 루트를 Android Studio로 열고 `docs/A01_QUICKSTART.md`에 따라 SDK와 Gradle JDK를 맞춥니다. 필요하면 File > Open으로 루트 settings.gradle.kts를 엽니다. 개인 SDK/JDK 경로와 local.properties를 서로 복사하지 않습니다.

Windows 검사:

```powershell
.\gradlew.bat --version
py -3 scripts/check_all.py --android
```

`py -3`가 없고 `python --version`이 3.10 이상이면 `python scripts/check_all.py --android`로 실행합니다. macOS에서는 `./gradlew --version`, `python3 scripts/check_all.py --android`를 사용합니다. 실행 전 터미널의 JAVA_HOME도 해당 PC의 JDK25와 맞아야 합니다.

팀원은 자기 PC의 결과를 별도로 기록합니다. 내 Windows 성공을 다른 PC의 성공으로 대신 표시하지 않습니다. B·C는 자기 feature만 개발하고 공통 AppRoot 연결은 A에게 전달합니다.

## 8. 다음 한 작업

**지금 내가 할 일은 A01 변경을 확인해 커밋하고, 작업 브랜치를 푸시한 뒤 main 대상 PR을 만드는 것입니다.** 그 PR이 반영되면 팀원들은 공통 기반을 받아 병렬 개발하고, 나는 A02를 시작합니다.

세부 기준: `docs/BOOTSTRAP_HANDOFF.md`, `docs/FIRST_TASKS.md`, `docs/GIT_DAILY.md`.
