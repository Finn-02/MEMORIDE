# C05 방문 반복 검증과 Windows 백업 인수

상태: IN PROGRESS. 2026-11-04 C 로컬 단위·검사 화면 검증을 수행했다. A04 실제 게임 초기화/저장 통합과 A05 서명 release 후보가 없으므로 C05 전체 완료는 아니다. C04 PR #8 병합 후 사용자 push 지시에 따라 완료한 로컬 시험·검사 화면·문서를 기존 C 브랜치 전달 대상으로 정리했다. 별도 PR은 아직 생성하지 않는다.

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
