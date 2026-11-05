# MEMORIDE

**Ride Becomes a Memory — 오늘의 이동이, 내일의 추억이 된다.**

자동차로 함께한 일상과 여행을 캐릭터와 섬의 성장으로 남기는 Pleos 차량 앱 프로토타입입니다. MEMORIDE의 가상 세계는 카일랜드 컨셉을 유지합니다. 지역 음식·의상·장식이 나만의 캐릭터 생활로 이어집니다.

## 팀원은 여기서 시작하세요

**이 README가 개발 시작 설명서입니다.** 초대 수락 → 공통 준비 → 파트 선택 → 자기 AI와 세부 설계·개발 → 작은 PR 순서로 진행합니다. 이미 파트를 정했다면 공통 준비를 마친 뒤 해당 파트 링크로 이동하세요.

**공통 앱에 저장된 섬·인벤토리·월드와 DEMO 저장·브리핑·동행·방문·시연을 연결했습니다.** A05 팀 서명 후보의 A/C 별도 Windows PC 설치·업데이트와 [Pleos의 실제 게임 기록 복원 사례](docs/A06_SIGNED_PLEOS_ACCEPTANCE.md)를 확인했고, 현재 [A06 통합 검증](docs/A06_INTEGRATION_VERIFICATION.md)을 진행 중입니다. Windows에서 Python59개·JUnit235개와 debug 빌드를 확인했습니다. 실제 사용자 확인 도착 저장·PLEOS 최종도착·최종 UI/최종 SHA 후보 인수는 남아 있습니다. 최초 환경 준비는 [A01 실행 안내](docs/A01_QUICKSTART.md)를 따르며 같은 Android 프로젝트를 사용합니다.

## 1. 모두 공통으로 준비하기

| 순서 | 할 일 | 완료 기준·바로가기 |
|---|---|---|
| 1 | PM이 보낸 private 저장소 초대를 수락 | 본인 계정으로 이 저장소가 열림 |
| 2 | Git·Python·Android Studio 준비 | [설치와 OS별 안내](docs/ONBOARDING.md) |
| 3 | 저장소 clone·로컬 Git 설정 | 아래 명령 및 [인증·설치 상세](docs/ONBOARDING.md) |
| 4 | 데이터·도구 정적 검사 | 아래 OS별 검사 PASS. Android NOT RUN과 구분 |
| 5 | Codex 또는 Claude를 같은 로컬 폴더에 연결 | [AI 설치·로그인·첫 세션](docs/LOCAL_AI_SETUP.md) |
| 6 | 파트 1·2·3을 정하고 자기 첫 작업 시작 | [파트 비교](docs/PART_GUIDE.md), 아래 역할 표 |

처음 한 번, 자신의 작업 상위 폴더에서:

```text
git clone https://github.com/Finn-02/MEMORIDE.git
cd MEMORIDE
git config --local core.autocrlf false
git config --local pull.ff only
git config --local fetch.prune true
```

본인의 `user.name`·`user.email`도 [설치 안내](docs/ONBOARDING.md)에 따라 설정합니다. 이미 clone했으면 반복 clone 대신 [최신 코드 받기](docs/GIT_DAILY.md)를 사용합니다. Download ZIP을 풀어서 각자 다른 프로젝트를 만들지 않습니다.

macOS PM:

```bash
python3 scripts/doctor.py
python3 scripts/check_all.py
```

Windows 팀원 PowerShell:

```powershell
py -3 scripts/doctor.py
py -3 scripts/check_all.py
```

Python 3.10 이상·표준 라이브러리만 사용합니다. **Python 가상환경·pip 패키지는 필수가 아닙니다.** Node.js·Docker·WSL·Conda도 팀 공통 필수가 아닙니다. AI CLI를 npm 방식으로 설치하는 사람만 해당 Node 요구사항을 확인합니다. Python 명령이 없거나 인증이 막히면 [설치 안내](docs/ONBOARDING.md)를 읽으세요.

## 2. 파트 선택 후 자기 링크로 이동하기

PM이 큰 기능 경계·목표·연결 계약을 정했습니다. **각 담당자는 자기 GPT·Codex·Claude와 세부 작업을 계획·설계하고 직접 개발·검증합니다.** 내부 함수·UI 배치·작업 쪼개기마다 PM 승인을 기다리지 않습니다.

| 역할 | 책임과 첫 작업 | 역할 카드 | 개발 시작·AI 프롬프트 |
|---|---|---|---|
| 파트 1 A · 노력 2 | 공통 앱·Pleos·여정·실제 제한·앱 연결·빌드. **A01 공용 기반부터** | [A01~06](roles/A.md) | [파트 1 시작](docs/FIRST_TASKS.md#part-1) |
| 파트 2 B · 노력 2 | 섬·캐릭터·의상/음식/장식·보상·유일한 게임 저장. **B01 기본 상태부터** | [B01~05](roles/B.md) | [파트 2 시작](docs/FIRST_TASKS.md#part-2) |
| 파트 3 C · 노력 2 | 동행 모의 화면·가상 방문·월드맵·시연. **C01 임시 캐릭터 슬롯부터** | [C01~05](roles/C.md) | [파트 3 시작](docs/FIRST_TASKS.md#part-3) |
| PM P · 노력 1 | 브리핑 코드·문구·지역 Catalog·fixtures. **P01 결과 화면부터** | [P01~03](roles/PM.md) | [PM 시작](docs/FIRST_TASKS.md#pm) |

세 명이 파트를 정하면 PM이 [배정표](config/team.json)에 GitHub ID를 한 번 기록합니다. 캐릭터·아이템 그림과 렌더링은 B, 지역·특산물 주제와 명칭·데이터는 PM, 월드맵 표현은 C입니다. 상세 경계는 [파트 설명](docs/PART_GUIDE.md)에 있습니다.

**PM이 전체 통합·최종 인수를 관리하고, A가 공통 앱에 각 기능을 연결합니다.** B·C·PM의 기능과 A의 기반이 하나의 앱·APK로 합쳐집니다. 작성자는 자기 기능의 시험과 오류 수정을 맡습니다. PM의 관리 시간은 개발 노력 1과 별도입니다.

## 3. 공통 Android 환경은 이렇게 맞춥니다

| 함께 고정할 것 | PC마다 달라도 되는 것 |
|---|---|
| 같은 main 기반·계약·데이터 ID | macOS/Windows·CPU·설치 폴더 |
| Gradle wrapper·AGP·Kotlin·Compose·AndroidX 등의 공통 버전 정의 | SDK·JDK의 개인 절대 경로·캐시 |
| Gradle 실행 JDK major·min/compile/target SDK | CPU에 맞는 AVD 이미지·debug 서명키 |
| UTF-8·줄바꿈·파일명 규칙 | 선택한 AI 도구·편집기 UI |

A가 [A01 전달 기준](docs/BOOTSTRAP_HANDOFF.md)에 따라 실제 호환되는 프로젝트를 생성·debug 빌드하고 버전을 [toolchain](config/toolchain.json)과 [실제 환경 기록](docs/ENVIRONMENT.md)에 남깁니다. 그 뒤 전원이 main을 받아 **같은 빌드 파일·wrapper를 사용**합니다. `toolchain.json`은 기록이며 버전을 자동 설치하는 프로그램이 아닙니다. A01의 `verified: true`는 기록된 Windows의 기본 debug 빌드 확인을 뜻하며 실제 SDK·모든 PC·release 성공 인증이 아닙니다.

A01 전에는 B·C·PM도 계약·샘플·상태 예제·화면 스케치를 준비할 수 있습니다. 실제 Compose 컴파일·Preview는 공통 프로젝트를 받은 뒤 합니다. 파트별 Android 프로젝트 생성이나 개인별 Gradle 업그레이드는 하지 않습니다.

A01 반영 후 [Android 설정 절차](docs/ONBOARDING.md)에 따라 SDK·Gradle JDK를 맞추고 검사합니다.

```bash
# macOS
./gradlew --version
python3 scripts/check_all.py --android
```

```powershell
# Windows PowerShell
.\gradlew.bat --version
py -3 scripts/check_all.py --android
```

A01에는 wrapper가 포함되어 있으므로 각자 기록된 JDK·SDK를 맞춘 뒤 Android 검사를 실행합니다. 정적 검사 PASS·debug 빌드 PASS·Pleos 설치/SDK PASS·release 설치 PASS는 서로 다른 결과입니다. OS별 실제 결과를 [환경 기록](docs/ENVIRONMENT.md)에 따로 남깁니다.

## 4. 매일 개발하는 순서

1. `git status`로 기존 작업을 확인·보존하고 최신 main을 받습니다.
2. 자기 파트의 기존 `part/a-...`, `part/b-...`, `part/c-...`, `part/pm-...` 브랜치를 사용합니다. 파트 브랜치는 처음 한 번만 만들고 다음 작업에도 유지합니다.
3. [AGENTS.md](AGENTS.md) → 자기 역할 → 해당 [WBS 작업](tasks/backlog.md) → 필요한 계약만 AI와 읽습니다.
4. 담당자가 내부 계획을 짧게 정하고 구현·시험합니다. 같은 폴더를 두 AI가 동시에 수정하지 않습니다.
5. diff·검사 결과를 확인하고 commit·push·작은 PR을 만듭니다. 연결 예제와 NOT RUN만 짧게 적습니다.
6. PM의 통합 순서에 따라 작은 PR을 main에 일찍 합칩니다. GitHub의 **Create a merge commit**을 사용하고 파트 브랜치를 삭제하지 않습니다. 병합 후 기존 변경을 보존하고 같은 파트 브랜치에서 `git fetch origin`, `git merge origin/main`으로 최신 공통 변경을 받아 다음 작업을 시작합니다. 이미 squash/rebase 병합했다면 다음 PR의 중복 diff를 먼저 확인하고 이력을 강제 초기화하지 않습니다.

복사할 수 있는 pull·branch·commit·push 명령과 충돌 대처는 [일상 Git 안내](docs/GIT_DAILY.md)에 있습니다. 기본 교차 리뷰는 A↔PM, B↔C입니다. 모든 PR과 내부 결정에 PM 승인을 추가하지 않습니다.

공유 타입·동작 변경만 A와 영향받는 소비자 한 명에게 [요청 템플릿](templates/communication.md)으로 확인합니다. 핵심 범위·새 비용·실제 제한 완화·저장 비호환·1일 이상 지연만 PM에게 올립니다. 평범한 내부 결정·일반 결함은 담당자가 해결합니다.

## 5. 경계와 MVP 범위

하나의 Android app 모듈에서 기능 소유권으로 나눕니다. 기능끼리 직접 import하지 않고 A의 AppRoot가 공통 DTO·콜백·캐릭터 슬롯으로 조립합니다. B만 게임 진행 상태를 저장합니다. 공통 파일의 동시 수정을 피하고 계약이 유지되는 내부 구현은 자율적으로 바꿉니다.

최소 시연은 **섬 준비 → 명시적인 DEMO 여정 → 도착 저장 → 브리핑 → 지역 기념품 사용**입니다. Daily Island는 일상 이동에 따른 섬 성장, Journey Archipelago는 준비된 지역 발견 카드입니다. 동행·동의 기반 방문은 실제 주차 상태에서 자체 화면으로 체험하는 모의 상황입니다.

실제 Pleos 목적지·경로·도착과 제한 입력은 A의 별도 어댑터에서 검증합니다. DEMO와 SDK 출처를 구분하고 실제 제한·UNKNOWN이 우선합니다. 실제 기본 내비 오버레이·V2V·상시 음성 호출·실차 방문 인증을 구현했다고 설명하지 않습니다. 상세 여행 이력·사진 앨범·Car Care·서버·로그인·결제·런타임 LLM은 이번 범위에 없습니다.

## 6. 일정·검사·비용·제출

D1 공용 기반 → D3 한 지역 전체 흐름 → D8 첫 release 설치 → D11 기능 동결 → D13 내부 제출을 목표로 합니다. [전체 로드맵](docs/16_kickoff_process.md)과 [시험 기준](docs/09_quality_tests.md)을 필요한 때 확인하세요. 날짜는 로드맵의 시작일 가정이며 미실행 항목을 완료로 바꾸지 않습니다.

Actions에는 push/PR 자동 트리거가 없고 수동 실행만 있습니다. 로컬 검사가 기본이며 새 유료 서버·API·자산·AI Actions를 도입하지 않습니다. Actions 비활성화·계정 비용 통제·병합 규칙은 별도 설정으로, 파일이 있다고 자동 적용되지는 않습니다. [PM 저장소 설정](REPO_SETUP.md)을 참고하세요.

팀 서명 중간 release APK는 A05에서 생성·교차 설치했습니다. 최종 제출 후보는 A06 인수 후 확정하며, APK 빌드 소스와 환경은 아래 A05 기록을 따릅니다. 심사용 private 접근과 공식 제출은 [인수 확인표](templates/acceptance_checklist.md)로 확인합니다.

A05의 [중간 release 서명·검증·C PC 전달 절차](docs/A05_RELEASE_HANDOFF.md)를 준비했습니다.
첫 팀 키로 서명한 중간 후보(versionCode 2/3)의 로컬 설치·동일 키 업데이트를 확인했습니다.
빌드 소스는 `ee3ed0f`이며, [C의 다른 Windows PC 설치·업데이트도 확인](docs/A05_C_ACCEPTANCE_REVIEW.md)했습니다.
이후 [새 Pleos에서 중간 서명 후보의 설치·진행 복원](docs/A06_SIGNED_PLEOS_ACCEPTANCE.md)도 확인했습니다.
전체 저장 호환 행렬·최종 UI/A06·최종 SHA 후보 인수는 아직 남아 있습니다.
키 없는 unsigned 빌드나 debug 키 검증을 서명 release 인수 완료로 표시하지 않습니다.

## 필요한 문서만 찾아보기

| 목적 | 문서 |
|---|---|
| 온보딩 전체 | [설치](docs/ONBOARDING.md) · [로컬 AI](docs/LOCAL_AI_SETUP.md) · [Git](docs/GIT_DAILY.md) · [첫 작업](docs/FIRST_TASKS.md) |
| 범위·브랜드 | [컨셉 기준 1.2](docs/Concept_Baseline_v1.2.md) · [브랜드](docs/BRAND.md) · [제품 범위](docs/01_product_scope.md) |
| 역할·구조·계약 | [파트 비교](docs/PART_GUIDE.md) · [구조](docs/02_ownership_architecture.md) · [데이터](docs/03_contracts_data.md) · [UI](contracts/ui_contracts.md) |
| 구현·검증 | [WBS](tasks/backlog.md) · [상태 규칙](docs/04_rules_states.md) · [최종 경계 검토](docs/18_final_review.md) · [품질](docs/09_quality_tests.md) |
| 진행·소통 | [로드맵](docs/16_kickoff_process.md) · [상태 카드](docs/status/) · [최소 요청](templates/communication.md) |
| OS·실제 결과 | [cross-platform](docs/15_cross_platform.md) · [환경 기록](docs/ENVIRONMENT.md) · [저장소 상태](REPOSITORY_STATUS.md) |
| 출처·자산 | [공식 자료](docs/sources.md) · [자산·라이선스](assets/ATTRIBUTION.md) |

개발 원본은 원격 저장소의 MD·JSON·소스입니다. 기존 DOCX·배포 ZIP은 스냅샷이며 앱 개발 중 새 변경의 기준이 아닙니다. 내부 package `io.kailand`·contentVersion 1.1.0·저장 schemaVersion 1·기존 ID는 브랜딩 때문에 일괄 변경하지 않습니다. 제공 PNG/SVG는 자체 도형 초안이며 실제 지역 제휴·공식 상품 인증을 뜻하지 않습니다. 전체 코드의 오픈소스 라이선스는 아직 정하지 않았습니다.
