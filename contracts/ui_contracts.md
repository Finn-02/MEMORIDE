# 화면 연결 계약

공통 입력과 콜백만 맞춘다. 아래는 Compose 공개 함수의 제안 서명이며 실제 코드는 아직 없다. D1 A01에서 A와 각 작성자가 이름·패키지만 확정한다. 화면 내부 ViewModel·컴포넌트·스타일은 작성자 자율이다. Modifier 등 Compose 타입을 순수 DTO 파일에 넣지 않는다.

| 작성자 | 공개 화면 | 입력 | 출력 |
|---|---|---|---|
| B | IslandScreen | GameSnapshot, Catalog, InteractionGate | suspend GameAction → MutationResult, onPrepareJourney, onOpenWorld, onOpenDemo |
| B | InventorySheet | GameSnapshot, Catalog, InteractionGate | 같은 게임 명령 콜백, onClose |
| B | AvatarView | AvatarPreview, Catalog, expressionKey, Modifier | 그림만 표시, 저장 부작용 없음 |
| C | CompanionScreen | JourneySnapshot, AvatarPreview, gate, avatarSlot | onBack, onHideChanged |
| C | WorldScreen | GameSnapshot, Catalog, gate | onSelectRegion(regionId), onOpenItems |
| C | VisitScreen | C의 로컬 VisitState, gate, avatarSlot | C 내부 요청·수락·거절·철회·닫기 |
| C | DemoPanel | JourneyPort, DemoControlPort, 초기화 콜백, gate | 여정 명령과 상태 표시 |
| PM | BriefingScreen | ArrivalReceipt, Catalog, AvatarPreview, gate | suspend onDismiss/onOpenItems → MutationResult |

공유 AvatarSlot은 `@Composable (AvatarPreview, String, Modifier) -> Unit` 모양이다. AppRoot가 B의 AvatarView를 넣는다. C가 B 패키지를 import하지 않는다. expressionKey는 idle, happy, eat만 사용하고 알 수 없는 값은 idle로 표시한다. C는 초기에는 회색 Box를 슬롯에 넣어 진행할 수 있다. renderer가 이용 제한을 해제하지 않으며 움직임은 C 또는 B가 현재 gate에 맞춰 멈춘다.

게임 명령 콜백은 `suspend (GameAction) -> MutationResult`로 두면 음식 사용 성공 이후에만 B가 짧은 식사 연출을 시작할 수 있다. 식사 연출은 일시 UI 상태이며 저장하지 않는다. A가 이 콜백 안에서 최신 gate·foreground를 검사한 뒤 실제 GameService.apply를 연결한다. GameService 자체를 모든 화면에 넘기지 않는다.

onSelectRegion은 A가 목적지 선택으로 연결한다. C는 지역 이름·좌표·보상 정책을 다시 작성하지 않고 Catalog를 읽는다. 브리핑 닫기와 아이템 열기의 acknowledgeReceipt 및 화면 이동은 A가 연결한다. PM 화면이 저장 함수를 호출하지 않는다.

샘플: `fixtures/ui_samples.json`. 실행 시 B의 상태가 로드되기 전에는 Loading을 보여주고, 샘플을 실제 사용자 상태로 표시하지 않는다. 샘플은 Preview·테스트·별도 표시된 개발 화면에서만 사용한다.

필수 UI 공통 규칙: 로딩·빈 상태·오류·이용 제한을 구분하고, 외부 효과는 Compose 본문에서 직접 실행하지 않는다. 모드·화면 전환 때 중복 listener를 만들지 않는다. 복귀 시 저장 상태에서 다시 그린다. 고정 px 대신 dp와 유연한 레이아웃을 사용한다.

DemoPanel은 dispatch를 await한 뒤 상태를 확인한다. Complete/Repeat는 A가 게임 저장 처리까지 기다린다. 같은 도착 반복에서는 phase가 바뀌지 않아도 원본 명령을 실제 재전달해야 한다. 저장 실패는 Failed이며 새 지급 0을 곧 성공으로 판단하지 않는다.


## 1.2 연결 보완

IslandScreen의 onPrepareJourney는 S03을 여는 () -> Unit 콜백이다. WorldScreen의 onSelectRegion은 목적지 후보만 채우고 S03에서 최종 확인한다. 선택만으로 완료·보상을 처리하지 않는다.

BriefingScreen의 onDismiss와 onOpenItems는 각각 suspend () -> MutationResult다. 화면은 클릭 job에서 처리 중 상태를 두고 중복 실행을 막는다. A가 현재 gate·foreground·표시 중 journeyId를 검사하고 acknowledgeReceipt의 성공 뒤에만 라우팅한다. 실패 시 PM은 화면과 재시도 선택지를 유지한다. 뒤로 가기도 같은 닫기 경로다. 오래된 ID는 새 pending을 지우지 않는다.

초기화에서 잠금과 cancel/join의 순서는 docs/18_final_review.md를 따른다. common 계약의 데이터 필드와 기존 Kotlin 초안은 이름 변경만으로 바꾸지 않았다.
