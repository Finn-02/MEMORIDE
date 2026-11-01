# private 저장소 초기 적용

이 파일은 사람이 적용할 절차다. Finn-02/MEMORIDE private 저장소는 확인했다. 초기 반영 결과는 REPOSITORY_STATUS.md를 따른다. 팀 초대·Actions 비활성화·비용·merge 설정은 아래 절차대로 PM이 실제 화면에서 확인한다.

## PM 준비

1. https://github.com/Finn-02/MEMORIDE 저장소가 private인지 확인한다. 이 배포의 초기 자료가 이미 반영되었다면 새로 git init하거나 같은 파일을 중복 업로드하지 않는다.
2. A·B·C의 정확한 GitHub ID를 확인해 collaborator로 초대한다. 개인정보 동의서는 저장소에 올리지 않는다.
3. Settings → Actions → General에서 이 저장소 Actions를 우선 Disable한다. 다른 저장소나 계정 전체 결제 설정은 바꾸지 않는다.
4. main 기본 브랜치와 squash merge·병합 후 브랜치 삭제를 설정한다. 계정이 지원하면 main 보호·리뷰 1개를 적용한다. Free private 지원을 가정하지 않는다.
5. 역할과 리뷰 짝 A↔PM, B↔C를 정한다. CODEOWNERS.example은 참고용이며 강제 기능이 아니다.

## 파트 1의 공통 앱 추가

ZIP 안 폴더의 내용물을 실제 저장소 루트로 사용한다. ZIP 자체와 바깥 중첩 폴더를 개발 루트로 커밋하지 않는다. 숨김 .github·.gitattributes·.gitignore도 포함한다.

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

clone → 로컬 줄바꿈 설정 → 같은 root를 Android Studio에서 열기 → 로컬 SDK/JDK 설정 → 정적 검사 → Android 검사 → 자기 part 브랜치 생성 순서다. 명령은 docs/15_cross_platform.md를 따른다. IDE 자동 업그레이드 제안은 혼자 적용하지 않는다.

## 수동 Actions를 쓰기로 할 때만

현재 계정의 포함량·다른 저장소 사용·초과 중단 설정을 확인한다. 불확실하면 계속 비활성화한다. 사용을 허용하면 workflow token은 read만, PR 생성·승인 권한은 끈다. 제공 workflow는 main에 둔 뒤 수동으로 실행한다. Android는 toolchain.json의 실제 기록이 있어야 한다. 서명·APK 업로드는 로컬에서 수행한다.

## 제출

공지된 심사용 계정은 이번 읽은 공개 문서에서 실제 ID를 찾지 못했다. 임의 ID를 만들지 말고 공지·운영 안내에서 확정한다. 심사용 작품 소개에는 개발자 실명·소속·연락처를 넣지 않고, 개인정보는 공식 운영용 신청서·동의서에서만 관리한다. 내부 개발 기준서와 역할 문서를 그대로 심사용 기획서에 붙이지 않는다. 서류·동의서·서명·팀원 CC, README, apk/팀명_앱이름_release.apk 또는 Release 링크, private 접근을 templates/acceptance_checklist.md로 확인한다. 아직 이 항목들이 완료됐다고 기록하지 않는다.


앱 런처 라벨은 MEMORIDE, 내부 applicationId 기본안은 io.kailand다. 이름 변경 때문에 데이터 ID·저장 키·공통 package를 일괄 치환하지 않는다. 실제 파트 배정은 config/team.json의 null을 채우고 카드·작업 ID는 유지한다.
