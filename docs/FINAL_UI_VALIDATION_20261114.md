# 최종 UI 후보 검증과 제출 전달 — 2026-11-14 KST

UI·아이콘·리허설 정리와 검증 결과를 기존 PR #29에 반영한다. **전체 인수는 HOLD**이며 Draft·미병합을 유지한다. 최종 README·APK 업로드·제출 커밋 확정·공개 전환·접수 메일은 PM 담당이다.

## 소스와 아티팩트 기준

- PM 최초 지정 소스: `a342825ea02550f4cb90d346494b748cae605cbb`.
- 최종 검수 APK 빌드 소스: `434abae4f58787230f18e2860ee9761fb9d7dcc4`.
- 이후 이 문서와 기존 전달 문서만 갱신한다. 문서 커밋이 추가된 PR HEAD와 APK 빌드 소스 SHA를 구분한다. APK를 재빌드한 것으로 표시하지 않는다.

|아티팩트|파일명|SHA-256|
|---|---|---|
|Debug 검수|`MEMORIDE_DEBUG_434abae4.apk`|`bcc3ec70c90d785f708c3f12fd9232dce50aec60208fa0b11c44ca272df96389`|
|Signed release v5|`독수리4형제_MEMORIDE_release.apk`|`b609491fe468a1003d3cd1bb22f41693ad88742f9611b73b849b0fc5687aea5c`|

Release는 `io.kailand`, versionName `0.1.0`, versionCode `5`, minSdk28/targetSdk37, non-debuggable이다. 기존 팀 인증서 SHA-256은 `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb`이며 독립 검증했다. 제출 파일명 변경 전후 APK 바이트/해시는 같다. 크기는 58,201,427 bytes다. 개인키·비밀번호·APK·검토 ZIP은 이번 소스 PR에 추가하지 않는다. APK와 ZIP은 사용자에게 전달했으며 PM의 `apk/` 또는 GitHub Releases 업로드와 구분한다.

## 마지막 변경

- `51cca9c`: 기존 로고의 벡터 adaptive icon 여백·배경 보완, debug 개발 진입 비활성/최근 작업 제외, 오래된 앱 소유 debug Android task 정리, `UxRehearsalActivity` 등록과 화면/격리 세션 제거.
- Pleos 하단바의 별도 최근 앱 기록에 남은 개발 DEMO 바로가기는 Android task 정리만으로 사라지지 않았다. 최근 앱 목록 갱신 후 아이콘 하나와 재부팅 유지가 확인된 표본이 있다. 앱 코드가 모든 Pleos 과거 바로가기 기록을 삭제한다는 뜻은 아니다.
- `434abae4`: 사용하지 않는 옛 썸네일 레이어 합성 시험을 실제 승인 HD 프레임 36종의 디코딩·표정/의상 구분·투명도·6프레임 캐시 검사로 교체했다. 기존 256px 승인 프레임 시험은 유지했다. 제품 이미지·렌더러·보상 로직을 이 시험 수정으로 변경하지 않았다.
- Android Studio의 로컬 실행 대상은 기본 Activity로 정리했다. 개인 `.idea` 설정은 커밋하지 않는다.

## 자동 검사 — 정확한 소스별 결과

Windows build26200/AMD64, Python3.10.4, 기록된 Android Studio JBR25, Gradle wrapper9.3.1/AGP9.1.1에서 각 Git 소스 아카이브로 `python scripts/check_all.py --android`를 실행했다. 관련 로컬 미추적 문서/폰트는 빌드 입력에 포함하지 않았다.

|소스|정적/Python|Android JUnit|전체 명령|
|---|---|---|---|
|`a342825` 원본|PASS / 66 PASS|391개 중 1 FAIL|exit1|
|`51cca9c` 중간|PASS / 66 PASS|같은 시험 1 FAIL|exit1|
|`434abae4` 최종|PASS / 66 PASS|391 PASS, 실패/오류/skip0|exit0, debug 빌드 PASS|

원본 실패는 C의 빵모자 썸네일을 옛 face layer 위에 합성하면 3표정이 같아지는 시험이었다. 현재 `AvatarView`는 썸네일 합성을 사용하지 않고 승인 완성 프레임을 표시한다. 기대값을 30으로 낮추지 않고 실제 HD36종을 검증하도록 정리했다. 최초 작업 트리 검사에서는 미추적 참고 문서의 CRLF로 중단됐으나 해당 문서를 임의 수정하지 않고 정확한 Git 입력으로 격리했다. 이전 중간 버전 PASS를 최종 결과로 재사용하지 않는다.

## 실제 debug Pleos 검수

Pleos Connect emulator2.1.0/API34 x86_64, Android Emulator37.2.13, RAM4GB, user10, 전체2560×1440/density160. 기존 AVD를 read-only/no-snapshot/headless SwiftShader/Vulkan disabled로 실행했다. 원본 저장·스냅샷 변경, clear/uninstall, DO/overlay/gear shell 주입 없음. 공식 콘솔 D/P는 사용자가 직접 조작했다.

|항목|이번 결과|
|---|---|
|정상 Main·아이콘·캐릭터→섬|PASS, 확인 표본 한정|
|실제 외부 내비 원주 선택→안내 시작→목적지 수신→native 아이콘 복귀|PASS 1회. 동일 Main task/PID, 캐릭터 재선택 없이 여정 대기|
|팝업 배경 탭 차단·아직 아니에요|PASS, 저장 해시 불변|
|도착 확인→결과|PASS. 연속 두 탭 표본 확인. 내부 원본/정확한 중복 보존은 단위 시험과 구분|
|실제 D 차단→P 복원|PASS. RESTRICTED 문구, enabled+clickable 앱 노드0, 이전 버튼 위치 탭에도 저장 해시 불변|
|UNKNOWN|비활성 전환 로그 확인. 지속적인 foreground UNKNOWN Pleos 검수는 NOT RUN|
|기본 확장↔분할|guest 1왕복 PASS. 확장1682×1210→분할841×1210→확장. 분할에서 스크롤 후 하단 버튼 도달 확인|
|저장 실패·원본 재시도·ack 실패·정확한 중복 보존|최종 단위 PASS / 최종 기기 실패 주입 NOT RUN|
|결과 닫기 완료·기존 adb 창 복원 재현|BLOCKED. 아래 host 종료로 완료 확인 불가|

2026-11-14 00:17:59 KST에 `qemu-system-x86_64-headless.exe`가 `c0000005`, module unknown으로 종료됐다. Windows Event1000 record35347/report `d41bd62e-c9e7-46f1-bf91-e3edc71c3215`. 결과 닫기 인근 발생이라는 시간 관계만 확인했으며 기존 STOPPED/NO_SURFACE나 앱·SDK·GPU 원인으로 단정하지 않는다. 이후 홈/ADB reorder 시험은 장치 소실로 진행하지 못했다. Windows visible 환경 인수나 실차 검수로 확대하지 않는다.

## Signed release 별도 검증

같은 빌드 소스의 깨끗한 worktree에서 사용자가 개인 터미널에 기존 팀 키 비밀번호를 직접 입력했다. 첫 서명 시도는 비밀번호 오류로 실패했고, 재입력 후 서명 도구 exit0/후보 폴더 생성 및 별도 `verify_release.py` exit0을 확인했다. 실행 정책 변경·새 키 생성·키/비밀번호 수집은 하지 않았다. 독립 검증은 기존 인증서·v2/v3 서명·non-debuggable·정식 Main 단일 launcher·Catalog/DEX·debug 전용 코드 제외를 확인했다. 소스 SHA는 빌드 담당자 선언값이며 APK에서 역산한 값은 아니다.

일반 Google APIs Play Store Android37.0 x86_64 AVD(RAM2GB, user0,1080×2340/density440)를 read-only/no-snapshot/headless로 사용했다. `adb install -r`로 signed v3→v5 업데이트 Success, `am start --user 0 -W -n io.kailand/.app.MainActivity` Status ok/COLD. appId10235·최초설치시각·CE/DE 데이터 디렉터리 inode가 같고, 설치 base.apk 해시도 전달본과 일치했다. 원시 게임 저장 바이트 전수 검증은 아니다.

실제 `이용 상태 미확인 / 실제 플랫폼 주차·이용 제한 입력 미연결` 화면과 조작 버튼0을 확인했다. 최초 System UI 응답 지연 창1회는 Wait 선택 후 복구됐고 이를 기록했다. 이 일반 Android 설치/UNKNOWN PASS를 signed release의 Pleos 내비·P/D/P·게임·창 검수 PASS로 확대하지 않는다.

## PM README 입력과 남은 인수

- 설계 기준1280×800 가로, 실제 Pleos 화면2560×1440/density160, 앱 확장 bounds `[853,90][2535,1300]`, 분할 `[853,90][1694,1300]`을 구분한다.
- 설치는 실제 serial/current user를 확인해 `adb -s <SERIAL> install -r <APK>`와 `am start --user <CURRENT_USER> -n io.kailand/.app.MainActivity`. 동일 팀 release 키 업데이트와 debug 키 설치본을 구분하며 데이터 삭제를 기본 설치 절차로 안내하지 않는다.
- 주차 P/이용 허용 및 Pleos 기본 내비·NaviHelper 목적지 조회 연결이 필요하다. UNKNOWN/제한 차단이 정상이며 우회하지 않는다. 앱 로그인/백엔드/새 유료 API 키 설정 없음.
- 여정 기록 수치는 기존 `USE_MOCK_TRIP_RECORDS=true`인 명시적 시연 표본이다. 원본 null 측정값을 보존하며 실제 주행 측정/방문 인증으로 설명하지 않는다.
- 결과 닫기/adb 창 복원 환경 오류 재검수, 기기 저장 실패·원본 재시도, signed release Pleos 및 B/C 독립 검수가 남는다. 실차·전체 ARM·전체 글자/창 조합·장시간 안정성은 NOT RUN.
- PM이 검수 결과를 취합해 병합을 결정한다. Create a merge commit과 파트 브랜치 유지 원칙을 따르며 이 작업에서 병합/Ready 전환은 하지 않는다. 빌드 소스 SHA와 PM이 정할 최종 제출 커밋 SHA를 구분한다.
