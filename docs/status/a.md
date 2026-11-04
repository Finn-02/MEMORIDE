# A 진행 상태

상태: A01 병합 완료. A02 DEMO 여정 처리·S03 개발 화면·C01 동행 연결의 로컬 검증 완료, 작은 PR 전달 준비.
기준: 기존 part/a-a01-bootstrap 유지. main 771ea57372bdfbd04cd94cb2b8e94e3ad41bae2e 기반이며 B02-2까지 포함. 이번 A02 커밋 SHA는 Git 기록/PR에서 확인한다.
완료: JourneyPort/DemoControlPort 구현, 목적지·revision·취소·원본 retry/replay, debug 전용 모의 저장 경계, 화면 재생성 시 여정 유지, C 호출 예제(docs/A02_JOURNEY_HANDOFF.md).
검증: Windows11 AMD64/JBR25.0.4/Gradle9.3.1. A02 단위17개 포함 Android114개·Python44개 및 debug 빌드 PASS. release Kotlin 컴파일 PASS는 서명/설치 결과가 아님.
기기: 일반 API37 x86_64에서 목적지/출발/도착/반복/변경/취소/실패·재시도/null/동행·Back/정식UNKNOWN 확인. 가로1920x1080,작은창840x1440·글자1.3에서도 ID 유지와 조작 접근 확인,1080x2400·글자1.0 복원.
한계: 실제 게임 저장·성장·보상·pendingReceipt·브리핑은 아직 A04 미연결. 현재 개발 샘플은 메모리 대역이며 프로세스 종료 시 여정이 사라짐. 실제 Pleos·차량 gate·U20 통합·서명 release 설치 NOT RUN.
다음: A02 교차 검토·main 반영. 다음 번호 작업은 A03 실제 SDK·제한 입력 확인이며, 시작 전 목적·필요 환경·검증 기준을 브리핑한다. A04에는 B/C/PM 공개 화면을 실제 저장 상태와 연결한다.
