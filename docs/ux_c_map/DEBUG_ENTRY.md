# A 연결용 debug 샘플 진입점

C의 `WorldMapSampleActivity`와 Preview는 debug 소스에만 있다. 공통 Manifest는 A 소유이므로 이번 PR에서 변경하지 않는다.
아래 항목은 로컬 UI 검사 때만 debug Manifest에 임시 등록했고, 검사 뒤 제거했다. release에는 등록하지 않았다.

```xml
<activity android:name="io.kailand.feature.world.WorldMapSampleActivity"
    android:exported="true" android:label="MEMORIDE C 월드맵 샘플" />
```

A가 독립 검사 진입점이 필요하다고 판단할 때만 debug application 아래 연결한다. 실제 앱 연결은 별도로 `NewWorldSlot`에 `WorldMapScreen`을 전달한다.
샘플은 기존 MainActivity의 개발 gate를 사용하므로 Automotive에서는 차단되며, 주차 판정/실차 허용 근거가 아니다.
`sample=fresh/wonju/regional`, `selected=true/false`, `history=available/empty/unavailable/loading` Intent extra를 읽는다.
`wide=true`는 일반 Android 스크린샷용 debug 방향 요청이며 Pleos의 실제 창 비율을 강제하거나 검수하는 용도가 아니다.
실제 Pleos 화면은 A가 실제 gate와 창 정책을 유지한 앱 연결에서 확인한다. production에 샘플 데이터를 전달하지 않는다.
