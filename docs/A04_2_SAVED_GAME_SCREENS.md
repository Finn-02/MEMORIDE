# A04-2 단일 저장 서비스·실제 화면 연결

2026-11-04 KST / Windows11 AMD64 / part/a-a01-bootstrap.
HEAD d8c0d09 위의 로컬 변경으로 검사했다. 이전 A04-1/B04 호스트 변경과 개인 문서 2개를 보존했다. 아직 커밋·push·main 반영하지 않았다.

## 바뀐 동작

- MemorideApplication의 lazy GameAppSession이 프로세스에서 B GameService와 A 명령 조정자를 각각 한 번 생성한다. 정식/개발 Activity와 구성 변경이 같은 서비스를 사용하며, 프로세스 재시작 때 B 저장을 다시 읽는다.
- 정식 MainActivity는 새 A 소유 GameAppRoot에서 실제 IslandScreen·InventorySheet·WorldScreen을 조립한다. 기존 AppRoot/DevelopmentJourneyRoot의 메모리 DEMO는 유지한다.
- debug 메뉴의 `A04 실제 저장 화면 확인`으로 일반 Android에서 실제 저장 화면을 연다. 이 진입점은 exported=false이고 release에서 제외된다. 화면에 `실제 로컬 게임 기록 사용`을 표시한다. B04 단일 이미지 확인 화면과는 별개다.
- 패키지 Catalog를 IO에서 한 번 읽고 B 서비스 readiness를 별도로 관찰한다. 로딩·Catalog 실패·저장 읽기 오류·이용 제한을 구분한다. 저장 오류를 빈 진행 상태로 바꾸거나 덮어쓰지 않는다.
- 캐릭터/인벤토리 명령은 coordinator.apply를 통한다. 실행 직전에 세대와 실제 최신 gate/foreground/readiness, 전경 Activity 소유자, 현재 화면 수명과 이동 버전을 확인한다. 대기 중이던 옛 화면의 입력이 새 전경 화면의 권한으로 실행되지 않게 한다.
- 제한/백그라운드에서는 실제 화면 구성을 제거해 인벤토리·식사 연출·이전 화면 콜백을 폐기한다. 상태 복귀 때 저장된 snapshot으로 다시 표시한다.
- 섬의 AvatarView는 B가 저장한 snapshot.avatar를 표시한다. 월드맵·인벤토리도 같은 snapshot을 사용한다.
- 월드에서 지역 선택은 여정 준비의 후보만 채운다. 내비 요청·도착·보상은 실행하지 않는다.

## 아직 연결하지 않은 동작

- 여정 준비/DEMO 경로에는 아직 미연결 안내가 있다. 기존 A02 메모리 DEMO는 종전 개발 화면에서 계속 실행할 수 있다.
- JourneySnapshot/도착 명령을 이 실제 서비스에 연결하는 것은 A04-3이다. 현재 GameAppSession의 context에는 실제 여정 공급자가 아직 없다.
- 저장된 pendingReceipt가 있으면 이를 보존하고 결과 화면 연결 대기 안내를 표시한다. 이 단계에서 임의 acknowledge하거나 보상을 재계산하지 않는다. 실제 PM 브리핑은 A04-3에서 연결한다.
- C 동행/방문·AvatarSlot·I03은 다음 연결 범위다. 이번 단계에서 외형 일치 통합을 완료했다고 표시하지 않는다.
- Runner/방문 정리·모드 전환·전체 초기화는 A04-4에서 연결한다. 이번 단계에는 reset 버튼이 없다.
- USER_CONFIRMED 출처 계약은 아직 B 확인 대기이며 공통 DTO·보상·저장 스키마를 변경하지 않았다.

## 실제 검사

환경: Python3.10.4 / JBR25.0.4 / Gradle9.3.1. 프로세스 JAVA_HOME만 기존 Android Studio JBR로 지정했다.

- `python scripts/check_all.py --android`: exit0. Python44개, JUnit204개(실패0/오류0/skip0), debug 빌드 PASS. tmp/a04-screens-full.log.
- 신규 단위 사례 1개: 옛 화면의 대기 중 콜백이 새 전경 화면의 이용 허용으로 저장되지 않음. A04 조정자 시험은 총12개다.
- `gradlew.bat --no-daemon :app:assembleRelease`: exit0, unsigned release 빌드 PASS. tmp/a04-screens-release.log. 서명 release 설치는 아니다.
- APK DEX 확인: MemorideApplication은 debug/release 모두 포함, DevelopmentGameActivity는 debug에만 포함.
- 일반 Android Medium_Phone_API_37.0 UI8개 PASS: 실제 섬/Catalog, 캐릭터 선택의 B 저장, 섬→월드, 월드 인벤토리 닫기 후 월드 유지, 지역 후보 선택만으로 보상 없음, force-stop 후 저장 외형 복원, 원래 캐릭터로 복원 및 다른 진행 보존, 전경 복귀 후 실제 섬 표시. tmp/a04-screens/results.json.
- 시험 변경은 캐릭터만 UI/GameService로 수행·원복했다. DataStore JSON은 읽기 전용으로 관찰해 성장·아이템·발견·영수증 등 다른 필드가 유지되는지 비교했다. 저장 파일을 직접 수정하지 않았다.
- 실제 섬 PNG 캡처 직접 확인: tmp/a04-screens/island-initial.png.
- Pleos 실제 P 허용 상태의 정식 MainActivity에서 저장된 섬 표시 확인: pleos-main.xml.
- 일반 정식 MainActivity UNKNOWN 확인: generic-main-unknown.xml. Automotive 개발 진입 차단 및 정식 화면 복원은 별도 캡처 기록으로 확인한다.
- git diff --check PASS.

검사 중 Catalog 판독기 이동 파일의 CRLF가 정적 검사에 걸려 LF로 수정 후 재검사했다. 기기 UI 도구의 null-root 오류도 있었다. 최초 하네스가 실패한 dump 뒤 이전 XML을 읽을 수 있어, 이전 파일 제거와 유효 XML 재시도로 수정했다. 첫 중단 시험은 전체 통과로 계산하지 않았고, 변경한 외형을 먼저 원복한 뒤 최종 APK에서 재시험했다. 마지막 전경 복귀는 런처가 이미 실제 섬으로 복귀했는데 하네스가 개발 버튼을 다시 찾는 문제였으며, 실제 복귀 화면을 별도 확인해 마무리했다. 단일 무중단 실행으로 8개가 통과한 것으로 표시하지 않는다.

## NOT RUN / 다음 작업

- 앱 화면의 저장 읽기 오류/Catalog 실패 강제 주입과 복구 UI 시험, 작은 화면·글꼴·전체 회전 조합은 NOT RUN.
- 실제 도착 저장·PM 브리핑·ack 이동·C 동행/방문·I03/I06·전체 U20·모드 전환은 NOT RUN.
- 이번 화면에서 실제 D/N/R 제한을 다시 주입한 시험은 NOT RUN. 이전 차량 gate 시험 결과와 이번 P 화면/개발 차단 결과를 구분한다.
- 실차·서명 release 설치·다른 PC 검증 NOT RUN.
- 다음 A04-3: DEMO 여정을 단일 서비스에 연결하고 pendingReceipt 기반 PM 브리핑·ack 성공 후 이동을 연결한다. 사용자 확인 출처는 B 확인 후 별도로 반영한다.
