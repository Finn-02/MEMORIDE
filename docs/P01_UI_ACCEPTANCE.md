# P01 독립 화면 인수 결과

- 확인자 / 시각: 로컬 Codex 직접 조작·관찰 / 2026-11-03T14:48:48+09:00
- 실제 OS: macOS 27.2 arm64
- 기기: emulator-5554, Medium Phone API 37.0, sdk_gphone64_arm64, Android 17 / API37. 일반 Android이며 Automotive feature 없음.
- 시작 소스: 93fb798044f0de276499b0cef43e1d9377ebfe72. 이번 변경은 debug 샘플의 안전 영역 여백과 모의 지연 500→2500ms뿐이다. 최종 커밋 SHA는 검토 ZIP REPORT.md에 기록한다.
- 최종 APK: app/build/outputs/apk/debug/app-debug.apk / SHA256 `9c0fe1d05baaaa97af8a100d1ef21c5b49e60cec5f0157d03003c2c1f718f88d`
- 실행: 기존 exported AndroidX PreviewActivity를 ADB로 실행. 실제 `topResumedActivity`와 UI dump/기기 캡처로 확인. 정적 Preview가 아닌 기기 런타임이다.
- 기본 조건: 물리1080×2400, 420dpi, font_scale=1.0. 임시 작은 창840×1440·font_scale=1.3 사용 후 해상도 override 제거와 font_scale=1.0 복원 확인.

| 사례 | 실행 샘플 | 실제 결과 | 근거 파일 |
|---|---|---|---|
| 지역 | RegionalBriefingPreview | PASS · DEMO·대전·20분·5km·기념품 3개 | regional.xml/png |
| 일상 | DailyBriefingPreview | PASS · 성장 0→1, 발견/기념품 생성 없음 | daily.xml/png |
| 측정 누락 | MissingMetricsBriefingPreview | PASS · 이동 시간/거리 행 생략, 0으로 대체하지 않음 | missing_metrics.xml/png |
| 빈 기록 | FreshBriefingPreview | PASS · 아직 도착 기록이 없어요. 영수증 없음 | fresh.xml/png |
| 재방문 | RevisitBriefingPreview | PASS · 추가 기념품 없음, 발견 기록 없음 | revisit.xml/png |
| 잘못된 ID | MissingIdsBriefingPreview | PASS · 일반 이름·누락 안내, 화면 유지 | missing_ids.xml/png; small_large_bottom.xml/png |
| 제한 | RestrictedBriefingPreview | PASS · 상세·버튼 없음, Back 후 같은 Activity·안내, 성공 없음 | restricted_back.xml/png |
| 미확인 | UnknownBriefingPreview | PASS · 상세·버튼 없음, Back 후 같은 Activity·안내, 성공 없음 | unknown_back.xml/png |
| 닫기 실패·재시도 | RetryBriefingPreview 새 세션 | PASS · 첫 실패 유지 → 다시 시도 → 샘플 성공·확인 완료, 두 버튼 disabled | dismiss_failure.xml/png; dismiss_success.xml/png |
| 아이템 실패·재시도 | RetryBriefingPreview 새 세션 | PASS · 첫 실패 유지 → 다시 시도 → 샘플 성공·확인 완료, 두 버튼 disabled | items_double_failure.xml/png; items_success.xml/png |
| 시스템 Back | RetryBriefingPreview 새 세션 | PASS · 첫 Back 실패 후 종료 안 됨 → 다음 Back 성공, 같은 독립 Activity 유지 | back_failure.xml/png; back_success.xml/png |
| 중복 입력 | RetryBriefingPreview 새 세션 | PASS · 빠른 더블클릭 및 아이템·닫기·Back 1,370ms 연속 입력, 처리 중 비활성 관찰, 첫 실패 하나 유지 | pending_disabled.png; duplicate_record_failure.xml/png; conditions.log |
| 긴 내용·작은 창 | MissingIdsBriefingPreview | PASS · 840×1440 / 420dpi (약320×549dp)에서 스크롤로 두 버튼 도달, 겹침 없음 | small_large_initial.xml/png; small_large_bottom.xml/png |
| 큰 글자 | MissingIdsBriefingPreview | PASS · 실제 font_scale=1.3에서 줄바꿈·스크롤·버튼 도달 확인 | small_large_initial.xml/png; small_large_bottom.xml/png; restored-settings.log |

UI dump의 텍스트 자식 노드는 enabled=true일 수 있다. 성공 뒤 버튼 비활성화는 부모 button View의 enabled=false로 확인했다. 처리 중 두 버튼의 회색 비활성 상태와 처리 안내는 직접 관찰하고 원본 기기 캡처로 보존했다. 콜백 횟수를 내부 계측했다고 주장하지 않으며, 첫 실패 뒤 성공 문구가 생기지 않은 관측과 기존 비동기 단위 시험을 구분한다.

표시·닫기/아이템/Back의 실패·재시도는 안전 영역을 수정한 APK에서 확인했고, 최종 모의 지연 변경 뒤 중복 처리와 작은 창/큰 글자를 확인했다. 모의 대기만 바뀌었으며 실제 처리 로직과 공개 화면 소스는 동일하다. 각 콜백 시험은 새 Preview Activity task로 초기화했고 사용자 저장 데이터를 초기화하지 않았다.

## 발견·수정 및 한계

- 개발 샘플 안내가 상태 표시줄과 겹쳤다. debug 호스트 Column에 fillMaxSize/safeDrawingPadding을 적용해 수정하고 기기에서 겹침 해소를 확인했다. 공개 BriefingScreen·Manifest·AppRoot·공통 gate·DTO는 변경하지 않았다.
- 도구의 세 입력이 약1.4초 걸려 500ms 모의 처리는 중복 검증에 짧았다. debug 콜백 대기만2.5초로 늘렸다. 실제 저장 지연이나 제품 정책을 바꾼 것이 아니다.
- install -r 직후 첫 Preview가 흰 화면인 경우가 있었다. focused PreviewActivity/AndroidRuntime 로그에서 반사 호출 오류·앱 예외는 확인하지 못했고, 동일 Preview Activity를 새 task로 다시 실행하면 정상 표시됐다. 원인은 확정하지 않았고, 이를 실제 앱 시작 검증으로 확대하지 않는다. ADB 재실행 방법을 전달한다.
- 수정 후 assembleDebug PASS: 최종18초,39 actionable tasks 중4 실행·35 UP-TO-DATE. 기존 Python34개/Android17개 성공은 이전 근거이며 이번에 재실행하지 않았다. 이번 수정은 샘플 레이아웃/모의 대기라 동작을 복제한 단위 시험을 추가하지 않았다.

## 실제 연결은 별도

| 항목 | 상태 |
|---|---|
| A04 acknowledgeReceipt와 성공 라우팅 | NOT RUN, 미연결 |
| 실제 저장 실패·복구 I06 | NOT RUN, 미연결 |
| Pleos·차량 gate·release | NOT RUN |

## 다음 한 작업

독립 P01 화면 인수는 위 기기·조건에서 완료했다. 작은 PR 전달 자료가 준비됐으며 다음은 작성자 지시하에 push/PR 전달이다. 이번에는 원격 작업을 수행하지 않았다. A04의 실제 저장·라우팅 연결은 별도 시험으로 유지한다.
