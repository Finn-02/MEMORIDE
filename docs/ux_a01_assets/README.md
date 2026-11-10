# UX-A01 Figma 아이콘 원본

MEMORIDE Figma 파일 `xFJG4ZaZg3baP5KmxbTtcx`에서 get_design_context가 제공한 원본 SVG를 다운로드했다. 임시 URL은 실행 코드에 포함하지 않는다.

| 원본 | 컴포넌트 / 아이콘 노드 | 원본 크기 | Android drawable |
| --- | --- | --- | --- |
| departure.svg | 5:43 / 5:44 | 32×32 | memoride_departure.xml |
| inventory.svg | 5:49 / 5:50 | 28×28 | memoride_inventory.xml |
| close.svg | 8:137 / 8:136 | 24×24 | memoride_close.xml |

Android vector drawable은 원본 SVG의 viewBox·path·stroke·rect를 그대로 변환했다. 닫기 아이콘은 Figma 버튼 내부 표시 크기인 28dp에 배치한다. 주요 버튼 32dp, 보조 버튼 28dp. 공통 버튼은 선택적 아이콘 슬롯을 제공하며 기능별 아이콘은 화면 담당자가 공급한다.

현재 호출 위치는 debug의 `UxFoundationPreview.kt`다. 제품 화면 연결은 후속 UX-A02~04 작업이다. SVG의 일러스트 자리표시자를 실제 게임 자산으로 취급하지 않는다.
