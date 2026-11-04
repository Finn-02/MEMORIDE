# A04-3 DEMO 도착·실제 저장·브리핑 연결

2026-11-05 KST / Windows11 AMD64 / part/a-a01-bootstrap.

기존 A04-1·2/B04 호스트 작업을 로컬 05bb2a9로 보존하고, PR #14가 병합된 origin/main edea1fd를 f0c52c8로 merge한 뒤 구현했다. 이 문서는 f0c52c8 위의 이번 변경을 검사한 기록이다. A04 코드는 아직 push/PR/main 반영 전이며, 최종 로컬 커밋은 Git 기록으로 확인한다. 개인 문서 2개는 커밋하지 않았다.

## 바뀐 동작

- 일반 Android debug의 `개발 DEMO → A04 실제 저장 화면 확인 → 여정 준비`에서 기존 DemoJourneyAdapter를 앱의 단일 B 서비스에 연결한다.
- 월드맵에서 고른 지역을 여정 준비의 최초 후보로 전달한다. 재진입은 요청했던 목적지 후보를 유지한다. 선택/READY만으로 보상은 없다.
- 완료 시각·시간·거리는 패키지 D03 fixture의 명시적 DEMO 입력이다. 실제 차량 측정이나 PLEOS 완료로 보내지 않는다. fixture 출처가 DEMO가 아니면 거부한다.
- 저장 직전에 현재 owner·gate·foreground·generation과 현재 여정 ID/source/kind/revision/REGION 목적지를 검사한다. 저장 실패 재시도는 기존 adapter가 보관한 동일 원본 명령을 전달한다.
- GuardedDemoJourney는 유효한 요청을 앱 scope에서 처리한다. 저장 직후 브리핑이 열려 호출 화면이 폐기돼도 이미 시작한 저장/원본 재생 상태 처리가 UI coroutine과 함께 취소되지 않는다.
- 브리핑은 B가 저장한 pendingReceipt로만 표시한다. 단계 ARRIVED/요청 Accepted만으로 성공 브리핑을 만들지 않는다.
- ReceiptCloseCoordinator는 acknowledge 대기 중 원본 영수증을 유지한다. pending이 먼저 null이 되어도 결과 대기 UI가 먼저 폐기되지 않는다. Success와 최신 화면/세대/제한 및 새 pending 여부를 확인한 뒤 섬/인벤토리로 이동한다. 실패 시 영수증과 PM 재시도 경로를 유지한다.
- PM 화면의 시스템 Back도 같은 acknowledge 경로를 사용한다. 이전 닫기 작업이 새 pending을 가리거나 닫지 않는다.
- C CompanionScreen에 B AvatarView를 감싼 AvatarSlot과 동일한 저장 AvatarPreview를 전달한다.
- Activity의 소유 키는 GameEntryModel에 유지한다. 회전 재생성은 같은 활성 여정을 사용하고 다른 Activity/프로세스는 별도 소유 키를 사용한다. 폐기된 UI 콜백은 lease 검사로 차단한다.
- 정식 PLEOS 진입점에는 DEMO 완료 조작을 노출하지 않는다. 실제 차량 여정/사용자 확인 출처 연결은 아직 별도다.

공통 DTO·GameService API·B 보상/저장 스키마·PM/C 내부 구현·Gradle 의존성은 바꾸지 않았다.

## 실제 검사

Python3.10.4 / JBR25.0.4 / Gradle9.3.1. 기존 wrapper 사용, 프로세스 JAVA_HOME만 지정했다.

- 집중 검사: `gradlew.bat --no-daemon :app:testDebugUnitTest --tests io.kailand.app.ArrivalBriefingIntegrationTest --tests io.kailand.app.ReceiptCloseCoordinatorTest --tests io.kailand.app.journey.GuardedDemoJourneyTest` exit0, 신규11개 PASS. tmp/a04-3-focused.log.
- 최종 `python scripts/check_all.py --android` exit0: Python44개·JUnit215개, 실패0/오류0/skip0, debug 빌드 PASS. tmp/a04-3-full.log.
- 최종 `gradlew.bat --no-daemon :app:assembleRelease` exit0, unsigned release 빌드 PASS. tmp/a04-3-release.log. 서명 release 설치 결과가 아니다.
- APK DEX 확인: GameEntryModel은 양쪽 포함, DevelopmentGameActivity는 debug에만 포함.
- unit의 B 연동은 실제 BGameService/보상 처리와 격리된 메모리 Store를 사용했다. 저장/ack 실패, 원본 재시도, 재생, 서비스 재생성 복원을 검사했다. 실제 DataStore 실패 UI 시험과 구분한다.
- 시간적 경계 시험: pending 제거와 ack 반환 사이 영수증 유지, 화면 폐기 후 이동 금지, 최신 pending 우선, 닫기 중복/취소, UI await 취소 뒤 앱 scope의 저장 및 원본 재생 상태 완료.

일반 Android Medium_Phone_API_37.0에 임시 Android 사용자 `MEMORIDE_A04_3_TEST`(ID10)를 만들어 실제 DataStore UI를 시험했다.

1. 월드 원주 후보→DEMO READY, 도착 전 보상 없음.
2. 저장 외형 모모/의상 없음으로 섬→동행→섬 기본 I03. PNG를 직접 확인했다.
3. 최초 원주 도착→원본 영수증·아이템3개 저장→브리핑.
4. 브리핑 시스템 Back→ack 완료→섬. pending만 제거, 원본 유지.
5. 같은 도착 재생→원본 영수증/보상 유지, 새 pending 없음.
6. 새 여정으로 원주 재방문→새 ID/영수증, 새 선물0.
7. 앱 force-stop/재시작→활성 여정이 없어도 저장된 pending 브리핑 복원.
8. 아이템 보기→ack 성공 뒤 인벤토리, 기존 지급·원본 영수증 유지.
9. 최종 수명 보강 APK에서 가로/세로 회전→같은 여정 ID·READY 유지, 저장 내용 변경 없음.

근거: tmp/a04-3/results.json, island-momo.png, companion-momo-full.png, first-briefing.png, restored-briefing.xml, briefing-items.xml, landscape-id.xml/visible-journey-id.xml, portrait-id.xml.

1~8은 회전 수명 보강 전 같은 A04-3 흐름에서 실행했고, 최종 보강 후 전체 회귀/unsigned release 및 9와 전체 동행 PNG를 추가 확인했다. 이를 최종 APK의 무중단 전체9개 재실행으로 표시하지 않는다. I03도 모모/의상 없음1사례이며 전체 캐릭터·의상 조합 인수가 아니다.

초기 임시 사용자 실행 중 System UI 응답 없음이 관측돼 첫 시험이 중단됐다. 복구 뒤 런처가 이미 실제 섬으로 돌아오는 상태도 하네스가 처리하도록 수정했다. 회전 시험은 UI 덤프 null-root 오류와 가로 화면 아래에 있는 ID를 바로 읽으려는 하네스 가정으로 중단됐고, 실제 화면/빈 AndroidRuntime 예외 로그를 확인한 뒤 스크롤 후 ID를 읽도록 수정해 재시험을 통과했다. 최초 실패들을 통과로 계산하지 않았다.

임시 사용자 종료/제거와 원래 사용자0 복귀를 확인했다. 원래 사용자 저장의 SHA256이 시험 전후 동일하다. tmp/a04-3/cleanup-result.txt. 저장 파일을 직접 수정하지 않았다.

Pleos 최종 APK: 실제 P 허용 상태 정식 섬 표시, 여정 준비에 DEMO 도착 버튼 없음, 정식 섬 복귀 확인. 일반 정식 진입점은 UNKNOWN 확인. pleos-main-ready.xml, pleos-no-demo-arrival.xml, generic-main-unknown.xml. 제한을 ADB로 해제하지 않았다.

## 남은 작업 / NOT RUN

- A04-4: C Runner/방문·모드 전환·reset 세대/정리 연결과 전체 U20.
- 사용자 확인 출처는 B 확인/공통 계약 반영 대기. 실제 사용자 확인 저장·PLEOS 자동 최종도착 연결은 미구현.
- I06 정상 저장/닫기/아이템 이동을 확인했지만 실제 기기 저장 실패·ack 실패 UI 강제 주입은 NOT RUN. 실패 결과의 보존/재시도는 unit에서 확인했다.
- B04 전체36조합·의상 포함 I03·여러 화면/글꼴 인수·방문 외형은 NOT RUN.
- 이번 APK의 실제 D/N/R·서비스 장애·실차·서명 release 설치·다른 PC 인수는 NOT RUN.
- 반복 재생 명령/활성 여정은 메모리 세션이다. 프로세스 재시작 후 영구 복원되는 것은 B의 원본 영수증/게임 상태/pending이다.
