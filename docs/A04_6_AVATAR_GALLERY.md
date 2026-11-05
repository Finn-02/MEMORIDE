# A04-6 B04 갤러리 개발 호스트 연결

2026-11-05 KST, 파트 A. 검토한 B PR은 [#16](https://github.com/Finn-02/MEMORIDE/pull/16),
HEAD `af388f13a62f4cf7578982cb66a64ba2a184485b`다. debug Kotlin 파일 1개만 추가하며
기존 AvatarView와 APK Catalog/PNG를 사용한다. 코드 검토와 아래 연결 시험에서 병합 차단 결함을 발견하지 않았다.

## 변경과 통합 순서

- 기존 A 파트 브랜치에 B 커밋 이력을 가져왔다. B의 갤러리 파일은 수정하지 않았다.
- DevelopmentActivity의 기존 `B04 캐릭터 이미지 확인` 진입과 시스템 Back을 유지한다.
  DevelopmentAvatarHost의 임시 샘플 대신 공개 `AvatarRuntimeGalleryScreen(onBack, modifier)`를 호출한다.
- 기존 gate가 허용할 때만 갤러리를 구성한다. 제한/배경 전환으로 해당 분기가 폐기되면
  갤러리의 임시 빠른 전환과 로딩도 취소된다. 갤러리 Back은 기존 개발 화면으로 복귀한다.
- 개발 gate, Automotive 차단, 저장/보상, 공통 계약, C/PM 구현과 release 진입점은 변경하지 않았다.
- A 호스트 연결은 기존 [PR #15](https://github.com/Finn-02/MEMORIDE/pull/15)에 포함한다.
  PM 검토 후 **#16 → #15** 순서로 Create a merge commit 병합을 권장한다.
  B 이력이 A 브랜치에 포함되어 있어 #16은 먼저 병합해도 중복 기능 변경이 생기지 않는다.
  이 작업에서는 main이나 GitHub PR을 병합하지 않았다.

## 직접 실행한 검증

Windows AMD64 / Python 3.10.4 / 기록된 JBR 25.0.4 / Gradle wrapper 9.3.1.

- `python scripts/check_all.py --android`: Python 44개, JUnit 230개 PASS
  (실패/오류/skip 0), debug 빌드 PASS.
- `gradlew.bat --no-daemon :app:assembleRelease`: unsigned release 빌드 PASS.
- `git diff --check`: PASS. APK DEX 검사에서 AvatarRuntimePreviewKt,
  DevelopmentAvatarHostKt, DevelopmentActivity는 debug에 존재하고 release에는 없다.
- 일반 Android API37 x86_64의 실제 DevelopmentActivity에서 9개 조합 페이지와
  fallback 페이지, 빠른 전환 최종 상태, 갤러리 Back, 시스템 Back을 확인했다.
  갤러리 전후 게임 저장값은 불변이었다. 이는 B 작성자의 기존 36개 조합 시각 검증과 별도의 호스트 연결 시험이다.
- 다른 Activity로 배경 전환 후 복귀하면 갤러리가 첫 페이지에서 새로 구성되는 것을 확인했다.
- **I03 PASS:** 공개 UI로 DEMO 대전 도착 저장 → 아이템 보기 → 빵모자 장착 → 모모 선택 →
  섬 → 새 DEMO 여정 준비 → 동행 모의 화면 → 섬 복귀를 실행했다.
  세 화면 캡처에서 모모·빵모자를 확인했고, 왕복 전후 전체 저장값이 같았다.
  외형을 별도로 복사하거나 저장 파일을 직접 쓰지 않았다.
- Pleos 2.1 API34의 실제 Automotive feature에서 DevelopmentActivity의 갤러리 진입이
  차단되는 것을 확인했다. 저장 SHA256은 불변이며 정식 Main으로 복귀했다.
- 일반 Android 시험은 임시 사용자 MEMORIDE_B04_GALLERY_TEST에서만 진행했다.
  종료 후 해당 사용자만 제거했고 원래 사용자 복귀와 원래 저장 SHA256 일치를 확인했다.

## 환경 실패와 재시험

AVD 부팅 완료 전 사용자 생성과 첫 사용자 전환 직후 Activity 실행은 시스템 준비 오류로 실패했다.
준비 완료 후 재시도했다. 임시 게스트의 초기 설정 상태에서 Home 키가 배경 전환을 만들지 않아
첫 배경 시험은 실패했다. 설정값을 바꾸지 않고 Settings Activity를 열어 실제 onPause를 유발해 통과했다.
사용자 전환 직후 첫 제거는 실패했고, 원래 사용자 복귀 후 `pm remove-user --wait`로 제거를 완료했다.
이 환경 실패를 앱 동작 통과로 집계하지 않았다. SDK XML과 DataStore Unsafe 경고는 남아 있다.

## NOT RUN과 다음 단계

IDE Run Preview/Layoutlib, 다른 밀도·회전, 손상 PNG, 빠른 전환의 모든 중간 프레임,
실차·주행 안전 인수·서명 release 설치·다른 PC는 NOT RUN이다. 갤러리는 샘플 외형이며
실제 저장 상태를 바꾸는 화면이 아니다. B의 전체 36개 조합 그림 품질 인수를 대신하지 않는다.

다음은 최신 PR HEAD에 대한 PM의 병합 판단과 A05 서명 release·교차 설치다.
개인 로그·캡처·임시 사용자 데이터와 검토 ZIP은 커밋하지 않는다.
