# MEMORIDE

**사용자의 여정(Ride)에 함께하는 가족의 소중한 기억(Memory)을 캐릭터와 섬의 성장으로 남기는 Pleos Connect 차량용 애플리케이션 프로토타입입니다.**

Ride Becomes a Memory — 오늘의 이동이, 내일의 추억이 된다.

가족과 함께한 이동과 여행을 지역 기념품·캐릭터·섬의 추억으로 이어갑니다. 캐릭터 선택, 여정 준비, 외부 내비게이션 복귀, 사용자 확인 도착과 기념품 결과를 하나의 앱에 연결했습니다. 동행·가상 방문은 주차 중 개념 시뮬레이션입니다.

## 1차 제출본

| 항목 | 값 |
| --- | --- |
| 팀 / 앱 | 독수리4형제 / MEMORIDE |
| 기본 APK 경로 | [apk/독수리4형제_MEMORIDE_release.apk](apk/독수리4형제_MEMORIDE_release.apk) |
| 보조 배포 경로 | [GitHub Release `v0.1.0-v6`](https://github.com/Finn-02/MEMORIDE/releases/tag/v0.1.0-v6) · 태그는 실제 APK 빌드 소스 `17bea54`에 고정 |
| 제출 파일명 | `독수리4형제_MEMORIDE_release.apk` |
| 패키지 / 버전 | `io.kailand` / `0.1.0` / versionCode `6` |
| APK 빌드 소스 | `17bea5438af9ea7b4fb3f3a431a19ab33777adc5` |
| APK 크기 | 58,201,427 bytes |
| APK SHA-256 | `7f6f2ae9ee4cf74145368add969c8f978b5c5e8195a4fa9980d47273600cb311` |
| 팀 서명 인증서 SHA-256 | `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb` |

소스와 제출 APK를 이 저장소에 함께 제공합니다. 기본 파일은 위 `apk/` 경로를 사용하며 기존 Release도 같은 바이트의 보조 배포 경로로 유지합니다. 최종 제출 커밋과 APK 빌드 소스는 구분합니다. 빌드 소스 이후의 제출 정리는 문서·출처·지도 생성 도구이며 앱 실행 코드와 패키징 입력 자산을 변경하지 않습니다. 키와 비밀번호는 포함하지 않습니다.

## 설치·실행

1. [제출 APK](apk/독수리4형제_MEMORIDE_release.apk)의 파일 화면에서 다운로드하거나, main의 **Code → Download ZIP**을 받아 `apk/독수리4형제_MEMORIDE_release.apk`를 사용합니다. 파일 크기와 SHA-256을 확인합니다.

   보조 Release 첨부의 실제 다운로드명은 `4._MEMORIDE_release.apk`입니다. 이 파일을 사용하면 `독수리4형제_MEMORIDE_release.apk`로 바꿔 설치할 수 있으며 바이트·서명·SHA-256은 동일합니다. Release 태그의 소스 ZIP은 빌드 소스 `17bea54` 기준이므로 제출 APK가 포함된 최종 main ZIP과 구분합니다.
2. Pleos Connect 에뮬레이터와 기본 내비게이션을 준비합니다. 실제 기어 P 및 플랫폼의 앱 이용 허용이 필요합니다.
3. 아래 명령에서 연결된 기기의 `SERIAL`과 현재 사용자 번호 `USER_ID`를 사용합니다. 검수 환경의 사용자 번호는 10이었습니다.

```text
adb devices -l
adb -s <SERIAL> install -r "독수리4형제_MEMORIDE_release.apk"
adb -s <SERIAL> shell am get-current-user
adb -s <SERIAL> shell am start --user <USER_ID> -W -n io.kailand/.app.MainActivity
```

기기 앱 목록의 MEMORIDE 아이콘으로도 실행할 수 있습니다. 동일 팀 인증서의 기존 release에는 `-r`로 업데이트할 수 있습니다. 다른 인증서의 debug 앱이 있으면 별도 AVD를 사용합니다. 기존 데이터 삭제를 설치의 전제로 삼지 않습니다.

**일반 Android에서는 `이용 상태 미확인` 안내와 조작 차단이 정상입니다.** Pleos 주차·이용 제한 입력이 연결되지 않은 상태이며, 일반 Android에서 전체 시연을 할 수 있다는 뜻이 아닙니다. 제한·UNKNOWN에서는 차단을 유지하고 ADB로 기어·이용 제한을 우회하지 않습니다.

## 확인된 시연 순서

캐릭터 선택 → 섬 → 여정 출발 → 기본 내비에서 `속초시청 강원` 검색·선택·안내 시작 → MEMORIDE 아이콘으로 복귀 → 여정 대기 → `목적지에 도착했어요` → 도착 확인 → 첫 방문 결과입니다.

동일 v6에서 일반 내비 UI로 속초시청(강원특별자치도 속초시 중앙로183)을 선택한 뒤 복귀, 기념품 3종 NEW 및 성장 0→1 결과가 확인됐습니다. 검수용 SDK 목적지 주입을 사용하지 않은 경로입니다. **결과의 `섬으로 가기` 동작은 이번 v6 기기 검수에서 미확인**이며 대표 동선 전체 PASS로 표시하지 않습니다.

도착은 사용자 직접 확인(`USER_CONFIRMED`)으로 실제 방문 인증이나 자동 최종 도착 판정이 아닙니다. 목적지명의 원주시·속초시·대전광역시/대전시를 등록 지역에 연결하는 프로토타입 정책입니다. 도시명 없는 상호의 역지오코딩은 지원하지 않으며 기존 Catalog 기준 좌표 일치 방식만 남아 있습니다. 일반 검색 기기 확인은 위 속초 경로에 한정됩니다.

거리·시간·연비·탄소 관련 화면 수치는 **시연용 표본**이며 실측값이 아닙니다. 화면에도 표본 안내를 표시합니다. 수집·음식·의상·장식과 진행 상태는 로컬에 저장합니다. 실제 V2V·백엔드·로그인 기능은 없습니다.

## 환경과 필요한 설정

| 구분 | 실제 기록 |
| --- | --- |
| Pleos Connect | 2.1.0 / Android 14(API 34) / x86_64 |
| Android Emulator | 37.2.13 / RAM 2GB / 사용자 10 |
| 화면 전체 | 2560×1440 px / density 160 |
| 확장 앱 영역 | 1682×1210 px, bounds `[853,90][2535,1300]` |
| 화면 검수 범위 | v6 확장 화면 확인. 설계 기준 1280×800과 구분하며 최신 분할 왕복은 미확인 |
| v6 실행 설정 | visible / read-only / no-snapshot / SwiftShader / Vulkan 비활성 |
| 빌드 PC | Windows build 26200 / AMD64 / Python 3.10.4 / Android Studio JBR 25 |
| 빌드 도구 | 저장소 Gradle wrapper 9.3.1 / AGP 9.1.1 / compile·target SDK 37 / min SDK 28 |
| 내비 연동 | NaviHelper 3.0.5 / 기본 내비 및 Navi Service / `NAVI_ROUTE`, `CAR_POWERTRAIN` 제공 필요 |

앱 이용에는 실제 플랫폼 주차 상태와 UX 허용 입력이 필요합니다. 설치·SDK 준비·내비 검색 및 경로 서비스에는 네트워크가 필요할 수 있습니다. 별도 유료 서비스·사용자 API 키는 필요하지 않습니다. APK에 ARM 계열 ABI가 포함돼 있지만 이번 기기 검수는 x86_64이며 실차·ARM 동작을 확인한 것은 아닙니다.

## 검증 상태와 제한

1차 프로토타입 제출 산출물로 v6를 선택했습니다. **전체 제품 인수 판정은 HOLD**이며 과거 버전의 PASS를 v6에 합산하지 않습니다.

- A 기록: 같은 제품 소스의 정적 검사, Python 66개, Android JUnit 398개, debug 빌드 PASS. PM이 이 환경에서 다시 실행한 결과가 아닙니다.
- signed v6 기록 및 전달 증거: 서명·정식 launcher·non-debuggable 검증, 기존 signed v4→v6 업데이트, 실제 내비 복귀와 위 속초 첫 방문 결과 확인.
- 미확인: v6 결과 닫기·재실행 복원 완료·아이템 사용/저장·재방문·분할/글자 전환·실차·장시간 안정성. 이전 창 복원 FAIL도 유지합니다.
- 일부 AVD에서 입력/표시 문제와 QEMU 종료가 기록됐으며 원인은 미확정입니다. 후속 최소 동선 재시도는 AVD 디스크 부족으로 APK 설치 전에 중단됐습니다. 이를 앱 실행 실패나 해결 완료로 바꾸지 않습니다.

상세 범위는 [v6 전달 기록](docs/SUBMISSION_RELEASE_V6_20261114.md), [PM 인수 이력](docs/UX_P03_ACCEPTANCE.md), [1차 제출 결정과 배포 상태](docs/FIRST_ROUND_SUBMISSION_20261114.md)를 참고하세요. 미검수 항목은 2차 준비에서 보완할 대상으로 남깁니다.

## 소스와 출처

개발·빌드 안내는 [개발 README](docs/DEVELOPMENT_README.md), [환경 준비](docs/ONBOARDING.md), [공통 실행](docs/A01_QUICKSTART.md), [v6 서명·환경 기록](docs/SUBMISSION_RELEASE_V6_20261114.md)에 있습니다. 기존 개발 README의 v4 설명은 당시 이력입니다. 이번 제출본은 위의 v6입니다.

자산의 제작 근거는 [ATTRIBUTION](assets/ATTRIBUTION.md), [이미지 81개 대응표](docs/asset_manifests/C_ART_PROVENANCE_20261113.json), [Natural Earth 지도 출처](docs/ux_c_map/SOURCE.md)를 따릅니다. PM은 B 자산의 공개 배포가 가능함을 확인했습니다. C의 Blender 재렌더 기록과 과거 AI 사용 여부 등 상세 제작 이력은 구분하며 독립적인 권리 감사 완료나 AI 미사용을 주장하지 않습니다. 저장소 전체에 새 오픈소스 라이선스를 부여하지 않습니다.

공개 준비를 위한 파일·Git 이력 점검 범위와 남은 설정 확인은 [공개 준비 검토](docs/PUBLICATION_REVIEW_20261114.md)에 기록했습니다. 저장소 열람 공개는 전체 제품 인수 PASS나 제3자 자산의 권리 보증을 뜻하지 않습니다.

## 접수 자료

팀명은 **독수리4형제**, 앱 이름은 **MEMORIDE**입니다. 제출 정보는 [최종 제출 점검표](docs/FINAL_SUBMISSION_REVIEW_20261114.md)를 따릅니다. 접수 메일에는 저장소 URL·최종 제출 커밋과 위 APK 경로를 기재하고, 선택 제출하는 시연 영상은 v6의 실제 확인 범위에 맞춥니다. 서명·개인정보 동의서는 저장소에 올리지 않습니다.
