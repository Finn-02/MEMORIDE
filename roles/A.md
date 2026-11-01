# 파트 1 플랫폼과 차량 여정

목표는 다른 세 사람이 같은 앱에서 개발하도록 기반을 빨리 공유하고, 실제 SDK와 게임 입력의 경계를 관리하는 것이다. A는 앱 조립 담당이며 각 기능 내부 버그를 대신 고치는 사람이 아니다.

## 읽을 것

AGENTS, docs/02·03·04·08·15·16, contracts의 DTO·UI 연결표, tasks/backlog의 A01~06. 필요한 SDK 페이지를 선택해 읽고 전체 채팅을 프롬프트에 넣지 않는다.

## 작업 순서와 산출물

1. A01: 도구 설치가 끝난 뒤 공통 Android 프로젝트·wrapper·테마·라우터와 계약을 먼저 빌드한다. 실제 버전은 config/toolchain.json과 ENVIRONMENT에 기록한다. Manifest·빌드 파일·AppRoot 소유자는 A다.
2. A02: fake 목적지 요청·READY·출발·도착·취소를 만들고 C가 쓸 JourneyPort·DemoControlPort를 제공한다. Accepted와 최종 저장 성공을 구분한다.
3. A03: Windows Pleos에서 NaviHelper 요청·조회·오류·실제 gate를 시험한다. SDK 탐색은 D1~2로 제한하고 B/C/PM 개발을 기다리게 하지 않는다.
4. A04: B의 completeArrival와 PM 브리핑을 연결한다. 도착 retry는 같은 명령, replay는 DEMO 마지막 명령이다. reset 전후 generation과 처리 순서를 확인한다. 모든 사용자 게임 콜백에서 최신 gate를 확인한다.
5. A05: D8까지 서명 release를 생성해 C PC에 설치한다. 키는 로컬 별도 보관하고 저장소·AI에 넣지 않는다.
6. A06: 통합 경계·제한·재시작·모드 전환을 시험하고 최종 빌드 SHA와 README의 환경을 맞춘다.

## 다른 파트와의 연결

입력은 목적지 선택, C의 DEMO 명령, SDK 콜백이다. 출력은 JourneySnapshot·gate·검증된 ArrivalCommand다. 게임 보상 판단은 B, 브리핑 레이아웃은 PM, 방문 타이머는 C가 소유한다. DTO를 바꿀 때 소비자 한 명과 의미를 확인하고 내부 SDK 타입은 밖으로 내보내지 않는다.

## 최소 검증과 완료

UI 파트가 generic fake에서 실행되는 공통 앱을 공유한다. U09~11 U14~16과 I01 I02 I05를 우선 확인하고, 실제 SDK와 가짜 시험의 기록을 구분한다. Windows 기준 release·C 백업 설치·서명 유지·초기화 3회 시연을 남긴다. SDK가 실패해도 성공 배지를 만들지 않는다.

## 막힐 때

SDK 오류는 버전·서비스 연결·권한·장비를 나눠 확인한다. 장비 2시간 이상 막힘은 백업 PC를 확인하고, 핵심 일정이 1일 이상 변할 때만 PM에게 판단을 요청한다. 화면 내부 레이아웃 변경은 작성자에게 바로 돌려준다.


작업 ID의 A는 기존 명세의 식별자다. 파트 선택·경계·충돌 예시는 docs/PART_GUIDE.md와 docs/18_final_review.md를 참고한다.

1.2 추가 확인: U20·I06과 reset 잠금 순서. 새로운 관리 절차가 아니라 해당 기능의 기존 시험에 함께 포함한다.

## 시작 안내와 자율 설계

[환경 설치](../docs/ONBOARDING.md)와 [AI 설정](../docs/LOCAL_AI_SETUP.md)을 마친 뒤 [이 역할의 첫 작업·시작 프롬프트](../docs/FIRST_TASKS.md#part-1)로 진행한다. 세부 작업 분해·내부 코드·UI 설계는 담당자가 자기 AI와 자율적으로 결정한다. 기존 계약 안의 내부 계획과 일반 결함마다 PM 승인을 요청하지 않는다. A01 공통 앱을 받은 뒤 같은 빌드 파일을 사용하며 별도 Android 앱을 만들지 않는다.
