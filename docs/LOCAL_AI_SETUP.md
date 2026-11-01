# Codex·Claude 로컬 개발 시작

**둘 중 익숙한 도구 하나를 주 도구로 선택한다.** 둘을 모두 설치하거나 동시에 같은 폴더를 수정할 필요가 없다. 개인 AI 구독의 실제 사용 가능 여부·한도는 본인 계정에서 확인한다. 앱에는 AI API·API 키·LLM 호출을 추가하지 않는다.

## 편집과 실행의 관계

Android Studio에서 MEMORIDE 루트를 열어 Gradle Sync·Compose Preview·에뮬레이터 실행을 한다. Codex·Claude의 로컬 앱/확장/CLI도 같은 루트를 열어 파일을 편집한다. 한쪽에서 고친 파일을 다른 쪽에서 그대로 읽으므로 별도 복사본이나 별도 앱을 만들지 않는다. IDE별 창은 달라도 된다.

| 선택 | 진행 방법 | 공식 안내 |
|---|---|---|
| Codex IDE 확장 | 지원 편집기에 공식 확장 설치 → 본인 계정 로그인 → clone한 폴더 열기 → 로컬 작업 선택 | [IDE](https://developers.openai.com/codex/ide) |
| Codex 로컬 앱 또는 CLI | 공식 설치 안내에서 OS별 방법 선택 → 저장소 루트를 작업 폴더로 지정. CLI라면 루트에서 `codex` | [시작](https://developers.openai.com/codex/quickstart), [CLI](https://developers.openai.com/codex/cli), [Windows](https://developers.openai.com/codex/windows) |
| Claude Code VS Code 확장 | 공식 확장 설치 → 본인 계정 로그인 → 같은 폴더에서 로컬 작업 | [VS Code](https://code.claude.com/docs/en/vs-code) |
| Claude Code CLI | 공식 native 설치 안내 → 새 터미널 → `claude --version` → 저장소 루트에서 `claude` | [설치](https://code.claude.com/docs/en/setup) |

2026-11-01 공식 안내를 확인했다. Windows 네이티브 경로를 우선하며 WSL은 팀 필수가 아니다. native 설치·IDE 경로를 선택한 사람에게 npm용 Node를 요구하지 않는다. 설치 방법은 변경될 수 있어 버전 번호나 실행 정책 우회 명령을 이 문서에 고정하지 않고 공식 설치 페이지를 따른다. AI 도구 버전은 앱의 Gradle 버전과 별개다.

로그인은 본인 구독으로 가능한 경로를 먼저 선택한다. CLI가 API 결제를 요구하면 API 결제를 켜는 대신 구독 지원 방식·로그인을 확인한다. 개인 토큰·구독 계정·API 키를 팀 공용 저장소에 넣지 않는다.

## 첫 세션

1. 변경할 파일이 없는지 `git status`로 확인하고 [작업 브랜치](GIT_DAILY.md)를 만든다.
2. AI가 [AGENTS.md](../AGENTS.md)를 읽게 한다. Claude용 [CLAUDE.md](../CLAUDE.md)는 같은 지침을 참조한다. 자동으로 읽었다고 가정하지 말고 첫 요청에 명시한다.
3. [역할별 첫 작업](FIRST_TASKS.md)에서 자기 파트 프롬프트를 복사한다. 자기 역할·해당 작업·관련 계약만 먼저 읽게 한다.
4. AI가 변경 경로·입력/출력·최소 구현·검증을 짧게 정리하고 바로 구현하도록 한다. PM의 함수별 승인 대기를 만들지 않는다.
5. diff와 실제 검사 결과를 사람이 확인하고 commit·push·작은 PR을 만든다. GitHub에 AI 앱·자동 리뷰·Actions를 설치할 필요가 없다.

## 자율 설계와 요청 기준

담당자가 내부 ViewModel·함수·컴포저블·상태 표현·작업 쪼개기·일반 버그 수정을 결정한다. 정해진 공개 타입·동작·소유 경로를 지키는 내부 결정은 PM 승인 대상이 아니다. 작게 시작해 한 기능씩 확인하고 필요할 때 [상태 기록](status/)에 현재 완료·다음 작업·막힘만 갱신한다.

공유 계약을 바꿔야 하면 A와 영향받는 소비자 한 명이 [요청 템플릿](../templates/communication.md)으로 확인한다. 그동안 현재 계약의 fake로 계속할 수 있다. 핵심 범위 변경·새 비용·실제 제한 완화·저장 비호환·1일 이상 지연만 PM 판단으로 올린다. 대기 중에 깨지는 계약을 먼저 반영하지 않는다.

## 토큰과 충돌 줄이기

- 한 번에 WBS 작업 하나 또는 작은 수정 하나를 맡긴다. 전체 채팅·ZIP·모든 명세를 매번 읽히지 않는다.
- 작업 ID와 경로·기대 결과·실패 로그의 핵심 부분을 준다. 전체 Gradle 로그·캐시·APK·모든 자산을 컨텍스트에 넣지 않는다.
- 같은 작업 폴더에서 Codex와 Claude를 동시에 편집시키지 않는다. 도구를 바꿀 때 먼저 이전 편집을 끝내고 상태·diff를 확인한다.
- 도구를 바꿀 때 현재 SHA·변경 파일·검사 결과·다음 한 작업만 인계한다. 전체 구현을 반복 재작성하지 않는다.
- AI에 임의 dependency 추가·버전 업그레이드·다른 feature 리팩터링·유료 API 도입을 맡기지 않는다. 새 라이브러리가 꼭 필요하면 A와 호환성만 확인하고 공통 버전에 고정한다.
- 로컬 파일 편집 허용과 push·PR 승인·배포 권한은 구분한다. force push·main 직접 구현·자동 merge를 기본 지시로 주지 않는다.

검사 명령과 결과·NOT RUN·다음 한 작업을 마지막에 남기면 충분하다. 모든 내부 판단을 장문의 보고서로 만들지 않는다.
