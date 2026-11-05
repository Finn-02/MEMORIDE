# A05 중간 release 서명 준비와 별도 PC 전달

이 작업은 최종 UI 인수가 아니라 서명·빌드·설치 경로 준비다. 기존 A 파트 브랜치와 PR #17은
유지하고, `b1ce371` main 기준의 임시 `codex/a05-release-preparation` 브랜치에서 분리했다.
USER_CONFIRMED enum이나 저장 활성화와 독립적이다.

## 현재 완료 범위와 남은 입력

서명 설정과 APK 검증 도구, C PC의 설치·업데이트 확인 절차를 제공한다.
작업 시작 때 로컬 release 인증정보가 없었고, 이후 사용자가 기존 팀 키가 없음을 확인했다.
첫 키 생성·서명 결과는 공개 상태 보고서로 따로 확인한다. AI에 키 파일/비밀번호를 전달하지 않는다.
**실제 팀 서명 release 생성과 C PC 설치는 아직 완료하지 않았다.**

기존 키가 있다면 같은 키로 업데이트를 유지해야 한다. 기존 키 여부를 확인한 다음
없다고 확정됐을 때만 담당자가 로컬 Android Studio 또는 같은 JDK의 keytool로 새 키를 만든다.
키·비밀번호·키 백업은 저장소, AI 채팅, APK 전달 ZIP에 넣지 않는다.
키의 실제 경로와 비밀번호는 담당자 개인 터미널/IDE에서만 입력한다.

## A의 로컬 빌드

2026-11-05 사용자 확인: 기존 팀 release 키가 없다고 확인됐다.
첫 팀 키는 저장소 밖 로컬 보관 위치에 PKCS12로 생성한다. 새 키를 기존 경로에 덮어쓰지 않는다.
PKCS12의 키 비밀번호는 keystore 비밀번호와 동일하게 사용한다.
Gradle의 teamRelease storeType도 PKCS12로 고정한다.
키 생성과 비밀번호 입력은 담당자의 별도 개인 터미널에서 진행하고 AI는 공개 인증서/결과만 확인한다.
첫 생성은 `scripts/initialize_release_key.py --confirmed-no-existing-key --build-tools <SDK Build Tools 경로>`로
개인 터미널에서 실행할 수 있다. 기본 보관은 Windows LocalAppData의 MEMORIDE/signing이다.
keytool의 숨김 입력으로 키를 만들고 공개 인증서를 내보낸 다음 서명 실행기로 이어진다.
기존 키/인증서가 있으면 덮어쓰지 않는다. 생성 후 빌드만 실패했다면 `--resume-existing`으로
같은 키를 이어서 사용한다. AI는 `tmp/a05/PRIVATE_STEP_STATUS.json`의 공개 상태만 확인한다.

기존 키와 공개 인증서 지문이 확인되면, 아래 보조 명령을 **담당자 개인 터미널에서 직접** 실행할 수 있다.
입력한 비밀번호는 숨기며 부모 셸의 환경 변수를 바꾸지 않는다. 빌드 입력이 커밋된 상태인지 확인하고,
서명 빌드 → 인증서/패키지 검증 → 해시가 일치하는 APK 복사 → 공개 RELEASE.json 생성을 수행한다.
키를 생성하거나 기기에 설치하지 않는다. AI 실행 콘솔처럼 대화형 터미널이 아니면 비밀번호 질문 전에 중단한다.

```powershell
python scripts/build_signed_release.py --version-code 2 `
    --build-tools "$env:ANDROID_HOME/build-tools/36.0.0" `
    --expected-cert-sha256 '<팀이 확인한 공개 인증서 SHA-256>'
```

keystore 경로·별칭을 입력하고 비밀번호 두 개를 숨김 입력한다. 성공 시 `tmp/a05-candidates/` 아래
후보 폴더에 `MEMORIDE.apk`와 `RELEASE.json`만 만든다. 이 두 파일과 공개 지문/빌드 메모를 C에게 전달한다.
실제 팀 키가 미확인이면 이 명령을 실행하거나 임의 키를 만들지 않는다. C 설치 결과는 별도로 받아야 한다.
아래는 같은 작업의 환경 변수·수동 명령 설명이다.

`app/build.gradle.kts`는 다음 **프로세스 환경 변수** 네 개가 모두 있을 때만 release를 서명한다.

| 변수 | 로컬 입력 |
|---|---|
| MEMORIDE_KEYSTORE_FILE | 저장소 밖에 보관한 기존 팀 keystore 경로 |
| MEMORIDE_KEYSTORE_PASSWORD | keystore 비밀번호 |
| MEMORIDE_KEY_ALIAS | 팀 키 별칭, androiddebugkey 사용 금지 |
| MEMORIDE_KEY_PASSWORD | 해당 키 비밀번호 |

일부만 설정하면 누락된 변수 이름을 표시하고 중단한다. 모두 없으면 기존 unsigned 빌드가 유지된다.
unsigned APK는 설치·전달용 서명 release가 아니다. debug 키로 자동 대체하지 않는다.

서명 담당자가 직접 실행할 PowerShell 예시다. 아래 프롬프트를 AI 실행에 넘기지 않는다.
JAVA_HOME/SDK는 기존 [환경 설정](ONBOARDING.md)을 따른다.

```powershell
$env:MEMORIDE_KEYSTORE_FILE = Read-Host '기존 팀 keystore 경로'
$env:MEMORIDE_KEY_ALIAS = Read-Host '팀 키 별칭'
$storeSecret = Read-Host 'keystore 비밀번호' -AsSecureString
$keySecret = Read-Host '키 비밀번호' -AsSecureString
$env:MEMORIDE_KEYSTORE_PASSWORD = [Net.NetworkCredential]::new('', $storeSecret).Password
$env:MEMORIDE_KEY_PASSWORD = [Net.NetworkCredential]::new('', $keySecret).Password
try {
    .\gradlew.bat --no-daemon :app:assembleRelease -PmemorideVersionCode=2
    if ($LASTEXITCODE -ne 0) { throw '서명 빌드 실패' }
} finally {
    Remove-Item Env:MEMORIDE_KEYSTORE_FILE, Env:MEMORIDE_KEYSTORE_PASSWORD,
        Env:MEMORIDE_KEY_ALIAS, Env:MEMORIDE_KEY_PASSWORD -ErrorAction SilentlyContinue
    $storeSecret = $null
    $keySecret = $null
}
```

macOS도 같은 네 변수를 현재 셸에만 export하고, 비밀번호는 `read -s`로 직접 입력한다.
`./gradlew --no-daemon :app:assembleRelease -PmemorideVersionCode=2` 후 네 변수를 unset한다.
셸 기록에 비밀번호 리터럴을 쓰거나 환경 변수 전체·signing report를 채팅에 붙이지 않는다.

`memorideVersionCode`는 1~2100000000 범위 정수만 허용한다. 기본값은 기존 1이며,
첫 중간 후보는 2, 다음 업데이트 후보는 더 큰 값으로 담당자가 관리한다.
applicationId는 `io.kailand`, versionName은 기존 `0.1.0`을 유지한다.
빌드 당시 Git SHA, versionCode, OS/JDK/wrapper와 실제 명령 결과를 기록한다.
서명 파일명과 실제 결과는 `app/build/outputs/apk/release/`에서 확인한다.

## 서명 APK 검증

담당자가 기존 팀 인증서의 **공개 SHA-256 지문**을 확인한다. 비밀번호·개인키와는 다르며
C에게 전달할 지문이다. 검증할 APK에서 읽은 지문을 아무 확인 없이 기대 지문으로 재사용하지 않는다.

```powershell
python scripts/verify_release.py app/build/outputs/apk/release/app-release.apk `
    --build-tools "$env:ANDROID_HOME/build-tools/36.0.0" `
    --expected-cert-sha256 '<팀이 확인한 공개 인증서 SHA-256>' `
    --source-sha (git rev-parse HEAD) `
    --report tmp/A05_candidate_2.json
```

macOS는 `python3`와 셸 변수 표기만 맞춰 같은 인자를 사용한다. `tmp` 폴더는 먼저 만든다.
기록된 JDK major를 사용하며, SDK Build Tools 경로에는 apksigner.jar와 aapt2가 있어야 한다.
검증 도구는 keystore를 열지 않고 비밀 서명 환경 변수를 검증 자식 프로세스에 전달하지 않는다.

확인 항목은 apksigner 성공, 기대한 단일 팀 인증서 및 v2/v3 서명, debug 인증서 거부,
applicationId·min/target SDK·versionCode, non-debuggable·정식 Main launcher,
debug 개발 Activity/갤러리의 DEX 제외와 Catalog 포함이다.
서명 없는 APK·debug APK·다른 인증서면 exit 2이며 PASS 보고서를 만들지 않는다.
PASS JSON에는 APK SHA256·공개 인증서 SHA256·버전·ABI와 담당자가 선언한 source SHA만 담는다.
**source SHA는 APK에서 암호학적으로 증명한 값이 아니다.** 빌드 명령/로그와 함께 기록한다.
같은 보고서 파일을 덮어쓰지 않으므로 새 후보에는 새 이름을 사용한다.

이 도구는 기기 설치, 화면 동작, 저장 복원, SDK 또는 차량 안전 시험을 수행하지 않는다.
예상대로 거부된 unsigned/debug 시험이나 합성 인증서 단위 시험을 실제 팀 서명 성공으로 표시하지 않는다.

## C PC 인수 — 서명 APK가 준비된 뒤

A는 검증된 APK 한 개, PASS JSON, 간단한 빌드/설치 메모를 전달한다. 키·비밀번호는 전달하지 않는다.
C는 다른 PC의 실제 OS/CPU, JDK/SDK, 대상 AVD/기기·Android API·ABI와 날짜를 기록한다.

1. 받은 APK 파일 SHA256을 A의 JSON과 대조하고, A가 확인한 공개 인증서 지문으로 같은 도구를 실행한다.
2. `adb devices -l`로 대상 serial을 명시한다. 기존 `io.kailand`가 있다면 서명과 보존할 저장 여부를 먼저 확인한다.
3. 새 기기/AVD 또는 이미 준비한 격리 시험 환경에 `adb -s <serial> install <apk>`로 최초 설치한다.
   기존 debug 설치와 서명이 다르면 삭제로 우회하지 않는다. 데이터 삭제 없이 시험 환경을 별도로 준비한다.
4. `adb -s <serial> shell am start -W -n io.kailand/io.kailand.app.MainActivity`로 정식 진입점을 실행한다.
   일반 Android에서 UNKNOWN으로 게임 조작이 막히는 것은 기존 정책이다.
   Pleos에서는 실제 P/이용 허용 조건을 따르며 모의 주차·ADB 제한 우회를 사용하지 않는다.
5. 기본 자산·화면 표시와 사용 가능한 범위의 동작을 확인한다. 개발 Activity가 보이지 않는지도 확인한다.
6. 실제 허용 화면의 공개 UI로 상태를 준비하고, 같은 팀 키·더 큰 versionCode의 다음 후보를
   `adb -s <serial> install -r <next-apk>`로 업데이트한다. 재실행 뒤 소유·외형·성장·pending 등의
   보존을 UI로 대조한다. 서명 불일치·다운그레이드 오류에서 자동 삭제/강제 우회하지 않는다.
7. 최초 설치, 업데이트/저장 보존, 일반 Android/Pleos, SDK 실차 결과를 각각 PASS/FAIL/NOT RUN으로 회신한다.

C의 실제 회신 전에는 X03·S01·S10·별도 PC 설치를 완료로 표시하지 않는다.
최종 UI가 반영되면 그 최종 SHA의 동일 서명 후보로 A06 인수를 다시 진행한다.

## 근거

[Android 빌드 변형·서명 설정](https://developer.android.com/build/build-variants),
[앱 서명과 키 관리](https://developer.android.com/studio/publish/app-signing),
[apksigner 서명 검증](https://developer.android.com/tools/apksigner).
[JDK keytool](https://docs.oracle.com/en/java/javase/25/docs/specs/man/keytool.html).
새 유료 서비스·API 키·원격 빌드·Actions 자동 실행은 추가하지 않는다.
