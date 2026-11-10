# UX-A01 Pleos 공통 UI 확인 화면

debug의 `io.kailand.app.UxFoundationActivity`는 기존 `VehicleGateMonitor`가 실제 주차 및 플랫폼 이용 허용을 확인한 경우에만 공통 UI 갤러리를 표시한다. UNKNOWN/RESTRICTED 또는 백그라운드에서는 갤러리를 표시하지 않는다. 일반 Android 개발 진입점의 가짜 허용 상태는 사용하지 않는다.

이 Activity는 GameService, 내비 observer, 게임 진행 명령을 참조하지 않는다. 버튼은 로컬 확인 문구만 바꾸며 실제 내비나 인벤토리를 열지 않는다. 새 권한·DO 선언·overlay·제한 우회는 추가하지 않았다. 기존 game 저장을 삭제하거나 초기화하지 않았다.

debug APK 설치 후 실행:

```text
adb -s <Pleos-device> shell am start -n io.kailand/io.kailand.app.UxFoundationActivity
```

2026-11-10 Windows 검수에서 기존 Pleos Connect 2.1 / API34 AVD에 업데이트 설치했고 P 상태에서 공통 UI가 표시되는 것을 확인했다. 현재 AVD 물리 화면은 2560×1440, density160이며 앱은 Pleos 시스템 UI가 차지하고 남은 창에 배치된다. 이 수치를 모든 Pleos 기기의 규격으로 일반화하지 않는다.

검증: 기존 내비 미커밋 코드를 제외한 분리 작업트리에서 새 Activity/Manifest만 추가해 JUnit264(실패/오류/건너뜀0), debug APK 빌드, release Kotlin 컴파일 PASS. 새 Activity는 debug에만 포함된다. 패키지 정적 검사 PASS. Pleos 화면 렌더링 확인은 전체 SDK 기능·게임 흐름·실차·서명 release 인수를 뜻하지 않는다. 이번 새 Activity의 D/P 전환·연결 상실 회귀는 NOT RUN이며 실제 gate 정책은 기존 구현을 재사용한다.

전체 화면 연결은 여전히 UX-A02~04 후속 범위다. 이 확인 화면은 공통 테마·버튼·패널 개발 결과를 보기 위한 것이며 Figma 전체 게임 화면 완성본이 아니다.
