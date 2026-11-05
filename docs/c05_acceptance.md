# C05 방문 반복 검증과 Windows 백업 인수

## 현재 판정표 — 2026-11-05 재정리

아래 표를 현재 판정으로 사용한다. 뒤의 날짜별 기록은 당시 결과이며, 이전의 'A04 구현 대기'나 'AppRoot 연결 미수행' 문구가 A04 전달본 검증 결과를 취소하지 않는다. C 브랜치와 미병합 A04 전달본의 결과는 합산하지 않는다.

| 대상 | 확인된 범위 | 아직 필요한 확인 |
|---|---|---|
| C 브랜치 | C01~C04와 B04 공통 슬롯 샘플, C 로컬 방문/Runner 시험 | A04 병합본에서 앱 전체 인수 |
| A04 전달본 `0850ea6` | 공통 검사 44/223개, 별도 실제 파일 경계 4개, 만료·모드 전환·background 관찰 | 실제 앱 수락/거절/닫기, 저장 의상 I03, UI 초기화 반복 S09 |
| 실제 차량 연동 | A가 기록한 P/D/P와 SDK 진단 | 실제 자동 도착·실차·release 설치; C의 일반 AVD 결과로 대체하지 않음 |
| C05 전체 | 진행 중 | 기기 UI 인수와 A05 동일 서명 release 백업 설치 |

### 다음 기기 인수 순서

일반 Android의 명시적 개발 DEMO에서 수행한다. 먼저 대상 소스 SHA·APK SHA256·기기 API/ABI를 기록한다. 기존 게임 데이터가 있으면 전체 초기화를 바로 실행하지 말고 데이터 소유자의 허용 범위를 확인한다. 서명 불일치나 비공개 Activity 접근 실패는 삭제 설치·Manifest 변경으로 우회하지 않는다.

1. 공개 DevelopmentActivity에서 A04 실제 저장 화면으로 진입한다. 요청마다 발신/수신 상태와 남은 시간을 확인하고 수락 → VISITING → 닫기를 확인한다. 새 요청은 실제 쿨다운 종료 후 거절하고 캐릭터가 표시되지 않는지 확인한다.
2. 새 요청을 철회하고 늦은 응답으로 방문이 생기지 않는지 확인한다. 앱 UI로 옛 응답을 재전달할 수 없으면 해당 부분은 기존 단위 시험 근거로만 남긴다.
3. DEMO 여정 완료 뒤 저장된 브리핑·아이템·월드맵을 확인한다. 같은 도착 반복에서 원본 영수증과 보유 수량이 유지되는지 확인한다. 사용자 확인 도착을 PLEOS 자동 도착으로 기록하지 않는다.
4. 실제 보유 의상을 적용한 뒤 섬·동행·수락한 방문의 외형을 비교한다. 앱 재실행 후에도 같은 저장 외형이 표시돼야 I03으로 판정한다. 샘플 슬롯 결과와 구분한다.
5. 초기화 가능한 시험 데이터에서 D01~D06 → 전체 초기화 → 재진입을 3회 수행한다. 각 회차의 성장·아이템·영수증·방문 종료·이전 타이머 비재개를 기록한다. 기기 UI와 파일 경계 시험 결과를 구분한다.
6. A05 후보가 전달되면 전달된 서명·SHA256을 확인하고 아래 Windows 백업 체크리스트를 수행한다. debug 결과로 release 항목을 완료 처리하지 않는다.

각 항목은 PASS/FAIL/NOT RUN, 대상 SHA, 실제 관찰을 짧게 남긴다. 실패 시 해당 기능 작성자에게 재현 단계를 전달하고 C에서 A/B/PM 내부 구현을 대신 변경하지 않는다.

상태: IN PROGRESS. C 로컬 검증에서 A04 전달본의 실제 파일 저장·반복·초기화 경계 교차 검증까지 진행했다. A04는 A 브랜치에서 확인했고 C 브랜치에는 병합하지 않았다. 실제 앱의 남은 UI 인수와 A05 서명 release 백업 설치가 있어 전체 완료는 아니다. 완료된 결과 문서는 기존 C 브랜치에 전달하고, 별도 PR은 사용자 지시 후 생성한다. 아래는 최신 결과부터 이어지는 시점별 기록이다.

## 2026-11-05: A04 전달본 교차 검증

A04 `0850ea6`이 A 브랜치에 push된 것을 확인해 detached checkout에서 검증하고 기존 C 브랜치 `01fceab`로 복귀했다. 미병합 A04를 C 브랜치에 병합하거나 A/B 소스를 수정하지 않았다. 별도 환경 문서 수정은 stash로 보존·복원했다. 따라서 아래 결과는 **A04 전달 커밋의 결과**이며 현재 C 브랜치의 정식 앱에 A04를 연결했다는 뜻이 아니다.

- 공통 `py -3 -X utf8 scripts/check_all.py --android`: exit 0, Python 44개·Android 223개 PASS(실패/오류/skip 0), debug BUILD SUCCESSFUL in 27s, 15 executed / 30 up-to-date.
- 별도 임시 앱 경계 시험 4개: exit 0, BUILD SUCCESSFUL in 13s, 2 executed / 25 up-to-date. 실제 `GameSimulationRuntime`·`GameCommandCoordinator`·C Runner/방문·BGameService/Preferences DataStore를 JUnit 임시 파일에 연결했다. A 기존 Runtime 시험의 메모리 대역과 구분한다. 이 4개는 223개 집계에 포함되지 않는다.
- D01~D06→전체 reset을 3회 반복해 성장/발견/영수증과 오래된 방문 token·타이머 차단을 확인했다. D03→D04에서는 원본 영수증과 저장 파일 바이트가 유지됐다. D06의 null 측정값도 유지됐다. 이는 파일 기반 소프트웨어 반복 검증이며 기기 UI S09 3회는 아니다.
- 실제 파일 쓰기 대기 중 reset이 완료되고 오래된 도착·GameAction·방문 입력이 재지급하지 않는 U20 파일 경계 시험 PASS. 실제 파일 보존 상태에서 모드 전환의 비파괴성과 옛 입력 차단 PASS. 명시적 IOException 주입 reset 실패→저장 보존/입력 차단→재시도 성공 PASS. 실제 기기 디스크 장애를 발생시킨 시험은 아니다.

검사 하네스·init script는 무시되는 tmp에만 보관하고 ZIP으로 전달한다. production C feature에 A/B import를 추가하지 않았으며 별도 앱 프로젝트·공통 계약·저장 스키마·릴리스 키를 만들지 않았다.

### A04 실제 앱의 일반 Android UI 관찰

기존 API 37 AVD의 공개 DevelopmentActivity에서 `A04 실제 저장 화면 확인` 버튼으로 진입했다. 비공개 DevelopmentGameActivity의 ADB 직접 시작은 exported=false로 거부돼 정상 공개 진입점을 사용했다. 접근 권한이나 Manifest를 바꾸지 않았다.

- 실제 저장 화면의 기본 토리/성장 0, 가상 방문 메뉴·목적 표시, 요청→15초 응답 타이머→만료·캐릭터 없음 관찰.
- 요청 중 PLEOS 모드로 전환→`모드 전환으로 방문을 종료했어요`, DEMO 복귀 뒤 이전 요청 비재개 관찰. 실제 PLEOS 여정이 연결됐다는 뜻이 아니다.
- 새 요청 중 Android 홈→최근 앱에서 복귀하면 섬으로 돌아왔고, 방문 재진입 시 화면 이탈 종료·캐릭터/이전 타이머 비재개 관찰.
- 수락 조작이 제한 시간 뒤 도달한 경우를 관찰했고 성공으로 기록하지 않았다. 이번 실제 앱의 수락·거절 성공은 NOT RUN/미확인이다. 앞선 별도 슬롯 하네스에서의 수락·거절과 구분한다.
- 설치 전·중간·검사 후 `memoride_game.preferences_pb`는 모두 없었다. 저장 내용 출력·보상 지급·의상 변경·게임 전체 초기화를 실행하지 않았다. 해시 일치라고 주장하지 않고 **파일 없음 유지**로 기록한다.

### 현재 완료와 남은 인수

파일 경계의 반복·U20·모드 보존은 C 교차 검증까지 PASS다. 실제 앱 수락/거절/닫기, 저장된 의상 I03, 기기 UI 전체 초기화 후 S09 3회, 큰 글자/회전/프로세스 복원과 A05 서명 release 교차 설치는 남아 있다. A04 구현 자체를 더 기다리는 단계는 지났으며, A04 리뷰·병합 및 위 기기 인수와 release 후보 전달이 다음 단계다. 사용자 확인 출처의 실제 저장은 B 계약 대기라는 A의 한계를 유지하고 기존 DEMO와 혼동하지 않는다.

## 2026-11-05: 최신 main과 B04 슬롯 인수

최신 main `edea1fd`(A03 gate·수동 도착 확인 개발 흐름, B04 렌더러, C01 패널 접기 PR #11 포함)을 기존 C 브랜치에 병합했다. C01의 중복 접기 변경은 C의 자연색 지도·200dp 기본 높이·동일한 숨김/확대 로직을 보존해 해결했다. A 소유 설정·플랫폼 코드는 main 그대로 수령했으며 직접 변경하지 않았다. 별도 환경 문서 수정도 보존·복원했다.

공통 `py -3 -X utf8 scripts/check_all.py --android`: exit 0, Python 44개·Android 195개 PASS(실패/오류/skip 0), debug BUILD SUCCESSFUL in 32s, 18 executed / 27 up-to-date. 이 집계에는 기존 C05 시험 3개가 포함된다. 실제 차량 입력이나 실차 안전 시험 성공을 뜻하지 않는다.

임시 **앱 조립 경계** 검사 화면에서 B의 실제 `AvatarView`를 같은 `AvatarSlot`으로 C01 `CompanionScreen`·C02 `VisitRoute`에 전달했다. main의 AppRoot·B 코드·C feature import·공통 계약·빌드 파일은 수정하지 않았다. 검사 화면과 init script는 무시되는 tmp 경로에만 있으며 검토 ZIP으로 전달한다. 일반 Android AVD 전용이며 Automotive 환경에서는 샘플을 표시하지 않도록 차단한다. Catalog만 읽고 게임 저장은 만들지 않는다. 외형/소유 목록은 명시된 샘플이며 실제 보유·보상 상태가 아니다.

실제 API 37 일반 Android 가로 AVD 관찰:

- 토리 기본 외형과 빵모자가 섬에 표시됨. 같은 `AvatarPreview(tori, daejeon_hat)`를 C01과 수락 후 C02에서 같은 B 레이어로 표시함을 관찰했다. 동일한 저장 상태에서 읽었다는 증거는 아니다.
- C02 하네스의 목적 표시 뒤 `시험 요청` → 별도 `수신 수락` 클릭으로 실제 controller가 REQUESTED→VISITING으로 전환하고 토리+빵모자를 표시했다. 명령을 자동 수락하지 않는다. 이 하네스 버튼은 controller의 토큰 검사 경로를 사용하며 원래 VisitScreen 수락 버튼의 새 클릭 시험과 구분한다.
- 방문 중 같은 슬롯에서 모모+빵모자·루루+빵모자 교체 표시를 관찰했다. 다른 두 의상과 모든 표정 조합의 기기 수동 확인은 NOT RUN이며 B 단위 시험과 구분한다.
- 새 요청 뒤 하네스 `수신 거절`을 클릭해 REJECTED·캐릭터 없음 관찰. 기존 쿨다운 경계 검증에는 하네스의 진행자 재시작을 사용하지 않는다.
- 회전 도구로 기기 방향을 바꿨을 때 Android 콘텐츠가 옆으로 남아 기존 가로 방향으로 복원했다. 세로 재배치·회전 상태 복원 인수는 PASS 처리하지 않았다. 최초 툴바 확대 모드 선택도 즉시 원복했으며 제품 검증 결과로 세지 않는다.

검사 전용 APK는 별도 init script의 `:app:assembleDebug`로 빌드했고, 기본 빌드 14s(7 executed / 32 up-to-date), 동의 도구 보완 후 2s(5 executed / 34 up-to-date) PASS. 재설치 직후 빈 PreviewActivity는 cold start로 복구했다. 이 APK에는 로컬 검사 화면이 추가돼 있으며 기준 release가 아니다.

완료 범위는 **B04 공통 슬롯의 샘플 렌더링 인수**다. 실제 B 저장 의상 변경→앱 재진입→섬/동행/방문 일치 I03, A04 전체 초기화·U20·실제 앱 S09, A05 서명 release 백업 설치는 계속 미완료다. A03 수동 도착 확인 개발 흐름도 실제 B 저장에는 아직 연결하지 않았으므로 기존 계약이나 보상 규칙을 C에서 변경하지 않는다.

## 이번 결과

| 항목 | 결과와 범위 |
|---|---|
| U07 늦은 수락 | 기존 시험으로 거절·만료·철회·이전 토큰 뒤 수락 거부 PASS. 기기 검사 중 수락 조작이 만료 뒤 도달한 경우 EXPIRED·캐릭터 없음 관찰. 기기에서 수락 성공은 이번에 확인하지 못함 |
| U08 활성 1개·쿨다운 | 기존 동시 요청·29,999/30,000ms 경계 시험 PASS. 실제 Handler 요청 만료 직후 재요청 시 30초 간격 안내·요청 거부 관찰 |
| 응답 경쟁 | 추가 시험: 수락/거절 동시 호출 중 한 번만 성공, 최종 상태·캐릭터·타이머 일치 PASS |
| reset 경쟁 | 추가 시험: 수락/reset 동시 호출 뒤 CANCELLED·입력 차단·타이머 0, 이전 응답/콜백 거부, 실패 통지 뒤 차단 유지 PASS |
| 3회 반복 오염 | 추가 시험: 거절→reset, 만료→reset, 수락/닫기→reset의 3회 연결 후 이전 요청/취소된 콜백을 강제로 재전달해도 새 요청·타이머에 영향 없음 PASS. 이는 C 로컬 부분이며 전체 게임 S09는 아님 |
| 실제 타이머 | API 37 일반 Android의 PreviewActivity에서 createVisitController의 SystemClock.elapsedRealtime·메인 Handler 사용. 요청 후 EXPIRED 및 캐릭터 없음 관찰. FakeClock 시험과 구분 |
| 실제 background | REQUESTED·응답 15초 화면에서 Android 홈으로 이탈, 동일 Activity 복귀 후 CANCELLED·자동 재개 없음 관찰. 실제 VisitRoute lifecycle 사용, AppRoot·Pleos 통합은 아님 |
| 화면 제거/복귀 | 검사 도구로 VisitRoute를 composition에서 제거·다시 배치 관찰. 이 조작은 IDLE에서 수행했으므로 활성 요청 중 화면 제거의 기기 증거로 사용하지 않음. 활성 요청 제거는 기존 단위 시험 PASS |
| reset 통지 대역 | 검사 도구의 beginReset→실패 통지에서 중단 유지, 동일 토큰 성공 통지 뒤 IDLE 복귀 관찰. B 저장 호출·실제 게임 초기화 없음 |

## 검사 화면 실행

Android Studio에서 `VisitAcceptancePreview.kt`의 `VisitAcceptancePreview`를 기존 AVD에 Run Preview한다. debug 전용이며 Manifest/AppRoot를 바꾸지 않는다. `C05 SAMPLE` 배너·현재 상태·응답 잔여 초가 표시된다. VisitRoute와 createVisitController를 그대로 사용하고 다른 feature나 A/B 내부 구현을 import하지 않는다.

- 기본 화면에서 요청·발신/수신 시점·수락/거절·철회·닫기를 조작한다. 15초 제한은 실제로 진행한다.
- 검사 도구의 `방문만 다시 준비`는 진행자용 로컬 세션 재시작이다. 일반 UI의 30초 쿨다운 검증에는 사용하지 않는다.
- `화면 이탈/복귀`는 composition 제거를, Android 홈은 실제 Activity background를 검증한다. 복귀 후 이전 요청을 자동 재개하지 않는다.
- `샘플 허용/UNKNOWN`은 이 격리 검사 화면의 입력이다. 실제 차량 gate를 바꾸거나 우회하지 않는다.
- `reset 시작/실패 통지/성공 통지`는 방문 controller 완료 통지 대역이다. 초기화 잠금·토큰만 확인하며 실제 저장 성공을 증명하지 않는다.
- controller는 composition 안에서 한 번 생성되고 이탈 시 정리된다. 이 Preview의 회전·프로세스 재생성에서 세션 유지 검증은 NOT RUN이며 A가 보유할 실제 owner와 구분한다.

## 실행 명령과 결과

Windows / 공통 JBR 25.0.4·wrapper 9.3.1:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests io.kailand.feature.visit.VisitControllerTest --tests io.kailand.feature.demo.DemoScenarioRunnerTest :app:assembleDebug
py -3 -X utf8 scripts/check_all.py --android
git diff --check
```

집중: C02 27개(기존 24 + 추가 3)·C04 23개 PASS, debug BUILD SUCCESSFUL in 4s(11 executed / 34 up-to-date). 전체: Python 44개·Android 123개 PASS, debug BUILD SUCCESSFUL in 10s(1 executed / 44 up-to-date). 전체 검사는 앞선 집중 빌드 산출물을 재사용했으며 집중 빌드에서 신규 Kotlin을 실제 컴파일했다. 이후 검사 화면 문장부호만 수정했다. APK 설치 Success와 PreviewActivity cold start Status ok를 확인했다. 최초 재설치 직후 빈 화면은 같은 PreviewActivity를 cold start해 해소했다.

이후 A02/B03/C04가 병합된 최신 main `b8faf56`을 반영하고 로컬 작업을 복원한 상태에서도 Python 44개·Android 165개 및 debug 빌드 PASS(7 executed / 38 up-to-date). 별도 A02/C04/B 파일 저장 경계 시험 5개는 [C04 검증 기록](c04_demo.md#a02-main-병합-후-로컬-경계-검증)을 따른다. 이는 실제 A04/AppRoot·전체 초기화 인수 완료를 뜻하지 않는다.

위 123개 시험 당시 로컬 C01~C03 그래픽 수정도 포함되어 있었다. 디자인은 별도 변경으로 전달한다. 초기 시험에는 A02가 포함되지 않았고, 이후 최신 main 반영 및 별도 경계 검증은 앞 문단과 C04 기록을 따른다. 실제 AppRoot의 A/B 조립은 여전히 미수행이다.

## Windows 백업 인수 체크리스트

- [x] 공통 wrapper/JBR 환경에서 debug 빌드 및 기존 일반 API 37 AVD 설치·검사 화면 실행
- [x] C05 수동 재현 절차·실제 결과·미실행 항목 기록
- [ ] A가 전달한 동일 서명 release APK·소스 SHA·APK SHA256·versionCode·지원 ABI/API 확인
- [ ] 후보 파일의 `Get-FileHash -Algorithm SHA256`을 A가 전달한 값과 대조
- [ ] 실제 백업 대상의 serial·API·ABI와 설치된 앱 서명을 확인
- [ ] A의 후보를 `adb -s <확인한-serial> install -r <확인한-release.apk>`로 설치하고 결과 기록
- [ ] 서명 불일치는 중단·기록. 데이터 삭제 설치, applicationId 변경, 제한 우회로 해결하지 않음
- [ ] S01 실행·S03 창/글자·S04 복귀·S08 동의·S09 전체 초기화 후 3회 시연 확인
- [ ] I03 의상 일치·I06 목적지 후보·U20 실제 Runner/여정/저장 초기화 연결 확인
- [ ] 필요 시 S10은 A의 같은 release 서명 이전/새 후보로 별도 검사

현재 이 PC 산출물은 app debug APK뿐이고 A의 서명 release 후보는 전달 확인되지 않았다. release 빌드·키 생성·서명 설정 변경은 C에서 수행하지 않았다. 일반 AVD 설치를 Pleos/실차 설치로 주장하지 않는다.

## 남은 인수

실제 AppRoot의 C 화면 진입, B 렌더러/의상 일치, A02/A04 실제 포트와 B GameSnapshot/영수증, 실제 게임 전체 초기화와 D01~D06·D03→D04를 연결해야 한다. 그 뒤 S08/S09·U20 및 release 백업 설치를 마치고 C05 완료를 판정한다. 현재 기기 수락/거절 성공·활성 화면 제거·가로/회전/글자 확대·프로세스 재생성·Pleos·release·운전 안전 시험은 NOT RUN이다.
