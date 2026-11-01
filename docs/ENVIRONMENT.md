# 실제 개발 환경 기록

현재 확인된 팀 구성은 PM macOS, A·B·C Windows다. CPU·설치 버전·실제 구동 결과는 아직 없다. A가 첫 빌드 뒤 config/toolchain.json과 아래 값을 함께 채운다. null·미확인은 정상 빌드의 증거가 아니다. 현재 applicationId는 내부 임시안이며 D1 이후 브랜딩 변경과 별개로 유지한다.

| 항목 | 팀 공통 실제 값 | 확인 방법 |
|---|---|---|
| Android Studio | 미확인 | About |
| Gradle 실행 JDK | 미확인 | wrapper --version 및 IDE Gradle 설정 |
| AGP와 Gradle wrapper | 미확인 | 공통 빌드 파일·wrapper properties |
| Kotlin과 Compose | 미확인 | 공통 버전 정의 |
| minSdk | 계획 28 | app 빌드 파일 |
| compileSdk와 targetSdk | 미확인 | app 빌드 파일 |
| NaviHelper | 계획 3.0.5 | 실제 해결된 의존성 |
| Pleos Emulator | 계획 2.1.0 API 34 | AVD와 앱 설치 |
| applicationId | 계획 io.kailand | A01에서 확정 |
| JVM criteria 사용 여부 | 미확인 | 생성 프로젝트와 wrapper 출력 |
| 주차·제한 입력의 실제 근거 | 미확인 | SDK 또는 플랫폼 API와 상태 매핑 |

## 공통 버전과 개인 설정의 관계

첫 공용 빌드 전달은 [A01 기준](BOOTSTRAP_HANDOFF.md), 팀원 설치는 [온보딩](ONBOARDING.md)을 따른다. 빌드 파일·wrapper·버전 카탈로그가 실제 dependency 버전을 결정하며 toolchain.json은 그 기록이다. 개인 경로·캐시·AVD·debug 키는 공유하지 않는다. 새로운 의존성은 A와 호환성을 확인하고 같은 정의에 고정한다. Python은 3.10 이상 표준 라이브러리만 필요하며 pip·venv·Node는 공통 필수가 아니다.

## PC별 확인

| 담당 | OS와 CPU | 정적 검사 | debug 빌드 | Pleos 설치·SDK | release 설치 |
|---|---|---|---|---|---|
| A 기준 PC | Windows CPU 미확인 | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| B | Windows CPU 미확인 | NOT RUN | NOT RUN | 필요 시 | 필요 시 |
| C 백업 PC | Windows CPU 미확인 | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| PM | macOS CPU 미확인 | NOT RUN | NOT RUN | 가능하면 검증 | 필요 시 |

## 로컬 명령

아래 wrapper·`--android` 명령은 **A01 공통 프로젝트를 받은 뒤** 사용한다. 현재는 doctor와 `check_all.py`의 기본 정적 검사만 실행할 수 있다.

macOS:
```bash
python3 scripts/doctor.py
./gradlew --version
python3 scripts/check_all.py --android
```

Windows PowerShell:
```powershell
py -3 scripts/doctor.py
.\gradlew.bat --version
py -3 scripts/check_all.py --android
```

doctor는 진단만 하며 설치·설정 변경·요금제 변경을 하지 않는다. Java major를 간단히 확인하지만 IDE의 실제 실행 JVM과 모든 AGP 호환성을 자동 검증하지는 않는다. wrapper의 Daemon JVM 출력이 다르면 기록부터 맞춘다. 개별 결과는 templates/environment_report.md를 사용한다.
