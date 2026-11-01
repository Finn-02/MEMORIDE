# A01 첫 공용 빌드 전달 기준

**담당 A, 소비자 B·C·PM.** 이 작업은 Android 기반을 한 번만 만드는 선행 작업이다. 다른 파트가 Android 프로젝트를 따로 생성하거나 실제 SDK 성공을 기다리게 하지 않는다. 기존 [저장소 적용 안내](../REPO_SETUP.md)와 [계약](../contracts/ui_contracts.md)을 지킨다. 내부 구조·라이브러리 선택의 세부 검토는 A가 자기 AI와 수행한다.

## 최소 전달물

| 항목 | 확인할 결과 |
|---|---|
| 프로젝트 위치 | 저장소 루트의 단일 `app` 모듈. README·명세·content·fixtures 보존 |
| 빌드 | 실제 생성한 `settings.gradle.kts`, 루트·app 빌드 파일, 필요한 properties·버전 카탈로그를 커밋 |
| wrapper | `gradlew`, `gradlew.bat`, properties·jar 네 파일, Mac 실행 비트. 시스템 Gradle에 의존하지 않음 |
| 버전 | AGP·Gradle·Kotlin·Compose/BOM·AndroidX·DataStore 등 실제 선택 버전을 공통 정의에 고정. `+`·latest·SNAPSHOT 사용하지 않음 |
| 의존성 정의 | 버전 카탈로그 등 한 방식으로 정리. 각 파트가 별도 버전 정의·중복 버전 카탈로그를 만들지 않음 |
| 계약 | 초안 DTO를 `core/contract`로 옮겨 컴파일. 초안 파일에 실제 소스 위치 표시. 이후 실행 타입의 기준은 컴파일되는 파일 |
| 화면 진입 | AppRoot·최소 테마·라우터와 A/B/C/PM 임시 화면. 각 담당자 파일을 나중에 연결하기 쉬운 작은 표면 |
| 개발 fake | 실제 Pleos 실행과 분리된 개발 전용 fake 진입점·확인 방법. UNKNOWN/실제 제한을 우회하지 않음 |
| 자산 | root content·fixtures 원본을 하나로 유지해 app assets로 포함하는 방법, PNG 매핑 방법 |
| 기록 | `config/toolchain.json`과 `docs/ENVIRONMENT.md`에 실제 버전·JDK 선택·OS·명령·결과·검증자·날짜 |

과도한 모듈·DI 프레임워크·새 빌드 시스템을 도입하지 않는다. 첫 공용 빌드가 먼저이며 SDK 통합·게임 완성·최종 UI까지 A01에 넣지 않는다. Gradle dependency locking은 상황에 따라 A가 검토할 수 있지만 첫 전달을 막는 별도 구축 작업으로 만들지 않는다. wrapper checksum은 공식 배포값을 확인해 쓸 수 있으며 임의값을 넣지 않는다.

## 완료와 검증 표시

1. 실제 Windows 기준 PC에서 wrapper `--version`, debug 단위 시험·debug APK 빌드를 실행한다.
2. 공통 도구가 기록된 값을 읽도록 실제 `config/toolchain.json`을 채운다. **기본 빌드가 성공한 뒤에만 `verified: true`**로 한다. 이 값은 Pleos SDK나 모든 PC 성공 인증이 아니다.
3. `scripts/check_all.py --android`를 실행해 도구와 실제 빌드가 연결되는지 확인한다. 실패·생략은 명시하고 코드 0으로 바꾸지 않는다.
4. 작은 PR로 main에 전달한다. PR에는 첫 화면 여는 방법·빌드 명령·컴파일 계약 경로·추가 설치 SDK·기록된 버전만 적는다.
5. B·C·PM은 main을 받아 자기 PC에서 debug 검사한다. 결과를 ENVIRONMENT에 따로 기록한다. 다른 OS 결과를 A의 성공으로 채우지 않는다.

모든 PC 성공을 기다리느라 A01의 첫 공용 소스 공유를 미루지 않는다. 반대로 기록의 `verified`만 true로 바꾸고 wrapper·실제 빌드를 생략해서는 안 된다. 최초 Windows 빌드 성공과 이후 Mac 빌드 성공을 구분한다.

## B·C·PM의 대기 없는 준비

[역할별 첫 작업](FIRST_TASKS.md)의 입력·공개 출력·완료 조건을 읽고 내부 구현을 작게 계획한다. B는 보상 예제와 상태 모델, C는 방문 상태 전이·화면 스케치, PM은 브리핑 문구·null 처리를 정리한다. fixtures와 계약을 사용할 수 있지만 독립적으로 Android 앱을 만들지 않는다. 실행 기반을 받는 즉시 자기 feature의 코드·시험을 공통 앱 안에서 진행한다.
