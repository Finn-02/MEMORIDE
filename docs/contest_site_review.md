# 공모전 사이트 확인과 개발 반영

확인일 2026-11-01. 공개 HTML 안내 17개 경로, 최종 포스터, 공식 DOCX 접수양식을 열어 검토했다. 사이트 원문 전체를 재배포하지 않고 개발·접수에 필요한 사실과 적용을 요약한다. 로그인 뒤 화면·비공개 공지·변경될 수 있는 계정 상태까지 확인했다고 주장하지 않는다.

## 확인한 경로

| 경로 | 확인 범위와 이번 적용 |
|---|---|
| https://appdevchallenge.auto-hmg.io/ | 전체 목표·접수·일정·최종 포스터 |
| https://appdevchallenge.auto-hmg.io/guide | 8개 가이드의 전체 탐색 |
| https://appdevchallenge.auto-hmg.io/guide/overview | 차량 맥락·탑승자·자유 주제와 타깃 |
| https://appdevchallenge.auto-hmg.io/guide/schedule | 마감일·PT·선정·전시 일정 |
| https://appdevchallenge.auto-hmg.io/guide/shortform | 선택 참여·세로 영상·별도 상, 본상 가산점과 구분 |
| https://appdevchallenge.auto-hmg.io/guide/submission | 양식·영상·소스·APK·private·README·재제출 |
| https://appdevchallenge.auto-hmg.io/guide/development | Pleos 환경과 SDK/API 사용계획 |
| https://appdevchallenge.auto-hmg.io/guide/judging-awards | 네 평가항목·비공개 배점·산출물·PT |
| https://appdevchallenge.auto-hmg.io/guide/rules | 참가·수정·전시 조건 |
| https://appdevchallenge.auto-hmg.io/guide/help | FAQ와 문의 경로 |
| https://appdevchallenge.auto-hmg.io/faq | 팀 전원 동의·CC·실행 앱·private 조건 교차 확인 |
| https://appdevchallenge.auto-hmg.io/notice | 현재 3개 공지 목록 |
| https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000006 | 출품 사실성·권리·실제 PT 시연·전시 의무 |
| https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000001 | 모집·제출·일정 재확인 |
| https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000005 | 설명회 10월 28일 14시, 다시보기 예정 안내 |
| https://appdevchallenge.auto-hmg.io/terms | 사실성·소스 및 자산 사용 권한·팀 동의·운영 범위 |
| https://appdevchallenge.auto-hmg.io/privacy | 접수 개인정보와 동의·보관 안내, 앱 게임 데이터와 구분 |

## 개발 계획에 반영한 것

핵심 평가는 차량특화성·창의성·기술 활용성·사업성이다. 사용자가 자기 캐릭터와 이동을 연결하고, 도착 후 짧게 성장과 지역 선물을 확인하는 장면으로 설명한다. SDK의 실제 요청·응답과 가상 시연을 나눠 보여준다. 지역 소비·갈등 감소·리텐션은 기대 가설이며 성과로 꾸미지 않는다. 공식 배점·우승 확률은 추정하지 않는다. [S01]

접수양식이 필수 기획서이고 개발 산출물은 선택 가산점 대상이다. 우리 팀은 프로토타입과 소스·release APK를 목표로 하지만 이 목표를 모든 참가자의 1차 필수 요구로 설명하지 않는다. 2차 PT에는 실제 동작하는 앱이 필요하므로 1차 제출 이후의 안정화도 고려한다. 최종 선정 뒤에는 새 기능 경쟁보다 현장 구동과 전시 준비를 우선한다. [S01, S02, S32]

상금·IP·전시 의무는 PM이 팀과 운영 안내를 확인할 항목이며, 별도의 앱 기능으로 추가하지 않는다. 무료 오픈소스·자산도 사용 조건과 출처를 기록한다. 동물의 숲의 캐릭터·화면·음악을 복사하지 않고 자체 도형 초안과 독립 UI를 사용한다. 서명한 동의서·전화번호는 코드 저장소와 AI 프롬프트에 넣지 않는다.

## 확인 결과와 제출 직전 확인

- 마감 시각은 공식 접수양식에서 11월 15일 23시 59분으로 확인했다. 제출 직전 변경 공지를 다시 확인한다.
- 심사용 GitHub 계정: 읽은 공개 문서에는 정확한 ID를 찾지 못함. 공지·운영 안내에서 확정 후 초대.
- 설명회 다시보기 실제 링크: 읽은 공지에는 제공 예정으로 안내. 영상 내용을 확인한 것으로 쓰지 않음.
- 공식 DOCX 접수양식: 11월 1일 공식 URL에서 직접 내려받아 본문·표를 확인했다. 운영용 01장과 심사용 02장의 개인정보 분리, 개발계획 5번, 7번 참고자료·8번 숏폼, 전원 서명 요구를 확인했다. 내용이 바뀔 수 있으므로 PM은 제출할 때 최신 공식 파일을 사용한다. [S40]
- 실제 SDK·권한·주차 입력·팀 PC 실행: D1~2 시험으로 확정. 문서 존재와 앱 성공은 별개.

위 항목을 임의의 값으로 채우지 않는다. 컨셉 기준서는 수정하지 않고, 제출 설명과 실제 구현 범위가 다르면 명세·README·영상의 해당 부분을 함께 맞춘다.

공식 빈 접수양식의 다운로드 당시 원본을 references/contest-application-consent_20261101.docx로 함께 제공한다. 원본을 고치지 않았고 서명·개인정보는 포함하지 않는다.


## MEMORIDE 1 2 재확인

2026-11-01 명칭 확정 시 제출·개발환경·심사·공지 목록을 재확인했다. 1차 시연·GitHub·APK는 선택 제출이며 제출 수준에 따른 가산점이 있고, 2차 PT 전 실제 동작 앱이 요구된다. 이번 팀은 소스와 release 제출을 목표로 한다. 네 평가항목의 공개되지 않은 가중치를 임의로 만들지 않는다. 현대자동차 공식 비전은 sources S42와 BRAND에 구분해 기록했다. 프로젝트의 시간 해석·Ride Becomes a Memory는 팀의 메시지다.
