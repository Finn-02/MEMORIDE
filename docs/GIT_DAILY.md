# 매일 사용하는 Git 흐름

**현재 팀 합의(2026-11-02): 브랜치는 파트 담당마다 하나씩 유지한다.** 기존 저장소의 기획·역할·범위·데이터/UI 계약을 기준으로 작업한다. 브랜치는 유지하되 작은 작업마다 커밋·PR로 자주 공유한다. PM이 전체 통합·최종 인수를 관리하며 A는 앱 연결·빌드를 구현한다. 작성자는 자기 오류를 수정한다.

아래 명령은 Mac Terminal과 Windows PowerShell에서 같다. 단, Python·Gradle 명령은 [환경 안내](ONBOARDING.md)의 OS별 명령을 쓴다.

## 파트 브랜치를 처음 만들 때

저장소 루트에서 `git status`로 현재 브랜치와 미커밋 변경을 먼저 확인한다. 변경이 있으면 해당 작업 브랜치에 작은 커밋으로 보존하거나, 의도적으로 stash한 뒤 복원 여부를 확인한다. 기존 작업을 버리는 `reset --hard`·`clean -fd`로 시작하지 않는다.

자기 파트 브랜치가 아직 없고 변경이 정리된 상태에서 처음 한 번만:

```text
git switch main
git pull --ff-only origin main
git switch -c part/b-b01-game-state
```

이름은 기존 파트 브랜치를 우선 사용한다. 다음은 초기 이름 예시다: `part/a-a01-bootstrap`, `part/b-b01-game-state`, `part/c-c01-companion`, `part/pm-p01-briefing`. 이미 있는 브랜치로 돌아가려면 `git switch 브랜치명`을 사용한다. `git switch -c`는 파트 브랜치 생성 때 한 번만 쓴다. A는 현재 `part/a-a01-bootstrap`을 A02 이후에도 사용하며 작업별 새 브랜치를 만들지 않는다.

## 현재 작업에 최신 main 받기

현재 브랜치의 변경을 먼저 커밋한 뒤 아래를 수행한다. A01이 공유되면 B·C·PM도 이 절차로 기반을 받는다.

```text
git fetch origin
git merge origin/main
```

다른 파트 브랜치를 차례로 직접 합치지 않는다. 공유할 결과는 main에 작은 PR로 먼저 반영한다. 충돌하면 해당 파일 소유자와 겹친 부분만 확인하고 검사 후 커밋한다. 중단하려면 merge 중이고 별도 미커밋 변경이 없음을 확인한 뒤 `git merge --abort`를 쓴다. force push는 필요 없다.

`pull.ff only` 때문에 분기된 main의 pull이 거절되면 자동으로 덮어쓰지 않는다. `git status`와 `git log --oneline --max-count=8`로 로컬 main에 작업 커밋을 만들었는지 확인하고, AI에게 그 커밋을 작업 브랜치로 보존하는 방법을 요청한다.

## 검증·commit·push·PR

1. 작업 중에는 변경 기능의 작은 시험을 수행한다.
2. PR 전에는 [공통 정적 검사](ONBOARDING.md)를 수행하고, Android 기반을 받은 뒤에는 영향받는 Android 시험·debug 빌드도 확인한다. 실제 실행하지 않은 항목은 NOT RUN으로 쓴다.
3. `git diff --check`, `git diff`, `git status`로 변경 범위를 본다. 수정한 파일 경로만 `git add 경로`로 올리고 `git diff --cached`를 확인한다. 실제 파일을 이해하지 않고 `git add .`로 로컬 파일 전체를 올리지 않는다.
4. `git commit -m "feat(B01): add initial game state"`처럼 작업 ID와 결과를 남긴다.
5. `git push -u origin HEAD`로 현재 브랜치를 올리고 GitHub의 Compare & pull request에서 **base: main**인 PR을 만든다. [PR 템플릿](../.github/pull_request_template.md)에 연결 함수 예제·검사·NOT RUN을 짧게 쓴다.
6. 기본 교차 리뷰는 A↔PM, B↔C다. 공유 계약은 A와 영향받는 담당자가 확인한다. PM의 통합 순서를 따르되 내부 설계마다 PM 검토를 요구하지 않는다. 병합은 프로젝트 담당자가 GitHub에서 수행한다.
7. 병합 후에도 같은 파트 브랜치를 유지한다. 미커밋 변경을 먼저 보존하고 `git fetch origin`, `git merge origin/main`으로 반영된 main을 받는다. PR은 다음 작은 작업의 변경만 담도록 확인한다. 파트 브랜치를 자동 삭제하거나 force push하지 않는다. GitHub의 **Create a merge commit**을 선택하고 파트 브랜치를 삭제하지 않는다. 이미 squash/rebase 병합했다면 중복 커밋이 보일 수 있으므로 다음 PR의 diff를 확인하고 이력을 임의로 덮어쓰지 않는다.

main은 A01 이후 최소 debug 빌드가 되는 상태를 목표로 한다. 통합 전용 장기 브랜치를 새로 두지 않는다. 작은 PR 병합을 일찍 반복하고 최종 main을 release 기준으로 고정한다.

## 공유 파일과 비용

| 파일·영역 | 기본 소유자 | 다른 파트의 처리 |
|---|---|---|
| Gradle·Manifest·AppRoot·공통 DTO·최소 테마 | A | 자기 feature만 수정. 필요한 연결 예제 전달 |
| 게임 상태·DataStore·보상·캐릭터 렌더러 | B | 공개 DTO·콜백·슬롯만 사용 |
| 동행·방문·월드·시연 UI | C | 게임 저장·실제 gate를 직접 수정하지 않음 |
| 브리핑·지역 Catalog·fixtures | PM | 데이터 ID·레이어 규격은 관련 담당자와 확인 |

Actions는 `workflow_dispatch`만 있고 push/PR 자동 실행은 없다. 로컬 검사를 기본으로 하며 PR마다 Actions를 누르지 않는다. 저장소 Actions 활성 여부·계정 비용 통제는 별도 설정이므로 자동 적용됐다고 가정하지 않는다. 새 봇·유료 CI·AI 자동 리뷰를 넣지 않는다.
