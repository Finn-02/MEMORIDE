# A 진행 상태

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
