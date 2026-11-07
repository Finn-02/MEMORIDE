# A06 사용자 확인 UI — 큰 글자·가로·복귀 검증

2026-11-07 KST / Part A / Windows build26200 AMD64.
시작 HEAD `87d65a1111137d6ababc8aade6fdcefaeba32f1d`, branch `part/a-a01-bootstrap`.
기존 C 비교 바이너리는 보존하고 새 일반 Android AVD에서 UI 결함을 재현·수정했다.

## 발견과 수정

기존 ManualJourneyScreen의 AlertDialog는 네 선택지를 스크롤 없는 confirmButton Column에 넣었다.
글자 배율2.0/가로/밀도420에서 설명이 표시되지 않고 마지막 ‘계속 이동 중이에요’가 잘렸으며,
질문 내부를 스크롤해도 마지막 버튼을 드러낼 수 없었다. 이 조건은 S03 결함으로 기록한다.
화면을 회전해 벗어날 수 있다는 이유로 PASS 처리하지 않았다.

A 소유 질문창만 Dialog/Surface와 하나의 verticalScroll Column으로 바꿨다.
제목·실제 방문 미인증 안내·네 선택지를 함께 스크롤하여 짧은 창에서도 전체 내용에 접근한다.
각 선택지의 최소 높이56dp, 문구·질문 token·선택 callback·이용 gate/busy 검사는 유지한다.
바깥 닫기는 기존처럼 CONTINUE이며 도착 저장을 호출하지 않는다.
ManualJourneyRuntime/저장 조정자/B·C·PM/공통 DTO/보상 규칙/스키마는 변경하지 않았다.

## 기기와 APK 구분

- 새 AVD: MEMORIDE_A06_UI_API37, emulator-5584, Android user0.
- 설치된 Google Play API37.0 x86_64 이미지로 생성, Medium Phone1080x2400/기본420dpi.
- Emulator37.2.13/build16740729, swiftshader/Vulkan disabled, cold boot/no snapshot load-save.
- RAM2048 요청은 emulator가4096MB로 상향했다. 호스트 권장16384MiB 대비15943MiB 경고가 있었다.
- 일반 Android 개발용 공개 DevelopmentActivity→A04 실제 저장 화면을 사용했다.
  일반 정식 Main/실제 Pleos 주차 허용 시험이 아니다. Pleos 제한 우회/가짜 실제 주차를 추가하지 않았다.
- C 고정 APK: 소스685d107, SHA256 `472761def4e579b4b850b5144ea69fa29faebc876d511743a1a0867580f5fcae`.
  변경 전 별도 파일로 복사해 해시 일치를 확인했으며 기존 전달 ZIP도 그대로다.
- 수정 UI 후보 APK SHA256: `dd25335cdd3986018374258e53820de242a035e62427d2491a5c94d77c16e108`.
  시작 HEAD에 이 문서의 질문창 패치를 적용해 빌드했다. 결과 커밋 SHA는 전달 REPORT에 기록한다.
  새 후보만 새 일반 AVD에 update 설치했다. C의 PC/기존 Pleos AVD/이미지는 변경하지 않았다.

## 직접 관찰 결과

| 조건/절차 | 실제 관찰 |
|---|---|
| 기존 APK, 세로/글자1.0/420dpi | 여정 안내·버튼·네 질문 선택지 표시 PASS |
| 기존 APK, 세로/글자2.0/420dpi | 질문의 네 선택지 표시 PASS |
| 기존 APK, 가로/글자2.0/420dpi | 설명 누락/마지막 선택지 잘림, 스크롤 불가 FAIL |
| 수정 후보, 같은 가로/글자2.0/420dpi | 상단 설명 표시, 스크롤 후 네 선택지 모두 접근 PASS |
| 수정 후보의 계속 이동 | 질문 닫힘·ACTIVE 유지, 저장 파일 없음 PASS |
| 도착 확인→대전 브리핑 | USER_CONFIRMED 원본1개/아이템3개/시간·거리null/pending 저장 PASS |
| 미확인 브리핑 홈 이동→기존 task 복귀 | 브리핑 재표시, 저장 JSON/파일 해시 동일 PASS |
| 미확인 브리핑 가로→세로/글자2.0 | 표시 유지, 저장 JSON/파일 해시 동일 PASS |
| 세로 큰 글자 브리핑 하단 | 아이템 보기/닫기까지 스크롤 가능, 닫기 후 pending만null PASS |
| 수정 후보, 세로/글자2.0/540dpi(폭320dp) | 안내·버튼에 스크롤로 접근, 질문 설명/네 선택지 표시 PASS |
| 폭320dp 질문 취소 | 새 저장·보상 없음, 직전 확인 완료 파일 해시 동일 PASS |
| 수정 후보, 세로/글자1.0/420dpi 복원 | 설명/네 선택지 표시, 바깥 닫기 무저장 PASS |

질문 상태에서 홈으로 나갔다 기존 task로 복귀할 때 섬 화면으로 돌아왔고 저장 파일은 없었다.
여정 준비로 재진입해 기존 활성 대전 여정을 확인했다. 화면 복귀 위치 보존까지 PASS로 확대하지 않는다.
복귀는 공개 launcher intent로 기존 task를 앞으로 가져와 HOT/현재 DevelopmentGameActivity를 확인했다.
UI 입력은 computer-use를 사용했고 ADB는 APK 설치/공개 진입·설정 intent/읽기 수집에만 사용했다.
글자/표시 크기는 Android Settings UI에서 변경 후 font_scale/wm density로 실제 적용값을 확인했다.
표시 크기 첫 탭은 실제 밀도 변경이 없어 적용 시험으로 세지 않았고 슬라이더 drag 후540을 확인했다.

## 저장·회귀 근거

실제 UI가 만든 DataStore를 run-as 바이너리 읽기로 수집했다. 저장 파일 주입/직접 변경은 하지 않았다.
미확인 상태 배경/복귀/회전4표본의 SHA256은 모두 `7132c3e5bc24c407c36e11c864fc8f4b6842b5573ddde4d7e4abedfbc5e73dbd`.
ack 후에는 pending만null인 저장과 정확히 일치했고, 취소/설정 변경/바깥 닫기 후 SHA256은 모두
`3eb81c0baf0427de0c3adc8a671200d8fb6225e50946e17caa9f602d1fb74038`로 동일했다.
최종 원본1개/대전 아이템3개/null 측정값을 보존했다.

`gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug`: exit0,45초,
전체JUnit264개(failure/error/skip0, XML 합산) 및 debug BUILD SUCCESSFUL. Kotlin/시험/task 실제 실행.
기록된 JBR25.0.4/wrapper9.3.1을 사용했다. 기존 SDK XML/Unsafe 경고는 남아 있다.
최종 정적/Python 결과는 REPORT에 기록한다. 새 레이아웃을 그대로 복제하는 단위시험은 추가하지 않았다.

## 환경 복구·한계

첫 새 AVD는 RAM 안내가 있는 부팅 구간에서 ADB offline이었고 창 캡처가 monitor crop 오류로 실패했다.
앱 설치 전에 SDK 정상 종료 후 -scale0.3으로 재실행해 RAM 안내를 확인하고 부팅 완료 후 설치했다.
첫 cold start5.421초였으며 초기화면5초 목표 PASS로 세지 않는다.
toolbar Home UIA 입력은 창 경계 밖 오류로 실행되지 않아 재관찰 후 Android 홈 제스처를 사용했다.
이런 도구/부팅 단계는 앱 결함이나 앱 crash로 집계하지 않는다.

종료 시 새 AVD는 세로/글자1.0/밀도420/섬 화면이며 계속 켜 두었다. 실제 저장은 이 새 AVD에만 있다.
이번 AndroidRuntime 조회에는 앱 FATAL이 없었지만 장시간 안정성/기존 QEMU 문제 해결 근거는 아니다.
TalkBack/전체 키보드 접근성, 실제 저장·ack 실패 UI, 모든 화면/화면 크기 조합, 실제 전원 차단,
수정 후보의 Pleos/실차/ARM/서명 release 및 최종 A06 인수는 NOT RUN이다.
기존 B 독립 검토9091507의 저장 로직은 변경하지 않았으며 새 UI 후보의 독립 인수로 확대하지 않는다.
다음은 C 고정 후보의 비교 결과 검토와 최종 UI 후보를 포함한 인수 범위 정리다.
