# MEMORIDE

**Ride Becomes a Memory**

자동차로 함께한 일상과 여행을 캐릭터와 섬의 성장으로 남기는 Pleos 차량 앱 프로토타입입니다. MEMORIDE의 가상 세계는 기존 카일랜드 컨셉을 유지합니다. 오늘의 이동에서 얻은 지역 음식·의상·장식이 캐릭터의 생활로 이어집니다.

## 현재 상태

**개발 착수 자료 v1.2이며, 아직 실행 가능한 Android 앱이나 APK가 없습니다.** README·명세·역할·계약 초안·데이터·도형 자산·정적 검증 도구를 제공합니다. 파트 1의 A01 공통 앱 생성과 첫 빌드가 선행 작업입니다. SDK·양 OS·release 실행 결과는 NOT RUN입니다.

## 먼저 읽기

1. [개발 시작 안내](START_HERE.md)
2. [파트 선택과 경계](docs/PART_GUIDE.md)에서 파트 1·2·3을 선택
3. [AI 작업 지침](AGENTS.md)과 자기 역할 카드
4. [작은 작업 목록](tasks/backlog.md), [공통 데이터 계약](docs/03_contracts_data.md), [화면 연결 계약](contracts/ui_contracts.md)
5. [macOS와 Windows 환경](docs/15_cross_platform.md), [전체 로드맵](docs/16_kickoff_process.md)

| 역할 | 범위 | 개발 노력 | 작업 ID와 카드 |
|---|---|---|---|
| 파트 1 | 공통 앱·Pleos·여정·제한·기능 연결·빌드 | 2 | A01~06 · [A](roles/A.md) |
| 파트 2 | 섬·캐릭터·아이템·보상·단일 저장 | 2 | B01~05 · [B](roles/B.md) |
| 파트 3 | 동행 모의 화면·가상 방문·월드맵·시연 | 2 | C01~05 · [C](roles/C.md) |
| PM | 브리핑 UI·문구·지역 데이터·검증 | 1 | P01~03 · [PM](roles/PM.md) |

실제 인원 배정은 팀이 결정하고 [배정표](config/team.json)에 GitHub ID만 기록합니다. PM의 관리 시간은 개발 비율 밖입니다. 역할별 작성자가 구현·관련 시험·오류 수정을 맡고, D3·D7에 실제 부담을 조정합니다.

## 실행 방법과 환경

현재는 앱 실행 대신 제공 데이터와 도구를 검사할 수 있습니다. Python 3.10 이상과 표준 라이브러리만 사용합니다.

macOS:
```bash
python3 scripts/check_all.py
```

Windows PowerShell:
```powershell
py -3 scripts/check_all.py
```

A01 이후 파트 1이 실제 Gradle wrapper·AGP·Kotlin·Compose·JDK 조합을 [toolchain](config/toolchain.json)과 [환경 기록](docs/ENVIRONMENT.md)에 확정합니다. 그 뒤 위 명령에 `--android`를 붙입니다. 직접 Gradle 검사는 macOS `./gradlew :app:testDebugUnitTest :app:assembleDebug`, Windows `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`입니다. 현재 wrapper가 없으므로 앱 검사는 미실행으로 차단됩니다.

## MVP 범위와 시연

섬 준비 → 명시적인 DEMO 여정 → 도착 저장 → 브리핑 → 지역 기념품 사용을 연결합니다. Daily Island는 기존 섬의 성장, Journey Archipelago는 준비된 지역 발견 카드입니다. 주행 동행과 동의 기반 방문은 실제 주차 상태에서 체험하는 자체 모의 화면입니다. 실제 기본 내비 오버레이·차량 간 통신·상시 음성 호출·실차 방문 인증을 구현했다고 설명하지 않습니다.

실제 Pleos는 별도 어댑터에서 목적지·경로·도착과 사용 제한을 검증합니다. SDK 입력과 DEMO 출처를 섞지 않으며, 실제 제한과 미확인은 모의 주행보다 우선합니다. 상세 여행 이력·사진 앨범·Car Care·서버·로그인·결제·런타임 LLM은 이번 범위에 없습니다.

## APK와 제출 상태

현재 APK는 없습니다. 파트 1이 서명 release를 실제 설치한 뒤 `apk/팀명_MEMORIDE_release.apk` 또는 GitHub Release 링크와 검증한 커밋·환경을 이 절에 기록합니다. 존재하지 않는 APK 링크를 만들지 않습니다. 심사용 private 접근과 공식 제출은 [인수 확인표](templates/acceptance_checklist.md)로 관리합니다.

## 협업과 비용

같은 Android app 모듈에서 기능 소유권으로 나눕니다. 기능끼리 직접 import하지 않고 공통 DTO·콜백·캐릭터 슬롯으로 연결합니다. 게임 저장은 파트 2만 씁니다. 작은 part 브랜치와 PR을 main에 일찍 합치고, 공통 파일은 파트 1이 조립합니다.

Actions의 push·PR 자동 실행은 없습니다. 저장소 Actions 비활성화를 우선하고, 필요하면 비용 통제를 확인한 뒤 수동 Ubuntu 검사만 사용합니다. 문서의 권장 설정이 이미 원격에 적용된 것은 아닙니다. 새 유료 서버·API·자산·AI Actions를 도입하지 않습니다. [저장소 적용 안내](REPO_SETUP.md)를 참고합니다.

## 문서와 자산

개발 원본은 MD·JSON·계약 초안입니다. DOCX와 문서 열람용 글꼴·공식 빈 접수양식은 배포 ZIP에 포함하며 초기 Git 커밋에서는 제외합니다. 기준 문서는 [컨셉 1.2](docs/Concept_Baseline_v1.2.md)·[브랜드 스토리](docs/BRAND.md)·[최종 경계 검토](docs/18_final_review.md)입니다. 기존 1.1 컨셉 원본은 ZIP의 references/frozen에 보존했습니다.

제공 PNG/SVG는 자체 도형 초안이며 실제 지역 제휴·공식 상품 인증을 의미하지 않습니다. 출처와 라이선스는 [자산 안내](assets/ATTRIBUTION.md), 외부 사실은 [공식 자료 목록](docs/sources.md)을 확인합니다. 전체 코드의 오픈소스 라이선스는 아직 정하지 않았습니다.
