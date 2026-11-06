# A 진행 상태

## 최신 회귀 — 2026-11-06 C 비교와 독립적인 사용자 확인 저장 경계

A 소유 통합시험3개를 추가해 오래된 ack/원본 재전달과 새 pending, KST 자정 저장 재시도,
승인된 저장→모드 전환→reset의 파일 복원을 확인했다. [상세](../A06_USER_CONFIRMED_BOUNDARY_REGRESSION.md).
사용자 확인14개/전체JUnit264개(failure/error/skip0), 정적/Python59개 PASS.
production·B/C·계약·저장 스키마 변경 없음. C 비교 APK 소스685d107/해시 불변, APK 패키징·설치/AVD 조작 없음.
B 독립 교차 검토와 C 호스트 종료 조사, 최종 release/A06 인수는 별도 진행이다.

## 최신 기기 인수 — 2026-11-06 사용자 확인 Pleos

앱 소스 `685d107`의 debug APK를 별도 공식 Pleos2.1/API34 AVD에서 정식 Main으로 검증했다.
실제 P/DO=false/UxR0에서 USER_CONFIRMED 저장→브리핑, 미확인/확인 완료 앱 재시작과
원본/아이템/pending 보존·재시작 전후 파일 해시 불변 PASS.
사용자 조작 D에서 DO=true/UxR80·앱 RESTRICTED·저장 불변을 확인했다.
초기 QEMU 두 차례 c0000005 종료 후, [후속 비교](../A06_DRIVE_CRASH_INVESTIGATION.md)에서 기본 조건과 Vulkan 비활성화 조건의 D 차단 화면/P 복귀를 각각 확인했다.
저장 JSON/파일 해시 불변이며 이번 표본은 기능 PASS다. 기존 종료 원인은 미확정으로 안정성 해결/전체 인수 PASS가 아니다.
[범위·복구·남은 인수](../A06_USER_CONFIRMED_PLEOS_ACCEPTANCE.md). 서명 release/실차/B 교차/C05 전체는 후속이다.

## 최신 연결 — 2026-11-06 사용자 확인 저장

B PR #20/C PR #21 포함 main `9a19c77`을 기존 A 브랜치에 `953dadc`로 반영했다.
전용 사용자 확인 모드에서 목적지/질문 토큰·실제 이용 상태·owner/세대/화면을 재검사한 뒤
기존 B 단일 저장과 PM pendingReceipt/비동기 acknowledge로 연결했다.
최초 ID/확인 시각/null을 재시도에 보존하고 모드/reset의 옛 입력은 차단한다.
Python59/JUnit261(신규 A 통합11개)/debug PASS. 일반 AVD에서 원주 확인→저장→브리핑→닫기/섬,
앱 파일의 USER_CONFIRMED/null/원본 보존/pending 해제 확인 PASS.
[상세/NOT RUN](../A06_USER_CONFIRMED_INTEGRATION.md). 실제 Pleos의 새 경로·최종 서명 후보/C05/A06 전체는 후속이다.
아래 B/C 대기·PR 보류 등은 각 기록 시점의 상태다.

## 최신 준비 점검 — 2026-11-05 A06 B/C

main a04b3c5 / 원격 B af388f1 / C184727a를 확인했다.
B05 USER_CONFIRMED 저장 보강은 원격 완료 근거 미확인으로 A 차단을 유지한다.
C 원격에는 UI3·방문 추가시험3개·debug 검사 화면·문서 등9파일이 미병합 상태다.
기존 C01~04 연결과 C PC 설치 인수는 완료 근거를 재사용한다.
현재 A 코드 집중시험88개 실제 실행 PASS. C 미병합 코드/B05 구현을 시험한 결과는 아니다.
[확인 근거·받을 결과·전달 초안](../A06_BC_READINESS.md). 메시지 전송/새 PR/병합은 하지 않았다.

## 최신 기기 인수 — 2026-11-05 A06 중간 서명 Pleos

새 공식 Pleos2.1/API34 x86_64 AVD에서 A05 팀 서명 v2 최초 설치·v3 업데이트와
정식 Main/DEMO 지역 보상·DAILY 저장을 확인했다. 모모·빵모자·성장1·세 보상 종류 보유·
미확인 브리핑과 확인 완료 상태의 프로세스 재시작/업데이트 복원 PASS.
[정확한 범위·오류 재시험·APK 소스](../A06_SIGNED_PLEOS_ACCEPTANCE.md).
APK 빌드 소스는 ee3ed0f이며 최신 main/최종 UI 빌드가 아니다. 기존 Pleos 앱·데이터는 보존했다.
A06 전체와 최종 제출 SHA 인수는 진행 중. 새 PR은 만들지 않는다.

## 첫 회귀 기록 — 2026-11-05 A06 통합 검증 착수

PR #18 병합 `507b987` 및 #19 포함 main `a04b3c5`를 기존 `part/a-a01-bootstrap`에 반영했다.
실제 임시 파일 DataStore와 A/B 조정 경계를 연결한 신규 시험 2개를 추가했다.
Python59/JUnit235 실제 실행·debug 빌드 PASS. 승인된 도착 저장 중 제한 변경과
확인 저장 중 화면 취소 후 snapshot 일치·파일 복원·중복 지급 방지를 확인했다.
[A06 근거/남은 인수](../A06_INTEGRATION_VERIFICATION.md). A06 전체 완료나 새 PR 발행은 아니다.
서명 release Pleos/X03·기기 게임 기록 보존/S02/S10·최종 UI는 후속이다.

## 이전 상태 — 2026-11-05 A05 교차 설치 인수

A04/공통 타입 PR #15~17 main 반영 뒤, A05는 임시 codex/a05-release-preparation 브랜치에서 진행했다.
첫 팀 키의 중간 서명 APK v2/v3(빌드 ee3ed0f)를 A/C의 별도 Windows PC에서
일반 API37 최초 설치·동일 키 업데이트·정식 Main/UNKNOWN 차단까지 확인했다.
**A05 WBS 조건 충족, PR #18 main 병합 완료(`507b987`).**
[C 근거 검토](../A05_C_ACCEPTANCE_REVIEW.md). 게임 기록 보존/S10 전체·Pleos/X03·실차·최종 UI는 A06 후속 인수다.
USER_CONFIRMED 타입은 공급됐지만 실제 저장 연결은 별도 후속이다.

## 이전 A04 진행 기록 — 아래 상태/검사값은 당시 기준

상태: A01·A02·A03(PR12/14) main 병합 확인. A04의 DEMO 저장·화면·Runner/방문·모드/reset 구현·검증 완료, 통합 PR 검토 준비. 실제 사용자 확인 출처 저장은 B 계약 대기다.
기준: part/a-a01-bootstrap 유지. main edea1fd를 f0c52c8로 merge한 뒤 A04-3 8ff3911 위에 A04-4를 구현했다. 실제 최신 커밋/PR은 Git에서 확인한다.
완료: 실제 P·UX 제한의 InteractionGate 정규화, foreground/연결 세대별 차단·정리, generic/Automotive 개발 샘플 경계 유지. debug NaviHelper3.0.5 진단의 목적지 요청·조회·취소 및 Back 종료·SDK 재접속.
검증: Windows11 AMD64/JBR25.0.4/Gradle9.3.1. Python44/JUnit223(실패/오류/skip0)·debug/unsigned release PASS. A04-4 격리 사용자 UI9사례·원래 저장 SHA256 보존·Pleos P 모드 전환/개발 차단/정식 복귀 확인. 서명 release 설치·실차·전체 시각 인수는 별개다.
기기: Pleos2.1.0 API34 x86_64에서 P 허용→D 제한→P 복귀와 Main 화면 이탈 UNKNOWN→복귀 재확인 PASS. Automotive 개발 DEMO 차단, generic 개발 DEMO 허용·정식 UNKNOWN PASS. 최신 main 통합 APK에서 Pleos 접속·조회·SDK 정리·정식 P 화면 추가 확인. docs/08_pleos_spike.md 8.8~8.11 참조.
SDK: 대전 요청 후 실제 내비 안내·onRouteStarted, 목적지 조회의 Catalog 좌표 일치, 취소 callback 및 일치 해제 확인. 경로 계산·안내 시작을 도착으로 취급하지 않는다. 실제 onError/연결 실패 강제 주입은 NOT RUN.
추가 도착 시험: Android 모의 GPS는 반영됐지만 Pleos 내비 위치는 유지돼 정상 도착 재현 미성공. 변경·취소 후 늦은 도착 시험은 NOT RUN. SDK 미지원으로 단정하지 않으며 자동 도착 연결은 보류한다. 원래 GPS·P·경로 취소·메인 화면 복원 확인. docs/08_pleos_spike.md 8.12 참조.
한계: SDK는 debug 진단에만 포함되며 정식 S03 PLEOS 여정 포트·자동 도착은 미연결. 최종 도착 데이터에 journeyId/revision/좌표가 없어 현재 여정 식별을 보장하지 않는다. 실제 최종 도착·서비스 장애·N/R·실차·서명 release 설치는 미검증.
A04 진행: 단일 B 서비스·실제 저장 화면·DEMO 도착/브리핑·C 동행/방문/Runner·모드/reset 연결 완료. 기본 I03·정상 브리핑 이동·U20 소프트웨어 경계 확인. 실제 기기 오류 UI·실차·사용자 확인 출처 저장은 남아 있다. 기존 A02 메모리 샘플과 실제 저장 화면을 구분한다.
수동 확인: 네 가지 응답·목적지/질문 토큰 고정·이전 카드 무효화·중복 초안 방지·주차 복귀 확인을 A 소유 코드로 구현. 실제 SDK 종료/취소 callback도 질문 힌트로만 처리한다. 공통 DTO·B 저장·PM 브리핑은 미수정. docs/MANUAL_ARRIVAL_CONTRACT_REVIEW.md에 검토안·실행법·실제 결과를 기록했다.
다음: 사용자 확인의 저장 출처/근거와 원본 영수증 표시를 B·PM과 확인한 뒤 A04에서 단일 GameService/readiness·저장된 pendingReceipt·비동기 acknowledge·reset generation을 연결한다. 검토 전 초안을 PLEOS 자동 도착으로 보내지 않는다.

B04 개발 연결(2026-11-04): A 소유 debug AvatarView 호스트와 DevelopmentActivity 진입 버튼을 로컬 구현. 실제 tori PNG 표시·UI4개·Automotive 차단 확인. Python44/JUnit192/debug/unsigned release 빌드 PASS, release DEX에서 debug 호스트 제외 확인. 초기 일반 AVD 응답 없음·재실행 timeout 이후 재시험 통과했으며 원인은 미확정. B의 36조합 갤러리 코드·I03은 미연결/NOT RUN. docs/B04_DEVELOPMENT_AVATAR_HOST.md 참조. 커밋·push·main 반영 전이다.

A04-1(2026-11-04): A 소유 GameCommandCoordinator의 명령 직렬화·최신 gate/foreground/readiness·현재 여정 검사·pending ID ack·reset 세대와 잠금 밖 정리를 로컬 구현. 신규11개/전체JUnit203개·Python44개·debug PASS. 실제 B 서비스/AppRoot 연결·모드 전환 세대 관리·Runner/방문 U20·I03/I06·이번 코드의 release는 NOT RUN. docs/A04_INTEGRATION_PROGRESS.md 참조. 다음은 단일 서비스·Catalog와 실제 화면 연결이며 사용자 확인 출처는 B 계약 확인 후 연결한다.

A04-2(2026-11-04): Application 수명의 단일 B 서비스·조정자, 실제 섬/인벤토리/월드 및 Catalog를 GameAppRoot로 연결. 최신 화면 소유자·폐기된 콜백 차단 추가. Python44/JUnit204/debug/unsigned release 빌드 PASS. 일반 Android 저장·재시작·외형 원복 등 UI8사례와 Pleos 정식 P 섬 표시 확인. 사용자 확인 출처·실제 도착·브리핑·C 동행/방문·전체 초기화는 후속. docs/A04_2_SAVED_GAME_SCREENS.md 참조. 현재 로컬 변경이며 main 미반영.

A04-3(2026-11-05): 기존 작업을 05bb2a9로 로컬 보존, origin/main edea1fd를 f0c52c8로 merge 후 DEMO→B 저장→PM 브리핑→ack 이동 및 C AvatarSlot 연결. 저장은 UI 취소와 분리하고 ack 대기 영수증을 유지한다. 회전 시 owner/여정 유지 보강. Python44/JUnit215/debug/unsigned release PASS. 격리 Android 사용자 UI9사례·기본 모모 I03 확인, 임시 사용자 제거·원래 저장 SHA256 동일 확인. Pleos 정식 P와 DEMO 도착 미노출·일반 정식 UNKNOWN 확인. 다음 A04-4는 Runner/방문·모드 전환·전체 reset/U20. 실제 사용자 확인 저장은 B 계약 대기. docs/A04_3_DEMO_ARRIVAL_BRIEFING.md 참조. 원격 PR/main 미반영.

A04-4(2026-11-05): GameSimulationRuntime으로 기존 C 공개 API를 연결하고 reset 시 패널/선택 유지, 잠금 밖 drain·실패 차단·옛 세대 거부·비파괴 모드 전환·방문 중단을 구현했다. 신규Runtime8개 포함JUnit223/Python44/debug/unsigned release PASS. D01~D06은 단위 통합, D01~D04/브리핑/방문/모드/동일값reset/BG복귀 UI9개 확인. 임시 사용자11 제거·원래 저장 해시 동일. debug taskAffinity 분리 후 Pleos P와 정식/개발 진입점 경계를 재시험했다. docs/A04_4_SIMULATION_AND_RESET.md 참조. 다음은 A04 PR 검토와 A05 서명 release/교차 설치, A06 인수 보완이다.
