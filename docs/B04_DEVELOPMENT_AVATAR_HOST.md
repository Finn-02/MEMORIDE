# B04 개발 이미지 화면 — A 연결 결과

2026-11-04 KST. Windows, `part/a-a01-bootstrap`, 작업 전 HEAD `d8c0d09`, 확인한 origin/main `ab2281b`. 아래 결과는 이 HEAD 위의 로컬 변경으로 검사했다. 아직 커밋·push·main 병합하지 않았다.

## 연결한 범위

- 기존 debug `DevelopmentActivity`에 `B04 캐릭터 이미지 확인` 버튼 추가.
- A 소유 debug `DevelopmentAvatarHost`에서 패키지 Catalog를 IO에서 읽고, Catalog를 캡처한 AvatarSlot으로 B의 실제 AvatarView를 렌더링한다.
- 현재 기본 화면은 tori/idle/의상 없음의 128dp 단일 연결 확인이다. B의 36개 조합 갤러리 자체는 아직 제공·연결되지 않았다.
- 시스템 Back 및 화면 내 돌아가기 버튼으로 개발 화면 복귀. 기존 여정 ViewModel은 유지한다.
- 기존 MainActivity의 개발 gate를 유지한다. 일반 Android debug 전경에서만 허용하고 Automotive·차량 모드·백그라운드는 차단한다. 제한 시 갤러리 구성을 제거해 임시 Compose 상태와 로딩 작업을 폐기한다.
- 두 변경 파일은 모두 src/debug에만 존재한다. 공통 계약·B 저장·보상·차량 gate·일반 AppRoot·release 라우터는 수정하지 않았다.

## B 갤러리 연결 위치

`DevelopmentActivity.AppContent()`의 `DevelopmentAvatarHost(gate, onBack)` 호출에 content를 전달하면 된다.

호스트 content 입력은 `(Catalog, AvatarPreview, AvatarSlot) -> Unit`인 Composable이다. B는 Catalog와 샘플 AvatarPreview를 받는 독립 debug Composable을 제공하면 된다. B 화면이 자체 AvatarView를 사용해도 되고 제공된 slot을 사용해도 된다. B가 app 호스트를 import하지 않으며 A가 B 화면을 주입한다. B 화면에 돌아가기 콜백이 필요하면 A의 onBack을 전달한다.

호스트는 세로 스크롤 Column이다. B 갤러리가 LazyColumn 등 자체 스크롤을 소유한다면 A가 content를 유한 높이 Box로 감싸고 호스트의 바깥 스크롤을 제거해, 같은 방향 무한 높이 중첩을 피한 뒤 시험한다. B 함수·레이아웃을 받기 전에 완전한 갤러리 연결로 보고하지 않는다.

## 실행

1. 일반 Android AVD에 debug APK 설치.
2. `MEMORIDE 개발 DEMO` (`io.kailand.app.DevelopmentActivity`) 실행.
3. 상단 `B04 캐릭터 이미지 확인` 선택.
4. 토리 PNG와 얼굴 표시 확인 → 돌아가기 또는 시스템 Back.

현재 일반 AVD는 Medium_Phone_API_37.0, 시험 serial emulator-5556이다. Pleos emulator-5554에서는 개발 진입점이 UNKNOWN으로 차단돼야 하며 제한을 우회하지 않는다.

## 실제 검사

- 프로세스 JAVA_HOME을 설치된 Android Studio JBR25.0.4로 지정, `python scripts/check_all.py --android`: exit0. 이 PC의 `py -3` launcher는 설치된 Python을 찾지 못해 기존 설치 python3.10.4 실행 경로를 사용했다.
- Python44개 PASS. JUnit192개, 실패0/오류0/skip0. `:app:assembleDebug` PASS. 로그 `tmp/avatar-host-check.log`.
- `gradlew.bat --no-daemon :app:assembleRelease`: exit0, unsigned APK 생성 PASS. 로그 `tmp/avatar-host-release.log`. 서명 release 설치 시험은 아니다.
- debug/release APK의 DEX 확인: DevelopmentActivity와 DevelopmentAvatarHostKt가 debug에는 있고 release에는 없음.
- 일반 Android UI4개 PASS: 진입·Catalog 로딩, 시스템 Back 복귀, 화면 내 Back 복귀, 백그라운드 후 복귀. `tmp/avatar-host/results.json`.
- `tmp/avatar-host/avatar-rendered.png` 직접 확인: 토리 몸·얼굴 PNG가 표시되고 placeholder가 아님.
- 최신 debug APK로 Pleos DevelopmentActivity 실행: UNKNOWN 표시, 갤러리 버튼 없음. `tmp/avatar-host/automotive-blocked.xml`. 이후 정식 MainActivity로 복원.
- `git diff --check` PASS.

첫 일반 AVD 실행에는 응답 없음이 표시됐고 초기 UI 시험은 실패했다. 부팅 완료 후 force-stop/relaunch 첫 응답도 timeout이었다. 이후 동일 UI 시험을 다시 실행해 4개를 통과했다. 원인이 앱인지 기기/호스트 부하인지 확정하지 않았으며, 첫 실패를 PASS로 계산하지 않는다. 확보한 제한된 logcat에서 명확한 앱 예외는 발견하지 못했다.

## NOT RUN / 다음 단계

- B의 36조합·작은 화면·의도적 로딩 실패·빠른 외형 변경 갤러리 시험은 NOT RUN.
- A04 실제 저장 상태·섬→동행→섬 I03·방문 외형 통합은 NOT RUN.
- 서명 release 설치·실차 시험 NOT RUN. unsigned release 빌드와 구분한다.
- B의 갤러리 코드 제공 후 content 연결 및 레이아웃 시험. 현재 로컬 변경을 공유할 때 기존 수동 도착 PR #14와 변경 범위가 섞이지 않도록 PR diff를 확인한다.
