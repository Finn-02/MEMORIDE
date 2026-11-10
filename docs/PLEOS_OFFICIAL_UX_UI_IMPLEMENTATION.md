# 공식 현대차·Pleos 기준과 MEMORIDE 와이어프레임 적용 지침

확인일: 2026-11-10 KST. 대상: A/B/C/PM 전원. 기준 Figma: [MEMORIDE 화면](https://www.figma.com/design/xFJG4ZaZg3baP5KmxbTtcx).

## 팀 공통 개발 지시

**개발 전에 본인 화면에 해당하는 최신 공식 문서와 Figma 프레임을 모두 읽고, Pleos 실제 앱 창에서 와이어프레임과 대조하며 구현한다.** 일반 Android에서 빌드되거나 휴대폰 크기로 표시되는 것만으로 완료 처리하지 않는다. 공식 문서를 확인했다는 말 대신 적용한 항목·실제 창·캡처·미확인 사항을 남긴다.

적용 순서는 실제 플랫폼 제한·공식 필수 요건 → 사용자가 확정한 기능 범위 → 와이어프레임의 흐름·정보 위계·스타일 → 임시 px 수치다. 대비·물리 가독성·터치·안전 영역 때문에 필요한 크기/색/간격 보정은 담당자가 진행한다. 기능 삭제·새 기능 도입·주행 허용 확대는 임의로 결정하지 않는다. 화면의 의미를 바꾸는 충돌은 근거와 최소 변경안을 남겨 PM과 조정한다.

기존 동행·방문·일상 이동 제외, 수동 복귀/사용자 도착 확인, 등록 지역 하루 최초 성장+1/상한3 규칙은 유지한다. 음성·새 서비스·야간 화면 등의 공식 권고를 읽었다는 이유로 새 기능 범위를 늘리지 않는다.

## 공식 자료의 성격

- 현대차그룹의 [Pleos UX 소개](https://www.hyundaimotorgroup.com/ko/story/pleos-connect-next-generation-infotainment-system)는 직관성·단순함과 시스템 영역/앱 영역 분리를 설명하는 배경 자료다. 이를 개별 버튼 치수의 근거로 사용하지 않는다.
- 구현 수치는 [Pleos UI 가이드](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro), 실제 동작은 [앱 설계 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro), 제출 검수는 [App Review Guidelines](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide)를 사용한다.
- 공개 목차를 따라 UI34개·앱 설계10개·심사6개, 총50개의 고유 문서 본문을 확인했다. 전체 URL과 적용 범위는 `PLEOS_OFFICIAL_SOURCE_INDEX.md`에 있다. 이미지로만 제공되는 세부 size/spacing/token은 본문만으로 검수 완료하지 않았다. 본인 컴포넌트 구현 전 해당 원본 도면과 실제 표시 크기를 추가 대조한다. 공개되지 않은 내부 가이드나 모든 배포 파일을 검증했다는 뜻은 아니다.
- 현재 공개 가이드와 팀 AVD/SDK 버전은 구분한다. 현 AVD는 Connect2.1/API34, NaviHelper는 별도 SDK 버전이다. 문서를 읽었다는 이유로 팀 툴체인을 임의 업데이트하지 않는다.

## 1. 화면·창·시스템 영역

[Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout)과 [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)를 따른다. 앱 영역은 시스템의 Driving view·Status bar·GNB와 다르고, 시스템 패널이 앱 위를 덮을 수 있다. Connect-S의 분할/확장과 Connect-W의 다른 창 비율을 고려한다. 최신 문서는 기존 Connect-L을 Connect-S로 재분류하므로 구 문서의 세 분류를 고정 제품 규격으로 복사하지 않는다.

- Figma 1280×720/640×720/720×1280는 배치 참조다. px=dp 고정 대입·기준 그림 전체의 단순 확대/축소·상단56 고정 예약은 금지한다.
- A는 실제 app window bounds, density, system insets를 **창 상태별로** 측정하여 공유한다. 현재 AVD 전체2560×1440/density160은 앱 창 크기가 아니다.
- B/C/PM은 Figma의 넓은 2열·좁은/세로 재배치를 실제 창에 대응한다. 제목·팝업·버튼은 콘텐츠에 맞게 높이를 늘리고 스크롤 가능하게 한다.
- [앱 구조·동작](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)과 [기능/성능 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)에 따라 resize 전후 기능과 작업 내용을 보존한다. 좁다는 이유로 이동 마무리·확인·취소·retry를 없애지 않는다. 보유 상태 등으로 의도적으로 숨기는 규칙과 창 크기로 기능을 누락하는 것을 구분한다.
- 시스템 제스처·window bar는 플랫폼 소유다. 앱이 동일 제스처로 시스템 영역을 흉내내거나 침범하지 않는다. 방향 Manifest 설정은 A가 실제 버전에서 검증한다.

## 2. 색·글자·아이콘

[UI 개요의 대비/가독성 기준](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)은 대비4.5:1, 중요 정보 글자 높이5mm·보조3mm를 안내한다. 굵거나 크다는 이유로 이 가이드의 대비 예외를 가정하지 않는다. 원본 UX의 중요4mm 설명은 정정한다.

[Typography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography)는 글자 위계와 판독성, 픽셀 높이 확보를 안내하며 시스템 폰트 자료로 Asta Sans를 제공한다. Figma의 Noto Sans KR을 반드시 Asta로 바꿔야 한다는 뜻은 아니다. 현재 Android fallback이 Figma 글꼴과 동일하다고 주장하지 말고 실제 한글 줄바꿈을 검수한다. 논리 density만으로 실제 mm를 인증하지 않는다.

- 원본 주요 버튼 Ocean 끝(#2E86AB)에 흰 글자는 계산상4.11:1, 흰 보조문구 alpha78%는 같은 바탕에서 약3.13:1이다. 이는 끝점 조합 계산이며 전체 버튼의 모든 픽셀을 측정한 것은 아니다. 실제 글자·아이콘이 놓이는 영역을 검사하고 바탕 어둡게 조정/불투명 글자 등 최소 보정을 적용한다. **Figma 원본 토큰은 대비 통과값이 아니다. A 후속 구현은 OceanAccessible #267493과 불투명 흰색으로 보정했다(범위: UX_A01_PLEOS_WINDOW_BASELINE.md). 전체 화면 대비·물리 가독성 인수는 별도다.**
- 반투명 패널은 섬·지도·그림 위에서 합성된 배경을 사용해 검사한다. placeholder 위 수치와 실제 자산 위 대비는 다를 수 있다.
- [Iconography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/contents-creation/iconography)의 기본 그리드48×48dp는 아이콘 제작 기준이다. 이것을 모든 아이콘의 고정 표시 크기나 최소 터치 크기로 해석하지 않는다. 원본 도면의 variant/size 및 실제 터치 박스를 구분한다.
- [아이콘 버튼](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/icon-button)은 의미가 직관적이어야 한다. 닫기에는 접근성 이름을, 주요 행동에는 글자 라벨을 유지한다. Figma의56dp 터치 값은 임시 구현값이며 공식 전역 최소치로 인용하지 않는다.

## 3. 상호작용·팝업·저장

[기본 버튼](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button)의 눌림/비활성/처리 중 상태를 구현한다. [텍스트 버튼](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/text-button)과 [팝업](https://document.pleos.ai/docs/connect/guide/docs-design/components/popups/system-popup)의 확인·취소 의미도 일관되게 유지한다.

[성능 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)에서 터치 피드백0.25초, 버튼 반응2초, 앱 실행/콘텐츠10초 기준을 확인했다. 이 수치는 저장 완료를 강제하는 타이머가 아니다. 비동기 저장에는 즉각 처리 중 상태를 보여 주고 완료 전 재입력을 막는다. 저장이 오래 걸렸다는 이유로 지급 실패를 단정하거나1.5초 후 결과로 이동하지 않는다.

- S02-2 확인/취소, S07 retry/섬 복귀를 명확히 구분한다. 팝업은 현재 앱 창 안에서 조작 가능해야 한다.
- S06 retry는 원본 ID/내용 유지, 닫기는 재시도 상태 보존. S05의 섬/캐릭터 선택은 receipt ack 성공 후 이동한다.
- [Tabs](https://document.pleos.ai/docs/connect/guide/docs-design/components/segmented-menu)의 선택/눌림 상태를 S03 세 탭에 대응한다. 색만으로 보유/잠김/사용 중을 구분하지 않는다.
- [Animation](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/animation)의 존재가 무한 회전이나 주행 중 연출 허가를 뜻하지 않는다. MEMORIDE는 정지 진행 표시와 주차 중 짧은 캐릭터 반응이라는 확정 흐름을 유지한다.

## 4. 주행·복귀·데이터

[주행 규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)와 [주의 분산 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements)를 적용한다. 게임을 내비 카테고리로 취급해 주행 허용을 확대하지 않는다. 주차 및 이용 허용을 함께 확인하며 UNKNOWN은 차단한다. D/제한 전환 시 게임 UI·연출·입력을 중지한다. DO/overlay/ADB 제한 우회는 추가하지 않는다.

- [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)의 일반 App-to-App 복귀 설명을 ‘내비 목적지 도착 시 자동 복귀 보장’으로 해석하지 않는다. 사용자가 MEMORIDE를 직접 여는 확정 흐름 유지.
- 내비를 띄운 사실/목적지 수신/사용자 확인은 각기 다르다. 저장 출처 USER_CONFIRMED와 미측정 null 유지. 복귀·resize는 새 보상 명령을 만드는 사건이 아니다.
- [앱 구조](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)의 복귀·재생성 전략에 맞춰 선택·탭·지도 상세·목적지 revision·pending receipt를 보존한다. 콜드스타트 S01 정책은 유지하되 이미 저장된 진행이나 미확인 결과를 잃지 않는다.
- [API 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)은 대상 차량에서 확인한다. 표의 일반 호환 안내를 background 수신·자연 도착·이동거리 수신 성공의 증거로 대체하지 않는다.
- [개인정보 심사](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-user-privacy-security)에 비춰 목적지 수신/보관의 목적·항목·보유기간·동의 처리를 A/PM이 점검한다. ‘도착했어요’는 개인정보 수집 동의 버튼이 아니다. 실제 수집 전 필요한 고지/동의가 빠져 있다면 누락 항목으로 기록하고 처리 방식을 확정한다. 새 로그인·분석 서비스는 추가하지 않는다.

## 5. 추가 검토와 범위 유지

- 주야간 자동 밝기/대비 권고는 현재 ‘코스탈 모닝 주간만’ 범위와 차이가 있다. 야간 지원 미확인으로 남기고 PM이 실제 제출 요건·범위를 결정한다. 주간1종만으로 전체 가이드 준수를 선언하지 않는다.
- [성능/리소스](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt)의 CPU·RAM 기준은 A 통합 검수 대상으로 기록한다. PNG 캐시/로딩을 절제하고 불필요한 background timer/polling을 중단한다. 측정 전 PASS 금지.
- [App Icon](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/contents-creation/app-icon)의 adaptive foreground/background·masking을 최종 앱 아이콘에 적용한다. 현재 Android 기본 아이콘은 최종 산출물이 아니다.
- [콘텐츠 안전](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-content-safety), [등록정보](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-app-registration-info)는 PM이 최종 제출 때 확인한다. 게임·지도·이미지 출처/권리와 사실에 맞는 스크린샷을 관리한다. 스토어 제출 이미지 규격을 런타임 앱 창 크기로 사용하지 않는다.
- 가이드 목차에 keypad, 입력 필드, slider, spinner, notification, widgets가 있다는 이유로 현재 flow 밖 기능을 구현하지 않는다. 적용 여부는 출처 목록에 구분했다.

## 완료 증거

각 파트 PR에 ‘해당 공식 URL·Figma frame ID / Pleos 버전·실제 app bounds·density·insets / 넓은·좁은·세로 캡처 / 상태·터치·대비·글자 검수 / PASS·FAIL·NOT RUN’을 남긴다. 정확한 수치를 아직 모르면 임의로 채우지 않는다. Preview는 독립 개발 수단이며 실제 Pleos 확인을 대신하지 않는다. 세부 화면별 항목은 `PLEOS_WIREFRAME_COMPLIANCE_MATRIX.md`를 사용한다.
