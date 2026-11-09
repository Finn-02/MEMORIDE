# UX-A01 · 각 파트 독립 개발 시작

## 이번 전달의 범위

A가 공통 Compose 테마·버튼·패널·화면 배경과 컴파일 가능한 화면 입력/콜백 제안, 독립 개발 예제를 준비했다. B/C/PM은 자기 feature 안에서 새 화면을 개발한다. 기존 `IslandScreen`, `WorldScreen`, `BriefingScreen`, `GameService`, `GameSnapshot`의 시그니처·저장 형식은 변경하지 않았다. 새 화면의 실제 AppRoot 연결과 제외 진입점 제거는 A의 다음 UX-A02~04 작업이다.

기준: 앞서 전달한 팀 공유 ZIP의 `UX_IMPLEMENTATION_ASSIGNMENTS_20261110.md`, Figma `xFJG4ZaZg3baP5KmxbTtcx`의 `MEMORIDE 화면`. 기존 문서와 충돌하면 최신 사용자 결정과 업무분담을 적용한다. 다른 파트 화면을 공통 UI 파일에 작성하지 않는다.

## 바로 사용하는 공통 UI

패키지 `io.kailand.core.ui`:

- `MemorideTheme`: 주간 코스탈 모닝 Material 색·기본 글자 스타일. Android의 한국어 글꼴 대체를 사용한다. Noto Sans KR TTF를 새로 묶거나 원격으로 요청하지 않았다.
- `MemorideColors`, `MemorideBrushes`, `MemorideDimens`: 공통 토큰. 기능 전용 아이템 카드·지도·섬 장면은 각 담당자가 구성한다.
- `MemorideScreen(modifier, applyInsets, content)`: 화면 전체 그라데이션과 safeDrawing insets. AppRoot에서 한 번 감쌌다면 하위 화면에서는 `applyInsets=false` 또는 이 셸을 중복 사용하지 않는다. 56dp 시스템 상태바를 앱에서 다시 그리지 않는다.
- `MemoridePanel`: 패널과 내부 간격. 고정 높이를 강제하지 않는다.
- `MemoridePrimaryButton(text, onClick, modifier, enabled, supportingText, leading)`: 주요 행동. 화면에 주요 행동 하나. 최소 88dp, 글자가 커지면 높이가 늘어난다.
- `MemorideSecondaryButton(text, onClick, modifier, enabled, leading)`: 보조 행동. 최소 72dp.
- `MemorideIconButton(contentDescription, onClick, modifier, enabled, icon)`: 최소 56dp, 읽어 줄 이름 필수. 내부 아이콘의 설명은 중복하지 않는다.
- `MemorideTextButton(text, onClick, modifier, enabled)`: 닫기 등 약한 행동. 최소 72dp.

`leading`은 선택적인 Composable 슬롯이다. 새 공통 UI를 쓰기 위해 모든 아이콘 제작을 기다리지 않는다. 이번에 내려받은 Figma 출발·인벤토리·닫기 원본은 [아이콘 출처](ux_a01_assets/README.md)를 참고한다.

배치는 현재 사용 가능한 **dp 창 너비와 글자 배율**로 결정한다. Figma 1280/640/720px를 기기 해상도나 dp로 고정하지 않는다. 상위 고정 높이로 버튼/글자를 자르지 말고 콘텐츠 화면에서 스크롤을 제공한다. `WideBreakpoint=840dp`는 이번 공통 UI 예제의 2열 기준이며 각 feature의 정보량에 맞춰 더 좁거나 큰 글자에서는 1열로 전환할 수 있다.

## 화면 연결 제안: 소비자 확인 후 실제 연결

정확한 컴파일 선언은 `core/ui/NewUxScreenSlots.kt`, 사용 예제는 debug의 `core/ui/NewUxSlotExamples.kt`에 있다. **추가 제안이며, A와 해당 소비자 확인 없이 기존 공개 화면 함수를 교체하지 않는다.** 내부 화면 개발은 이 입력을 받는 새 Content/Preview로 바로 시작한다. 예제는 저장·SDK·다른 feature를 직접 참조하지 않는다.

| 담당 | 슬롯 제안 | 담당자가 할 일 |
| --- | --- | --- |
| B | `CharacterSelectionSlot` | 기존 캐릭터 ID·외형으로 3카드 구현. `onSelect(id)` await 중 입력 차단. 실패 시 화면 유지. A가 B 저장 성공 후에만 섬으로 이동한다. |
| B | `NewIslandSlot` | 섬 장면·성장·장식 표시. A의 `IslandDestinationSlot`을 배치. `replacesActions=true`일 때 캐릭터 선택/새 출발/인벤/월드 버튼을 모두 숨긴다. false의 안내 카드에서는 기본 메뉴를 유지한다. |
| B | 기존 인벤토리 명령 계약 | 현재 `GameAction` / suspend `MutationResult` 사용. A 목적지·SDK 코드를 import하지 않는다. |
| C | `NewWorldSlot` | snapshot/catalog/history/gate와 닫기 콜백만 받는다. 상세 선택은 C 내부 상태. 인벤토리 이동·목적지 요청을 추가하지 않는다. |
| PM | `NewBriefingSlot` | 확정 영수증·현재 외형·두 비동기 콜백. 섬 또는 캐릭터 선택은 A 콜백의 ack 성공 뒤 이동. 기존 `onOpenItems`에 캐릭터 선택 의미를 덮어씌우지 않는다. |
| PM | `ArrivalSaveSlot` | Saving/Failed 표시, retry/닫기 콜백만 호출. 저장/타이머 구현 없음. 실패 이유는 A 제공 문구 사용. |

슬롯은 호환 함수 타입이므로 각 feature 공개 Composable을 함수 참조 또는 람다로 A에 제공하면 된다. 예제 변수 `CharacterSelectionExample`, `IslandExample`, `WorldExample`, `BriefingExample`, `ArrivalSaveExample`가 실제 컴파일되는 최소 소비 예시다. 이 예제를 제품 화면으로 연결하지 않는다.

## B → A → C 지역 기록

추가 제안 DTO는 `core/contract/RegionVisitHistory.kt`다. GameService의 기존 구현 의무는 늘리지 않았다.

- `RegionVisitSummary`: regionId, visitCount, firstVisitedAtEpochMs, lastVisitedAtEpochMs.
- `RegionVisitHistory.Loading`: 아직 불러오지 않음. 0회/빈 지도로 단정하지 않는다.
- `Available(emptyList())`: 정상 조회한 빈 기록.
- `Unavailable(message)`: 조회 불가. 예시 날짜나 횟수로 채우지 않는다.

B는 고유한 **확정 REGION 영수증**으로 집계한다. `discoveredRegionIds`만 보고 방문 횟수/날짜를 만들지 않는다. A가 state를 받아 C에 전달한다. B가 실제 구현을 전달하기 전에는 live에서 Loading/Unavailable을 사용하고, 명시적 Preview에만 샘플을 쓴다. 소비자가 날짜를 출력할 때 UTC epoch → Asia/Seoul → `yyyy.MM.dd (E)`로 표시한다. DEMO도 포함할 수 있는 게임 기록이며 실제 방문 인증이 아니다.

성장 규칙 변경은 B가 별도로 구현한다: 등록 REGION 도착 확인 시 KST 하루 최초 +1, 총 상한3, 최초 지역 선물과 원자 저장. A는 DAILY 명령을 추가하지 않는다. 이번 starter가 성장 규칙 구현 완료를 뜻하지 않는다.

## A가 이어서 연결할 항목

1. 소비자별 입력/콜백 확인 뒤 새 화면을 AppRoot에 연결. 콜드스타트 S01 / 같은 프로세스 내비 복귀 시 목적지 우선.
2. 새 내비 출발·실행 실패, 목적지 상태·도착 확인·등록 지역 판별 연결.
3. 기존 게임 명령 조정 경로에서 최신 foreground/gate/generation/화면 버전을 검사. 선택 저장 성공·결과 ack 성공 후에만 이동.
4. 저장 응답 전 결과 화면 금지. retry는 원본 ID·내용 유지, 닫기도 원본 재시도 상태 보존.
5. 새 사용자 화면과 네 시연에서 동행/방문/DAILY/범용 모드 행/기존 준비 화면 진입 제거. 기존 저장·영수증은 보존.

## 독립 확인 방법

1. 공통 프로젝트와 기존 part 브랜치를 유지한다. APK나 별도 Android 프로젝트로 화면을 개발하지 않는다.
2. starter의 공통 UI 파일을 받은 뒤 Android Studio에서 `UxFoundationPreview.kt`의 넓은/좁은/세로/411dp 글자2.0 Preview를 실행한다.
3. 일반 Android debug의 개발 DEMO 진입점 → `UX-A01 공통 UI 확인`에서도 버튼을 시험할 수 있다. 게임 저장이나 실제 내비를 호출하지 않는 예제다. Automotive에서는 기존 development gate가 계속 UNKNOWN으로 차단한다.
4. 자기 화면은 본인 feature Preview/fixture로 개발하고 공개 함수와 입력·콜백 예시를 PR에 적는다. A 소유 AppRoot·공통 계약은 동시에 수정하지 않는다.

## 현재 제한

이 starter는 전체 앱 UX 교체 완료가 아니다. 기존 제품 화면/제외 진입점은 아직 남아 있다. Figma와 동일한 Noto Sans KR 글꼴, 정밀 그림자·모든 글자 대비·실제 Pleos 물리 크기는 최종 인수 전 확인이 필요하다. 원본 주요 버튼 그라데이션의 밝은 끝 대비 문제는 후속 보정에서 OceanAccessible #267493과 불투명 흰 글자로 조정했다. 해당 색상 쌍의 최소 계산 대비는5.25:1이며 전체 화면 대비/물리 글자 검수 완료를 뜻하지 않는다. 실제 Pleos 창 측정 및 적용 기준은 UX_A01_PLEOS_WINDOW_BASELINE.md를 따른다. 주행·안전·SDK·서명 release 검증은 별도다.

## 이번 검증

- Windows AMD64, Python 3.10, 기록된 JBR25 / Gradle wrapper9.3.1.
- `scripts/check_all.py`: exit 0, 정적 검사와 Python 59개 PASS.
- 공유할 코드 커밋 `16d3d8e`만 분리한 깨끗한 작업트리: `gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin` exit 0. JUnit **264개**, 실패/오류/건너뜀0, debug APK 및 release Kotlin 컴파일 PASS. 정적 검사/Python59도 별도 PASS.
- 기존 미커밋 내비 작업을 포함한 A 원래 작업트리에서는 같은 명령으로 JUnit **276개**와 debug/release 컴파일 PASS. 이 276개를 공유 커밋만의 시험 수로 표시하지 않는다.
- 일반 Android API37 AVD: 주요 버튼 반응, 보조 버튼 반응, 비활성 버튼 무반응, 약411dp/글자2.0 스크롤 도달, 1280×720·density160 가로 렌더링 확인. 글자 배율과 표시 크기는 검수 뒤 원래 값으로 복원.
- NOT RUN: 다른 PC, 전체 새 flow 연결, 새 성장 규칙, 모든 feature 화면 회귀, Pleos·실차, 서명 release 설치, 물리 크기 및 플랫폼 전체 대비 인수.
- 기존 콘텐츠/시험은 아직 DAILY 성장 규칙을 포함한다. 이번 59/276 PASS를 새 REGION 성장 규칙의 검증으로 해석하지 않는다.

후속 업데이트: [실제 Pleos 창 기준·공통 대비 보정](UX_A01_PLEOS_WINDOW_BASELINE.md). 아래 기존 커밋 검증 수와 구분하여 후속 코드의 JUnit267(기존264+대비3) 및 실제 확장/분할 창 증거를 기록한다.
