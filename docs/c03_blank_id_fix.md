# C03 공백 아이템 ID 검증 보완

2026-11-05, C 브랜치 `part/c-c01-companion`, 기준 HEAD `184727a`.

월드맵의 빈 아이템 ID 검사가 빈 문자열만 확인하여 공백·탭·개행만 있는 ID를 카탈로그 오류로 보고하지 않았다. 해당 항목이 지역에서 참조되지 않으면 목적지 후보 전달도 허용됐다. 기존의 빈 ID 차단 조건을 `isBlank()`로 통일했다. 정상 Catalog의 표시·후보 전달, 공통 DTO, 콘텐츠 ID, 저장 및 보상 규칙은 변경하지 않는다.

회귀 시험은 빈 문자열·공백·탭·개행 ID마다 오류 표시, 후보 전달 차단, 보유/전체 개수 제외를 확인한다. 수정 전 WorldPresentationTest 11개 중 새 시험 1개 실패(exit 1), 수정 후 공통 검사 Python 44개·Android 196개 실패/오류/skip 0, debug 빌드 PASS(exit 0, 11s, 7 executed / 38 up-to-date). `git diff --check` PASS. Windows AMD64, JBR 25.0.4, 공통 wrapper 9.3.1 사용. DataStore protobuf의 기존 Unsafe 경고는 남았다.

실제 UI·Pleos·실차·release·다른 PC는 이번에 NOT RUN이다. 기존 환경/인수 문서 수정은 보존하고 이번 코드 수정과 구분했다. commit/push/PR/병합은 하지 않았다. 다음은 A04 전달본 기기 UI 인수이며, 이 수정으로 C05 전체 완료를 주장하지 않는다.
