# 저장소 설정과 공개 제출

이 파일은 사람이 적용할 절차다. 초기 개발 기록은 REPOSITORY_STATUS.md, 현재 APK와 실행 안내는 README.md를 따른다. 2026-11-14 공개 준비 점검 당시 저장소는 private이며, 공개 전 점검 범위와 남은 확인은 [공개 준비 검토](docs/PUBLICATION_REVIEW_20261114.md)에 기록한다. 설정 파일만으로 실제 GitHub 설정이 적용됐다고 판단하지 않는다.

## PM 준비

1. https://github.com/Finn-02/MEMORIDE 저장소의 현재 공개 범위와 기본 브랜치를 확인한다. 초기 자료를 다시 git init하거나 중복 업로드하지 않는다. 공개는 아래 제출 안내와 점검 결과에 맞춰 진행한다.
2. 이미 보낸 초대의 수락 여부와 A·B·C의 정확한 GitHub ID를 확인한다. 재초대가 필요한지는 실제 접근 실패 때만 확인한다. 개인정보 동의서는 저장소에 올리지 않는다.
3. Settings → Actions → General에서 이 저장소 Actions를 우선 Disable한다. 다른 저장소나 계정 전체 결제 설정은 바꾸지 않는다.
4. main을 기본 브랜치로 사용하고 **Create a merge commit**으로 병합한다. 파트 브랜치는 유지하며 자동 삭제·자동 병합·force push를 사용하지 않는다. 계정이 지원하면 main 보호·리뷰를 적용하되 유료 기능을 전제로 하지 않는다.
5. 역할과 리뷰 짝 A↔PM, B↔C를 정한다. CODEOWNERS.example은 참고용이며 강제 기능이 아니다.

## 파트 1의 공통 앱 추가

현재 원격 main을 개발 기준으로 사용한다. 기존 ZIP은 오프라인 스냅샷이다. 이미 반영된 저장소 위에 ZIP을 덮어쓰거나 git init을 반복하지 않는다. [A01 전달 기준](docs/BOOTSTRAP_HANDOFF.md)을 따른다.

현재 자료가 반영된 main을 clone하고 작은 브랜치에서 A01을 시작한다. ZIP은 오프라인 배포용 스냅샷이며 개발 기준은 같은 버전의 원격 MD다.

```bash
git clone https://github.com/Finn-02/MEMORIDE.git
cd MEMORIDE
git config --local core.autocrlf false
git switch -c part/a-a01-bootstrap
```

기존 원격에 force push하지 않는다. DOCX·ZIP·문서용 TTF는 초기 Git 대상에서 제외하고 MD와 앱 자산을 개발 원본으로 쓴다. 기준서 원본은 배포 ZIP의 references/frozen에 보존한다.

Android Studio가 기존 문서를 덮어쓰지 않게 잠깐 별도 빈 폴더에서 앱을 만든 뒤 Android 프로젝트 파일만 저장소 루트로 옮기는 방법을 쓸 수 있다. 완성된 제품 프로젝트는 app 모듈 하나이며, 문서·content·fixtures를 삭제하거나 중첩 프로젝트로 만들지 않는다. wrapper 4개 파일을 커밋하고 Windows에서 실행 비트도 기록한다.

```bash
git update-index --chmod=+x gradlew
```

공통 계약은 app의 core/contract로 이동한 뒤 컴파일한다. contracts/KailandContracts.kt는 이 시점부터 참고용이라고 표시하거나 제거해 두 기준이 따로 발전하지 않게 한다. UI 공개 연결 표면도 D1에 확인한다. 개인 local.properties·JDK 경로·서명키는 커밋하지 않는다.

content와 fixtures는 Gradle sourceSets 또는 작은 복사 과정 중 하나로 앱 assets에 포함한다. 원본을 두 군데 수작업 수정하지 않는다. B의 PNG는 Android resource 규칙에 맞춰 매핑한다. 첫 debug 빌드 뒤 config/toolchain.json의 검증 기록과 ENVIRONMENT를 채워 공유한다.

## 팀원 시작

팀원은 [README](README.md)에서 시작해 [온보딩](docs/ONBOARDING.md)·[로컬 AI](docs/LOCAL_AI_SETUP.md)·[첫 작업](docs/FIRST_TASKS.md)을 순서대로 따른다. A01 전에는 기초 설정·정적 검사·내부 준비를 하고, A01 이후 같은 프로젝트를 받아 Android 검사한다. IDE 자동 업그레이드 제안은 혼자 적용하지 않는다.

## 수동 Actions를 쓰기로 할 때만

현재 계정의 포함량·다른 저장소 사용·초과 중단 설정을 확인한다. 불확실하면 계속 비활성화한다. 사용을 허용하면 workflow token은 read만, PR 생성·승인 권한은 끈다. 제공 workflow는 main에 둔 뒤 수동으로 실행한다. Android는 toolchain.json의 실제 기록이 있어야 한다. 서명·APK 업로드는 로컬에서 수행한다.

## 제출

2026-11-14 확인한 [공식 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)에 따르면 심사용 계정을 Collaborator로 초대할 필요가 없다. 비공개로 접수할 수 있으나 11월 16일까지 Public으로 전환하고 11월 23일까지 공개 상태를 유지한다. 공개가 어려우면 마감 전 소스+APK 압축파일 다운로드 주소를 접수 메일로 보내는 대체 방법을 따른다.

접수 메일에는 저장소 URL·최종 제출 커밋·실제 Release 페이지를 기재한다. 현재 v6의 빌드 소스와 제출 문서가 있는 최종 main 커밋은 구분한다. 개인정보·개별 동의·서명은 운영용 접수 자료로 관리하며 저장소에 올리지 않는다. 팀원 전원 CC와 제출 항목은 [인수 확인표](templates/acceptance_checklist.md)에서 확인한다. 실제 메일 발송·공개 전환을 확인하기 전에 완료로 기록하지 않는다.


앱 런처 라벨은 MEMORIDE, 내부 applicationId 기본안은 io.kailand다. 이름 변경 때문에 데이터 ID·저장 키·공통 package를 일괄 치환하지 않는다. 실제 파트 배정은 config/team.json의 null을 채우고 카드·작업 ID는 유지한다.
