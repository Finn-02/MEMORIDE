# 공개 준비 검토 — 2026-11-14 KST

## 후속 확인

아래는 공개 전 검토 당시의 기록이다. 이후 PM은 B 자산의 공개 배포 가능 여부와 커밋 작성자 정보 공개 동의를 확인했고, Actions Disable actions 선택 및 Save를 완료했다고 보고했다. 저장소/Release의 비로그인 열람과 APK 다운로드도 PM이 확인했다. 이번 최종 제출 점검에서 저장소 public 상태를 다시 확인했다. Actions 설정은 사용자 확인으로 기록하며 독립 API 검증을 주장하지 않는다. 이전 검토에서 남긴 과거 PNG 66개는 이번 public clone의 Git 객체에서 바이트를 확보·해시 확인하고 이미지/메타데이터를 추가 검토했다. 모두 캐릭터·아이템 등 렌더/도형 자산으로 개인 사진·연락처를 발견하지 못했다. 아래 표의 미검토 표기는 당시 기록이며 이번 후속 점검과 구분한다.

## 결과와 범위

검토 기준 main은 `9134e1ef332117e0099c016f68b5349064e0affb`다. signed release v6의 게시·크기·GitHub SHA-256 digest는 전달 자료와 일치한다. 빌드 소스 태그 `v0.1.0-v6`는 `17bea5438af9ea7b4fb3f3a431a19ab33777adc5`를 가리킨다. 전체 제품 인수 HOLD는 유지한다.

현재 파일과 확인한 텍스트 이력에서 공개를 막는 실제 인증정보·개인정보 유출은 발견하지 못했다. 자동 패턴 점검과 주요 설정/코드 정적 검토 결과이며, 모든 취약점이 없다는 보증이나 침투 시험은 아니다. 이번 검토로 저장소 공개 범위를 변경하지 않았다.

| 대상 | 실제 점검 |
| --- | --- |
| 현재 main 541개 파일 | 전부 바이트 확보 후 Git blob SHA 일치. 소스·설정·문서의 전체 텍스트 패턴 점검, 이미지 메타데이터와 설계도/와이어프레임 확인 |
| 현재 브랜치·태그·PR ref의 도달 가능한 이력 | 155개 커밋·133개 고유 tree·934개 고유 blob의 경로/종류 확인. 텍스트 753개 전부 조회·바이트 해시 확인 및 패턴 점검 |
| 과거 바이너리 | 181개 중 115개를 제공된 APK/프로젝트 문서와 공식 원본에서 바이트 대조. 나머지 66개는 과거 PNG로 경로·크기만 확인했고 이미지 바이트/화면은 미검토. 삭제된 ref의 도달 불가능 객체는 범위 밖 |
| PR/Issue 공개 내용 | 31개 PR의 제목/본문·일반 댓글 2개·Release 본문 점검. 조회된 inline review 댓글 0개 |
| Actions | 조회된 workflow run 0건. workflow는 workflow_dispatch만 사용, contents: read 및 persist-credentials: false. 설정의 실제 enabled/disabled·비밀 설정·ruleset은 연결에서 조회 불가해 미확인 |
| 실행 파일 | Gradle wrapper JAR는 공식 gradle/gradle v9.3.1 원본과 Git blob SHA 일치. Android/SDK 재실행이나 APK 재빌드는 하지 않음 |

개인키 PEM, GitHub/AWS/Google/OpenAI/Slack 토큰, JWT, 자격증명이 포함된 URL, 문자열 비밀번호/키 할당, 국내 휴대전화·주민등록번호 형식을 점검했다. 실제 키나 개인 연락처는 발견하지 못했다. 서명 인증서 지문·APK 해시·키 입력용 환경변수 이름은 비밀값이 아니다. 흔한 값 패턴만으로 모든 형식의 비밀을 탐지할 수 있는 것은 아니다.

## 확인·수정 사항

- release Manifest는 allowBackup=false, 정식 MAIN launcher만 exported=true다. 진단/개발 화면은 debug 소스에 분리돼 있다. release에 제한 우회·DO/overlay 선언을 추가하지 않았다.
- 서명 스크립트는 getpass와 자식 프로세스 환경변수를 사용한다. 실제 키·비밀번호·키 저장소 파일은 추적된 파일·이력 경로에서 발견되지 않았다.
- 현재 앱 데이터는 로컬 DataStore이며 로그인·자체 서버·사용자 API 키를 추가하지 않았다. 내비 로그는 상태만 기록하고 목적지명·좌표를 직접 로그 문자열에 출력하지 않는다.
- .gitignore에 개인키·SSH 키·자격증명 JSON·메모리 덤프 제외를 보완했다. 이 규칙은 이미 커밋된 비밀을 지우는 기능이 아니며 과거 이력은 별도로 점검했다.
- 개인 Mac 사용자명이 들어간 절대 저장 경로를 현재 사용자의 Downloads 표현으로 정리했다. 과거 Git 이력의 해당 경로 문자열은 남아 있으며 삭제/force push로 이력을 재작성하지 않았다.
- 초기 squash/브랜치 삭제 안내를 merge commit/파트 브랜치 유지로 수정했다. 심사용 Collaborator 초대 안내를 최신 공식 제출 기준으로 정리했다.
- 문서용 글꼴과 실제 APK의 3개 OFL 글꼴을 구분하고 기존 저작권·라이선스 원문 위치를 연결했다. 새 오픈소스 라이선스를 임의 부여하지 않았다.

## 공개 시 알아둘 사항

Git 이력의 작성자 이름·이메일은 공개된다. 검토한 커밋에는 GitHub noreply 외 일반 메일 주소도 있다. 이는 소스 본문의 연락처 노출과는 별개다. 앞으로의 커밋은 GitHub noreply 주소를 선택할 수 있지만 기존 작성자 정보가 소급 변경되지는 않는다. 이번 작업에서 메일 주소를 공개 문서에 추가하거나 이력을 재작성하지 않았다.

B의 캐릭터/섬 원본에 대한 과거 제작 경위·외부 재료의 최초 출처는 아직 확인 대기다. C 재렌더와 외부 텍스처 검사 기록으로 이를 대신하지 않는다. 알려진 침해 사실을 발견한 것은 아니지만 권리 확인 완료라고 표시할 근거도 없다. 제작 담당자가 팀 제작 또는 공개 배포 허용 근거를 확인해야 한다.

실제 GitHub 설정에서 Actions 비활성화 유지·병합 후 브랜치 자동 삭제 OFF·원하는 브랜치 보호 및 공개 전환 후 보안 기능 상태를 확인한다. 이 문서가 해당 설정을 적용한 것으로 해석하지 않는다. 공개 전환은 파일·커밋·PR/Issue·Release와 실행 이력을 함께 공개하므로 별도로 수행한다.

## 제출에 사용할 자료

- 저장소: https://github.com/Finn-02/MEMORIDE
- Release: https://github.com/Finn-02/MEMORIDE/releases/tag/v0.1.0-v6
- 다운로드: https://github.com/Finn-02/MEMORIDE/releases/download/v0.1.0-v6/4._MEMORIDE_release.apk
- APK 크기: 58,201,427 bytes
- APK SHA-256: `7f6f2ae9ee4cf74145368add969c8f978b5c5e8195a4fa9980d47273600cb311`

다운로드 이름과 한글 표시명 차이는 README와 Release에 안내돼 있으며 파일 바이트는 같다. 접수 메일에는 이 점검을 반영한 후 실제 최종 main 커밋을 기재하고 APK 빌드 소스와 구분한다. 개별 동의·서명 문서는 저장소에 올리지 않고 대표자/팀원 전원 서명·날짜와 CC를 공식 접수 자료에서 확인한다.

## 확인한 공식 자료

- [GitHub 공개 범위 변경과 영향](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/managing-repository-settings/setting-repository-visibility)
- [민감정보와 Git 이력 정리](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository)
- [공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission): 심사용 초대 불필요. 비공개 접수 시 11월 16일까지 공개하고 11월 23일까지 유지. 공개가 어려우면 마감 전 소스+APK 압축파일 다운로드 주소를 제출하는 대체 방법을 따른다.
