# A 진행 상태

상태: A01·A02 main 병합 확인. A03 플랫폼 gate 구현·NaviHelper 기본 기술 검증 및 최신 main 통합 검증 완료. 최종 도착 식별 한계를 명시해 교차 검토 대상으로 전달한다.
기준: part/a-a01-bootstrap 유지. main 18f10a1(B03·C04·C01 패널 보완·A02 포함)를 A03 소스4997a00에 baf56f1로 merge. 충돌 없음. 실제 PR head는 Git/PR에서 확인한다.
완료: 실제 P·UX 제한의 InteractionGate 정규화, foreground/연결 세대별 차단·정리, generic/Automotive 개발 샘플 경계 유지. debug NaviHelper3.0.5 진단의 목적지 요청·조회·취소 및 Back 종료·SDK 재접속.
검증: Windows11 AMD64/JBR25.0.4/Gradle9.3.1. 최신 main 통합 Python44개·Android169개(신규 gate7개 포함), 실패/오류/skip0, debug 빌드 PASS. release Kotlin 컴파일 PASS는 서명/설치 결과가 아니다.
기기: Pleos2.1.0 API34 x86_64에서 P 허용→D 제한→P 복귀와 Main 화면 이탈 UNKNOWN→복귀 재확인 PASS. Automotive 개발 DEMO 차단, generic 개발 DEMO 허용·정식 UNKNOWN PASS. 최신 main 통합 APK에서 Pleos 접속·조회·SDK 정리·정식 P 화면 추가 확인. docs/08_pleos_spike.md 8.8~8.11 참조.
SDK: 대전 요청 후 실제 내비 안내·onRouteStarted, 목적지 조회의 Catalog 좌표 일치, 취소 callback 및 일치 해제 확인. 경로 계산·안내 시작을 도착으로 취급하지 않는다. 실제 onError/연결 실패 강제 주입은 NOT RUN.
추가 도착 시험: Android 모의 GPS는 반영됐지만 Pleos 내비 위치는 유지돼 정상 도착 재현 미성공. 변경·취소 후 늦은 도착 시험은 NOT RUN. SDK 미지원으로 단정하지 않으며 자동 도착 연결은 보류한다. 원래 GPS·P·경로 취소·메인 화면 복원 확인. docs/08_pleos_spike.md 8.12 참조.
한계: SDK는 debug 진단에만 포함되며 정식 S03 PLEOS 여정 포트·자동 도착은 미연결. 최종 도착 데이터에 journeyId/revision/좌표가 없어 현재 여정 식별을 보장하지 않는다. 실제 최종 도착·서비스 장애·N/R·실차·서명 release 설치는 미검증.
A04 대기: 실제 저장·성장·보상·pendingReceipt·브리핑·I06/U20 연결은 미구현/미검증. A02 샘플은 메모리 대역이고 프로세스 종료 시 여정이 소실된다. B03·C04가 main에 있어도 AppRoot 실제 조립 완료를 의미하지 않는다.
다음: A03 교차 검토·merge commit 병합. 다음 번호 A04는 목표·필요 입력·검증 기준 브리핑 후 단일 GameService/readiness·저장된 pendingReceipt·비동기 acknowledge·reset generation을 연결한다.
