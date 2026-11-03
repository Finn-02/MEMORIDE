# C03 월드맵·지역 상세·수집 목록 준비

상태: 2026-11-03 C03 독립 화면·읽기 표시·후보 전달·시험 구현 완료. 실제 앱 연결·기기 통합 시험은 NOT RUN.
기준: C 브랜치 `97fbd98` 이후 로컬 구현, [화면 명세](05_ui_storyboard.md), [최종 연결 경계](18_final_review.md), [UI 계약](../contracts/ui_contracts.md), [Catalog](../content/catalog.json).

## 구현·연결·실제 검사

- `feature/world/WorldScreen.kt`: 공개 WorldScreen과 C 내부 상세 선택. 미발견 콘텐츠 잠금, 발견 지역의 기념품 목록·보유 여부, 카탈로그 오류·빈 목록·이용 제한, 가로/세로 재배치를 구현했다. 상세 뒤로는 로컬 선택만 닫고 최상위 뒤로는 A 라우터에 맡긴다. 새 onBack 계약은 추가하지 않았다.
- `WorldPresentation.kt`: 등록된 아이템 ID 기준 수집 수, 지역 발견과 아이템 소유의 분리, 누락·중복·소속 오류·좌표 오류·이미지 경로 검사를 한다. 콘텐츠 오류가 있는 목적지 후보는 비활성화한다. 지역 ID와 좌표를 새로 만들거나 게임 상태를 변경하지 않는다.
- `WorldItemImage.kt`: Catalog의 assetKey로 얻은 PNG 경로만 IO dispatcher에서 읽고 로딩·누락·디코드 실패를 구분한다. 경로가 바뀌면 이전 이미지 상태를 재사용하지 않는다. 파일 경로를 아이템 ID로 합성하지 않으며 새 이미지 에셋은 만들지 않았다.
- `WorldPresentationTest.kt` 10개: 실제 저장소 Catalog와 UI fixtures를 읽어 W01~08의 읽기·후보 콜백·오류 경계를 시험한다. 파생 테스트 입력 외에 별도 콘텐츠 목록을 작성하지 않았다.
- debug `WorldPreview.kt`: 실제 패키지 Catalog/fixtures를 IO에서 읽는 Preview 4개 정의. 정적 Preview는 비동기 로딩 문구에 머무를 수 있어 Interactive Mode로 샘플 로드를 확인한다. fresh 대화형 Preview에서 0/9·3지역 잠금과 카드 선택 후 잠긴 상세·활성 여정 준비 버튼을 관찰했다. 나머지 Preview의 화면 인수와 실제 앱 연결은 구분한다.

검사: Windows에서 `gradlew.bat --no-daemon :app:testDebugUnitTest --tests io.kailand.feature.world.WorldPresentationTest :app:assembleDebug` exit 0, C03 10개 PASS·debug 빌드 성공(12 tasks executed / 33 up-to-date). 최종 코드의 `py -3 scripts/check_all.py --android` exit 0, Python 34개·Android 43개(A01 4+C01 8+C02 21+C03 10) PASS, debug 빌드 성공(11 tasks executed / 34 up-to-date).

A는 B의 로드된 실제 상태와 Catalog를 아래 공개 함수에 연결한다. WorldScreen은 자체 스크롤을 가지므로 높이가 제한된 컨테이너에 배치한다. 로딩/읽기 실패는 기존 A 경계에서 처리하고 기본 snapshot이나 Preview로 대체하지 않는다. 아래는 문서 예제이며 AppRoot·Manifest·계약·테마·의존성·Catalog·fixtures는 수정하지 않았다.

```kotlin
WorldScreen(
    snapshot = loadedGameSnapshotFromB,
    catalog = loadedCatalog,
    gate = currentGate,
    onSelectRegion = { regionId -> /* A가 S03 목적지 후보만 채움 */ },
    onOpenItems = { /* A가 B 인벤토리로 이동 */ },
    modifier = Modifier.fillMaxSize(),
)
```

클릭 순간 최신 gate·foreground·Catalog를 재검사한다. 공개 계약에 foreground를 추가하지 않고 lifecycle에서 관찰하며 화면 제거 시 observer와 이미지 로딩 효과를 해제한다. 실제 lifecycle·이미지 디코딩·뒤로/회전·콜백 이동의 기기 시험은 NOT RUN이다. 사용자 지시에 따라 C02·C03을 기존 파트 브랜치로 공유하며 실제 전송 결과는 전달 보고서와 PR에 기록한다.

## 범위와 데이터

WorldScreen은 GameSnapshot, Catalog, gate를 받아 onSelectRegion(regionId), onOpenItems만 내보낸다. C의 지역 선택 상태는 일시 UI 상태이며 게임 저장에 넣지 않는다. B의 ViewModel·저장소·GameService를 참조하거나 소유·발견 상태를 직접 변경하지 않는다.

지도는 실제 도로·GPS 지도가 아닌 군도 선택 보드다. 첫 구현은 Catalog 순서의 정적 지역 카드로 하고 확대·자유 이동·지도 SDK·외부 타일은 넣지 않는다. 지역 이름·테마·아이템명·assetKey는 전달받은 Catalog에서 읽는다. 별도 Kotlin 콘텐츠 목록을 만들지 않는다.

| 표시값 | 읽는 기준 |
|---|---|
| 지역 목록 | Catalog.regions |
| 발견 여부 | region.id가 GameSnapshot.discoveredRegionIds에 포함되는지 |
| 지역별 기념품 목록 | region.itemIds를 Catalog.items의 id로 조회 |
| 보유 여부 | item.id가 GameSnapshot.ownedItemIds에 포함되는지 |
| 전체 수집 수 | Catalog에 등록된 아이템 중 보유한 ID 수 / 등록된 전체 아이템 수 |
| 이미지 | item.assetKey → Catalog.assets. 누락 시 기본 아이콘·누락 설명 |

발견과 소유는 각각 읽는다. 발견 지역이라고 음식·의상·장식이 모두 있다고 추정하지 않는다. 알 수 없는 저장 ID는 등록된 수집 수에 더하지 않고 원본 상태를 삭제하지도 않는다. 누락 참조는 해당 항목의 오류로 표시하며 임의 아이템을 만들지 않는다. Catalog 검증 실패를 조용히 정상 콘텐츠처럼 표시하지 않는다.

GameSnapshot에는 여정 source나 방문 날짜가 없다. 아이템 보유를 실제 지역 방문 인증으로 쓰거나 PLEOS 성공 배지를 만들지 않는다. Preview에서는 별도 DEMO 샘플 표시를 유지하고 실제 앱에서 B의 로드된 상태를 샘플로 대체하지 않는다.

## 화면 스케치와 행동

```text
월드맵 · 지역 발견 보드          기념품 보유 n / 전체 m

[대전: 발견함] [원주: 미발견·잠금] [속초: 미발견·잠금]
                 ↓ 지역 카드 선택
지역 상세
  발견: 지역 테마·아이콘·기념품별 보유/미보유
  미발견: 지역 이름·미발견 안내, 기념품 상세는 잠금

[이 지역으로 여정 준비]          [보유 아이템 보기]
```

지역 카드 선택은 C 내부에서 상세를 여는 동작이다. '이 지역으로 여정 준비'에서만 onSelectRegion(regionId)를 한 번 호출한다. A가 S03 목적지 후보를 채우며 최종 요청은 사용자가 S03에서 확인한다. 지역 카드나 준비 버튼에서 출발·도착·발견·보상 처리를 실행하지 않는다.

미발견 잠금은 발견 콘텐츠 상세의 잠금이다. 처음 방문할 지역도 목적지 후보로 선택할 수 있으므로 미발견이라는 이유로 여정 준비를 막지 않는다. 제한 여부는 gate가 결정한다. 보유 아이템 보기는 onOpenItems로 A를 거쳐 B의 인벤토리를 연다. C에는 먹기·입기·장식 적용 명령을 넣지 않는다.

뒤로 가기는 상세가 열려 있으면 상세부터 닫고, 최상위에서의 뒤로 이동은 A의 라우터 처리를 따른다. 새 onBack 계약을 임의 추가하지 않는다. 창 크기 변경은 지역 선택이나 여정을 새로 만들지 않는다. 전체 초기화 등으로 입력이 바뀌면 최신 snapshot을 다시 읽고, Catalog에서 사라진 선택 ID는 해제한다.

작은 창에서는 카드 세로 목록, 가로에서는 목록과 상세의 두 영역으로 재배치한다. 미발견·보유·오류는 색상 외에 아이콘·텍스트로 구분한다. 버튼은 공통 터치 규격과 텍스트 1.3배를 고려한다. B의 색조는 참고하되 A 공통 테마·B 자산은 수정하지 않는다.

## 로딩·빈 상태·오류·제한

| 조건 | 화면과 동작 |
|---|---|
| B 상태가 로드되지 않음 | A의 로딩 경계에서 로딩 표시. 빈 GameSnapshot·Preview를 실제 상태로 주입하지 않음 |
| B 읽기 실패 | A의 오류·재시도 경계 유지. 미발견 0개 상태로 간주하거나 초기화하지 않음 |
| 정상 빈 소유 상태 | 미발견 지역 이름·잠금과 보유 0 표시. 초기화·새 보상 없음 |
| Catalog 지역 0개 | 준비된 지역 없음 표시. 목적지 선택 비활성화 |
| Catalog 참조·이미지 누락 | 해당 항목에 오류·기본 아이콘 표시. 임의 좌표·아이템·assetKey로 보정하지 않음 |
| UNKNOWN / RESTRICTED | A 제한 화면·이유를 따르고 지역 선택·아이템 이동 중단. 오래된 버튼도 최신 gate로 검사 |
| background / 복귀 | 외부 효과·listener를 만들지 않고 복귀 시 최신 읽기 상태로 다시 그림 |

로딩·오류 표현을 위해 공통 WorldScreen 서명에 임의 필드를 추가하지 않는다. A01의 실제 AppRoot·실행 타입을 확인한 후 기존 경계로 연결한다.

## 구현 후 확인할 입력 예제

아래는 [기존 UI 샘플](../fixtures/ui_samples.json)을 사용하는 검증 설계다. W01·02·05·06·08의 읽기 계산과 W03·04·07의 명령 경계는 단위 시험 10개로 확인했다. W03·04의 AppRoot 이동, W07 실제 lifecycle/gate 전환, W09 기기 크기/접근성, W10 A의 로딩/읽기 실패 경계는 NOT RUN이다. fixtures는 추가·수정하지 않았다.

| 예제 | 입력·행동 | 기대 결과 |
|---|---|---|
| W01 | fresh 샘플 | 3지역 이름·미발견 잠금, 보유 0/9. 최초 방문할 지역도 여정 준비 가능 |
| W02 | regional 샘플 | 대전 발견, 빵·빵모자·간판 보유, 원주·속초 미발견, 전체 보유 3/9 |
| W03 | 대전 카드 선택 후 여정 준비 클릭 | 카드 선택은 로컬 상세만, 버튼은 daejeon 후보 콜백 1회. 새 journeyId·저장·보상은 C에서 생성하지 않음 |
| W04 | 보유 아이템 보기 클릭 | onOpenItems만 호출. C가 B 화면·ViewModel 직접 참조하지 않음 |
| W05 | 합성 시험 입력: 대전 발견, 소유는 빵만 | 실제 소유 1개만 보유로 표시. 발견만으로 3개 지급·소유 추정 없음 |
| W06 | 알 수 없는 저장 ID 또는 누락 Catalog 참조 | 원본 게임 상태 보존. 등록 수량만 집계, 오류·누락 표시 |
| W07 | 표시 중 gate 제한·UNKNOWN으로 변경 후 오래된 버튼 클릭 | 후보 선택·아이템 이동 차단. DEMO로 gate 덮어쓰기 없음 |
| W08 | 초기화 성공 후 빈 상태 전달 | 최신 미발견·보유 0 표시. 이전 발견·아이템 캐시를 다시 저장하지 않음 |
| W09 | 작은 창·가로·글자 1.3배 | 지역명·상태·준비 버튼 잘림 없음, 상세 닫기 후 선택 복귀 일관성 |
| W10 | 로딩·읽기 실패·정상 빈 상태 각각 입력 | 서로 구분되며 샘플이나 자동 초기화로 실패를 덮지 않음 |

W02·W08은 I01의 저장 상태 연결, W03은 I06의 목적지 후보 연결, W09는 S03으로 확인한다. 단순 스타일용 자동 테스트를 늘리기보다 읽기 계산의 경계와 실제 연결 흐름을 검증한다.

## 다음 한 작업

A가 실제 로드된 B snapshot과 Catalog 및 두 콜백을 연결한 뒤 I01·I06과 상세 뒤로/회전·제한 흐름을 확인한다. 실제 PNG 표시·누락 fallback은 기기에서 검증한다. 독립 후속 작업은 C04 JourneyPort/DemoControlPort 기반 수동 시연 제어이며 A의 포트 실제 구현 전에는 기존 fake로 검증한다. C03 전체 인수 완료는 연결 후 판정한다.
