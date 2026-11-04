# A 진행 상태

상태: A01·A02·A03(PR #12) main 병합 확인. 사용자 요청에 따른 수동 도착 확인 흐름의 A 개발 검증 구현 완료. 실제 저장은 사용자 확인 출처·영수증 구분의 B·PM 계약 검토 대기다.
기준: part/a-a01-bootstrap 유지. A03 PR #12·B04 PR #13이 병합된 main ab2281b를 사용자 확인 소스a85aaa6에 6a14714로 merge. 충돌 없음. 실제 PR head는 Git/PR에서 확인한다.
완료: 실제 P·UX 제한의 InteractionGate 정규화, foreground/연결 세대별 차단·정리, generic/Automotive 개발 샘플 경계 유지. debug NaviHelper3.0.5 진단의 목적지 요청·조회·취소 및 Back 종료·SDK 재접속.
검증: Windows11 AMD64/JBR25.0.4/Gradle9.3.1. 최신 main 통합 Python44개·Android192개(수동 확인 신규12개·gate7개 포함), 실패/오류/skip0, debug 빌드 PASS. release Kotlin 컴파일 PASS는 서명/설치 결과가 아니다. 동일 A 코드의 수동 확인 UI7개 사례 PASS, B04 기기 인수는 별개다.
기기: Pleos2.1.0 API34 x86_64에서 P 허용→D 제한→P 복귀와 Main 화면 이탈 UNKNOWN→복귀 재확인 PASS. Automotive 개발 DEMO 차단, generic 개발 DEMO 허용·정식 UNKNOWN PASS. 최신 main 통합 APK에서 Pleos 접속·조회·SDK 정리·정식 P 화면 추가 확인. docs/08_pleos_spike.md 8.8~8.11 참조.
SDK: 대전 요청 후 실제 내비 안내·onRouteStarted, 목적지 조회의 Catalog 좌표 일치, 취소 callback 및 일치 해제 확인. 경로 계산·안내 시작을 도착으로 취급하지 않는다. 실제 onError/연결 실패 강제 주입은 NOT RUN.
추가 도착 시험: Android 모의 GPS는 반영됐지만 Pleos 내비 위치는 유지돼 정상 도착 재현 미성공. 변경·취소 후 늦은 도착 시험은 NOT RUN. SDK 미지원으로 단정하지 않으며 자동 도착 연결은 보류한다. 원래 GPS·P·경로 취소·메인 화면 복원 확인. docs/08_pleos_spike.md 8.12 참조.
한계: SDK는 debug 진단에만 포함되며 정식 S03 PLEOS 여정 포트·자동 도착은 미연결. 최종 도착 데이터에 journeyId/revision/좌표가 없어 현재 여정 식별을 보장하지 않는다. 실제 최종 도착·서비스 장애·N/R·실차·서명 release 설치는 미검증.
A04 진행: 단일 B 서비스와 실제 섬·인벤토리·월드의 저장 상태 연결 완료. 도착 보상·pendingReceipt 브리핑·C 동행/방문·I03/I06/U20 통합은 미완료. 기존 A02 샘플은 메모리 대역이고 프로세스 종료 시 여정이 소실된다. 실제 저장 화면과 구분한다.
수동 확인: 네 가지 응답·목적지/질문 토큰 고정·이전 카드 무효화·중복 초안 방지·주차 복귀 확인을 A 소유 코드로 구현. 실제 SDK 종료/취소 callback도 질문 힌트로만 처리한다. 공통 DTO·B 저장·PM 브리핑은 미수정. docs/MANUAL_ARRIVAL_CONTRACT_REVIEW.md에 검토안·실행법·실제 결과를 기록했다.
다음: 사용자 확인의 저장 출처/근거와 원본 영수증 표시를 B·PM과 확인한 뒤 A04에서 단일 GameService/readiness·저장된 pendingReceipt·비동기 acknowledge·reset generation을 연결한다. 검토 전 초안을 PLEOS 자동 도착으로 보내지 않는다.

B04 개발 연결(2026-11-04): A 소유 debug AvatarView 호스트와 DevelopmentActivity 진입 버튼을 로컬 구현. 실제 tori PNG 표시·UI4개·Automotive 차단 확인. Python44/JUnit192/debug/unsigned release 빌드 PASS, release DEX에서 debug 호스트 제외 확인. 초기 일반 AVD 응답 없음·재실행 timeout 이후 재시험 통과했으며 원인은 미확정. B의 36조합 갤러리 코드·I03은 미연결/NOT RUN. docs/B04_DEVELOPMENT_AVATAR_HOST.md 참조. 커밋·push·main 반영 전이다.

A04-1(2026-11-04): A 소유 GameCommandCoordinator의 명령 직렬화·최신 gate/foreground/readiness·현재 여정 검사·pending ID ack·reset 세대와 잠금 밖 정리를 로컬 구현. 신규11개/전체JUnit203개·Python44개·debug PASS. 실제 B 서비스/AppRoot 연결·모드 전환 세대 관리·Runner/방문 U20·I03/I06·이번 코드의 release는 NOT RUN. docs/A04_INTEGRATION_PROGRESS.md 참조. 다음은 단일 서비스·Catalog와 실제 화면 연결이며 사용자 확인 출처는 B 계약 확인 후 연결한다.

A04-2(2026-11-04): Application 수명의 단일 B 서비스·조정자, 실제 섬/인벤토리/월드 및 Catalog를 GameAppRoot로 연결. 최신 화면 소유자·폐기된 콜백 차단 추가. Python44/JUnit204/debug/unsigned release 빌드 PASS. 일반 Android 저장·재시작·외형 원복 등 UI8사례와 Pleos 정식 P 섬 표시 확인. 사용자 확인 출처·실제 도착·브리핑·C 동행/방문·전체 초기화는 후속. docs/A04_2_SAVED_GAME_SCREENS.md 참조. 현재 로컬 변경이며 main 미반영.
