# 17 사실 확인 자료와 적용 경계

최초 확인일은 2026년 10월 30일, 이번 개발 착수 개정의 재확인일은 2026년 11월 1일이다. 공모전 공개 안내·Pleos 핵심 문서·OS·Git·비용 통제 자료를 재확인했다. 과거 저장소 사례의 SHA는 10월 30일 확인 기록이며 최신 커밋으로 재확인한 값이 아니다. 링크의 문서 내용은 바뀔 수 있으므로 실제 착수 시 설치 버전과 계정 화면을 확인한다. 아래 자료는 외부 사실의 근거이며, 이 명세의 역할·점수·시간·화면·데이터 구조는 팀을 위한 설계안이다. 본문 [Sxx]는 이 목록을 가리킨다.

| ID | 1차 출처 | 사용한 범위와 한계 |
|---|---|---|
| S01 | [공모전 심사와 시상](https://appdevchallenge.auto-hmg.io/guide/judging-awards) | 네 가지 1차 기준, 비공개 배점, 선택 산출물·PT 동작 요구. 우승 보장 근거 아님 |
| S02 | [공모전 지원과 제출](https://appdevchallenge.auto-hmg.io/guide/submission) | 2026-11-14 재확인: 소스·release APK·README·최종 커밋, 심사용 초대 불필요. private 접수 후 11/16까지 공개·11/23까지 유지 또는 소스+APK ZIP 대체 제출 |
| S03 | [Pleos 프로젝트와 SDK 신청](https://document.pleos.ai/docs/pleos-only/create-my-project) | Vehicle State·NaviHelper 신청 없이 사용, 음성 등 준비 상태. 실제 런타임 성공 아님 |
| S04 | [NaviHelper 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro) | 3.0.5, 내비 요청·조회·콜백·해제. 오버레이 허용 근거 아님 |
| S05 | [NaviHelper 이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners) | 경로 탐색과 목적지 도착 구분, 구현별 차이 |
| S06 | [NaviHelper 설치](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk) | Maven https://nexus-playground.pleos.ai/repository/maven-releases/ 및 ai.pleos.playground:NaviHelper:3.0.5 |
| S07 | [Pleos 주행 안전 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline) | CarUxRestrictions, 움직임 제한, 운전 중 게임 유도 방지. 안전 인증 아님 |
| S08 | [Pleos 앱 구조](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model) | Driving view 보호, 창 크기와 방향, 생명주기 |
| S09 | [Pleos 에뮬레이터 설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) | API 28 이상·Gradle 8 이상·2.1.0 안내, CPU별 설치는 실제 환경 확인 |
| S10 | [Android Studio](https://developer.android.com/studio) | 공식 개발·Preview 도구. 별도 유료 클라우드·AI 기능 도입 근거 아님 |
| S11 | [Android DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | 비동기·일관·트랜잭션 소규모 저장, 큰 관계 데이터는 다른 선택 검토 |
| S12 | [Android 시험 기초](https://developer.android.com/training/testing/fundamentals) | 지역·기기 시험과 동작·사용성 검증의 구분 |
| S13 | [Android 앱 서명](https://developer.android.com/studio/publish/app-signing) | APK 설치·업데이트의 서명 필요, 로컬 release 서명 |
| S14 | [GitHub 브랜치 보호](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches) | Free private에서 보호를 강제할 수 있다고 가정하지 않음 |
| S15 | [GitHub Code owners](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners) | 요금제별 지원, 예제 파일만으로 보호되지 않음 |
| S16 | [GitHub Actions 청구](https://docs.github.com/en/billing/concepts/product-billing/github-actions) | private 사용·저장 과금. 공식 페이지의 포함량 표와 compute allowance 표현이 다를 수 있어 특정 분 수를 비용 보장으로 사용하지 않음. 계정 화면 재확인 |
| S17 | [GitHub 예산과 중단](https://docs.github.com/en/billing/concepts/budgets-and-alerts) | 계량 제품의 사용 중단 설정과 알림 구분 |
| S18 | [agentsmd AGENTS](https://github.com/agentsmd/agents.md/blob/main/AGENTS.md) | 실제 파일 확인, 구체적 명령·작은 지침 구조만 참고. SHA 3cf929b91208081e40df9dec3835e4ff54bf4dc4 |
| S19 | [Next.js AGENTS](https://github.com/vercel/next.js/blob/canary/AGENTS.md) | 앞 150줄 확인, 구조·관련 문서·작은 범위 검증 참고. SHA 8e40378f6632fa27a043eb23a06a2d2c6a8ad3c4 |
| S20 | [Claude Code CLAUDE](https://github.com/anthropics/claude-code/blob/main/CLAUDE.md) | 실제 파일 확인, 워크플로 권한·비밀 최소화 원칙. 전용 runner·AI Actions는 복사하지 않음. SHA 410ad0f67663043920889a2b4cc380867acc1e3b |
| S21 | [OpenAI AGENTS 안내](https://learn.chatgpt.com/docs/agent-configuration/agents-md) | 저장소 지침과 경로별 규칙 |
| S22 | [Claude 프로젝트 지침](https://code.claude.com/docs/en/memory) | CLAUDE.md에서 @AGENTS.md import, 자동 로딩과 버전 차이 |
| S23 | [GitHub 수동 트리거](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow) | workflow_dispatch 수동 실행 |
| S24 | [OpenAI 저장소 지식 관리 사례](https://openai.com/index/harness-engineering/) | 큰 지침 하나 대신 지도와 연결된 문서. 사례의 생산성 수치를 팀 효과로 전용하지 않음 |
| S25 | [현대차 이전 앱 아이디어 소개](https://www.hyundaimotorgroup.com/ko/story/hyundai-motor-company-pleos-connect-app-idea-contest) | 차량 펫·지역 미션·소통 등 유사 요소의 공개 이력. 지금 대회의 평가 배점과 구분 |
| S26 | [Android Studio 설치 요구사항](https://developer.android.com/studio/install) | Mac·Windows·CPU·메모리와 가상화 요구. 모든 팀 PC에서 설치 성공했다는 뜻 아님 |
| S27 | [Android 빌드의 Java](https://developer.android.com/build/jdks) | IDE Gradle JDK·JAVA_HOME·Daemon JVM criteria 구분 |
| S28 | [Gradle 빌드 환경](https://docs.gradle.org/current/userguide/build_environment.html) | JVM 설정·로컬 경로, 임의 버전 조합 보장 아님 |
| S29 | [Git gitattributes](https://git-scm.com/docs/gitattributes) | text·eol·LF/CRLF·바이너리 처리 |
| S30 | [Pleos 앱 카테고리](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category) | 핵심 목적별 분류와 주행 제한. Entertainment는 우리 설계 제안 |
| S31 | [공모전 일정](https://appdevchallenge.auto-hmg.io/guide/schedule) | 11월 15일 마감일·PT·전시 일정. HTML 일정은 날짜만 안내, 23시 59분은 S40 공식 양식에서 확인 |
| S32 | [출품 유의사항 공지](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000006) | 사실성·권리·PT 시연·행사 의무. 법률 검토 의견이 아닌 운영 안내 확인 |
| S33 | [저장소 Actions 설정](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository) | 저장소별 비활성·token 권한. 설정은 PM이 실제 적용해야 함 |
| S34 | [GitHub 예산 설정](https://docs.github.com/en/billing/how-tos/set-up-budgets) | 저장소 또는 계정 범위·알림과 사용 중단의 차이 |
| S35 | [Actions 비용 개념](https://docs.github.com/en/actions/concepts/billing-and-usage) | private 포함 사용량과 초과 청구. 해당 계정의 남은 무료량 미확인 |
| S36 | [Android Gradle Plugin](https://developer.android.com/build/releases/about-agp) | 선택한 AGP의 Gradle·JDK 호환표 확인 위치 |
| S37 | [Pleos 주행 규제 구현](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide) | CarUxRestrictionsManager와 변경 listener. 실제 앱 구현·컴파일은 별도 |
| S38 | [Pleos 지역별 주행 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy) | DO 미선언 Parking/Drive 기본 정책·지역 차이 |
| S39 | [Pleos Virtual Console](https://document.pleos.ai/en/docs/pleos-only/app-test-guide/virtual-console-guide) | 가상 기어 P/R/N/D 등 시험 입력. 앱에 가짜 제한 해제 버튼을 넣는 근거 아님 |
| S40 | [공식 접수양식 DOCX](https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx) | 11월 1일 직접 다운로드·본문과 표 확인. 23시 59분 마감, 운영·심사 자료 분리, 개발계획 5번과 개별 서명. SHA256 07be893d58b0e96ab7788f0d189d6829f0e59a2cee4bc18cb90ec37eeda2f5fd |
| S41 | [NanumGothic 원본과 OFL](https://github.com/google/fonts/tree/main/ofl/nanumgothic) | 문서 열람용 무료 글꼴과 OFL.txt 동봉. 게임 PNG의 자체 도형과 구분 |
| S42 | [현대자동차 공식 기업이념](https://www.hyundai.com/kr/ko/info/ideology) | 2026-11-01 확인. 자동차에서 삶의 동반자로 비전. 현대를 시간으로 해석하는 내용은 팀의 기획 관점 |

## 첨부 자료

- L01: 웹서비스 기획 1, 45쪽. 비즈니스 모델·기획 사이클·시장과 고객·여정의 전체 텍스트와 도표 배치 검토.
- L02: 웹서비스 기획 2, 25쪽. UX·정보 구조·와이어프레임·플로우·스토리보드 검토.
- L03: 웹서비스 기획 3, 49쪽. A/B·프로토타입·MVP·PM·WBS·선행 관계·위험 관리 검토.

강의의 원문과 이미지는 이 패키지에 포함하지 않았다. 본문에 적힌 인수 기준·구현량·시간·위험 우선도는 카일랜드 제약에 따른 새 설계안이다. 강의의 예제·그래프 수치로 팀의 성능이나 시장성을 주장하지 않는다.

## 아직 실측하지 않은 것

SDK 실제 호출 성공, Android 컴파일·APK 설치, 기기 성능, 사용자 수요, 리텐션·지역경제·갈등 감소 효과, 주행 안전, 세계 최초성. 제공된 콘텐츠 정적 검사 결과와 위 항목을 혼동하지 않는다.
