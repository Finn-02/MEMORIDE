# UX-C01 월드맵 독립 구현 시작

2026-11-10 KST / Part C / Windows 11 AMD64.

## 현재 단계

기존 C05 기능과 별개의 새 UX-C01~04 배정에 따른 작업이다. C07이라는 신규 단계를 만들지 않는다.
기존 C 브랜치에 origin/main `40d1f2f`를 반영했다. A/B 코드와 공유 계약은 수정하지 않았다.

- UX-C01: `WorldMapScreen`과 독립 Preview 추가. 빈 상태·발견한 등록 지역만 표시·선택/상세 닫기·화면 닫기.
- 최초 자리표시자를 Natural Earth 대한민국 정적 벡터로 교체했다. 실제 Catalog 위도·경도를 지도와 같은 투영으로 배치한다. 출처/재현은 [지도 출처](ux_c_map/SOURCE.md)를 따른다.
- 넓은 창은 지도/상세 2열, 좁은 창·큰 글자는 세로 스크롤. 선택 ID는 rememberSaveable로 보관하며 새 읽기 상태에서 사라진 지역은 상세를 표시하지 않는다.
- gate/foreground 허용 전에는 지도와 조작을 제거한다. 클릭 시 최신 상태를 다시 확인한다.
- 인벤토리·목적지 요청·저장 쓰기를 추가하지 않았다. 기존 WorldScreen과 AppRoot는 그대로이며 새 제품 연결은 A 담당이다.

## A 연결 예

새 함수는 `core/ui/NewUxScreenSlots.kt`의 `NewWorldSlot`과 동일한 매개변수 순서다.

```kotlin
val world: NewWorldSlot = { snapshot, catalog, history, gate, close, modifier ->
    WorldMapScreen(snapshot, catalog, history, gate, close, modifier)
}
```

A의 상위 셸이 insets를 한 번 적용한다. C는 고정 56dp 시스템 영역을 그리지 않는다.
RegionVisitHistory는 이미 제공된 읽기 계약만 소비한다. UX-C02 후속으로 방문 횟수/날짜/보유 기념품 상세 표시를 구현했다.
샘플은 기존 debug Preview 자산에서만 읽으며 실제 사용자 저장에 주입하지 않는다.

## 확인 근거와 남은 단계

Figma `xFJG4ZaZg3baP5KmxbTtcx` 프로토타입을 내장 브라우저에서 직접 조작해 빈 지도 `49:1428`, 원주 발견 `49:1501`, 원주 선택/상세 `10:305`를 확인했다.
레이어 MCP는 호출 한도로 차단되어 세부 수치는 ZIP S04 명세를 사용했다. 처음처럼 목록형 버튼을 지도 대신 쓰지 않고 실제 지도 위 아이콘, 상단 제목/안내/닫기, 지도 오른쪽 또는 아래 상세 배치로 보정했다.
기존 ZIP의 지시 문구 자체를 코드 삭제나 저장 규칙 변경 승인으로 취급하지 않았다.

[공식 Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window) 본문을 이번 작업에서 확인했다.
창별 가로/세로 전환을 고려한다. 아이콘 버튼 공식 페이지는 조회 실패했으므로 치수 도면 확인 완료를 주장하지 않는다.
공통 버튼·색상·패널은 A의 컴파일 계약과 토큰을 재사용한다. 실제 대비/물리 크기/터치 인수와 별개다.

남은 순서: PM 지도 출처 검토·추가 창/아이콘 검수 → UX-C03 네 시연 → A 실제 연결 → UX-C04 실제 Pleos 창별 검사.
Preview 코드 컴파일을 렌더링 확인이나 실제 Pleos 인수로 표시하지 않는다.

## UX-C02 — 방문 기록 상세 구현

방문 횟수는 제공된 RegionVisitSummary만 표시한다. 날짜는 UTC epoch를 Asia/Seoul의 `yyyy.MM.dd (E)`로 변환하며 연도를 포함한다.
Loading/Unavailable은 미확인, Available에서 해당 지역 기록이 없으면 알려진 빈 기록(0번)으로 구분한다.
null 날짜는 미확인이다. epoch0을 누락으로 취급하지 않으며, 중복 지역 행·음수 횟수·첫 날짜가 최근 날짜보다 늦음·0번인데 날짜 있음은 임의 복원/합산하지 않는다.
기념품은 해당 지역 Catalog 참조 중 소유한 아이템만 표시한다. 종류/이미지/오류를 읽기 전용으로 보여 주며 사용·인벤토리 이동·지급 명령을 추가하지 않았다.

독립 debug Preview는 예시 횟수/날짜라는 안내를 함께 표시한다. 실제 연결에서는 B/A가 확정 읽기 기록을 공급해야 하며 C가 fixture를 채우지 않는다.
기본 regional fixture는 기존 packaged sample의 대전이다. 이후 원주 전용 debug fixture를 추가했으며 기존 Catalog에서 지역·아이템을 읽어 구성한다. 파일의 날짜/횟수 예시는 debug에서만 공급한다. 네 시연의 저장/성장/오류 흐름 완료 증거로 사용하지 않는다.

Windows `JAVA_HOME=기록된 Android Studio JBR`로 `py -3 -X utf8 scripts/check_all.py --android` exit0.
Python59, JUnit280(신규 방문 표시4개 포함) 실패/오류/skip0, debug 빌드16초(11 executed/34 up-to-date) PASS.
KST 연도 경계·날짜 null/epoch0·로딩/실패/알려진 빈 기록·잘못된 입력을 단위시험했다.
실제 B 공급 데이터 연결·UX-C02 버튼/스크롤 전수·Pleos/큰 글자/회전/전체 대비/접근성·release는 NOT RUN이다.

위 문단은 초기 구현 시점 결과다. 후속 PR 전 일반 Android 좁은 창/글자2.0 표본은 아래 추가 결과를 따른다.

## 파일/Figma 대조 후 지도 수정과 실행 검사

빈 지도/원주 아이콘/선택 상세의 순서를 실제 프로토타입에서 확인했다. 원본의 형태를 유지하되 정적 지형은 공개 원본을 사용했다. 별도 지도 SDK/API/유료 서비스/권한/네트워크 호출을 추가하지 않았다.
A의 실제 창 기준 `1682×1210`은 2열, `841×1210` 세로 분할은 아래 상세로 분기한다. 폭만으로 세로 분할을 2열로 판단하던 조건을 수정했다. Figma px를 실제 dp로 고정하지 않는다.

최초 지도 XML을 단일 path로 합친 빌드는 AAPT의 `STRING_TOO_LARGE` 치환 경고 후 Compose에서 `Unknown command for: R`로 종료됐다. 해당 실패 캡처를 성공 근거로 쓰지 않았다. 생성기를 링별 path로 수정했고 path 크기/명령 검사 시험을 추가했다.
수정 후 Windows 공통 검사 exit0: Python59/JUnit284(실패/오류/skip0), debug 빌드12초(10 executed/35 up-to-date) PASS.
기존 C 일반 API37/1080×2400/density420/font1.0 환경에서 데이터 유지 설치 후 공개 debug 샘플을 실행해 빈 지도·원주 발견·선택 상세 캡처를 확인했다. 같은 PID 생존과 이 세 표본 중 새 fatal 기록 없음도 확인했다.
짧은 일반 Android 가로 창에서는 지도/상세 하단이 화면 아래로 이어진다. 이번 캡처는 지도와 상단 상세의 실제 렌더링 확인이며 전체 스크롤/버튼 도달 인수는 아니다. 실제 Pleos·원주 저장·전체 시연·전체 접근성 완료로 확대하지 않는다.

## UX-C01~02 PR 전 추가 확인

- 일반 API37 전용 AVD(1080×2400, density420, 약411dp 세로)에서 원주 아이콘 터치→상세, 날짜·기념품3종·상세 닫기까지 스크롤 도달, 닫은 뒤 지도 복귀 PASS.
- 월드맵 상단 닫기는 샘플 finish 콜백까지 실행했다. 실제 섬 route 복귀는 A 미연결로 NOT RUN이며 동일 결과로 표시하지 않는다.
- 공개 Android 설정 UI로 글자2.0을 적용하고 아이콘 선택·방문 날짜·기념품3종·상세 닫기 도달과 닫기 후 복귀 PASS. 검사 후 UI로1.0 복원, 읽기로 확인했다.
- 세 지역이 발견됐을 때 설명 영역이 겹치지 않도록 실제 좌표와 설명 위치를 분리하고 연결선을 표시한다. 최초 간격 회귀 시험 실패 후 배치를 수정했다. 약363×360dp 보드의 글자1.0/2.0 세 지역 배치 시험 PASS. 세 지역 전부의 실제 기기 터치 전수는 NOT RUN이다.
- 최종 간격 수정 후 JUnit285(실패/오류/skip0), Python59/debug 빌드14초 PASS. 최종 APK에서 글자2.0 원주 선택·하단 도달·상세 닫기를 다시 확인했다. 기본 글자 전체 조작은 직전 후보에서 확인했고 최종 후보 기본 글자는 복원 화면 확인이다.
- 공통 Manifest에 임시 추가했던 debug 검사 진입점은 PR에서 제외했다. [A용 연결 예제](ux_c_map/DEBUG_ENTRY.md)를 전달하며 production AppRoot/기존 WorldScreen/공유 DTO는 변경하지 않았다.
- 실제 Pleos 창별 조작, live B 기록·저장 승계, 실제 섬 복귀, 네 시연, TalkBack/OS 다중창/전체 대비·물리 터치·접근성은 후속 항목이다. Preview·일반 Android 보조 검수로 UX-C04 완료를 선언하지 않는다.
