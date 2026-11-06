# A06 사용자 확인 저장 — Pleos 기기 인수

2026-11-06 KST / Part A / Windows AMD64.
앱 시험 소스 `685d107b5a10cd43b4887638d12e0445b3aef383`, 기존 `part/a-a01-bootstrap`.
PR #22의 새 사용자 확인 경로를 공식 Pleos에서 검사했다. 이번 작업은 기기 인수/문서이며 앱 코드 변경은 없다.

## 대상과 경계

- 설치된 공식 Pleos Connect 2.1.0 / API34 x86_64 이미지와 Pleos_Connect_v2_0 프로필로
  별도 AVD `MEMORIDE_A06_UC_PLEOS`, serial `emulator-5582`, Android 사용자10을 생성했다.
- RAM2048MB, cold boot, snapshot load/save 없음. 기존 서명 release/debug Pleos AVD와 키는 변경하지 않았다.
- 시험 APK는 debug다. SHA256 `472761def4e579b4b850b5144ea69fa29faebc876d511743a1a0867580f5fcae`.
  정식 공개 MainActivity를 사용했고 개발 Activity/fake gate는 사용하지 않았다.
- 초기 P 기어 표시와 CarService `DO=false / UxR=0`, 앱 `PARKED_ALLOWED`를 확인했다.
- 사용자 확인 출처는 USER_CONFIRMED다. 실제 내비 자동 도착/실차 방문 인증으로 표시하지 않는다.
- Chrome Virtual Console은 사용자가 직접 조작했다. AI는 에뮬레이터 UI와 읽기 전용 ADB 근거를 확인했다.
  포트 연결은 공식 안내의 `adb -s emulator-5582 forward tcp:8088 tcp:8088`만 사용했다.
  기어/제한 ADB 주입·제한 해제·DO 선언·overlay·저장 주입은 하지 않았다.

## 직접 검증

| 사례 | 실제 결과 |
|---|---|
| P에서 정식 Main → 사용자 확인 모드 → 여정 준비/질문 | PASS |
| 계속 이동/취소 응답 | 게임 저장 미생성 관찰. 도착 저장 성공으로 집계하지 않음 |
| 대전 REGION → 도착했어요 | PASS: 사용자 확인 브리핑, 성장0/대전 발견/기념품3개 |
| 앱 전용 DataStore 읽기 | PASS: USER_CONFIRMED, 시간/거리 null, 원본1개와 pending |
| 미확인 상태 앱 force-stop/재실행 | PASS: 동일 브리핑 복원, 저장 전체 값/파일 SHA256 불변 |
| 브리핑 닫기 | PASS: 섬 이동, pending null, 원본 영수증/아이템3개 유지 |
| 확인 완료 후 force-stop/재실행 | PASS: 브리핑 재개 없음, 섬 복원, 저장 전체 값/파일 SHA256 불변 |
| D 제한 신호/저장 | 재시험에서 DO=true/UxR=80(0x50), 앱 RESTRICTED, 저장 전체 값/파일 해시 불변 확인 |
| D 제한 화면/P 복귀 | BLOCKED: 화면 캡처 전 QEMU가 다시 종료. P/D/P 전체 PASS로 집계하지 않음 |

원본 여정 ID `1746d9c8-6fab-4543-b9e7-66c86605e9a6`.
저장 전 앱 파일 없음도 재부팅 뒤 확인했다. 저장 성공/확인 완료는 UI와 실제 파일을 함께 대조했다.
파일은 앱 debug 권한 `run-as io.kailand --user 10`으로 읽기만 했고 JSON을 주입하지 않았다.

## 오류와 복구

첫 질문 중 사용자가 D로 변경한 직후 emulator/QEMU 프로세스와 ADB가 사라졌다.
Windows Application 이벤트1001에서 `qemu-system-x86_64.exe` APPCRASH,
`StackHash_c8a4 / c0000005`를 확인했다. 사용자는 창을 닫지 않았다고 회신했다.
앱 단독 crash 또는 D 입력과의 인과관계로 단정하지 않는다. 이 첫 D 사례는 PASS가 아니다.

같은 AVD를 wipe 없이 다시 cold boot했다. 지원 옵션 `-gpu software`에서
명시적 `-gpu swiftshader`로 바꾸고 RAM2048을 유지했다. 단일 재시작 뒤 위 저장/복원 사례를 통과했다.
이것으로 에뮬레이터 crash의 영구 해결을 주장하지 않는다.

사용자 조작으로 D를 한 번 재시험한 결과 CarService의 DO=true/UxR=80과 앱 RESTRICTED를 확보했다.
시점의 원본 영수증/보유 아이템/pending 및 파일 SHA256은 직전 확인 완료 상태와 같았다.
이후 창 활성화 timeout과 QEMU/ADB 소실이 다시 발생했다. Windows 이벤트1001,
2026-11-06 18:00:16 KST에 동일 QEMU APPCRASH/c0000005를 확인했다.
따라서 신호/저장 관측은 유효하지만 D 제한 화면·P 복귀는 BLOCKED다.
일반 software와 명시적 SwiftShader에서 각각 종료가 관찰됐고 반복 재시도는 중단했다.
종료의 내부 원인/기어 입력과의 인과관계는 확정하지 않았다. 앱 코드/차량 제한을 완화해 해결하지 않는다.
검증 AVD는 종료 상태이며 데이터는 보존했다. 기존 서명 후보/Pleos AVD는 변경하지 않았다.

미확인 브리핑 cold start의 첫 `am start -W`는 Status timeout이었다.
프로세스 생존/파일 보존을 확인하고 공개 Main으로 재진입해 실제 브리핑을 관찰한 결과만 PASS로 판정했다.
다른 cold start에는 약6.0/8.8초가 기록됐다. 초기화면5초 목표/성능 인수 PASS로 세지 않는다.

읽기 근거 도구의 첫 실행은 파일 없음 메시지가 exec-out stdout/exit0으로 반환돼 JSON 파싱에 실패했다.
명령 성공과 파일 존재를 분리해 수정 후 기준 상태를 다시 수집했다. 앱 저장 결함으로 세지 않는다.

## NOT RUN / 남은 인수

서명 release의 이 새 SHA 설치/업데이트, 실차/ARM 기기, 실제 PLEOS 자동 최종도착,
실제 디스크 고장/전원 차단, UI 저장/ack 실패 주입·옛 token 기기 재주입, 접근성/회전 전수는 NOT RUN.
Python59/JUnit261/debug 빌드는 앞선 동일 앱 소스 검증을 재사용하며 이번 기기 작업에서 반복 실행하지 않았다.
문서의 정적/Python 검사와 기기 UI/CarService 인수는 별도 결과다.
B 교차 인수, C05 전체 S09/접근성 및 최종 SHA 서명 후보/A06 전체 인수는 남아 있다.
