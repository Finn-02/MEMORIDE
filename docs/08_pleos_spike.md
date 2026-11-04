# 8 Pleos 연동과 초기 기술 검증

## 8 1 확인된 기능과 미확인 기능

2026년 10월 30일 공식 문서 기준 NaviHelper는 내비 목적지·경유지·경로 요청, 정보 조회, 내비 상태 이벤트를 제공한다. 소개 문서는 3.0.5를 기준으로 하며 요청·응답은 Navi Service와 연결된다. 실제 이벤트는 내비 구현과 상태에 따라 다를 수 있다. Vehicle State와 NaviHelper는 신청 없이 사용 가능한 SDK로 안내되어 있다. 문서 존재가 팀 장비에서의 실제 성공을 뜻하지는 않는다. [S03, S04, S05]

| 항목 | 이번 처리 | 검증 증거 |
|---|---|---|
| 목적지 요청·조회 | 실제 연동 우선 | 호출 이름·응답 또는 오류·화면 |
| 도착 이벤트 | 지원 시 게임 완료에 연결 | 정확한 콜백과 여정 일치 근거 |
| 차량 거리·시간 | 조건부, 미지원이면 생략 | 값·단위·관측 구간 |
| 기본 내비에 캐릭터 삽입 | 미확인, 이번 구현 아님 | 모의 화면이라고 표시 |
| 상시 이름 호출·자유 음성 | 모의 자막·프리셋 | 실제 STT로 표기하지 않음 |
| 실제 차량 간 통신 | 가상 A·B로 대체 | 네트워크 연결 주장 금지 |

requestNavigationScreen이나 NAVI_CUSTOM_MAP라는 이름만으로 임베디드 지도·오버레이 지원을 추론하지 않는다. 게임 화면은 독립 앱 영역이고 Driving view를 가리지 않는다. 정식 내비 통합은 별도 플랫폼 협의·지원 검증 항목이다. [S08]

## 8 2 환경 구성 기본안

Kotlin, Android Studio의 Compose 앱, minSdk 28 이상, Pleos Connect Emulator를 사용한다. 공식 안내는 Gradle 8 이상과 Emulator 2.1.0을 제시한다. 실제 호환되는 AGP·Gradle·Kotlin·Compose·JDK 조합은 D1에 A가 한 번 빌드해 docs/ENVIRONMENT.md에 고정한다. 인터넷의 최신 버전을 각각 조합하거나 다른 팀원이 임의 업그레이드하지 않는다. [S09]

NaviHelper 3.0.5 의존성과 공식 Maven 저장소 경로는 sources.md에 기록한다. 설치된 버전의 실제 타입·콜백을 확인한 뒤 어댑터를 구현한다. Kotlin 타입 초안의 이름은 SDK의 실제 서명이라고 가정하지 않는다. 위도·경도는 named argument 또는 명확한 변환 함수를 사용하고 순서를 확인한다. [S06]

공통 프로젝트는 A가 만든 뒤 바로 공유한다. B·C·PM은 같은 Gradle wrapper와 라이브러리 버전으로 시작한다. 한 사람의 개인 SDK 경로가 들어가는 local.properties는 커밋하지 않는다. 각자 최초 Gradle sync를 완료해 의존성을 받아 둔다. 필요한 도구와 에뮬레이터 설치가 끝나기 전의 완전 오프라인 실행은 보장하지 않는다.

## 8 3 D1과 D2의 제한된 실험

| 순서 | 시험 | 합격 또는 기록할 결과 | 중단·우회 기준 |
|---|---|---|---|
| 1 | 공식 에뮬레이터 부팅·빈 앱 설치 | 환경 버전과 실행 화면 | 장비 문제 2시간이면 백업 PC 확인 |
| 2 | NaviHelper 의존성·초기화 | 빌드, 연결 성공 또는 오류 | 의존성·권한·서비스 원인 분류 |
| 3 | 목적지 조회·요청 | 실제 응답 1건 또는 재현 오류 | 4시간 이상 무진전이면 나머지 파트와 독립 |
| 4 | 플랫폼 제한·주차 상태 입력 | 이용 제한 신호와 복귀 | 확인 불가하면 실제 모드는 차단 유지 |
| 5 | 도착 콜백·목적지 일치 | 이벤트 1건과 게임 입력 변환 | D2 종료 시 미지원이면 DEMO 시연 명시 |

전체 기술 탐색 예산은 A의 첫 1~2일이며 시간 숫자는 권장 상한이다. 실패 후 며칠씩 전원이 기다리지 않는다. B·C·PM은 D1부터 계약의 샘플 데이터로 개발한다. 초기 실패를 감추거나 UI 버튼 클릭을 SDK 성공으로 기록하지 않는다. D7에 실제 연동을 한 번 더 확인할 수 있으나 핵심 화면 완성을 중단하지 않는다.

## 8 4 어댑터 구현 경계

NaviHelper 초기화, listener 등록·해제, release는 A의 어댑터에서 관리한다. 화면이 재구성될 때마다 listener를 새로 추가하지 않는다. Android lifecycle에 맞춰 구독을 정리하며 연결 오류를 UI 스레드에서 무한 재시도하지 않는다. 요청 시간 제한과 재시도는 1회 버튼 방식으로 시작한다. [S04]

권한은 실제 사용하는 요청·조회에 필요한 최소 범위만 선언한다. 새로운 권한을 임의로 붙여 에러를 덮지 않는다. SDK 호출이 실패해도 모의 화면과 게임은 DEMO 모드로 독립 동작할 수 있다. 모드를 바꿀 때 이전 SDK 콜백을 새 DEMO 여정으로 받아들이지 않도록 source와 세션 토큰을 확인한다.

모든 진단 기록에는 빌드 SHA, SDK·에뮬레이터 버전, 호출, 관측 결과, 미확인 사항을 짧게 남긴다. 실제 위치·번호판·개인정보를 기록하지 않는다. 동작 영상은 진단과 제품 시나리오를 구분한다. 도착이 모의 입력이면 브리핑의 거리·시간도 모의이거나 미표시여야 하며 실제 운행 기록으로 바꾸지 않는다.

## 8 5 제한 처리의 출고 기준

게임 앱의 주행 중 제한을 우회하는 코드는 출고하지 않는다. 실제 플랫폼이 이용을 차단하면 동행 모의 화면·방문·인벤토리는 중단한다. 주행 개념 시연은 실제 주차 상태에서 가상 속도와 경로를 바꾸는 방식이다. A의 실제 제한 상태는 C의 시연 패널로 해제할 수 없게 한다. [S07]

게임 앱을 실제 운전자에게 노출해도 주의 분산이 없다고 검증한 상태는 아니다. 본 MVP의 시스템 시험은 정책 신호에 반응하는지와 주차 모의 장면의 구현을 확인한다. 향후 양산에서는 플랫폼 심사·화면 통합·운전자 주의 분산 검증이 별도로 필요하다.

## 8 6 이번 재확인과 실행 순서

2026-11-01 공식 페이지를 다시 확인했다. 공모전 가이드의 SDK 목록과 실제 사용 가능 상태는 구분해야 한다. 프로젝트 안내는 Vehicle State·NaviHelper를 신청 없이 사용 가능으로, 음성·FusedLocation·Vehicle Control을 준비 중으로 안내한다. 이번 필수 계획은 NaviHelper에 집중하며, 주차·제한 확인을 위한 최소 상태 API만 검토한다. 포털에 프로젝트를 만들고 필요한 조건을 확인하는 것은 A와 PM이 한 번만 수행한다. 비밀 값은 저장소나 프롬프트에 넣지 않는다. [S03, S04]

앱의 핵심 카테고리는 Entertainment를 우선 검토한다. 자체 길 안내를 제공하지 않는 게임을 Maps/Navigation으로 분류해 제한을 피하지 않는다. 이는 팀의 분류 제안이며 최종 플랫폼 판단을 대신하지 않는다. 주행 동행과 방문의 이번 구현은 주차 상태의 컨셉 시연이다. [S07, S30]

Mac용 공식 이미지 feed는 arm64 경로이고 Windows/Linux용 feed는 별도 경로다. 실제 CPU를 확인하고 설치한다. 전원에게 SDK 실험을 맡기지 않고 A의 Windows PC를 기준 실행 환경, C의 Windows PC를 시연 백업으로 지정한다. PM은 Mac에서 브리핑·데이터와 앱 빌드를 확인하며 가능한 경우 Mac Pleos도 시험한다. 한 OS에서의 성공을 다른 OS 성공으로 기재하지 않는다. [S09, S26]

공통 앱 골격 A01은 약 60~90분의 첫 전달 목표로 두고 SDK 성공과 분리한다. 이 시간은 추정치다. 먼저 같은 계약의 DEMO 앱을 공유한 뒤 NaviHelper 의존성·서비스 연결을 검증한다. UI 파트가 SDK 런타임에 막히면 A는 generic Android에서 사용할 fake 경로를 제공한다. 한 Gradle 모듈에서 SDK 컴파일 의존성 해석까지 실패한다면 복잡한 flavor 설계를 즉시 추가하기보다 별도 로컬 SDK 실험 프로젝트에서 원인을 좁힌 뒤 작은 PR로 반영한다. 이 실험 프로젝트는 제품 코드를 중복 구현하거나 자동으로 제출하는 대상이 아니다.

## 8 7 주차와 제한 시험의 근거

공식 주행 규제 구현 문서는 CarUxRestrictionsManager의 변경 listener를 안내하고, 지역 정책 문서는 DO 미선언 앱의 Parking 허용·Drive 불가를 구분한다. A는 설치한 버전에서 이 매핑과 실제 주차 상태의 근거를 확인해 기록한다. 속도 0 또는 제한 플래그 하나의 존재만으로 임의 주차 판정을 만들지 않는다. [S37, S38]

Virtual Console의 Gear Selection으로 P/R/N/D를 설정해 실제 gate 변화를 시험할 수 있다. 이것은 플랫폼의 시험 입력이며 앱 내부 DEMO 여정과 별개다. 제한을 끄는 ADB 명령을 정상 사용 경로·제출 시연의 우회 장치로 쓰지 않는다. 주차 입력이 끝내 확인되지 않으면 실제 플랫폼 게임 이용은 UNKNOWN으로 두고, generic Preview 시연과 실제 SDK 기술 검증 범위를 구분해 제출한다. [S39]

## 8 8 A03 첫 런타임 확인 — 2026-11-04 KST

- Windows 11 x64, 기존 `part/a-a01-bootstrap`의 A02 커밋 `bf8dd1f` 위 로컬 A03 변경. 아직 별도 커밋·push 전이다.
- 사용자가 SDK Manager에서 약관 확인·이미지 설치를 완료했다. 공식 `Pleos_Connect_v2_0` 프로필로 `MEMORIDE_Pleos_2_1_API34` AVD 생성: Pleos Connect 2.1.0 / Android 14 API34 x86_64 / RAM4GB / Cold boot. Android Emulator37.2.13, WHPX 정상. `emulator-5556`의 `sys.boot_completed=1` 확인.
- 기존 debug APK 설치 및 MainActivity 실행 PASS. 화면은 플랫폼 입력 미연결 UNKNOWN 유지. 차량 feature와 Navi Service 패키지 존재를 확인했으며, 존재 확인 자체를 SDK 연결 성공으로 취급하지 않는다.
- NaviHelper3.0.5를 debugImplementation으로 추가했다. 공식 Maven은 `ai.pleos.playground` 그룹만 조회한다. debug 전용 `PleosProbeActivity`는 NAVI_ROUTE 읽기 권한으로 연결·목적지 조회만 수행하며 게임 gate·도착·보상을 만들지 않는다. release에는 이 Activity·권한·SDK 의존성을 넣지 않았다.
- 실제 probe 결과: initialize → onConnected → getDestinationInfo → onDestinationInfo 수신 PASS. 목적지 payload는 출력하지 않는다. 이 결과는 목적지 설정·경로 완료·최종 도착 확인을 뜻하지 않는다.
- 실행: `adb -s emulator-5556 shell am start -W -n io.kailand/io.kailand.app.PleosProbeActivity`. 일반 Android 플랫폼에서는 Automotive 확인 후 중단한다. 앱 목록용 launcher는 추가하지 않았다.
- `:app:assembleDebug` PASS(1m40s). `py -3 scripts/check_all.py --android` exit0: Python44개·JUnit114개, 실패/오류/skip0, debug 빌드 PASS. 단위 시험 task 실제 실행(최종45 task 중4 실행·41 up-to-date). DataStore Unsafe 경고 잔존.
- 로컬 근거: `tmp/a03-preflight/sdk-debug-build.log`, `check-all.log`, `navi-probe.log`, `pleos-main.png`, `pleos-probe.png`. 기기 로그 시각은 에뮬레이터 설정값이며 작업 일시는 KST 기준이다. 로그·이미지·SDK 파일은 커밋하지 않는다.
- NOT RUN: requestRoute·취소·최종 도착/여정 일치, 실제 CarUxRestrictions·주차 입력의 앱 gate 연결, 화면 이탈·재진입 수명주기 검증, 실제 B 저장·A04·I06/U20, 서명 release 설치. 재진입 명령은 실제 화면 전환이 확인되지 않아 PASS로 기록하지 않는다.
- 다음: 공식 플랫폼 주차·제한 입력을 관찰하고 안전한 gate 매핑을 구현한다. 정식 앱 UNKNOWN을 임의로 허용하지 않는다.

## 8 9 실제 플랫폼 gate 연결 — 2026-11-04 KST

- A03의 다음 단계로 정식 MainActivity에 `VehicleGateMonitor`를 연결했다. Automotive API29 이상에서만 생성하며, 일반 Android 정식 진입점과 Automotive 개발 DEMO 진입점은 UNKNOWN을 유지한다.
- 입력은 공개 Car API의 `CarUxRestrictionsManager`와 `CarPropertyManager.GEAR_SELECTION`이다. `CAR_POWERTRAIN` normal 읽기 권한만 추가했다. `android.car` 컴파일 라이브러리와 런타임 uses-library는 optional로 지정해 일반 Android 설치를 유지한다. 속도·위치·차량 제어 권한은 추가하지 않았다.
- 허용 조건: foreground + Car 서비스 연결 + 유효한 주차 P + DO 필요 없음 + activeRestrictions=0. 제한 플래그 또는 비주차 기어는 RESTRICTED, 누락·오류·연결 미확인은 UNKNOWN이다. 속도0이나 DEMO 상태로 주차를 판정하지 않는다.
- onPause에서 즉시 차단하고 listener 해제·disconnect. 재진입·서비스 연결 변화마다 관측값을 비우고 세대 토큰을 교체한다. 과거 lifecycle/manager 콜백으로 새 세션을 열 수 없게 한다. 정식 제한 화면의 제목을 RESTRICTED/UNKNOWN에 맞게 구분했다.
- Pleos2.1.0 API34 실제 시험 PASS: P에서 메인 화면 허용 → 공식 Virtual Console의 D 전환에서 RESTRICTED 및 조작 없는 제한 화면 → P 복귀에서 PARKED_ALLOWED. 서비스 dump에서도 D의 DO=true/0x50을 관측했다. D의 속도는0으로 유지했으며 가속은 시험하지 않았다.
- MainActivity에서 DevelopmentActivity로 이동하면 UNKNOWN, Back 복귀 시 새로운 관측을 거쳐 허용을 확인했다. Automotive의 개발 DEMO는 주차 P여도 차단된다. 기어 P와 시험 중 잠시 닫았던 FRUNK/TRUNK를 시험 전 상태로 복원했다. 두 에뮬레이터 동시 실행은 PC 부하가 커져 순차 시험했다.
- Generic API37의 최종 APK 설치·개발 DEMO 홈 실행 PASS, 정식 MainActivity UNKNOWN PASS. 차량 라이브러리 부재로 인한 앱 충돌은 관측하지 않았다.
- 회귀: `py -3 scripts/check_all.py --android` exit0, Python44개·JUnit121개(신규 gate7개), 실패/오류/skip0, debug 빌드 PASS. 이후 optional 런타임 라이브러리 선언과 제한 안내 문구를 반영해 `:app:assembleDebug :app:compileReleaseKotlin` PASS 후 최종 APK에서 P/D/P 및 generic 진입점을 다시 확인했다. release 서명·설치는 수행하지 않았다.
- 기존 API34와 호환되는 property callback/status API는 compileSdk37에서 deprecated 경고가 발생한다. DataStore Unsafe 경고도 남아 있으나 검사·빌드는 성공했다.
- 로컬 근거: `gate-build-test.log`, `gate-final-check.log`, `gate-ui-final-build.log`, `gate-device.log`, `gate-final-parked.xml/png`, `gate-final-drive.xml/png`, `development-automotive.xml`, `generic-development.xml`, `generic-main.xml` (모두 tmp/a03-preflight 아래, 커밋 제외).
- NOT RUN: 실제 Car 서비스 장애/권한 거부/기어 unavailable 강제 주입(순수 정책 시험으로만 차단 확인), N/R·실제 이동 속도·실차, 실제 release 설치, NaviHelper 목적지 요청·최종 도착 일치, A04 게임 저장·I06/U20 통합. SDK probe 수명주기 검증도 별개로 남아 있다.
- 다음: NaviHelper 목적지 요청·조회·취소와 최종 도착 이벤트의 식별 근거를 검증한다. 실제 저장·보상 연결은 A04다. 이 A03 변경은 기존 A 브랜치의 로컬 변경이며 아직 commit/push하지 않았다.

## 8 10 NaviHelper 목적지 명령 확인 — 2026-11-04 KST

- debug `PleosProbeActivity`를 실제 목적지 요청·조회·취소 진단으로 확장했다. `NAVI_ROUTE_SEARCH`는 debug Manifest에만 추가했다. Catalog의 지역 좌표를 IO에서 읽고 named argument로 RouteInfo에 전달한다. 버튼 입력 시 최신 foreground·실제 VehicleGate·SDK 연결을 모두 재확인한다.
- 초기 조회 → Catalog 일치 없음. 대전 요청 → 실제 내비 앱의 대전 경로 후보 표시 → 내비의 안내 시작 → `onRouteStarted` 관찰. 진단 화면으로 복귀 후 조회 → `catalog=[daejeon], selectedMatch=true` 확인. `cancelRoute` → `onRouteCancelled` 및 `catalog=[], selectedMatch=false` 확인. 조회 좌표 비교는 진단이며 게임 도착 인증이 아니다.
- SDK 요청 반환은 결과 확정으로 표시하지 않는다. 15초는 관찰 구간 안내이며 늦은 이벤트를 성공·실패로 임의 변환하지 않는다. 이벤트에는 요청 ID가 없어 겹친 명령의 응답 소유권을 보장하지 않는다. 이번 시험은 수동 순서로 진행했다.
- 내비 화면 전환 동안 probe의 조작은 차단하고 SDK 이벤트 관찰만 유지한다. Back은 명시적 finish로 처리해 listener 제거와 SDK release를 수행한다. 종료 시 `listenerRemoved=true sdkReleased=true`, 재진입 후 단일 초기화·연결, 조회 1건에 콜백 1건, 재종료 시 해제 확인. 앞선 중단 명령도 실제 로그에서 최종 조회와 해제까지 완료됨을 확인했다.
- 실제 NaviHelper3.0.5가 참조하는 navi-data3.0.4의 DestinationArrivedInfo는 duration/distance/name/address만 제공한다. journeyId/revision/POI ID/좌표가 없으며, 현재 환경에서 최종 도착 이벤트는 발생시키지 않았다. 이름·주소 일치만으로 과거 여정의 늦은 이벤트를 구분한다고 주장하지 않는다. 자동 ArrivalCommand 생성과 실제 보상 연결은 보류한다.
- 관찰하지 못한 것: onRoutingCompleted·onDestinationChanged·onDestinationArrived와 실제 SDK 오류 callback. 문서가 안내하는 이벤트가 모든 내비 구현에서 발생한다고 가정하지 않는다. duration/distance 단위·결측값 매핑도 실제 도착 근거와 함께 후속 검증한다.
- A03 기본 검증 범위인 실제 요청·조회·취소 및 gate 입력은 확인했다. 최종 도착의 안전한 식별은 미해결 한계이며 필수 DEMO→저장 경로(A04)를 막지 않는다. SDK 타입을 공통 DTO로 노출하거나 B의 저장·보상 계산을 변경하지 않았다.
- 실행은 `adb devices -l`과 `adb -s <serial> emu avd name`으로 Pleos를 식별한 뒤 `adb -s <serial> shell am start -W -n io.kailand/io.kailand.app.PleosProbeActivity`. 이 시험의 실행 serial은 emulator-5554였으며 이전 절의 5556을 고정값으로 가정하지 않는다. P·UX 허용 상태에서만 버튼이 활성화된다.
- 자동 검사: `py -3 scripts/check_all.py --android` exit0, Python44개·JUnit121개·debug 빌드 PASS. 처음 발견된 probe CRLF를 UTF-8/LF로 수정한 뒤 재검사했다. Back 명시 종료 보완 후 debug 재빌드와 기기 종료·재진입 시험 PASS. 다른 파트 통합 전 결과이며 최신 main 반영 후 결과는 아래 절에 기록한다.
- 로컬 근거: tmp/a03-preflight의 `navi-commands.log`, `navi-final-check.log`, `navi-lifecycle-build.log`, `navi-guidance.png`, `navi-commands-evidence.png`, `navi-after-query.xml`, `navi-final-reopened.xml`. 좌표·이름·주소의 SDK 응답 payload는 로그에 남기지 않는다. 기존 이미지에 표시된 시간·거리는 내비 예상값이며 실제 도착 영수증이 아니다.

## 8 11 최신 main 통합 및 A03 전달 기준 — 2026-11-04 KST

- origin/main `18f10a1`에서 A02 PR #10, B03 PR #9, C04 PR #8 및 C01 패널 PR #11 병합을 확인했다. A03 소스는 `4997a00`으로 보존하고 같은 A 파트 브랜치에 main을 merge했다(`baf56f1`). 충돌 없음. 다른 소유자의 파일은 수정하지 않았다.
- 통합 상태 `py -3 scripts/check_all.py --android` exit0: Python44개·JUnit169개, 실패/오류/skip0, debug 빌드 PASS(45 task 중12 실행·33 up-to-date). `:app:compileReleaseKotlin` PASS(8 task 중1 실행·7 up-to-date). 로그는 tmp/a03-preflight/latest-main-check.log 및 latest-main-release-compile.log.
- 통합 APK를 현재 Pleos emulator-5554에 설치해 gate=PARKED_ALLOWED·SDK 연결과 버튼 활성화, 목적지 조회 콜백1건, Back 종료의 listener 제거·SDK release 성공을 확인했다. 정식 MainActivity의 실제 P 허용 화면도 확인했다. latest-main-probe.xml / latest-main-navi.log / latest-main-home.xml에 로컬 근거를 남겼다. 이전 P/D/P·generic 기기 시험은8.9의 결과이며 이번 재검사를 모든 기기·모든 시나리오 재실행으로 확대해 해석하지 않는다.
- A03 기본 SDK 기술 검증과 플랫폼 gate 구현은 전달 가능하다. 정식 S03의 PLEOS 여정 포트·자동 도착 처리는 아직 연결하지 않았으며 NaviHelper는 debug 진단에만 포함된다. 최종 도착의 여정 일치·단위/결측 해석, 실제 서비스 장애 주입·N/R·실차, A04 저장/브리핑/I06/U20, 서명 release 설치는 미검증이다.
- 지원 API의 실제 응답·오류를 UI에 표시하지만 실제 onError/연결 실패를 강제로 발생시킨 시험은 NOT RUN이다. 필수 DEMO 흐름은 A04에서 단일 B GameService·readiness·pendingReceipt·acknowledge 성공 후 이동·reset generation과 연결한다. 이 기록은 최종 제품 인수나 실차 안전 검증이 아니다.
