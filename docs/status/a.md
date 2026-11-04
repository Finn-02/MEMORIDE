# A 진행 상태

상태: A01 병합 완료. A02 PR #10을 PM에게 검토·병합 요청 전달. A03 Pleos·NaviHelper 접속/조회와 실제 주차·제한 gate 연결 검증 완료. 목적지 요청·최종 도착 검증은 남아 있으며 A03 변경은 로컬 작업 중이다.
기준: 기존 part/a-a01-bootstrap 유지. main 771ea57372bdfbd04cd94cb2b8e94e3ad41bae2e 기반이며 B02-2까지 포함. 이번 A02 커밋 SHA는 Git 기록/PR에서 확인한다.
완료: JourneyPort/DemoControlPort 구현, 목적지·revision·취소·원본 retry/replay, debug 전용 모의 저장 경계, 화면 재생성 시 여정 유지, C 호출 예제(docs/A02_JOURNEY_HANDOFF.md).
검증: Windows11 AMD64/JBR25.0.4/Gradle9.3.1. Android121개(A02 17개·gate 신규7개 포함)·Python44개 및 debug 빌드 PASS. release Kotlin 컴파일 PASS는 서명/설치 결과가 아님.
기기: 일반 API37 x86_64에서 목적지/출발/도착/반복/변경/취소/실패·재시도/null/동행·Back/정식UNKNOWN 확인. 가로1920x1080,작은창840x1440·글자1.3에서도 ID 유지와 조작 접근 확인,1080x2400·글자1.0 복원.
플랫폼: Pleos2.1.0 API34 x86_64에서 실제 P 허용→D 제한→P 복귀, Main 화면 이탈 UNKNOWN→복귀 재확인 PASS. Automotive 개발 DEMO 차단, generic 개발 DEMO 허용·정식 UNKNOWN도 PASS. docs/08_pleos_spike.md 8.8~8.9 참조.
한계: 실제 게임 저장·성장·보상·pendingReceipt·브리핑은 아직 A04 미연결. 개발 샘플은 메모리 대역이며 프로세스 종료 시 여정이 사라짐. NaviHelper 목적지 요청·최종 도착, 실제 서비스 장애 주입·N/R·실차·U20 통합·서명 release 설치 NOT RUN.
다음: A02 교차 검토·main 반영과 별개로 A03 NaviHelper 목적지 요청·조회·취소·최종 도착 일치 근거를 확인한다. A04에는 B/C/PM 공개 화면을 실제 저장 상태와 연결한다.
