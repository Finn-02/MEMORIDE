# 저장소 초기 반영 상태

2026년 11월 1일 Finn-02/MEMORIDE의 private main에 개발 착수 자료 v1.2를 반영했다.

- 최초 README 커밋: fae16ffc1dd7f2628bf641e49330ffdefc32bd3c
- 전체 착수 자료 커밋: fdfbae99c2d6e6f6b94eb57002614140901d6758
- 전체 자료의 tree: e12be156a1e966a01fd999178823734f457a68ff
- 반영 파일: README·MD 명세·역할·계약 초안·JSON·Python 도구·자체 PNG/SVG·다이어그램·GitHub 협업 설정 119개
- 원격 tree의 파일 목록과 각 blob SHA를 로컬 파일 바이트와 대조해 일치 확인
- 이 상태 문서의 완료 기록은 위 자료 커밋 다음의 별도 문서 커밋으로 갱신

DOCX·ZIP·문서 열람용 TTF·공식 빈 동의서·캐시·서명키는 초기 커밋에서 제외하고 배포 ZIP으로 제공한다. 서명이나 개인정보가 입력된 양식은 저장소와 AI에 올리지 않는다. Android 앱·Gradle wrapper·APK는 아직 없으며 SDK와 양 OS 실행은 NOT RUN이다.

## PM이 실제로 확인할 설정

PM은 팀원 초대를 완료했다고 알렸다. 실제 수락·로컬 clone은 각 팀원이 확인한다. 파트 결정 후 정확한 GitHub ID를 config/team.json에 기록한다. Actions는 Settings → Actions → General에서 우선 Disable하고, 필요할 때만 비용을 확인한 뒤 수동 검사에 사용한다. 기본 브랜치·squash merge·리뷰 규칙은 REPO_SETUP.md를 따른다. 계정에 없는 유료 보호 기능을 전제로 하지 않는다.

이번 작업에서는 collaborator 초대, 저장소 Actions 활성/비활성, 예산·결제·브랜치 보호 설정을 변경하지 않았다. 제공 workflow는 수동 실행 전용이며 push/PR 자동 트리거가 없다. 실제 실행 건수는 배포 직전 원격에서 별도 확인한다.

## 개발 시작 안내 보완

같은 1.2 범위를 유지하며 README를 팀원 시작 설명서로 개편했다. 설치·인증·Mac/Windows 명령·로컬 AI·파트별 첫 작업·Git 일상 흐름·A01 공용 빌드 전달 기준과 EditorConfig를 추가했다. 기존 119개 파일은 초기 자료 반영 시점의 수치다. 현재 파일 수는 Git tree를 따른다.

공통 Android 도구 버전은 아직 검증 전이며 네 PC의 설치가 완료된 상태가 아니다. 이번 온보딩 변경은 실제 앱·wrapper를 생성하지 않는다. 원격 문서는 이전 DOCX·ZIP보다 최신이며, 원래 컨셉·기능 경계·노력 2:2:2:1은 유지한다.
