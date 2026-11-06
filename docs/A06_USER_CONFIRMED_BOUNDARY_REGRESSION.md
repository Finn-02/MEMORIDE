# A06 C 비교와 독립적인 사용자 확인 저장 경계 회귀

2026-11-06 KST / Part A / Windows build26200 AMD64.
시작 HEAD `3d28c0b0d79f82160048f36d87e81275e73b07b4`, branch `part/a-a01-bootstrap`.

## 범위와 격리

C의 Pleos/QEMU 교차 시험을 기다리는 동안 A 소유 통합시험만 보강했다.
production 앱 동작·B 구현·저장 키/스키마·공통 계약·C 코드는 변경하지 않았다.
실제 BGameService와 PreferencesGameSaveStore를 사용하며 모든 쓰기는 JUnit TemporaryFolder의 새 파일로 한정한다.
DataStore scope를 cancelAndJoin한 후 같은 임시 파일로 서비스를 재생성해 복원을 확인한다.
이는 JVM 서비스/파일 재생성 시험이며 Android 프로세스·실제 차량 신호 시험이 아니다.

C 비교 APK는 소스 `685d107b5a10cd43b4887638d12e0445b3aef383`의 기존 바이너리로 고정했다.
SHA256 `472761def4e579b4b850b5144ea69fa29faebc876d511743a1a0867580f5fcae`.
이번 Gradle 명령은 unit-test task이며 assemble/package/install을 실행하지 않는다.
기존 APK 해시 불변을 확인하고 AVD/SDK 이미지/renderer/콘솔/기어는 조작하지 않았다.
이번 테스트·문서 SHA와 C 비교 APK 소스 SHA를 구분한다.

## 추가한 시험 3개

1. **오래된 acknowledge / 원본 재전달 / 새 pending**
   사용자 확인 REGION 저장→확인 완료 후 같은 지역의 새 사용자 확인을 저장한다.
   옛 브리핑 닫기는 라우팅/파일 변경 없이 거부된다. 서비스를 재생성한 뒤 B 공개 경계로 옛 원본을
   재전달해도 AlreadyApplied로 원본을 반환하고 새 pending을 유지한다.
   새 영수증을 닫을 때 pending만 지워지며 원본2개/지역 아이템3개가 보존된다.
   원본 재전달은 B 공개 API 검증이며 A generic USER_CONFIRMED 진입을 활성화한 것이 아니다.
2. **KST 자정 전 확인 / 저장 실패 / 자정 후 재시도**
   2026-11-06 23:59:59 KST의 DAILY 확인 저장을 주입 실패시킨 뒤 다음 날00:00:01에 재시도한다.
   원래 ID·확인 시각·null 측정값으로11월6일 성장1을 저장한다.
   다음 새 확인은11월7일 성장2, 같은 날 세 번째 확인은 성장2를 유지한다.
   파일 재생성 후 원본3개/최신 pending/성장2를 복원한다. 시계와 실패는 시험에서 주입하며 실제 디스크 장애 시험이 아니다.
3. **승인된 저장 / 대기 중 모드 전환 / 초기화**
   USER_CONFIRMED 저장이 승인돼 저장소에 진입한 동안 DEMO 모드 전환을 대기시킨다.
   저장을 먼저 마친 후 모드 전환·명시적 reset을 직렬 실행하고 세대2를 확인한다.
   옛 질문/재시도는 거부되며, 재생성 후에도 원본/아이템/pending이 초기화 상태로 유지된다.

각 신규 시나리오는15초 timeout을 사용한다. B가 게임 파일을 쓰는 유일한 경계를 유지한다.
실패 주입 wrapper는 실제 파일 commit 이전의 IOException만 만든다.

## 검증

- 기록된 JBR25.0.4 / Gradle wrapper9.3.1 사용.
- `gradlew.bat --no-daemon :app:testDebugUnitTest --tests io.kailand.app.ManualJourneyIntegrationTest`: exit0,14개 PASS, failure/error/skip0.
- `gradlew.bat --no-daemon :app:testDebugUnitTest`: exit0, 전체264개 PASS, failure/error/skip0(XML 합산). task 실제 실행,40초.
- `python -X utf8 scripts/check_all.py`: exit0, 정적 검사/Python59개 PASS. Android 옵션 없이 실행했다.
- `git diff --check` PASS. 기존 SDK XML/Unsafe 경고는 남아 있다.
- 이번 작업에서 debug APK 패키징/release 빌드를 재실행하지 않았다.

## 남은 것

2026-11-07 후속: [B 독립 검토 회신](A06_B_USER_CONFIRMED_INDEPENDENT_REVIEW.md)을 수용했다.
B 보고 기준 동일9091507/독립14개 PASS 및 지정 저장 경계 검토 적합이며, 아래 검토 대기는 당시 기록이다.

이것은 A PC에서 실제 B 서비스를 연결한 통합시험이며 B 담당자의 독립 교차 검토 완료가 아니다.
B가 원본 retry/pending/ack/복원 경계를 검토할 수 있도록 요청 메시지와 시험 파일을 별도로 전달한다.
C의 이미지 설치/기어 반복/호스트 QEMU 종료 원인 규명은 별도 진행이다.
기기 저장·ack 실패 UI, 전원 차단/실제 디스크 고장, 최신 서명 release·실차·최종 A06 인수는 NOT RUN.
신규 기능이나 crash 해결을 주장하지 않는다.
