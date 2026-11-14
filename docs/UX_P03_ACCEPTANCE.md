# UX-P03 · 원주 문구, 자산 출처, 네 시연 인수 기준

2026-11-10 KST · PM 초안 · 2026-11-13 KST 최신 후보 `a342825` 행 추가(기존 `84d473e` 기록은 변경 없이 보존) · 2026-11-14 KST ABC 최종 자료 종합 검토 반영(8장, 기존 SHA별 판정 보존) · **전체 인수: HOLD**

## 0. 판단 기준

| 항목 | 값 |
| --- | --- |
| 제품 상태 판단 기준 | **통합 PR [#29](https://github.com/Finn-02/MEMORIDE/pull/29) `84d473e7256d7b8316b81be834fa1579a8eee3d4`**. Draft·미병합이며 `main`은 `bb36e7e`(#27 병합)다 |
| #29에 포함된 PM 화면 | PR [#26](https://github.com/Finn-02/MEMORIDE/pull/26) `db5a1eb`. #29의 조상으로 이력 보존 병합됨. #26 자체는 Open |
| 이 문서 작성 위치 | `part/pm-p01-briefing`(`db5a1eb`)의 미커밋 초안. #29 소스는 `git show 84d473e:<경로>`로 읽기만 함 |
| 병합 순서 | #26·#28·#29의 처리 순서와 최종 병합 경로는 PM 조율 대상(미결) |
| #29 최신 후보 (2026-11-13) | **`a342825ea02550f4cb90d346494b748cae605cbb`** (`feat(branding): use MEMORIDE logo as adaptive app icon`). `gh pr view 29`의 `headRefOid`와 PM이 확인한 SHA가 **일치**(차이 없음, `확인`). PR 상태 Open, base `main`. `84d473e` 뒤 커밋 6개(`0f54142`, `b2d9c11`, `3c36263`, `c4b021d`, `bfd350b`, `a342825`), 203개 파일 변경 |
| #29 검토 대상 (2026-11-14) | 종합 검토 기준 PR HEAD **`c53e3f795f6142053eb89660c8768d839cc93c12`** · APK 빌드 소스(선언값) **`434abae4f58787230f18e2860ee9761fb9d7dcc4`**. 두 SHA 차이는 커밋 1개, `docs/FINAL_UI_VALIDATION_20261114.md`·`docs/UI_FINAL_HANDOFF_20261113.md`만 변경(`확인`: `git diff --stat 434abae c53e3f7`). 결과는 8장 |
| #29 현재 HEAD (2026-11-14 PM 조회) | **`17bea5438af9ea7b4fb3f3a431a19ab33777adc5`** (`Recognize city destinations and refine visit result layout`, 12:57 KST). 검토 기준 `c53e3f7`과 **다름**: 제품 코드 변경 포함(`NavigationInbox.kt` 도시명 목적지 인식, `RegionResultPleosLayout.kt`, 시험 2개, 문서 3개). PR 상태 Open · Ready(Draft 아님). 8장 종합 검토는 `17bea54`를 다루지 않는다 |
| 기존 기록의 적용 범위 | 아래 모든 `PASS`·`FAIL`·`부분 PASS`는 기록된 후보 SHA(`84d473e`, 일부 `836f0d9`)에만 해당한다. `a342825`로 이어지지 않으며, 최신 후보 결과는 4장·5장의 `a342825` 행에 따로 적는다. A 인계 문서(`docs/UI_FINAL_HANDOFF_20261113.md`)도 최종 HEAD의 `check_all.py --android`·단위 시험·APK 빌드·앱 실행·화면 검수를 NOT RUN으로 기록한다 |

결과 표기:

| 표기 | 뜻 |
| --- | --- |
| `확인` | 이 문서를 쓰며 PM이 저장소·커밋에서 직접 대조한 사실 |
| `확인 (B 회신)` | 담당 파트의 회신으로 확정한 사실. PM이 원본(저장소 밖 파일)을 직접 열어 검사한 것은 아님 |
| `PASS (A 기록)` | 담당자 PR·문서에 기록된 결과. PM이 직접 실행한 결과가 아님 |
| `PASS (C 확인)` | C가 지정 후보 APK로 실제 실행·판정하고 증거(캡처·저장 비교)를 전달한 결과. PM이 직접 실행한 결과가 아님 |
| `부분 PASS` | 대표 화면·조작만 확인. 전체 조합·접근성 인수가 아님 |
| `원인 미확정` | 판정과 함께 붙이는 표기. 이상의 원인(앱·OS·셸 조작)이 정해지지 않음 |
| `PM 직접: NOT RUN` | PM이 해당 후보를 직접 실행하지 않음 |
| `NOT RUN` | 해당 후보·환경에서 실행 기록이 없음 |
| `… 대기` | 담당자의 답이나 후속 작업이 필요함 |

일반 Android 에뮬레이터·JUnit 결과는 보조 근거다. 연결 앱·Pleos·실차 결과를 대신하지 않는다. 화면 번호 S04~S07은 2026-11-10 와이어프레임 기준이다.

## 1. 원주 문구

| 화면·값 | 표기와 기준 | 근거 (`84d473e`) |
| --- | --- | --- |
| S05 첫 방문 제목 | `원주를 처음 발견했어요` | `feature/briefing/RegionBriefing.kt` — 받침에 따라 을/를 자동 선택. `RegionBriefingTest` |
| S05 첫 방문 설명 | 확정 영수증의 지급 수량으로 `원주 기념품 N개를 받았어요.` 수량·목록은 `ArrivalReceipt.grantedItemIds`에서만 읽는다. 지급 0개이면 `이번 방문에서 받은 새 기념품은 없어요.` | 같은 파일 |
| S05 재방문 | 제목 `원주에 다시 왔어요` · 설명 `이번 방문에서 받은 새 선물은 없어요.` 지역 기념품은 Catalog로 보여 준다. `보유 중`은 실제 소유 입력이 계약으로 올 때만 쓴다(현재 없음 → 미표시) | 같은 파일. 지급 항목이 있는 재방문 영수증은 상충으로 보고 새 화면을 만들지 않는다 |
| S05 성장 | 영수증 `growthBefore → growthAfter` 그대로. 화면에서 계산하지 않는다 | `BriefingText.kt` 재사용 |
| S05 원주 기념품 | 감자 음식 `wonju_potato`(음식) · 은행잎 머리장식 `wonju_crown`(의상) · 은행나무 `wonju_tree`(장식) | `content/catalog.json` 확인. 지역 테마는 게임용 표현이며 공식 특산품·제휴 인증이 아니다 |
| S05 이번 여정 기록 | `USER_CONFIRMED`는 네 칸 모두 `—` + `확인 안 됨`. `DEMO`는 출처 `DEMO · 모의 여정`과 영수증 거리·시간만. `PLEOS`는 계약으로 받은 값만. 연비·누적 주행은 입력이 없어 항상 `—` | `RegionBriefingTest` (null은 `—`, 실제 0은 `0m`·`0초`) |
| S05 출처 | `사용자 확인 완료 · 실제 방문 인증 아님` / `DEMO · 모의 여정` / `PLEOS · 여정 기록` | `BriefingText.kt`. #29 리허설은 합성 목적지(DEMO)와 USER_CONFIRMED 도착(측정값 null)을 쓴다 |
| S04 방문 날짜·횟수 | B가 확정된 REGION 영수증으로 만든 `RegionVisitSummary`를 표시한다. 날짜는 Asia/Seoul·연도 포함 | `확인`: B `BGameService.regionVisitHistory` 생산 → `GameAppRoot` 수집·전달 → C `WorldBoard` 표시. Loading/Unavailable 구분 유지 |
| S06 저장 중·실패 | 저장 중 `여정 결과를 저장하고 있어요`. 실패 `결과 저장을 마치지 못했어요` + A의 `Failed.message`. 결과를 확인할 수 없으면 미지급을 단정하지 않는다 | `feature/briefing/ArrivalSaveScreen.kt` |
| S07 내비 실행 실패 | 제목 `내비를 직접 열어 주세요` · 안내 `현재 앱에서 내비를 여는 연결을 사용할 수 없어요. 내비 앱을 직접 이용한 뒤 주차 후 MEMORIDE를 열어 주세요.` · **버튼 두 개: `다시 시도`(주요)와 `섬으로 돌아가기`(보조)** | `확인`: `app/GameAppRoot.kt` `ProductRoute.NAVIGATION_FAILURE`에 두 버튼이 모두 있다. `다시 시도`는 출발 요청을 다시 보내고(`ProductNavigation.kt`), 요청이 실패하면 S07에 머물고 성공하면 섬으로 이동한다. `섬으로 돌아가기`는 바로 섬으로 이동한다. 실제 외부 내비 재시도 성공은 NOT RUN (A03). A 소유 문구 |

디자인 대조: S05 첫 방문 [12:482](https://www.figma.com/design/kxIVr8Cu5xLA6nHuOVgayJ/?node-id=12-482) · S05 재방문 [12:534](https://www.figma.com/design/kxIVr8Cu5xLA6nHuOVgayJ/?node-id=12-534) · S06 실패 [23:613](https://www.figma.com/design/kxIVr8Cu5xLA6nHuOVgayJ/?node-id=23-613).

Figma와 다르게 둔 문구:

- 재방문 `기념품은 이미 모두 모았어요.` — 소유 입력이 없어서 쓰지 않는다.
- S06 `보상은 아직 지급되지 않았어요` — 결과가 불확실할 수 있어서 쓰지 않는다.

## 2. 섬 PNG 13개 제작 근거

`assets/starter`의 `*_v3.png` 13개는 커밋 `ab19ac1`(2026-11-09, `feat(B06): integrate approved Blender-rendered assets and island scene`)에서 추가됐다.

- 대응 SVG가 없다.
- 현행 `assets/ATTRIBUTION.md`에 별도 항목이 없다. ATTRIBUTION은 `assets/starter`의 15개 SVG와 대응 PNG만 팀 자체 단순 도형 초안으로 기록한다.
- `db5a1eb`와 `84d473e` 사이에 이 13개·`docs/B06_7L_ASSETS.md`·`ATTRIBUTION.md`의 변경은 없다(`확인`).

| 묶음 | 파일 | 앱 사용 위치 | 제작 근거 (`docs/B06_7L_ASSETS.md`) |
| --- | --- | --- | --- |
| 기본 섬 1 | `island_base_v3.png` | `feature/island/IslandScene.kt` | `create_island_base_v3.py` |
| 성장 3 | `growth_path_v3.png`, `growth_flowers_v3.png`, `growth_rest_area_v3.png` | `IslandScene.kt` (단계 0~3 누적) | `build_island_growth_v3.py`가 `island_base_v3.blend`의 카메라·조명으로 렌더 |
| 슬롯 장식 9 | `decor_{bakery_sign,ginkgo,shell}_slot_{back,left,right}_v3.png` | `feature/island/IslandDecorationLayers.kt` | `build_island_decor_b067b_visual.py`의 B06-7B 렌더. 아트 보관본 `*_b067b_visual.png`와 동일 바이트이며 이름만 변경했다고 기록 |

해시 대조 결과 (`확인`, 결과 유지):

- 13개 모두 SHA-256이 `docs/B06_7L_ASSETS.md`의 기록과 일치한다(13/13).
- 공통 형식은 1024×1024 RGBA 캔버스(B06 기록).
- 제작 스크립트와 `.blend` 원본은 저장소에 없다. 저장소만으로는 외부 재료 사용 여부와 사용 조건을 판단할 수 없다.

### 제작 방식·원본 보관 (B 회신, 2026-11-10)

| 항목 | 내용 | 상태 |
| --- | --- | --- |
| 구성 | 기본 섬 1, 성장 레이어 3, 장식 슬롯 이미지 9 = 13개 | `확인 (B 회신)`. 저장소의 13개 파일·B06 해시표와 일치(`확인`) |
| 제작 방식 | Blender 장면을 Python 제작 스크립트로 구성하고 1024×1024 RGBA PNG로 렌더링 | `확인 (B 회신)` |
| 제작 스크립트 3개 | `create_island_base_v3.py`, `build_island_growth_v3.py`, `build_island_decor_b067b_visual.py` | `확인 (B 회신)`. `docs/B06_7L_ASSETS.md`의 기록과 같은 이름 |
| 원본 보관 위치 | Blender 원본과 제작 스크립트는 팀 Google Drive `02_MEMORIDE_3D_Models` | `확인 (B 회신)`. PM이 Drive를 직접 열어 보지는 않음 |
| 스크립트 검사 | 제작 스크립트에서 외부 3D 모델 import와 외부 이미지 텍스처 로딩을 찾지 못함 | B 검사 결과 (외부 로딩 미발견). PM 재검사 없음 |
| `.blend` 내부 참조 검사 | Blender 원본 안의 외부 라이브러리(링크·append)·외부 이미지 참조 여부 | **B 검사 대기.** 이 검사 전에는 "외부 자산 미사용"으로 확정하지 않는다 |
| 외부 자산이 확인될 경우 | 출처, 라이선스, 수정 여부, 요구 고지를 B가 별도 정리 | 해당 시 대기 |
| 제출 고지 권장 문구 (B 제안) | "Blender를 이용한 자체 3D 모델링 및 고정 시점 PNG 렌더링" + 원본 보관 기록 | 최종 제출 고지는 회신 대기(`.blend` 검사 결과와 생성형 AI 여부 확인 뒤 확정) |

스크립트 검사와 `.blend` 내부 참조 검사는 서로 다른 검사다. 스크립트에 외부 로딩 코드가 없어도 `.blend` 파일 안에 저장된 외부 링크나 이미지 경로는 따로 확인해야 한다.

## 3. 자산 출처 표

| 사용 위치 | 파일·제작 근거 | 상태 |
| --- | --- | --- |
| S04 지도 | `app/src/main/res/drawable/memoride_korea_map.xml` — Natural Earth 1:10m Admin 0 Countries **5.1.1**의 `ADM0_A3=KOR` 레코드를 정적 벡터로 변환(53개 링, 3060개 좌표). 병합된 PR [#27](https://github.com/Finn-02/MEMORIDE/pull/27)(`bb36e7e`) | 출처·재현 검토 **PASS** (아래) · Pleos 지도·지역 상세 **부분 PASS (C 확인)**, 좁은 창 841×1210·font 2.0 대표 조작(5장) · UX-C04 전체 렌더링·터치 인수 NOT RUN |
| S05 기념품 아이콘 | `food_potato`, `outfit_ginkgo_crown`, `decor_ginkgo`의 SVG·PNG 모두 있음. ATTRIBUTION의 자체 제작 초안 범위 | `확인` |
| 재방문 섬 기본·성장 4개 | 2장 참조 | 해시 `확인`. 제작 방식·스크립트 3개·Drive 보관 위치 `확인 (B 회신)`. 스크립트 외부 로딩 미발견(B 검사). `.blend` 내부 참조 검사 대기. 생성형 AI 여부·최종 제출 고지 회신 대기 |
| 재방문 섬 슬롯 장식 9개 | 2장 참조 | 위와 같음 |
| S05 첫 방문 그림 | #29에서 A가 PM `scene` 슬롯으로 첫 방문 아바타를 전달(`docs/UX_P26_A_CONNECTION.md`). 아바타가 없을 때는 PM 자리표시자 | 승인 캐릭터 PNG 출처 질문에 포함 |
| 재방문 섬 장면 | #29에서 B의 읽기 전용 섬 장면을 PM `scene` 슬롯으로 연결 | 섬 PNG 13개 + 승인 캐릭터 PNG 질문 적용 |
| 승인 캐릭터 PNG 36개 | `app/src/main/assets/avatars/approved/{tori,momo,lulu}/` (`84d473e`에서 36개 `확인`). B06 문서에 Blender 원본·렌더 경로·해시 기록 | 이번 B 회신 범위 밖. 출처·외부 재료·생성형 AI·보관 위치·제출 고지 모두 회신 대기 |

### S04 지도 출처·재현 검토 — PASS

| 항목 | 값 |
| --- | --- |
| 출처 문서 | [`docs/ux_c_map/SOURCE.md`](https://github.com/Finn-02/MEMORIDE/blob/bb36e7ee21a25b5446a530384efa9d7e3d57ae08/docs/ux_c_map/SOURCE.md) |
| 공식 원본 | `https://naturalearth.s3.amazonaws.com/10m_cultural/ne_10m_admin_0_countries.zip` |
| 기록된 원본 ZIP SHA-256 | `ce1ac7036499a0edd641fbc093cd209a98f96a49d2eca8480aaacad35138a7f6` |
| 이용 조건 | Natural Earth Terms of Use — public domain(수정·상업 이용 가능). SOURCE.md에 본문 확인 기록 |
| 재현 생성기 | `scripts/build_world_map.py <원본 ZIP>` |
| 재현 결과 | 공식 ZIP을 내려받아 SHA-256이 기록값과 일치. `scripts/build_world_map.py`로 재생성한 XML이 PR XML과 바이트 단위로 일치(`cmp PASS`). 53개 링, 3060개 좌표 |
| PR XML·재생성 XML SHA-256 | `74671ba1feb6e7eb95f0f9a43533b70382d5749e7fc75f5d01cfd011bba7b415` |
| 지도 XML Git blob | `0d803b872b556387ec45c8a32b4c7440b9cb192a` — #27 HEAD `5e6757c`, #27 병합 `bb36e7e`, #29 후보 `84d473e`에서 동일(`확인`) |
| 검토 근거 | [PM S04 지도 출처·재현 검토 (REPORT.md·verification.log)](https://chatgpt.com/library?file_id=libfile_c46e7c5d8f5c81919f8507360e9f24a6&account_id=personal) — 로컬 사본 `MEMORIDE_S04_MAP_REVIEW_20261110.zip` |
| 고지 | 앱 S04에 `게임의 발견 기록 · Made with Natural Earth` 표기(`feature/world/WorldBoard.kt`, `확인`). XML 첫 줄 주석 `Natural Earth 5.1.1 / public domain`. 출처 표기는 선택 사항이다. `assets/ATTRIBUTION.md`에는 지도 항목이 아직 없음(제출 자산 목록에 넣을지 C 결정) |

이 검토의 범위는 출처와 재현이다. Pleos 실제 창의 지도 렌더링·지역 상세·방문 기록 연동은 C의 `84d473e` 검수에서 좁은 창·font 2.0 대표 조작만 부분 PASS다(5장). 전체 창 크기 조합·아이콘 실측 터치 크기·접근성(UX-C04 전체)은 NOT RUN이다.

### `a342825`에서 갱신이 필요한 자산 근거

위 PASS·`확인`은 `84d473e`(지도는 blob `0d803b8`) 기준으로 유지한다. `a342825`에는 아래 변경이 있어 근거를 다시 맞춰야 한다(변경 사실은 `git diff 84d473e a342825`로 `확인`, 재검증은 NOT RUN).

| 대상 | `a342825`의 변경 (`확인`) | 갱신이 필요한 근거 |
| --- | --- | --- |
| S04 지도 XML `memoride_korea_map.xml` | Git blob `0d803b8` → **`d747c50cb48beec7ce2489ecba76e49b4639325a`**, 파일 SHA-256 `74671ba1…` → **`5be83ba3e703a820bae7c517139ee600613f5bc25e8eb9e8aa43bb2ff72e6ad5`** (커밋 `b2d9c11`). 53개 경로의 `pathData`는 변경 전과 같고(추출 해시 일치), 경로마다 `fillColor #FFF8E6→#FFF6E0`, `strokeColor #E8D5A6→#EBCF96`, `strokeWidth 0.8→3`만 바뀜. 첫 줄 Natural Earth 주석 유지 | `scripts/build_world_map.py`는 여전히 `#FFF8E6`·`#E8D5A6`·`0.8`을 출력하므로 **위 `cmp PASS`(재생성 XML = PR XML)는 `a342825` XML에 적용되지 않는다.** 생성기를 같이 고치거나 "생성기 출력 + 스타일 후처리"를 SOURCE.md에 기록하고 새 해시로 재현 근거를 다시 남겨야 함. 지형 좌표 출처(Natural Earth 5.1.1)는 바뀌지 않음 |
| 승인 캐릭터 PNG 36개 `avatars/approved/` | 36개 모두 교체(M) + `avatars/approved_hd/` 36개 추가(A, 1024px). 출처 기록 `docs/C_INVENTORY_ART_CANDIDATE_20261113.md`, 해시 `docs/asset_manifests/C_INVENTORY_ART_20261113.json`(C 전달 ZIP과 일치했다는 A 기록, PM 재대조 없음) | 3장 표의 "`84d473e`에서 36개 `확인`"과 B06 해시 기록은 교체 전 파일 기준. 교체본·HD본 72개에 대해 제작 방식·외부 재료·생성형 AI·보관 위치·제출 고지(B 회신 현황 1~7)를 다시 받아야 하며, B06/ATTRIBUTION 갱신 여부도 정해야 함 |
| S05 기념품 등 썸네일 9개 `assets/starter/*.png` | `food_potato`·`outfit_ginkgo_crown`·`decor_ginkgo`를 포함한 9개 PNG가 C 3D 후보로 교체됨. `assets/ATTRIBUTION.md`는 변경 없음 | 3장 "S05 기념품 아이콘 = ATTRIBUTION의 자체 제작 단순 도형 초안 범위" 근거가 `a342825`에는 맞지 않음. ATTRIBUTION 갱신 필요 |
| 섬 PNG 13개 `*_v3.png` | 변경 없음(`확인`) | 2장 기록 그대로 |
| 그 밖의 새 이미지 (참고) | `drawable-nodpi/memoride_journey_car_v1.png`(`docs/JOURNEY_CAR_ASSET.md`에 생성형 이미지 도구 사용 기록), `island_logo_mark.png`, 런처 아이콘, 폰트·아이콘 라이선스 파일 4개 | 이 문서의 기존 범위 밖. 제출 자산 목록 포함 여부는 별도 확인 |

### 섬 PNG 13개·승인 캐릭터 PNG 36개 — B 회신 현황

| # | 확인 항목 | 섬 PNG 13개 | 승인 캐릭터 PNG 36개 |
| --- | --- | --- | --- |
| 1 | 제작 방식과 도구 | `확인 (B 회신)`: Blender 장면을 Python 스크립트로 구성해 1024×1024 RGBA로 렌더링. Blender 버전은 회신에 없음 | 회신 대기 |
| 2a | 제작 스크립트의 외부 모델 import·외부 이미지 텍스처 로딩 | B 검사: 찾지 못함 (외부 로딩 미발견) | 회신 대기 |
| 2b | `.blend` 원본 내부의 외부 라이브러리·이미지 참조. 외부 자산이 있으면 원본 주소, 라이선스, 수정 여부, 고지 문구 | **B 검사 대기** | 회신 대기 |
| 3 | 생성형 AI 사용 여부. 사용했다면 도구와 이용 조건(제출 사용 가능 여부) | 회신 대기 | 회신 대기 |
| 4 | 원본 `.blend`·스크립트 보관 위치 | `확인 (B 회신)`: 팀 Google Drive `02_MEMORIDE_3D_Models`. 보존 책임자와 심사 때 원본 제시 절차는 회신에 없음 | 회신 대기 |
| 5 | 최종 제출 고지: 문구와 `assets/ATTRIBUTION.md` 항목 추가 방식(제작 경로·해시는 `docs/B06_7L_ASSETS.md` 참조) | 회신 대기. B 권장 문구 "Blender를 이용한 자체 3D 모델링 및 고정 시점 PNG 렌더링" + 원본 보관 기록. 2b·3 결과 뒤 확정 | 회신 대기 |
| 6 | 제출본에서 실제 쓰는 캐릭터 프레임 목록(첫 방문 아바타 포함) | — | 회신 대기 |
| 7 | 다시 렌더하면 B06 해시표와 ATTRIBUTION을 같은 PR에서 갱신하는지 | 회신 대기 | 회신 대기 |

## 4. 네 시연 대본과 합격 기준

진행 원칙:

- 시연마다 독립된 준비 상태를 만든다. #29 리허설은 세션별 격리 B 저장소를 쓰고 기존 사용자 저장을 초기화하지 않는다.
- 실제 gate와 UNKNOWN 차단은 유지된다.
- 시연 입력(DEMO 합성 목적지, USER_CONFIRMED 도착, A 주입 실패) 결과와 실제 SDK 결과는 별도 줄에 적는다.

#29 프리셋 이름은 `feature/demo/UxDemoPresets.kt` 기준이다.

| 시연 | 대본 | 준비 | 합격 기준 |
| --- | --- | --- | --- |
| 원주 새 지역 (`NEW_REGION`) | "처음에는 원주가 지도에 없고 기념품도 잠겨 있습니다. 주차 후 원주 도착을 확인하면 결과가 저장되고 원주 기념품 세 개가 열립니다." | 원주 미발견, 원주 기념품 3개 잠김 | ① 저장이 확정된 뒤에만 S05 첫 방문 ② 제목 `원주를 처음 발견했어요`, 지급 3개가 영수증과 일치 ③ `섬으로 가기`·`캐릭터 선택` 모두 ack 성공 뒤에만 이동하고 실패 시 결과 유지 ④ S04에 원주 표시, 인벤토리 3개 해제 ⑤ 출처 표기가 실제 입력 경로와 일치 |
| 원주 재방문 (`REVISIT`) | "원주를 다시 방문하면 방문 기록은 쌓이고, 이미 받은 기념품은 다시 지급되지 않습니다." | 원주 발견·기념품·방문 기록이 있는 상태 | 같은 KST 날짜의 새 여정: 성장 0, 새 선물 0, `원주에 다시 왔어요`, S04 방문 횟수 +1과 연도 포함 KST 날짜, B 섬 장면 표시. `보유 중`은 소유 입력이 있을 때만 |
| 저장 실패 후 재시도 (`SAVE_RETRY`) | "저장 결과를 확인할 수 없으면 같은 여정으로 다시 시도합니다." | A가 확정 저장 실패를 1회 주입 | ① 저장 중은 정지 표시이며 타이머·탭으로 성공하지 않음 ② 실패 시 S05를 열지 않음 ③ 재시도·닫기 뒤 재시도 모두 같은 원본 ID·시각·내용 ④ 선물·성장이 각각 한 번만 반영 |
| 내비 실행 실패 (`NAVIGATION_FAILURE`) | "내비를 열지 못하면 다시 시도하거나 섬으로 돌아갈 수 있습니다." | A가 내비 실행 실패를 1회 주입 | ① S07 도달, `다시 시도`와 `섬으로 돌아가기` 두 버튼이 모두 보임 ② 실패 시점까지 출발·도착 저장·보상이 모두 0 ③ `섬으로 돌아가기`와 `다시 시도` 성공을 서로 다른 행(아래 4-1, 4-2)으로 판정 |
| 성장 규칙 (별도 검증) | — | 성장 0~3 상태, 서로 다른 KST 날짜의 REGION 완료 | 다음 KST 날짜의 첫 등록 지역 완료는 성장 +1·새 선물 0. 같은 날 두 번째는 0. 상한 3에서는 0. 상한이어도 첫 발견 선물은 지급 |

### 인수표

C 검수 환경 (`84d473e`, C 회신 2026-11-10 22:51 KST):

| 항목 | 값 |
| --- | --- |
| 소스 | `84d473e7256d7b8316b81be834fa1579a8eee3d4`. 정확한 SHA의 git archive 사본에서 빌드했고, 빌드 후 소스 바이트 일치. 패치 재적용 없음 |
| 빌드 | Windows 11 26200 / AMD64 · JBR 25.0.4 · 저장소 Gradle wrapper · `py -3 scripts/check_all.py --android` 종료 0 (Python 66, JUnit 376, 실패·오류·스킵 0, debug APK) |
| APK | **SHA-256 `0dc7d2cf2d3cbaa06bf6b66c0a064980ceeb500607a1502922bbb485b42d8edb`** · 41,092,443 bytes · 설치된 `base.apk`와 동일 · apksigner v2 검증 PASS · 공개 debug 인증서 SHA-256 `40f41acd69b25e0319595b64a3003fe4211fe5c11a670a2eb4884557e92733f2` |
| 기기 | 전용 신규 AVD `MEMORIDE_PR29_C_REVIEW` · Pleos 2.1 / API 34 / x86_64 image revision 1 · Emulator 37.2.13.0 · RAM 4096 · Vulkan off · user 10 · 디스플레이 2560×1440 / density 160 |
| 조건 | 실제 P / 속도 0의 `PARKED_ALLOWED`. 제한 우회·D/P 전환·기존 저장 초기화 없음. 합성 DEMO 목적지 + USER_CONFIRMED 도착(측정값 null)의 시연 전용 B 저장 |
| 한계 | 로컬 debug이며 팀 서명 release·실차 결과가 아님. 첫 재방문 중 창 복원 문제(5장)를 보존한 뒤 남은 시험은 같은 AVD의 새 Android task에서 진행 → 네 시연이 한 task에서 연속 통과했다는 뜻은 아님 |
| 증거 | C 회신 ZIP `MEMORIDE_C_PR29_PM_REPLY_20261110_225114_KST.zip`의 `REPORT.md`, `PM_REPLY.md`, `environment.json`, `evidence/*` |

| # | 행 | 담당 | 후보 SHA | 기기·빌드 | 결과 | PM 직접 실행 | 증거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 원주 새 지역 (`NEW_REGION`) | C 실행·판정 | `84d473e` | 위 C 환경 · APK `0dc7d2cf…` | **PASS (C 확인)** — 빈 지도 → 이동·명시적 도착 → PM 결과 → 섬 복귀. 성장 0→1, 아이템 3, 영수증 1, USER_CONFIRMED/null 측정값 | NOT RUN | C ZIP `evidence/new-region-{empty-map,result,pass}.png`, `new-region-pass.stores.json` |
| 2 | 같은 날 원주 재방문 (`REVISIT`) | C 실행·판정 | `84d473e` | 같음 | **PASS (C 확인)** — 성장·소유 불변, 새 방문 영수증만 추가, 원본 영수증 불변 | NOT RUN | C ZIP `evidence/revisit-{before,pass}.stores.json`, `revisit-pass.png` |
| 3 | 저장 확정 실패 → 닫기 → 섬 → 원본 재시도 (`SAVE_RETRY`) | C 실행·판정 | `84d473e` | 같음 | **PASS (C 확인)** — debug 저장 실패 훅 발생, 실패 시 B 저장 전체 동일. 같은 원본 명령으로 재시도 성공, 1회 지급 | NOT RUN | C ZIP `evidence/save-retry-{confirmed-failure,pass}.png`, `save-retry-*.stores.json` |
| 4-1 | 내비 실패 → S07 → `섬으로 돌아가기` (`NAVIGATION_FAILURE`) | C 실행·판정 | `84d473e` | 같음 | **PASS (C 확인)** — debug 1회 실패 표시 후 섬 복귀. 실패 전·후·복귀 B 저장 전체 동일, 무지급 | NOT RUN | C ZIP `evidence/navigation-failure-{screen,pass}.png`, `navigation-failure-*.stores.json` |
| 4-2 | 내비 실패 후 S07 `다시 시도` → 실제 외부 내비 실행·목적지 수신 성공 (버튼은 #29에 있음, `확인`) | A (A03) | `84d473e` | 실제 내비 launcher·Pleos SDK | **NOT RUN (A03)** — 4-1의 섬 복귀 PASS와 별개 | NOT RUN | C `PM_REPLY.md` "NOT RUN — 실제 외부 내비 launcher/A03 연결 인수 전" |
| 5 | 다음 KST 날짜 첫 등록 지역 완료 +1 · 상한 3 (성장 규칙) | B | `84d473e` | JVM 단위 시험(A·C의 `check_all.py --android` JUnit 376, 실패 0) | JVM **PASS (A·C 기록)** · 기기 시연 **NOT RUN** | NOT RUN | 구현: `BGameService.kt` `completionDate`·`hasUsedGrowthDate`(`확인`). 시험: `RegionArrivalTest`(`d03CommitsGrowthGiftsDiscovery…`, `pleosNullMeasurementsAndOutOfOrderGrowth…`, `firstDiscoveryStillGrantsGiftsWhenGrowthIsCapped`), `DailyArrivalTest.distinctDatesGrowToCatalogCapAndKeepReceiptsAtCap` |

#### 최신 후보 `a342825` 검증 행 (2026-11-13 추가)

위 `84d473e` 행은 그대로 둔다. `a342825`는 아직 어떤 주체도 실행 기록이 없다(A 인계 문서: 최종 HEAD 빌드·시험·앱 실행 NOT RUN, 새 내비 복귀 회귀 시험 5개 작성만 함). APK 식별값 없음.

| # | 행 | 담당 | 후보 SHA | 기기·빌드 | 결과 | PM 직접 실행 | 증거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 원주 새 지역 (`NEW_REGION`) | C 실행·판정 | `a342825` | 미정 (APK 없음) | **NOT RUN** | NOT RUN | — |
| 2 | 같은 날 원주 재방문 (`REVISIT`) | C 실행·판정 | `a342825` | 미정 | **NOT RUN** | NOT RUN | — |
| 3 | 저장 확정 실패 → 닫기 → 섬 → 원본 재시도 (`SAVE_RETRY`) | C 실행·판정 | `a342825` | 미정 | **NOT RUN** | NOT RUN | — |
| 4-1 | 내비 실패 → S07 → `섬으로 돌아가기` (`NAVIGATION_FAILURE`) | C 실행·판정 | `a342825` | 미정 | **NOT RUN** | NOT RUN | — |
| 4-2 | S07 `다시 시도` → 실제 외부 내비 실행·목적지 수신 → 앱 복귀 → 여정 대기 | A (A03) | `a342825` | 실제 내비 launcher·Pleos SDK | **NOT RUN** — `b2d9c11`의 내비 복귀 개선(MainActivity 재사용·SavedStateHandle 복원)은 코드 변경일 뿐 결과가 아님 | NOT RUN | — |
| 5 | 성장 규칙 (+1 · 상한 3) | B | `a342825` | JVM `check_all.py --android` | JVM **NOT RUN** · 기기 시연 **NOT RUN** (`84d473e`의 JUnit 376 PASS를 이어받지 않음) | NOT RUN | — |
| 6 | 시연용 주행 수치 표시 | A·PM | `a342825` | 미정 | **NOT RUN** — 아래 주의 참조 | NOT RUN | — |

**시연용 주행 수치 표시가 켜져 있음 (`확인`):** `a342825`의 `feature/briefing/RegionResultPresentation.kt`에서 `ResultDemoOptions.USE_MOCK_TRIP_RECORDS = true`이고 `RegionBriefingScreen` 기본값으로 쓰인다. 원본 값이 없으면(**USER_CONFIRMED 영수증 포함**) S05 여정 기록 네 칸에 고정 표본(첫 방문 `38.2km`·`52분`·`13.6km/L`·`276.0km`, 재방문 `42.6km`·`48분`·`14.2km/L`·`318.4km`)을 넣고 `시연용 화면이라 기록은 임의로 넣은 값이에요.` 안내를 붙인다. 영수증 측정값 null은 저장에 유지된다는 A 기록이 있다(`docs/UI_FINAL_HANDOFF_20261113.md`). 따라서:

- 1장 "S05 이번 여정 기록"(`USER_CONFIRMED`는 네 칸 모두 `—` + `확인 안 됨`) 기준은 `84d473e` 근거로 남고, `a342825` 화면과는 다르다. 제출·시연 빌드에서 이 표시를 켤지 PM 결정이 필요하다.
- 이 표본 수치는 실제 주행 측정·방문 인증·PLEOS 결과 근거로 쓰지 않는다. 시연 기록에는 "시연용 표본 수치 표시 ON"을 별도로 적는다.
- 같은 파일에서 S05 문구도 바뀌었다(제목 `…처음 발견했어요!`, 설명 `새 기념품 N개를 받았어요.`). 1장 문구표와의 대조는 `a342825` 재검수 때 갱신한다.

이전 기록 (참고):

- A의 `84d473e` Pleos 리허설: `SAVE_RETRY`·`REVISIT` PASS. APK 해시는 A 검토 ZIP(PM 미수령).
- A의 `836f0d9` 표본: 네 프리셋 PASS.
- 이 두 기록은 위 C 확인 결과로 대체하지 않고 근거로만 남긴다 ([PR #29 검증표](https://github.com/Finn-02/MEMORIDE/pull/29)).

`확인`한 구현 사항 (`84d473e`):

- REGION 성장: 등록 지역 영수증의 KST 완료 날짜마다 처음 한 번만 +1, 상한 `dailyGrowthCap`(Catalog 3). 성장 없이 끝난 과거 REGION 완료도 그 날짜를 사용한 것으로 센다.
- DAILY는 성장했던 영수증만 날짜를 사용한 것으로 센다.

## 5. 연결 앱 화면 인수 (PM 화면)

| 항목 | 합격 기준 | 현재 근거 | 결과 |
| --- | --- | --- | --- |
| S05 이동 | 두 버튼·Back 각각 B ack 성공 뒤 이동, 실패 시 결과 유지, 중복 입력은 한 번만 처리 | #29: `ReceiptCloseCoordinator`·B `acknowledgeReceipt` 경유. owner/gate/lease/generation 검사. Pleos `84d473e`에서 결과 캐릭터 선택 ack, 좁은 창 두 CTA·물리 Back ack | PASS (A 기록) · PM 직접 NOT RUN · 실기기 ack 실패 주입 NOT RUN(JVM 경계에서만 시험) |
| S06 입력 | 재시도·닫기가 최신 런타임 busy 상태를 검사. 늦은 응답·연타에도 중복 저장·전환 없음. 같은 실패 반복 뒤에도 재시도 가능 | #29: 실패 저장 닫기 → 섬 → 원본 다시 열기 → 재시도. Saving 중 경합은 PM/A/B JVM 시험 | 시나리오 PASS (A 기록) · 실기기 Saving 경합 전수 NOT RUN |
| 좁은 창 · font 2.0 대표 조작 | 큰 글자·좁은 창에서 두 CTA와 주요 버튼 도달·동작 | C `84d473e`(APK `0dc7d2cf…`), font_scale 2.0, 실제 task 841×1210: PM 결과 두 CTA 스크롤 접근, 결과의 캐릭터 선택 CTA 동작, S01 3종 선택, S02 버튼 접근 | **부분 PASS (C 확인)** · PM 직접 NOT RUN · S06/S07 큰 글자 전체 NOT RUN |
| S04 지도·지역 상세 | 방문 기록·KST 날짜·아이템·닫기가 실제 창에서 표시·조작됨 | 같은 C 조건: 지도·지역 상세에서 방문 2회, 연도 포함 KST 날짜, 아이템 3, 닫기 확인 | **부분 PASS (C 확인)** · UX-C04 전체 NOT RUN |
| 다른 시스템 화면 → 앱 전면 복귀 | 결과 화면 유지, 저장 불변 | C: 재방문 결과 유지, B 저장 전체 동일 | PASS (C 확인) |
| adb 경로 화면 복원 | 창 크기·글자 배율을 바꾼 뒤 앱 화면이 복원되고 상태·저장이 유지됨 | C 재현: 재방문 결과에서 font 2.0, `am task resize`로 841×1210 조작 뒤 1682×1210 확대와 `font_scale 1.0` 복원을 연속 적용 → **앱 화면이 복원되지 않음**. Pleos 홈/DrivingView가 전면, 앱 창 `NO_SURFACE`, `REORDER_TO_FRONT` 1회로 복원 안 됨. 저장은 유지됨(변화 없음), 수집 로그에서 앱 크래시는 확인되지 않음(프로세스 생존) | **FAIL 1건 (C 확인), 원인 미확정** — 앱·OS·셸 창 조작 중 원인 미정. C ZIP `window-restore-excerpt.txt`, `evidence/wide-restore-blank-observation.png` |
| Pleos 기본 창 조절 경로 | 확장 1682×1210 ↔ 분할 841×1210을 정상 시스템 창 조절(window bar)로 전환. 앱 화면 복원, pending 유지, 저장 재호출 없음, 넓은 S05 패널 ≤520dp와 버튼 도달 | C 시험은 adb task geometry 경로뿐. 위 FAIL이 기본 창 조절에서도 생기는지 미확인 | **NOT RUN (A 재현 대기)** |
| 큰 글자 (전체) | 모든 화면에서 글자 확대 시 스크롤 끝 버튼 도달 | 일반 Android 보조 확인(PR #26) + 위 대표 조작 부분 PASS | Pleos 전체 인수 NOT RUN |
| Android Studio Preview | S05/S06 Preview 실제 렌더링 | 컴파일만 확인 | NOT RUN |
| 실차·물리 갤럭시·서명 release | 각각 별도 기록 | `compileReleaseKotlin` PASS(A 기록)는 설치가 아님 | NOT RUN |

### 5-0. 최신 후보 `a342825` 화면 인수 행 (2026-11-13 추가)

위 표의 결과는 모두 `84d473e`(C APK `0dc7d2cf…`) 기준으로 보존한다. `a342825`는 별도 행으로만 기록한다.

| 항목 | 후보 SHA | 결과 | 비고 |
| --- | --- | --- | --- |
| S05 이동·S06 입력 | `a342825` | NOT RUN | 결과 화면 레이아웃(`RegionResultPleosLayout.kt` 등) 변경 |
| 좁은 창 · font 2.0 대표 조작 | `a342825` | NOT RUN | |
| S04 지도·지역 상세 | `a342825` | NOT RUN | 지도 XML 스타일 변경(3장 갱신 필요 표) |
| 다른 시스템 화면 → 앱 전면 복귀 | `a342825` | NOT RUN | |
| adb 경로 화면 복원 | `a342825` | **NOT RUN** | **`84d473e`의 FAIL 1건(원인 미확정)은 해결되지 않은 상태로 유지한다.** `b2d9c11`의 내비 복귀 개선은 외부 내비에서 돌아올 때 캐릭터 선택 반복을 막는 변경이며, C가 재현한 `am task resize`·`font_scale` 연속 조작 뒤 `NO_SURFACE`·Pleos 홈 전면 현상을 다룬다는 근거가 없다. 해결 처리는 같은 adb 경로를 `a342825`(또는 이후) APK로 다시 실행해 복원·저장 유지를 확인한 뒤에만 한다 |
| Pleos 기본 창 조절 경로 | `a342825` | NOT RUN (A 재현 대기) | |
| 큰 글자 (전체) | `a342825` | NOT RUN | |
| 캐릭터 HD·교체 이미지 표시 | `a342825` | NOT RUN | 크기·선명도·표정/의상 조합, HD 로더 폴백(A·C 기록: 미검수) |
| 실차·물리 갤럭시·서명 release | `a342825` | NOT RUN | |

### 5-1. B 화면 갤럭시 검수 (B 계획, 아직 미실행)

B 회신(2026-11-10): Part B 최신 코드가 #29에 포함된 것을 확인했고, 앞으로 #29의 최신 통합 APK를 기준으로 검수한다. 실제 검수 전이므로 아래 항목은 PASS로 보고하지 않는다. 갤럭시의 일반 앱 복귀(홈 → 앱)와 실제 Pleos 제한 전환은 서로 다른 행으로 판정한다.

| 항목 | 합격 기준 | 담당 | 후보 SHA | 기기·빌드 | 결과 |
| --- | --- | --- | --- | --- | --- |
| S01 → S02 → S03 터치 흐름 | 캐릭터 선택 → 섬 → 인벤토리 이동이 터치로 정상 동작 | B | #29 최신 통합 APK (SHA 미정) | 물리 갤럭시 | NOT RUN |
| 화면 재생성 후 상태 유지 | 인벤토리 탭·장식 선택이 회전 등 화면 재생성 뒤 유지 | B | 같음 | 같음 | NOT RUN |
| 홈 화면 복귀 후 상태 유지 | 홈 → 앱 복귀 뒤 인벤토리 탭·장식 선택 유지 (일반 앱 복귀) | B | 같음 | 같음 | NOT RUN |
| 저장 실패 시 선택 유지 | 장식 배치 저장이 실패하면 선택 상태 유지 | B | 같음 | 같음 | NOT RUN |
| 배치 성공 후 선택 해제 | 장식 배치 저장이 성공하면 선택 해제 | B | 같음 | 같음 | NOT RUN |
| Pleos 실제 제한 전환 | 주차 해제 등 실제 제한 전환 뒤 상태 유지·조작 차단 (갤럭시 앱 복귀와 별도) | B·A | 같음 | Pleos AVD/차량 | NOT RUN |

## 6. 최종 인수 기록 양식

시연·화면마다 다음을 남긴다:

- 후보 SHA, APK 식별값(파일 SHA-256)
- 환경: 기기/AVD, OS, Pleos 이미지
- 앱 창 크기, density, 글자 배율
- 준비 상태, 실제 결과, 캡처 파일명

`PASS`·`FAIL`·`NOT RUN`은 아래 각각에 따로 기록하고 실행 주체(A/C/PM)를 적는다. 한쪽 결과로 다른 쪽을 대신하지 않는다.

- 시연 입력
- 실제 SDK
- 실차
- 서명 release

## 7. 답변을 받아 채울 항목

| 담당 | 항목 | 채울 위치 |
| --- | --- | --- |
| A | adb 경로 화면 복원 FAIL 1건: 호스트 복귀·앱 생명주기 재현으로 원인과 수정 범위 확인 | 5장 'adb 경로 화면 복원' 행 |
| A | Pleos 기본 창 조절(window bar)로 확장↔분할 전환 재현·검수 | 5장 'Pleos 기본 창 조절 경로' 행 |
| A | A03: 실제 외부 내비 실행·목적지 수신·정상 재시도 결과 | 4장 4-2행 |
| A·C | S06/S07 큰 글자 전체, UX-C04 전체(창 크기 조합·실측 터치 크기·TalkBack·대비) | 5장 |
| B | 섬 PNG 13개: `.blend` 내부 외부 라이브러리·이미지 참조 검사 결과(2b), 생성형 AI 사용 여부(3), 최종 제출 고지와 ATTRIBUTION 반영 방식(5), 재렌더 시 갱신 규칙(7). 가능하면 Blender 버전과 원본 보존 책임자 | 2장 제작 방식 표, 3장 B 회신 현황 표 |
| B | 승인 캐릭터 PNG 36개: 확인 항목 1~7 전체(출처·외부 재료·생성형 AI·보관 위치·제출 고지·실제 사용 프레임) | 3장 B 회신 현황 표 |
| B | 갤럭시 검수 결과(#29 최신 통합 APK SHA 포함). 일반 앱 복귀와 Pleos 제한 전환 구분 | 5-1장 |
| B | 성장 규칙의 기기 시연(다음 KST 날짜 +1, 상한 3) 방법 또는 JVM 시험으로 갈음할지 결정 | 4장 5행 |
| C | ATTRIBUTION에 지도 항목을 넣을지 결정. Pleos 창별 S04 지도 렌더링·아이콘 터치 판정 결과 | 3장 S04 지도 출처·재현 검토 |
| PM | #26·#28·#29 병합 순서 확정 뒤 판단 기준 SHA 갱신. PM 직접 실행 결과가 생기면 "PM 직접 실행" 칸 갱신 | 0장, 4장 |
| A | `a342825` 정적·단위·debug 빌드 결과와 APK SHA-256. 이후 C·A 재검수의 후보 식별값 | 0장, 4장 `a342825` 행 |
| A·C | `a342825`에서 adb 경로 화면 복원 재실행(같은 조작 순서). 결과가 나오기 전 `84d473e` FAIL은 유지 | 5-0장 |
| PM | `USE_MOCK_TRIP_RECORDS = true` 유지 여부와 시연·제출 빌드 표기 방식 결정. 1장 S05 문구·여정 기록 기준 갱신 | 1장, 4장 `a342825` 6행 |
| C | 지도 XML 스타일 변경(`d747c50`)에 맞춘 생성기·SOURCE.md·해시 재현 근거 갱신 | 3장 갱신 필요 표 |
| B·C | 교체된 캐릭터 PNG 36개·HD 36개·썸네일 9개의 출처 회신(1~7)과 B06·ATTRIBUTION 갱신 | 3장 갱신 필요 표, B 회신 현황 표 |
| A | signed release에서 진단 도구 없이 일반 사용자가 지원 목적지를 선택해 지역 도착까지 가는 경로 확정·보고. `17bea54` 도시명 인식이 그 답인지 제품 정책·회귀 검토와 함께 설명 | 8-4장 |
| A·C | 같은 signed release에서 결과 표시 → 닫기 → 섬 → 음식·의상·장식 사용·저장 → 프로세스 재실행 → 같은 날 재방문 미중복 | 8-2장 release 행 |
| A·C | release 창·글자 변경 뒤 안정화된 최종 표시 캡처와 실제 버튼 조작 기록. B 대리 결과·복원 화면은 PNG와 XML을 같은 시점에 다시 수집 | 8-2장, 8-3장 |
| A | 최신 후보(`17bea54` 또는 이후)의 소스·APK SHA-256과 영향 범위 재검증 결과 | 0장, 8-5장 |
| A | `17bea54`의 `check_all.py --android` 원시 로그(정적·Python 66·JUnit 398 요약·XML)와 debug APK(`091c1ca8…`) 전달. 받으면 PM이 해시를 대조하고 8-8의 1~4행 표기를 갱신 | 8-8장 |
| A | C 지도·출처 5파일 패치를 중복 확인 뒤 한 번만 반영 | 8-6장 |
| B (또는 원본 제작자) | B 원본의 최초 제작 경위·생성형 AI·외부 재료·보관 위치 | 8-6장, 3장 B 회신 현황 표 |

## 8. 2026-11-14 ABC 최종 자료 종합 검토 반영

출처: `MEMORIDE_ABC_FINAL_REVIEW_20261114_125620_KST.zip`(SHA-256 `e51a823c4d3de98e70b3ffc2734adba3634951053b94a90913b888c9c272b9d1`)의 `REPORT.md`·`NEXT_ACTIONS.md`. 이 검토는 Linux cloud에서 아래 세 ZIP을 대조한 문서 검토이며, Android 빌드·JUnit·기기 실행·서명 도구를 새로 실행하지 않았다. PM이 이번 문서 작업에서 확인한 것은 0장의 SHA 관계와 `17bea54`의 변경 범위뿐이고, 아래 결과는 **PM 직접: NOT RUN**이다.

| 입력 ZIP | SHA-256 |
| --- | --- |
| `MEMORIDE_PM_B_C_HANDOFF_20261114_010722_KST.zip` | `f418c4e90a06f854f1ae983dfc02dd18e7453c6518a7a0665a5ede899ea1e1dc` |
| `MEMORIDE_A_PROXY_B_VALIDATION_20261114_023123_KST.zip` | `80134e5a41ae9804e899371a4d9c8002a288c9e97dbb8c3faebe8a097371c614` |
| `MEMORIDE_C_FINAL_PLEOS_RECHECK_20261114_123536_KST.zip` | `96fb3bb0ca79d7802a40890761b53ce9df370b0744e9551f062f57e1884d3ad4` |

추가 표기 (이 장에서만 사용):

| 표기 | 뜻 |
| --- | --- |
| `PASS (A 기록)` | A가 자기 PC에서 실행한 결과 (0장 표기 그대로) |
| `PASS (A 대리)` | 파트 2(B) 범위를 A·사용자가 대신 실행한 결과. **B 독립 검수가 아니다** |
| `PASS (C 독립)` | C가 A와 다른 PC에서 같은 APK로 실행한 결과 |
| `debug` / `release` | 아래 debug APK와 signed release APK. 한쪽 결과로 다른 쪽을 대신하지 않는다 |

### 8-1. APK 식별

| 구분 | 값 |
| --- | --- |
| 빌드 소스 | `434abae4` (빌드 담당 선언값 + Git 비교로 연결. APK에서 소스를 역산한 것은 아님) |
| debug | SHA-256 `bcc3ec70c90d785f708c3f12fd9232dce50aec60208fa0b11c44ca272df96389` · 68,431,143 bytes |
| signed release | `독수리4형제_MEMORIDE_release.apk` · SHA-256 `b609491fe468a1003d3cd1bb22f41693ad88742f9611b73b849b0fc5687aea5c` · 58,201,427 bytes · 팀 인증서 기록 `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb` (서명은 동봉 검증 기록 확인 범위) |
| A·B 대리·C 설치 식별 | 같은 debug·release 해시 (검토자 대조 PASS) |
| C 환경 | A와 다른 PC · host GPU NVIDIA RTX 5070 · Vulkan off · Pleos 2.1 / API 34 |

### 8-2. 판정표 (빌드 소스 `434abae4`)

| # | 항목 | 실행 주체 | 빌드 | 결과 | 적용 범위·주의 |
| --- | --- | --- | --- | --- | --- |
| 1 | 정적 · Python 66 · JUnit 391 · debug 빌드 | A | debug | **PASS (A 기록)** — 391개, 실패·오류·skip 0. 검토자가 suite별 요약·명령 로그로 합산 | 동봉 JUnit XML은 55개뿐. 같은 391개 기준으로 `a342825`·중간 `51cca9c`는 1 FAIL(A 기록). 실패 시험은 현재 렌더러가 쓰지 않는 옛 썸네일 합성 검사였고 HD 36종 검사로 교체됨(단순 허용 아님) |
| 2 | B 관련 시험 166개 재실행 | A 대리 | JVM | **PASS (A 대리)** — 19개 suite, 실패·오류·skip 0 | 391개와 중복 포함. 557개로 합산하지 않음. B 독립 PASS 아님 |
| 3 | A debug 기기 조작 | A | debug | **PASS (A 기록)** — 내비 왕복, 도착 결과, 팝업 취소·배경 차단, 사용자 공식 D/P 차단·복원, 기본 창 왕복 | 결과 닫기 근처 QEMU 종료로 이후 NOT RUN |
| 4 | A 일반 API 37 release 설치 · UNKNOWN 차단 | A | release | **PASS (A 기록)** | 일반 Android이며 Pleos 핵심 기능 PASS로 확대하지 않음 |
| 5 | 원주 첫 방문 · 재방문 · 결과 닫기 2회 · 월드맵(방문 2회·기념품 3개) · 기본 창 손잡이 1왕복 · 큰 글자·좁은 창 대표 하단 조작 · 프로세스 재시작 저장 보존 | C | debug | **PASS (C 독립)** · 저장 18항목 재검산 18/18 일치(검토자) | 목적지 준비는 **debug SDK 진단 도구의 정확한 Catalog 좌표 요청**. 일반 내비 검색만으로 원주를 발견한 근거가 아님. 저장 해시는 C 수집값에 의존(protobuf 원본 미동봉). **signed release PASS로 올리지 않는다** |
| 6 | release 설치 · 기본 섬/월드 · 내비 실행 후 복귀 | C | release | **PASS (C 독립)** | — |
| 7 | release 첫 방문 저장 · 결과 닫기 · 인벤토리 · 재방문 | C | release | **NOT RUN (C)** | — |
| 8 | release 첫 방문(선물 3개·성장 0→1) · 재시작 뒤 pending 결과 유지 | A 대리 | release | **부분 확인 (A 대리)** — UI XML에 결과 텍스트 존재 | PNG 3장이 XML과 다름(8-3). **정상 화면 표시·복원 PASS 근거에서 제외** |
| 9 | release 결과 닫기 · 아이템 사용·저장 · 재방문 | A 대리 | release | **NOT RUN / BLOCKED** — QEMU 소실 | 닫기 탭 시점에 장치가 없던 경우를 앱 ACK 결함으로 판정하지 않음 |
| 10 | release 일반 목적지 검색 → 지역 도착 → 기념품 전체 흐름 | A·C | release | **HOLD** — 일반 원주(C)·속초(A 대리) 검색 목적지가 Catalog 참조 좌표와 달라 등록 지역으로 연결 안 됨 | 8-4 |
| 11 | 저장 실패·내비 실패·원본 재시도 | A·B | JVM / 기기 | JVM **PASS (A 기록)** · 최신 후보 기기 **NOT RUN** | 최종 후보에서 `UxRehearsalActivity` 제거로 프리셋 재실행 불가. 4장 `84d473e` 프리셋 PASS를 최신 후보로 옮기지 않음. 제출 release에 실패 주입 UI를 넣어 해결하지 않음 |
| 12 | 창·글자 전환 | C | debug | **PASS (C 독립, 해당 표본)** | 기존 장애 해결로 일반화하지 않음 |
| 13 | 창·글자 전환 | C | release | **FAIL (전환 품질, C 독립)** · 원인 미확정 — UI dump timeout, 전환 뒤 빈 화면(PNG 15) 후 섬 재표시(PNG 16) | 표시 지연 관찰. 영구 정지·`NO_SURFACE`로 단정하지 않음 |
| 14 | 4장 1~6행 `a342825` 기기 시연 | — | — | 2026-11-13 기록 **NOT RUN 그대로** | 8장 결과는 `434abae4` 빌드에 대한 것 |

### 8-3. B 대리 화면 증거의 한계

B 대리 자료는 실행자·환경·범위를 표시한 유효한 보조 근거로 채택하되, 다음 PNG는 정상 결과 화면이나 화면 복원을 증명하지 않는다. 캡처 도구가 PNG를 먼저 찍고 약 5~7초 뒤 XML을 수집해 같은 순간이 아니다. 이 차이만으로 저장 실패·조작·거짓 보고를 단정하지 않는다.

- `22-first-visit-result.png` — XML은 결과, PNG는 희미한 도착 확인 팝업 잔상
- `visible-control/05-first-result.png` — XML은 결과, PNG는 확인 팝업 잔상
- `visible-control/06-pending-restored.png` — XML은 결과 텍스트, PNG는 빈 앱 영역

B에게 기기 검수를 반복 요청하지 않는다. 안정화 뒤 새 캡처는 A·C가 확보한다.

### 8-4. 일반 목적지(POI) 연결 제한

- `434abae4`/`c53e3f7`의 `NavigationInbox.kt`는 목적지 위도·경도가 Catalog 참조점과 각각 0.00001 미만 차이일 때만 `regionId`를 만들고, `regionId`가 없으면 `GameAppRoot`가 완료 확인을 막는다. 목적지 수신은 되지만 보상 지역으로 연결되지 않는 문제이며, SDK 수신 실패와 다르다.
- 진단 도구로 준비한 시연은 정식 저장 처리의 근거이지만, 제출 APK 단독 사용 동선의 근거가 아니다. 시연 기록에는 "진단 도구로 목적지 준비"를 명시한다.
- **`17bea54`(현재 HEAD, 검토 이후 커밋)**: 목적지 이름에 `원주시`·`속초시`·`대전광역시`/`대전시`가 들어 있거나 이름이 지역명과 같으면 그 지역으로 연결하고, 여러 지역이 걸리면 연결하지 않는다(`확인`: 코드 diff). 검토가 "이름만 보고 도시 인증으로 처리하는 변경은 별도 제품 정책과 회귀 검토가 필요"하다고 한 범주다. 빌드·시험·APK·기기 결과는 **NOT RUN**이며(2026-11-14 13:03 PM 기록 시점. A의 자동 검사 기록은 8-8에서 보완), 이 커밋으로 10행을 해결 처리하지 않는다. A의 정책 설명과 새 APK 해시, 영향 범위(도착 확인·`USER_CONFIRMED` 표기·중복 지역 이름) 재검증이 필요하다.

### 8-5. 기존 판정과의 관계

- 4장·5장의 `84d473e` 판정(PASS·부분 PASS·FAIL)과 2026-11-13 `a342825` NOT RUN 행은 그대로 둔다.
- 5장 adb 경로 화면 복원 **FAIL(`84d473e`, `STOPPED`/`NO_SURFACE`)은 historical OPEN으로 유지**한다. 이번 debug 정상 표본(12행)과 release의 `RESUMED`·surface 존재 표본을 같은 원인이나 완전 해결로 합치지 않는다.
- C host GPU 조건에서 QEMU 종료가 관찰되지 않은 것은 대체 검수 환경의 근거일 뿐이며, SwiftShader가 유일한 원인이라거나 이전 장애가 해결됐다는 판단이 아니다.
- Ready for review 전환은 검토 가능 상태를 뜻하며 인수 PASS·병합 완료가 아니다. 0장 첫 행의 "Draft" 표기는 2026-11-10 시점 기록이다.
- 4장 6행 시연용 주행 수치 표시: 제출 README·영상 설명은 측정값이 시연 표본이고 도착은 `USER_CONFIRMED`라는 사실에 맞춘다.

### 8-6. 지도·아트 출처

| 항목 | 상태 |
| --- | --- |
| 현재 지도 XML | SHA-256 `5be83ba3…`(검토자 확인, 3장 갱신 필요 표와 같음) |
| 캐릭터 72개 + 썸네일 9개 = 81개 | debug 81/81 · release 81/81이 C provenance 파일별 해시와 일치(검토자 대조). 파일 동일성 확인이며 권리 검증이 아님 |
| C 지도·출처 보완 패치 5파일 (`scripts/build_world_map.py`, `docs/ux_c_map/SOURCE.md`, `docs/ux_c_map/MAP_REPRODUCTION.json`, `assets/ATTRIBUTION.md`, `docs/asset_manifests/C_ART_PROVENANCE_20261113.json`) | `c53e3f7` 기준 독립 사본에서 `git apply --check` 통과. **#29 미반영(대기)**. 검토자는 공식 지도 ZIP으로 재생성하지 않음. `17bea54`에서 다시 확인 필요 |
| `assets/ATTRIBUTION.md` | 아직 과거 단순 도형 초안 설명 |
| B 원본 최초 제작 경위·생성형 AI·외부 재료, 섬 PNG 13개 최종 출처 | **대기**. C의 Blender 재렌더·생성형 초안 제외·외부 텍스처 검사와는 별개 |

### 8-7. 전체 판정

**전체 인수·최종 병합 권고: HOLD.** 남은 핵심은 (1) signed release에서 진단 도구 없이 가능한 일반 목적지 → 지역 도착 경로(`17bea54` 검증 포함), (2) 같은 release의 결과 닫기 → 아이템 사용·저장 → 재실행 → 재방문, (3) release 창·글자 전환 안정성, (4) 출처 패치 반영과 B 원본 출처다. 병합은 Create a merge commit·파트 브랜치 유지 원칙을 따르며, 이번 작업에서 커밋·푸시·병합·Ready 변경은 하지 않았다.

### 8-8. 최신 후보 `17bea54` 검증 상태 (2026-11-14 보완)

PR #29 HEAD는 다시 조회해도 `17bea5438af9ea7b4fb3f3a431a19ab33777adc5`이다(`확인`). 근거는 PR #29 본문과 `docs/LATEST_UI_CANDIDATE_20261114.md`(`17bea54`)다. A가 기록한 환경은 Windows AMD64 build 26200 · Python 3.10.4 · Android Studio JBR 25 · 저장소 Gradle 9.3.1 / AGP 9.1.1이고, 실행은 Git 기준 소스에 변경 4파일만 반영한 격리 작업 트리에서 했다고 기록돼 있다.

| # | 항목 | 실행 주체 | 결과 | 비고 |
| --- | --- | --- | --- | --- |
| 1 | 정적 검사 (`check_all.py --android`) | A | **PASS (A PR·문서 기록, PM 직접 실행 NOT RUN, 원시 로그·최신 APK 추가 수령 대기)** | exit 0이라는 기록 |
| 2 | Python 66 | A | **PASS (A PR·문서 기록, PM 직접 실행 NOT RUN, 원시 로그·최신 APK 추가 수령 대기)** | 전달 문서 반영 뒤 정적·Python 66 재검사와 `git diff --check` PASS도 A 기록 |
| 3 | JUnit 398 (Android) | A | **PASS (A PR·문서 기록, PM 직접 실행 NOT RUN, 원시 로그·최신 APK 추가 수령 대기)** | 실패·오류·skip 0. 도시명 인식 신규 6개, 도착 저장 통합 신규 1개 포함. 8-2 1행의 `434abae4` 391 PASS와 별개 |
| 4 | debug 빌드 | A | **PASS (A PR·문서 기록, PM 직접 실행 NOT RUN, 원시 로그·최신 APK 추가 수령 대기)** | 기록된 debug APK SHA-256 `091c1ca8fc5a2e83a6ddd201ad2a7ce85a3e0acadaa81b0a7d6b56dcbf1d6c37`. 커밋 전 후보 상태에서 빌드했으며, 커밋 전후 제품 소스 4파일이 바이트 단위로 같다는 것은 A 기록. PM은 이 APK를 받지 않았고 해시도 대조하지 않음 |
| 5 | 최신 후보 실제 기기 설치·조작 (Pleos 주차·제한·UNKNOWN, 저장 실패·원본 재시도·ACK 실패) | A·C | **NOT RUN** | A 문서도 NOT RUN으로 기록 |
| 6 | 일반 목적지(도시명 포함 POI) → 지역 도착 → 기념품 | A·C | **NOT RUN** | 도시명 인식은 JVM 시험만 있음. 도시명 없이 상호만 오는 경우는 지원하지 않음(A 문서) |
| 7 | 화면 검수 (결과 화면 순서·간격·스크롤, 창·글자 전환) | A·C | **NOT RUN** | — |
| 8 | 새 signed release (재서명·설치 검수) | A | **NOT RUN** | 기존 release `b609491f…`(소스 `434abae4`)에는 도시명·결과 화면 수정이 **포함되지 않음**(A 기록). debug APK를 제출 release로 표시하지 않음 |
| 9 | 도시명 연결 문제(8-2 10행) 해결 완료 | — | **HOLD** | 자동 시험 PASS 기록만으로 해결 처리하지 않음. 제품 정책 확인(이름 기반 지역 인정 범위, `USER_CONFIRMED` 표기)과 6행 기기 결과가 필요함 |

8-2의 `434abae4` 판정, 4장·5장의 `84d473e` 판정과 `a342825` NOT RUN 행은 그대로 둔다.

## 9. signed release v6 및 1차 프로토타입 제출 결정 (2026-11-14)

이 절은 앞선 SHA별 이력을 수정하지 않고 추가한다. 8-8은 당시 17bea54의 검증 상태이며 아래의 후속 수령 상태와 구분한다.

| 항목 | 후속 확인 |
| --- | --- |
| 빌드 소스 | 17bea5438af9ea7b4fb3f3a431a19ab33777adc5 |
| 통합 HEAD | 8b532a342711ca76be00027834d4c3b4c0320932; 빌드 소스 이후 전달 문서 2개만 변경 |
| signed release v6 | 0.1.0 / versionCode 6 / io.kailand / 58,201,427 bytes |
| APK SHA-256 | 7f6f2ae9ee4cf74145368add969c8f978b5c5e8195a4fa9980d47273600cb311; PM 검토 환경에서 직접 재계산 일치 |
| 서명/업데이트/실행 | A 기록과 같은 v6의 전달 자료; 기존 팀 키, signed v4→v6 설치 및 Main 실행 확인 |
| 일반 목적지 | 같은 v6의 앞선 A 실행 증거: 내비 UI에서 속초시청 강원 선택·안내→MEMORIDE 아이콘 복귀→도착 확인→기념품 3종 NEW/성장 0→1. SDK 목적지 주입 미사용. 다른 지역 일반 검색은 NOT RUN |
| USER_CONFIRMED/표본 | 사용자 확인을 실제 방문 인증으로 표시하지 않음. 주행 수치는 실측이 아닌 시연 표본. PM의 1차 제출 정책으로 이 범위를 수용 |
| 섬 복귀/결과 닫기 | v6 NOT RUN/BLOCKED. QEMU 종료로 확인하지 못했으며 버튼 결함 확정은 아님 |
| 추가 최소 재시도 | AVD 디스크 부족(약 1.77GiB 잔여)으로 APK 설치 전 중단. 재시도 앱 실패로 바꾸지 않음 |
| 일반 Android UNKNOWN | A의 같은 release 기록: 정식 안내/조작 차단. 실제 Pleos D↔P·foreground UNKNOWN 검수 대체 아님 |
| 출처 보완 | C의 기존 지도/81개 자산 근거를 이번 제출 정리에 반영. 최초 B 제작 경위와 섬 13개 출처 미완료 유지 |
| 전체 제품 인수 | **HOLD 유지.** 과거 PASS/FAIL/partial PASS 및 창 복원 FAIL은 닫지 않음 |
| 1차 제출 산출물 | **PM이 v6 선택.** 문서·소스 정리와 APK 게시/메일 제출 완료는 구분 |

Python 66·JUnit 398·debug PASS는 A의 기존 기록이고 이번 PM 문서 정리에서 다시 실행한 Android 결과가 아니다. A 대리 166을 더하지 않고 C의 이전 debug PASS를 v6 release로 올리지 않는다. 자세한 전달 범위는 [v6 기록](SUBMISSION_RELEASE_V6_20261114.md), 배포 상태는 [1차 제출 결정](FIRST_ROUND_SUBMISSION_20261114.md)을 따른다.
