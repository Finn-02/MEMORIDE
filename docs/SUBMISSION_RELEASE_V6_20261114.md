# 제출용 signed release v6와 README 입력 자료

2026-11-14 KST. 최신 코드의 서명 APK와 설치 결과를 전달한다. **산출물 서명/설치 PASS와 전체 인수 HOLD를 구분**한다. PM이 README·APK 업로드·병합·제출 커밋·공개 전환·접수 메일을 담당한다.

## APK와 소스

|항목|값|
|---|---|
|제출 파일명|`독수리4형제_MEMORIDE_release.apk`|
|APK 빌드 소스 전체 SHA|`17bea5438af9ea7b4fb3f3a431a19ab33777adc5`|
|APK SHA-256|`7f6f2ae9ee4cf74145368add969c8f978b5c5e8195a4fa9980d47273600cb311`|
|크기|58,201,427 bytes|
|패키지 / 버전|`io.kailand` / versionName `0.1.0`, versionCode `6`|
|SDK|minimum28 / target37|
|팀 인증서 SHA-256|`8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb`|
|포함 ABI|arm64-v8a, armeabi-v7a, x86, x86_64. 실제 검수는 x86_64만 수행|

도시명 포함 목적지 인식, 결과의 기념품→섬 성장→이번 여정 기록→섬 복귀 순서, 간격32dp와 기록 높이 제한 제거를 포함한다. 이전 v5 소스434abae4/해시b609491f…와 다른 APK다. 이 전달 문서가 추가된 이후 PR HEAD와 실제 APK 빌드 소스17bea543은 구분한다. 제품 코드는 이번 전달 작업에서 변경하지 않았다.

Git worktree를 소스17bea543에 고정하고, 기존 팀 키 담당자가 개인 터미널에서 `scripts/build_signed_release.py --version-code 6`을 실행했다. 비밀번호는 숨김 입력으로만 전달했고 키/비밀번호를 수집·커밋·ZIP에 포함하지 않았다. 빌드 exit0 및 원래 소스 SHA 유지 확인. 독립 `verify_release.py`로 인증서·v2/v3 계열 서명·패키지·non-debuggable·정식 Main 단일 launcher·개발 전용 코드 제외·패키지 자산을 재검증했다. 파일명 변경 후에도 APK 해시가 동일하다. 소스 정보는 깨끗한 고정 worktree의 빌드 이력이며 APK에서 Git SHA를 추출한 것으로 주장하지 않는다.

## README: 설치·실행

```text
adb devices
adb -s <SERIAL> install -r "독수리4형제_MEMORIDE_release.apk"
adb -s <SERIAL> shell am get-current-user
adb -s <SERIAL> shell am start --user <USER_ID> -W -n io.kailand/.app.MainActivity
```

기기 목록의 MEMORIDE 로고를 눌러 실행할 수도 있다. 이번 Pleos 검수의 USER_ID는10이다. 동일 팀 인증서의 기존 release는 `-r` 업데이트로 데이터 삭제 없이 설치한다. 별도 debug 인증서 앱이 설치된 기기는 직접 업데이트가 되지 않으므로 별도 검수 AVD를 사용했다. 사용자 기존 debug AVD를 uninstall/clear하거나 초기화하지 않았다. 설치 성공이 전체 게임 데이터 보존을 증명하는 것은 아니며 아래 확인 범위를 따른다.

실행 흐름은 캐릭터 선택→섬→여정 출발→기본 내비에서 목적지 설정/안내 시작→MEMORIDE 아이콘 복귀→여정 대기→목적지에 도착했어요→도착 확인→저장 후 결과다. 복귀할 때 기존 캐릭터를 유지하며, 도착은 사용자 직접 확인이고 실제 방문 인증/자동 최종도착이 아니다.

## README: 필요한 설정과 범위

- 공식 Pleos 차량 환경에서 실제 기어 P와 플랫폼 UX 이용 허용이 확인돼야 조작할 수 있다. 제한·연결 실패·UNKNOWN에서는 차단한다. DO/overlay 선언이나 ADB 기어/제한 우회는 사용하지 않는다.
- 기본 내비 및 Navi Service, 플랫폼의 `NAVI_ROUTE`/`CAR_POWERTRAIN` 권한 제공이 필요하다. 기기 상태는 읽기 전용이다. 로그인·별도 서버·사용자 API 키는 없다. 설치/SDK 준비·내비 검색과 경로 서비스에는 네트워크가 필요할 수 있다.
- 목적지명에 원주시/속초시/대전광역시·대전시가 포함되면 해당 등록 지역으로 인식한다. 도시명 없는 상호의 주소 조회/행정구역 역지오코딩은 추가하지 않았다. 도시명 없는 목적지는 기존 Catalog 기준 좌표 일치 fallback만 지원한다.
- 기록·수집·의상·장식은 로컬 저장이다. 명시된 시연용 주행 수치는 실측 거리·시간·연비가 아니다. 동행/가상 방문은 주차 중 개념 시뮬레이션이며 실제 V2V나 내비 오버레이가 아니다.

## README: 실제 검수 환경

- Windows11 build26200/AMD64, Python3.10.4, Android Studio JBR25, committed Gradle9.3.1/AGP9.1.1, NaviHelper3.0.5.
- 설치 기록의 Pleos Connect emulator2.1.0, Android14/API34/x86_64(system-image source.properties revision1). Android Emulator engine37.2.13.0/build16740729. RAM2GB/user10.
- 실제 전체 화면2560×1440/density160. 앱 확장 bounds `[853,90][2535,1300]`=1682×1210. 설계 기준1280×800과 구분한다. 최신 v6 분할 왕복은 NOT RUN이며 과거 debug의 분할841×1210 기록을 v6 결과로 재사용하지 않는다.
- AVD `MEMORIDE_A06_SIGNED_PLEOS`(5588), 비교용 `MEMORIDE_A06_V4_PLEOS_STEP1`(5590)을 각각 read-only/no-snapshot/visible/SwiftShader/Vulkan disabled로 실행했다. 원본 AVD 데이터/스냅샷과 사용자 debug AVD5554는 보존했다. 게스트 시계와 호스트 시계는 다를 수 있으며 이벤트 시각은 호스트 KST다.
- 별도 일반 Android Google APIs Play Store API37.0/x86_64 AVD는 RAM2GB/user0, 1080×2340/density440, read-only/no-snapshot/headless로 사용했다. 이 결과는 Pleos SDK/P·D 검증과 구분한다.

## 실제 결과와 남은 항목

|항목|최신 v6 결과|
|---|---|
|서명·패키지·release 경계|PASS. 정식 Main launcher1개, debug 아님, 기존 팀 인증서 일치|
|기존 signed v4→v6 업데이트|PASS. `adb install -r` Success, appId10175/최초설치시각 유지, 설치된 APK를 pull해 해시가 전달본과 일치함을 확인|
|Main 실행·선택 상태|PASS. COLD/Status ok. 첫 AVD의 모모 선택, 비교 AVD의 루루 선택 복원 확인. 전체 의상·장식·성장 보존 전수 검증은 아님|
|앱 아이콘|PASS 범위: 비교 AVD 앱 목록에 MEMORIDE 로고1개를 시각 확인하고 눌러 복귀. GNB의 과거 최근 앱 아이콘 캐시 전체 정리를 주장하지 않음|
|실제 내비→목적지→아이콘 복귀|PASS. 실제 한글 키보드로 속초시청 검색, ‘속초시청 강원’/‘강원특별자치도 속초시 중앙로183’ 선택 후 안내 시작. 검수용 SDK 요청 도구/기준 좌표 주입 없이 정상 아이콘 복귀로 재선택 없는 여정 대기 진입|
|도시명/보상 안내|PASS. 원본 ‘속초시청 강원’ 유지, 속초 지역 및 ‘회 접시 + 2개’ 표시. 원주/대전 일반 검색의 이번 실기기 재검수는 NOT RUN|
|아직 아니에요|PASS. 팝업 닫힘/동일 목적지·예상 선물·성장0 대기 유지|
|도착/연속 입력|PASS 범위: 두 번 연속 도착 tap 후 첫방문 결과에 회 접시·파도 목도리·바다 조개3개 NEW, 루루 성장0→1 확인. 내부 원본 영수증 ID/방문 횟수 전수 대조는 아님|
|프로세스 재시작·결과 닫기|BLOCKED. force-stop 및 재실행 COLD/Status ok까지 성공했으나 로딩 화면 후 QEMU 종료. 결과 복원/ACK 완료 PASS로 표시하지 않음|
|일반 Android UNKNOWN|PASS 범위: 같은 v6 업데이트/정식 Main 실행, ‘이용 상태 미확인 / 실제 플랫폼 주차·이용 제한 입력 미연결’, 게임 clickable0 확인. 최초 System UI ANR1회는 Wait 후 복구했으며 안정성 이슈로 보존. 실제 Pleos foreground UNKNOWN 검수를 대신하지 않음|
|인벤토리·재방문·창 전환|이번 기기 검수 NOT RUN/BLOCKED. 저장 실패·원본 재시도·ACK 실패 강제 주입, 실제 D→P와 지속적인 foreground UNKNOWN Pleos도 NOT RUN|

첫 AVD는 업데이트 전 v4에서도 버튼 입력이 반응하지 않았고, v6에서 홈 복귀/프로세스 재시작 후에도 input dispatcher에 Main surface `NO_INPUT_CHANNEL / NOT_VISIBLE`, 빈 touchableRegion이 남았다. WindowManager는 HAS_DRAWN/isOnScreen=true로 보고했다. 비교 AVD에서는 입력과 실제 내비·도착 저장이 동작했으므로 이를 v6 앱 전체의 입력 불가로 일반화하지 않는다. 기존 창 복원 문제의 원인이 해결됐다는 뜻도 아니다.

비교 AVD는 2026-11-14 **13:45:58 KST**에 `qemu-system-x86_64.exe`, exception `c0000005`, module unknown으로 종료했다. Windows Event1000 record35647/report `2b135385-8a6c-4c70-8705-67a8b2aa1c08`. 재실행 후 로딩 PNG만 수집했고 UI dump는 exit255였다. 앱 Java 예외/저장 실패/SDK/GPU 중 근본 원인은 미확정이다. 해당 Pleos 검수는 추가 반복하지 않았다.

자동 검사는 같은 제품 소스의 정적/Python66/Android398/debug PASS이며 실패·오류·skip0이다. debug 단위 시험과 이번 signed release 실기기 결과는 구분한다. 일반 Android의 최신 UNKNOWN PASS를 실제 Pleos 제한 인수로 확대하지 않는다.

## 전달과 다음 단계

사용자에게 정식 파일명 APK, REPORT/검증 메타데이터/전체 소스 SHA/APK SHA-256, 필요한 캡처·로그·자동 시험 결과를 ZIP으로 전달한다. APK/ZIP/서명키는 이번 PR에 커밋하지 않는다. PM이 apk/ 또는 GitHub Releases에 올리고 최종 README와 제출을 담당한다. 안정적인 Pleos 환경의 남은 결과 닫기·복원·인벤토리·재방문·창/제한 검수를 취합한 뒤 병합 여부를 판단해야 하며 **전체 인수 HOLD**를 유지한다.
