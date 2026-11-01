# MEMORIDE 개발 시작 안내

제품명은 MEMORIDE이며, 기존 카일랜드 섬 세계관과 기능 범위를 유지한다. 버전 1.2는 브랜드 확정과 파트 선택·연결·검증 준비를 정리한 개정이다. 이름을 정했다는 이유로 상세 추억 서비스나 새 기능을 추가하지 않는다.

## 오늘 진행할 순서

1. PM과 팀원이 docs/PART_GUIDE.md를 읽고 파트 1·2·3을 한 명씩 선택한다. config/team.json의 null을 실제 GitHub ID로 채운다. PM은 주준경이며 GitHub ID는 Finn-02다.
2. 모두 README와 AGENTS, 자기 roles/A·B·C·PM.md를 읽는다. A/B/C는 파트 1/2/3의 작업 ID 별칭이다.
3. private 저장소 `https://github.com/Finn-02/MEMORIDE.git`를 clone한다. 저장소에는 착수 자료만 있으며 아직 앱 프로젝트는 없다.
4. 공식 Android Studio·Git·Python 3.10 이상을 준비하고 docs/15_cross_platform.md로 로컬 설정을 맞춘다. 팀원은 Windows PowerShell, PM은 macOS Terminal을 쓴다.
5. 파트 1이 A01에서 공통 Android 프로젝트·wrapper·공통 계약을 먼저 공유한다. 앱을 네 개 만들지 않는다. 그동안 다른 파트는 제공 샘플·순수 함수·화면 초안으로 진행한다.
6. 파트 1은 첫 빌드 결과와 실제 버전을 config/toolchain.json·docs/ENVIRONMENT.md에 기록한다. 도구의 최신 버전을 각자 임의로 조합하지 않는다.
7. macOS `python3 scripts/check_all.py`, Windows `py -3 scripts/check_all.py`를 실행한다. A01 이후 `--android`를 추가한다.
8. 작은 part 브랜치에서 자기 첫 작업을 만들고 한 명의 교차 리뷰 후 main에 합친다. 파트 전체가 끝날 때까지 통합을 미루지 않는다.

## 시작 입력

파트 1은 contracts/KailandContracts.kt와 fixtures/scenarios.json을 사용한다. 파트 2는 ArrivalCommand를 직접 넣어 보상·저장을 시험한다. 파트 3과 PM은 fixtures/ui_samples.json으로 렌더러와 SDK를 기다리지 않고 화면을 만든다. 샘플을 실제 사용자 상태에 자동 주입하지 않는다.

## 반드시 지킬 경계

파트 1이 차량 입력·실제 이용 제한·앱 조립을 관리한다. 파트 2만 영구 게임 상태를 변경한다. 파트 3의 방문은 일시 상태이며 보상을 만들지 않는다. PM 브리핑은 저장된 결과만 표시한다. 공통 계약의 의미 변경은 파트 1과 실제 소비자 한 명이 확인한다. 기능 내부 설계·일반 오류는 각 담당자가 해결한다.

## 이름과 기술 ID

앱 표시명·문서·저장소는 MEMORIDE다. 가상 섬 세계관은 카일랜드로 부를 수 있다. `io.kailand`·`KailandContracts.kt`·contentVersion 1.1.0·저장 schemaVersion 1·기존 item/region ID는 유지한다. A01에서 내부 기본안을 확인하되 브랜딩만으로 모두 바꾸지 않는다.

## 확인할 상태

이 자료는 Android 앱·APK·실제 SDK 성공 결과물이 아니다. D1 공통 빌드, D2 SDK 관찰, D3 한 지역 연결, D8 첫 release 설치를 우선한다. 자세한 상태는 docs/PACKAGE_VERIFICATION.md를 읽는다.
