# UX-A01 후속 · Pleos 실제 창 기준과 공통 대비 보정

기준일: 2026-11-10 KST. 환경: Windows, Pleos Connect2.1/API34 AVD. 최신 팀 문서와 Figma 5:43(주요 버튼)의 디자인 문맥을 다시 확인하고 작업했다. Figma 자체는 변경하지 않았다.

## 실제 창 기준

현재 AVD 전체 디스플레이는2560×1440px/density160이다. 아래는 전체 화면과 구분해 Android currentWindowMetrics와 Compose 콘텐츠 viewport로 읽은 **앱 창**이다. density1이어서 이 측정 환경에서는 px와 dp 숫자가 같지만 모든 기기에 px=dp를 대입한다는 뜻은 아니다.

| 실제 시스템 상태 | 앱 창 bounds(px) | 앱 창/Compose viewport | density | insets |
| --- | --- | --- | --- | --- |
| 가로 확장 | (853,90)~(2535,1300) | 1682×1210 | 1 / 160dpi | systemBars/cutout/IME 모두0, IME 숨김 |
| 시스템 window bar를 드래그한1:1 분할 | (853,90)~(1694,1300) | 841×1210 | 1 / 160dpi | systemBars/cutout/IME 모두0, IME 숨김 |

시스템 상태바·Driving view·GNB가 이 앱 창 바깥에 이미 배치되어 있다. 따라서 현재 상태에서 추가로 상단56dp를 비우면 이중 예약이다. 공통 MemorideScreen이 실제 insets를 한 번 처리한다. 다른 preset/키보드/차량에서도 항상 insets0이라고 가정하지 않는다. 위 수치는 현재 AVD의 실측 샘플이며 다른 팀 PC의 절대 고정값이 아니다.

‘portrait 방향 강제’ 진단에서는1198×1210 창과 좌우 검은 여백이 생겼다. 정상 분할 기준으로 채택하지 않았고, 진단용 방향 요청은 최종 코드에서 제거했다. **실제 분할은 시스템 window bar의 정상 드래그로 확인했다.** Manifest 방향 고정으로 Figma 비율을 억지로 만들지 않는다. Figma640×720 가로 좁은 창과720×1280은 배치 참조이며, 이 AVD에서 해당 숫자와 같은 별도 preset을 검증한 것은 아니다.

## B/C/PM 적용

- 실제 사용 가능한 width와 height, 글자 배율을 함께 보면서 넓은2열과 좁은/세로1열을 선택한다. 폭만840dp 이상인지 검사하면841×1210 세로 분할을 넓은2열로 오인할 수 있다.
- 팀원이 받은 Figma 프레임은 정보 순서·비율·컴포넌트 위계의 기준이다. 실제 window에서 콘텐츠가 잘리면 유연한 높이·재배치·스크롤을 적용한다.
- AppRoot의 상위 셸이 insets를 처리하면 하위 화면에서 중복 처리하지 않는다. 시스템 UI를 앱에서 그리지 않는다.
- 창 크기가 바뀌어도 선택·탭·지역 상세·목적지·pending receipt를 잃지 않게 한다. 재구성이 새 저장/보상 명령의 트리거가 되어서는 안 된다.
- 이 검수는 공통 UI 갤러리의 결과다. 각 feature 화면의 반응형 인수는 담당자가 별도로 수행한다.

## 공통 대비 보정

원본 Ocean #2E86AB는 artwork 참조값으로 유지하고, 흰 글자가 올라가는 공통 주요 버튼의 gradient 끝과 Material secondary에 **OceanAccessible #267493**를 사용한다. 계열·역할을 유지하면서 공식 대비 기준에 맞춘 구현 보정이다. Figma 버튼의 보조문구 alpha78% 대신 불투명 흰색을 사용하고 크기로 위계를 유지한다. 비활성 주요 버튼 글자는 ProgressEmpty 위의 TextPrimary로 변경한다.

| 실제 텍스트/바탕 조합 | 계산상 대비 |
| --- | --- |
| 흰색 / Navy #1F4E79 | 8.66:1 |
| 흰색 / OceanAccessible #267493 | 5.25:1 |
| 비활성 TextPrimary / ProgressEmpty | 12.37:1 |

색상 쌍 수치 검증이며 그림 위 반투명 패널·pressed ripple·모든 이미지·실제 물리 글자 크기 전체를 통과했다는 뜻은 아니다. 기존 Figma 원본 색과 잘 구분하여 문서·스크린샷에서 설명한다. B/C/PM이 주요 버튼을 직접 재구현하면서 원본 Ocean+alpha78% 조합으로 돌아가지 않게 공통 버튼을 사용한다.

## 측정 재현

공유 debug APK에서 `UxFoundationActivity`를 실행한다. `MemorideWindow` logcat 태그가 currentWindowMetrics, decor 크기, visible frame, density/fontScale/orientation, systemBars/cutout/IME, 실제 Compose viewport를 JSON으로 기록한다. 변경이 있을 때만 기록하고 onPause에서 관측을 정리한다. 화면·기어·플랫폼 제한 값을 쓰는 도구가 아니다. 게임 저장·목적지·내비 명령은 호출하지 않는다.

```text
adb -s <Pleos-device> shell am start -n io.kailand/io.kailand.app.UxFoundationActivity
adb -s <Pleos-device> logcat -d -v raw -s MemorideWindow:I *:S
```

실제 창 크기는 Pleos 시스템 window bar로 변경한다. `wm size/density`로 실제 preset인 것처럼 가장하거나 제한을 우회하지 않는다. 실제 주차/gate 허용 조건은 유지한다.

## 검증과 남은 범위

독립 작업트리에서 기존 내비 미커밋 변경을 제외하고 이번 공통 UI/측정 변경을 적용했다. Python59, JUnit267(기존264+대비3), debug APK 빌드·release Kotlin compile을 확인한다. 창 전환 시 gate가 잠시 UNKNOWN이 되어도 SaveableStateHolder가 허용 화면 밖에서 복원 상태를 보관한다. 제한 중에는 갤러리 자체를 제거한다. 최종 결과와 실제 창 전환 증거는 이번 검토 ZIP의 REPORT.md에 기록한다.

NOT RUN: 전체 새 flow 연결, B 새 REGION 성장 규칙, 전체 feature의 창별·물리 글자·모든 배경 대비, 별도 차량/다른 Pleos preset, 실차·서명 release. 새 실제 창 측정과 팔레트 보정이 이러한 항목의 완료를 뜻하지 않는다.

다음 A 작업: 합의된 소비자 화면을 새 route에 연결하고 목적지 카드·도착 확인·내비 실패·저장/ack 흐름을 적용한다. 기존 제외 기능의 진입점 제거는 UX-A02~04에서 수행한다.
