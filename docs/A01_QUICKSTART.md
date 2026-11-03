# A01 공용 앱 시작하기

이 브랜치는 기존 저장소 루트에 단일 `app` 모듈을 추가한다. 섬·방문·보상·Pleos 실제 연동은 후속 작업이며 지금 보이는 각 파트 화면은 임시 연결 자리다.

## 준비와 빌드

1. 이 저장소 루트를 Android Studio에서 연다. Gradle 프로젝트 불러오기/Sync를 실행한다.
2. SDK Manager에서 **Android SDK Platform 37.0**, Build-Tools **36.0.0**, Platform-Tools를 설치한다. `minSdk=28`이므로 API 28 이상 기기를 대상으로 한다. SDK 37은 컴파일 도구이며 시연용 Pleos API 34 이미지와 별개다.
3. Gradle 실행 JDK는 **25**로 맞춘다. 이 Windows 빌드에서는 설치한 Android Studio의 내장 JBR 25.0.4를 사용했다. IDE의 Gradle JDK와 터미널 `JAVA_HOME`을 각 PC의 실제 JDK 위치로 설정한다. SDK/JDK 개인 경로를 Git에 올리지 않는다.
4. SDK 경로는 Android Studio가 만든 각자의 `local.properties`에만 둔다. 다른 팀원의 파일을 복사하지 않는다.
5. 저장소 루트 Terminal에서 아래를 실행한다. Windows에서 `py -3`가 안 되고 `python --version`이 3.10 이상이면 `python`으로 대체한다.

Windows:

```powershell
.\gradlew.bat --version
python scripts/check_all.py --android
```

macOS:

```bash
./gradlew --version
python3 scripts/check_all.py --android
```

최초 실행은 Gradle/Maven 다운로드가 필요하다. 시스템 Gradle은 설치하지 않는다. 버전 원본은 `gradle/libs.versions.toml`, wrapper properties, `app/build.gradle.kts`다. AGP 9.1.1의 내장 Kotlin 2.2.10을 쓰므로 별도 `kotlin-android` 플러그인을 추가하지 않는다. Compose/serialization 플러그인도 같은 Kotlin 버전이다. Gradle 출력의 embedded Kotlin 2.2.21은 빌드 스크립트용이며 앱 Kotlin과 다른 값이다.

## 화면 열기

- **MEMORIDE**: 정식 진입점 `io.kailand.app.MainActivity`. 실제 플랫폼 입력이 아직 없어 UNKNOWN/이용 제한을 표시한다. A03 전에는 게임 허용으로 바꾸지 않는다.
- **MEMORIDE 개발 DEMO**: debug APK에만 있는 `io.kailand.app.DevelopmentActivity`. 일반 Android 에뮬레이터에서 임시 화면과 슬롯 연결을 확인하는 명시적 개발 샘플이다. 저장·보상·도착을 실행하지 않는다. Android Studio의 실행 구성을 만들 때 Launch를 Specified Activity로 선택하면 이 Activity를 지정할 수 있다.
- 개발 DEMO는 Automotive feature 또는 car UI mode인 기기에서 차단된다. 샘플 허용은 실제 주차를 확인했다는 뜻이 아니며 실제 플랫폼 gate의 대체 수단이 아니다. 화면 이탈 시에도 허용을 닫는다. release에는 개발 Activity가 포함되지 않는다.

실제 release 시연 패널은 C04/A04에서 별도로 연결한다. 지금의 개발 샘플을 release DEMO 완성으로 기록하지 않는다.

## 팀원에게 연결할 표면

| 위치 | 역할 |
|---|---|
| `app/src/main/java/io/kailand/core/contract/KailandContracts.kt` | 기존 초안을 옮긴 실행 계약. 공개 필드·의미 변경 없음 |
| `app/src/main/java/io/kailand/core/ui/AvatarSlot.kt` | 기존 UI 문서의 캐릭터 슬롯 타입 |
| `app/src/main/java/io/kailand/app/AppRoot.kt` | A가 B/C/PM의 공개 화면·콜백을 연결할 자리. 현재 임시 화면만 있음 |
| `app/src/main/java/io/kailand/app/CatalogAssets.kt` | 원본 Catalog/fixture 읽기, Catalog assetKey로 PNG 열기 |
| `app/src/main/java/io/kailand/core/platform/DevelopmentGate.kt` | 일반 Android 개발 샘플의 제한. 실제 Pleos 어댑터는 A03 |

B는 `feature/island`, C는 `feature/companion`·`visit`·`world`·`demo`, PM은 `feature/briefing`에 자기 코드를 추가한다. 공개 화면은 `contracts/ui_contracts.md`를 따른다. 서로의 feature 내부를 import하지 않는다. A가 AppRoot에서 연결한다. A01의 임시 화면을 각 파트의 실제 기능 완료로 간주하지 않는다.

`content/catalog.json`, `fixtures/*.json`, `assets/starter/*.png`는 `syncMemorideAssets`가 build 폴더로 복사한다. 원본만 수정하고 생성된 파일은 편집·커밋하지 않는다. APK 경로도 각각 `content/catalog.json`, `fixtures/ui_samples.json`, `assets/starter/char_tori.png`처럼 원본 상대 경로를 유지한다. 이미지의 실제 경로는 Catalog의 `assets[assetKey].path`를 사용한다. 음식·의상·장식 ID로 파일명을 합성하지 않는다.

## 확인 범위

실제 명령 결과는 `docs/ENVIRONMENT.md`와 `docs/status/a.md`에 기록한다. debug 단위 시험·APK 빌드와 에뮬레이터 실행, 실제 Pleos·제한·release 설치는 별도다. 후자를 실행하기 전에는 NOT RUN이다. 첫 PR은 A01만 포함하며 A02~A06 완료를 기다리지 않는다.

버전 선택 근거: [AGP 9.1.1 공식 호환표](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [Gradle JVM 호환표](https://docs.gradle.org/current/userguide/compatibility.html), [AGP 내장 Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin).
