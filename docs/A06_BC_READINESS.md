# A06 B/C 연결 준비 상태 확인

2026-11-05 22:30 KST / Part A / Windows AMD64 / JBR25.0.4 / wrapper9.3.1.
확인 시작 A HEAD `a47eb5d`, 브랜치 `part/a-a01-bootstrap`.
**이 문서는 원격으로 공유된 코드·PR·근거의 확인 결과다. 다른 PC의 미푸시 작업 상태를 추정하지 않는다.**
기존 문서의 요청 문장은 검토 자료로 읽었으며, 이번 범위는 준비 상태 확인이다.
B/C 구현 수정·미병합 코드 채택·다른 사람에게 메시지 전송은 수행하지 않았다.

## 확인한 원격 기준

`git fetch origin` 성공 후 main은 `a04b3c57cf8448192adaeb912ecf25978e63a0d3`이었다.
PR #17 계약 공급과 #18 A05, #19 브리핑 수정은 포함되어 있다.
GitHub `repo:Finn-02/MEMORIDE is:open` 조회 결과 열린 PR은 0개였다.

| 대상 | 원격 HEAD | main 대비 공유 상태 | 판단 |
|---|---|---|---|
| B `part/b-b01-game-state` | `af388f13a62f4cf7578982cb66a64ba2a184485b` | 미병합 diff 없음. PR #17 병합 커밋은 해당 원격 이력에 없음 | USER_CONFIRMED B05 구현/시험 완료 근거 미확인 |
| C `part/c-c01-companion` | `184727aacebef55e6ab752f1db53789a81af92a6` | 미병합9파일. PR #17 및 A04 #15/A05 #18 이후 main은 해당 원격 이력에 없음 | 기존 연결 사용 가능. 추가 C 변경은 최신 main 통합·회귀 후 검토 필요 |

B 상태 문서의 NOT STARTED와 C의 AppRoot 대기 체크는 현재 기능 전체 상태로 신뢰할 수 없다.
실제 B 게임 저장/보상과 C01~04 연결은 이미 main에 있다. 해당 소유자의 상태 문서는 직접 덮어쓰지 않았다.
아래 판단은 코드·커밋과 A/C의 시점별 실행 근거를 우선한다.

## B05: 실제 사용자 확인 저장 연결은 아직 대기

공통 `DataSource.USER_CONFIRMED`, `ArrivalEvidence.USER_CONFIRMED_DESTINATION`과 PM 표시 분기는 main에 있다.
반면 현재 BGameService의 `invalidArrivalReason`은 여전히 DEMO 여부만 비교한다.
따라서 비DEMO 출처/evidence 사이를 정확히 구분하는 3개 허용 조합 검증이 아니다.
USER_CONFIRMED 측정값의 null 전용 입력/저장 복원 검증도 없고, 신규 출처의 B 회귀 시험도 찾지 못했다.

A `GameCommandCoordinator.completeArrival`은 신규 source 또는 evidence를 B 호출 전에 거부한다.
이 차단은 유지해야 한다. 타입 공급만 보고 삭제하면 준비되지 않은 B 저장 경계를 열게 된다.
실제 수동 확인은 `ManualArrivalController`의 메모리 초안과 debug 진단 사용까지이며, 정식 저장 경로가 아니다.

B에서 받을 결과:

1. 최신 main 반영 branch/SHA와 B05 변경 PR/커밋.
2. source/evidence 9조합 중 정확한3조합 허용·6조합 거부/저장 불변 시험.
3. USER_CONFIRMED의 두 측정값이 null일 때만 허용. 0/양수도 거부.
4. 원본 ID·최초 확인 시각·출처·null 유지, 중복/실패 재시도/ack/최신 pending 보존.
5. 실제 파일 DataStore 왕복과 재생성 복원, 과거 영수증 전체 불변조건 검증.
6. 기존 DEMO/PLEOS 저장 상향 호환과 신규 저장의 구버전 읽기 실패/원본 보존 근거.
7. 실제 시험 명령/결과, NOT RUN과 저장 스키마 유지 확인.

기준은 [공통 원문](MEMORIDE_USER_CONFIRMED_COMMON_STANDARD_v1.0.md)과
[이미 공급한 A→B 인계](USER_CONFIRMED_CONTRACT_HANDOFF.md)다. 새 계약을 다시 설계할 단계는 아니다.
이후 A가 정식 수동 여정의 gate/foreground/owner/generation/revision/질문 토큰·동일 명령 재시도를 연결한다.
단순히 A 차단만 삭제하거나 신규 mode 버튼만 추가하지 않는다.

## C: 기존 연결과 미병합 변경을 구분

현재 GameAppRoot에는 WorldScreen의 지역 선택→준비 후보, 읽기 인벤토리 열기,
CompanionScreen/VisitRoute의 실제 B avatarSlot, DemoPanel/Runner와 reset 연결이 있다.
A04의 I03/주요 흐름과 A05 C PC 설치 인수는 기존 근거를 재사용한다.
따라서 C가 A04 연결이나 첫 release 전달을 새로 기다려야 한다고 판단하지 않는다.

C 원격의 미병합9파일:

- production UI3: CompanionScreen 자연색 모의 지도/기본 높이200dp/문구 배치,
  VisitScreen의 장식용 편지 그림, WorldScreen 지역 그림·카드 배치.
- VisitControllerTest: 수락/거절 경쟁, reset/수락 경쟁, 3회 부분 reset과 늦은 입력 시험3개.
- debug `VisitAcceptancePreview`: 샘플 입력으로 방문 수명주기·reset 통지를 관찰하는 검사 화면.
- 문서4: c01_companion, c04_demo, c05_acceptance, status/c.

이 차이에는 공개 콜백/DTO·VisitController 본체·보상/저장 스키마 변경이 없다.
하지만 최신 main에 통합한 컴파일/기기 인수는 이번에 실행하지 않았으므로 병합 승인으로 해석하지 않는다.
특히 C가 보고한 방문27개와 이번 main의 방문24개는 다르다. 신규3개는 아직 main에 없다.

C 문서의 임시 파일 시험4개는 A04 `0850ea6`에서 실행했다고 명시돼 있다.
회귀 원문은 검토하되, 그 하네스는 tmp/ZIP이고 현재 C 브랜치에 커밋된 시험으로 주장하지 않는다.
문서의 샘플 슬롯 외형/Preview 수락 시험을 실제 저장 상태 I03·실제 앱 동의 시험으로 대체하지 않는다.
C가 보낸 v2/v3 설치 결과는 [A05 C 인수 검토](A05_C_ACCEPTANCE_REVIEW.md)에서 이미 수용했다.

C에서 받을 결과:

1. 기존 C 브랜치에 최신 main을 반영한 SHA와, 위9파일 중 이번에 전달할 의도된 변경 범위.
2. 추가 방문3개를 포함한 실제 회귀 결과와 debug/정식 코드 경계 확인.
3. 실제 앱 S08 수락/거절/만료/철회/닫기, 화면 이탈·복귀·모드/reset 중단 중 완료/미완료 구분.
4. S09 전체 게임 초기화 후 시연3회, 큰 글자·창 크기·외형 연결의 남은 인수 근거.
5. 예전 AppRoot/A05 대기 표시 갱신. 이미 수용한 C PC 설치 근거는 재사용.

새 캔버스 그림과 화면 배치를 A가 임의로 최종 디자인으로 채택하지 않는다.
C가 변경을 정리한 후 검토하고, 최종 UI 인수는 별도 수행한다.

## 이번 실행과 한계

`gradlew.bat --no-daemon :app:testDebugUnitTest`에 다음6개 `--tests` 필터를 전달했다.

- `io.kailand.app.GameCommandCoordinatorTest`: 14
- `io.kailand.app.GameSimulationRuntimeTest`: 9
- `io.kailand.feature.companion.CompanionPresentationTest`: 8
- `io.kailand.feature.visit.VisitControllerTest`: 24
- `io.kailand.feature.world.WorldPresentationTest`: 10
- `io.kailand.feature.demo.DemoScenarioRunnerTest`: 23

exit0, **88개 실제 실행 PASS**, failure/error/skip0. `testDebugUnitTest`는 실제 실행했고 컴파일은 UP-TO-DATE 재사용했다.
USER_CONFIRMED 차단/기존 출처 전달, 모드/reset/옛 입력과 방문 경계를 현재 A 코드에서 확인했다.
이번에 B05 신규 구현·C 미병합9파일의 컴파일/시험·Android UI/기기/release·전체 회귀는 NOT RUN이다.
문서 package/link·diff 검사는 별도로 수행한다. 새 PR·main 병합·Actions·다른 파트 메시지 전송은 하지 않는다.

## 실행 순서

1. B의 결과를 받기 전까지 사용자 확인 실제 저장 차단 유지.
2. B/C에 아래 전달 초안과 기존 공급 문서를 공유할 수 있다. 담당자 PC의 미푸시 작업이 있으면 SHA와 함께 회신받는다.
3. A는 B 대기와 독립적으로 현재 앱의 C 통합 시연/인수 행렬을 계속 검증할 수 있다.
4. B05가 준비되면 A 수동 확인 저장 연결, C 변경이 준비되면 해당 UI/시험 통합 검토를 수행한다.
5. 최종 UI 이후 최신 SHA 서명 후보 인수. A06 결과가 모이기 전에 새 PR을 반복 생성하지 않는다.

## 전달 초안 — 아직 전송하지 않음

### B에게

PR #17 공통 계약은 main에 반영됐고 현재 main은 a04b3c5입니다.
원격 B 브랜치 af388f1에서는 USER_CONFIRMED B05 구현/시험을 확인하지 못했습니다.
로컬 진행 내용이 있으면 branch/SHA와 함께 공유해 주세요.
기존 인계 문서 기준으로 3개 source/evidence 조합·두 측정값 null 전용·원본/중복/ack·실제 DataStore 복원·
기존 저장 상향 호환/구버전 실패 시 원본 보존을 구현·시험하고 변경 커밋/PR, 명령/결과, NOT RUN을 부탁드립니다.
enum은 이미 공급됐으며 schemaVersion1/기존 저장 키/보상 규칙은 유지합니다.
A는 결과를 확인한 뒤 정식 수동 확인 저장 경로를 연결하겠습니다. 현재 진입 차단은 유지 중입니다.

### C에게

A04 연결과 A05 서명 v2/v3 C PC 설치 인수는 완료됐고, A06 통합 검증을 진행 중입니다.
원격 C 브랜치184727a의 미병합9파일(UI3·방문 시험3개·debug 검사 화면·문서4)을 확인했습니다.
기존 C 브랜치에서 최신 main a04b3c5를 반영하고 이번에 전달할 변경 범위와 회귀 결과를 공유해 주세요.
실제 앱의 방문 수락/거절/만료/철회/닫기, 이탈·모드/reset 중단, 전체 초기화 후 시연3회와 큰 글자/창 크기의
완료·미완료를 구분해 부탁드립니다. 이미 확인한 별도 PC 설치 근거는 다시 만들 필요가 없습니다.
과거 AppRoot/A05 대기 문구도 실제 상태에 맞춰 정리해 주세요. A는 기존 저장 상태/외형/실제 gate를 공급합니다.
