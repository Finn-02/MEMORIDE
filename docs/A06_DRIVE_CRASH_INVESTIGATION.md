# A06 D 전환/QEMU 종료 원인 분리

2026-11-06 KST / Part A / Windows build26200 AMD64.
앱 소스는 `685d107`, 조사 시작 HEAD는 `31b52ac`, 기존 `part/a-a01-bootstrap`이다.
앱/저장/차량 gate 코드를 바꾸지 않고 기존 격리 AVD에서 실행 조건을 비교했다.
**이 문서는 종료 문제 해결 선언이 아니다.** 기존 두 번의 QEMU 비정상 종료는 유효한 결함/환경 장애 기록으로 유지한다.

## 확정한 사실과 정정

- Android Emulator37.2.13/build16740729, 공식 Pleos2.1/API34 x86_64 이미지.
  후속 재확인: package.xml의 설치 revision은2, source.properties의 Pkg.Revision은1로 서로 다르다.
  기존 revision1 단정과 C에게 요청한 revision1 고정 조건은 정정한다.
  [메타데이터·공식 배포·프로필 재확인](A06_PLEOS_IMAGE_REVISION_CORRECTION.md) 참조.
- GPU는 Intel Iris Xe, 드라이버31.0.101.4314, 호스트 RAM 약16GB다.
  [현재 Pleos 공식 최소 GPU](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator)는
  RTX3060 Laptop6GB 또는 명시된 동급 AMD 외장 GPU다. 이 PC는 해당 목록에 못 미친다.
  이것만으로 기존 crash 원인을 GPU라고 확정하지 않는다.
- 과거 `-memory 2048`은 요청값이다. emulator 로그의 `Increasing RAM size to 4096MB`와
  hardware-qemu.ini에서 **실제4096MB**를 확인했다. 과거 문서의 RAM2048 실행 표현은 실제값으로 읽으면 잘못이다.
- 초기 GPU software 실행과 swiftshader 실행은 두 덤프에서 서로 다른 Vulkan DLL(lavapipe/SwiftShader)을 로드했지만,
  둘 다 구형 `gles_swiftshader/libGLESv2.dll` 경로를 사용한다.
- 두 host minidump 모두 c0000005 읽기 접근 위반이며 exception address는 모듈 이미지 범위 밖이다.
  fault thread 스택의 같은 위치(+0x48)에서 libGLESv2+0x6673a0 주소 후보가 나왔다.
  **단순 주소 스캔이며 심볼을 이용한 stack unwind가 아니다.** 그래픽 경로 조사 단서일 뿐 원인 모듈 확정이 아니다.
  원시 덤프/메모리 내용은 전달하지 않는다.
- 기존 로그에는 opengl32sw 로드 실패/시스템 OpenGL fallback 및 ColorBuffer 오류가 있다.
  경고의 존재만으로 c0000005와의 인과관계를 확정하지 않는다.
- 당시 commit 사용량은 약23~24GB/40.9GB, 가용 물리 RAM은 약0.8~1.2GB까지 낮아졌다.
  자원 압박은 있으나 commit limit 초과/OOM을 입증한 결과는 아니다.

## 한 조건씩 비교

모두 같은 `MEMORIDE_A06_UC_PLEOS`/emulator5582/user10, cold boot/no snapshot load-save,
같은 앱 APK/저장, 요청 RAM2048/실제4096을 사용한다. Chrome 콘솔 기어는 사용자가 직접 조작했다.

| 조건 | 실제 확인 | 해석 |
|---|---|---|
| 기존 swiftshader, MEMORIDE force-stop | stopped=true/프로세스 없음 확인 후 D60초 이상 유지·P 복귀, QEMU 생존 | 이 1회는 미재현. 앱 무관 환경 원인 확정도 아님 |
| 같은 QEMU 세션, MEMORIDE 사용자 확인 모드 실행 | D 약190초, RESTRICTED/플랫폼 주행 차단 화면, P 복귀/PARKED_ALLOWED, QEMU 생존 | 같은 설정에서도 정상 사례 존재. D/앱 실행만으로 항상 종료되지는 않음 |
| D 상태 창 활성화/컴퓨터 도구 캡처 | 플랫폼 ‘주행 중에는 사용할 수 없습니다’ 화면 직접 관찰 | 이번에는 도구 캡처 단계도 미재현 |
| `-gpu swangle` 요청 | 실제 로그/설정/DLL은 swiftshader GLES4.0.0.1 | 다른 렌더러 비교로 집계하지 않음. 사용자 수동 종료는 crash로 집계하지 않음 |
| `-gpu swiftshader -feature -Vulkan` | 로그 Vulkan disabled, vk_swiftshader DLL 미로드, 같은 GLES DLL. D 약141초→P 복귀, 차단 화면→섬 화면, QEMU 생존 | 이 1회 PASS. Vulkan 비활성화가 해결책이라는 인과관계는 미확정 |

Vulkan 비활성화 시험의 gate는 UTC 11:22:07에 RESTRICTED, 11:24:28에 PARKED_ALLOWED로 복귀했다.
연결 모니터는 총303초 동안 소실을 관찰하지 않았고 조사자가 정상 중지했다.
P 복귀 후 CarService DO=false/UxR0, 실제 섬 화면을 직접 확인했다.
기준 상태와 각 D/P 표본의 저장 JSON 및 원본 파일 SHA256은 모두 동일했다.
원본 영수증 1개/대전 아이템 3개/pendingReceiptId=null/시간·거리 null을 유지했다.
관측된 결과는 이번 비교 표본의 기능 통과이며 장시간 안정성 인수는 아니다.

새 cold boot의 guest 로그에는 Pleos vendor diagnostic_report/vehicle_ext 서비스 SIGABRT가 있었다.
시간은 D 입력 전 부팅 구간이며 QEMU는 이후 계속 동작했다. 이를 D의 원인이나 앱 crash로 집계하지 않는다.
guest 시각은 UTC-8이며 호스트 KST와 구분해 UTC로 대조했다.

## 재현 가능한 그래픽 분리 실행

설치된 SDK의 emulator.exe에서 다음 옵션을 사용한다. 다른 인스턴스가 같은 AVD를 사용 중이면 실행하지 않는다.

```text
emulator.exe -avd MEMORIDE_A06_UC_PLEOS -port 5582 -no-snapshot-load -no-snapshot-save -gpu swiftshader -feature -Vulkan -memory 2048
```

이는 [Android 공식 그래픽 진단 옵션](https://developer.android.com/studio/run/emulator-troubleshooting)의 Vulkan 비활성화다.
차량 제한/VHAL/주차 입력을 해제하는 옵션이 아니다. 앱은 계속 UNKNOWN/RESTRICTED를 차단한다.
이를 안정화된 기본값이나 최종 제출 환경으로 확정하지 않는다.

## 아직 해결하지 못한 것

기존 c0000005의 직접 발생 위치/재현 필요조건은 미확정이다. 정상 1회나 graphics 옵션 변경만으로 해결 처리하지 않는다.
장시간/반복 조건, 최소 GPU를 충족하는 기존 다른 PC 비교, native debugger의 심볼 기반 분석은 남아 있다.
최신 드라이버 설치/SDK 변경/새 유료 장비/보안 설정 변경/저장 초기화는 이번에 하지 않았다.
앱 단독 crash 또는 원본/보상 손상은 이번 비교에서 관찰하지 않았지만 모든 앱 영향을 배제한 결론은 아니다.

다음은 같은 APK/저장과 기어 절차로 최소 사양을 충족하는 기존 PC에서 반복 비교하고,
이 PC의 재발 덤프를 native debugger에서 심볼 기반으로 분석하는 것이다.
일반/Vulkan 비활성화 모두 정상 사례가 있으므로 추가 D/P 1회만으로 해결 선언하지 않는다.
현재 격리 AVD는 P/섬 화면으로 켜 두었으며 자동 기어 조작이나 지속 모니터는 남기지 않았다.
이번 변경은 문서와 조사 근거뿐이다. 기존 Python59/JUnit261/debug PASS는 동일 앱 소스의 이전 결과이며,
이번 기기 조사에서 Kotlin 시험/debug/release 빌드는 다시 실행하지 않았다.


## 2026-11-07 후속 — 최신 서명 후보와 종료 요청 오류

[최신 v4 인수](A06_V4_RELEASE_ACCEPTANCE.md)에서 A의 P/D/P·저장·업데이트와 C 일반 Android 교차 설치를 별도 확인했다. 이는 위 고정685d107 원인 비교의 대체가 아니다. C 앱 미실행 D 조건의 최초 c0000005 실패와 재시도1회 통과는 함께 유지하며, C Vulkan-off 비교는 대기 중이다.

별도로 A의 headless QEMU 종료 요청 뒤 combase.dll/c0000602를2회 관측했다. 한 관측은 앱 force-stop/PID없음 확인 후였다. 기존 D 접근 위반과 시점/코드가 다르며 같은 원인으로 단정하지 않는다. 종료 요청의 OK ACK만으로 완전 정상 종료라 판정하지 않는다. 첫 오류 AVD 재부팅 후 기존 외형·ack 완료 표시 복구는 확인했으나 안정성 해결 선언은 아니다. 정확한 시각·보고ID·범위·미실행은 최신 v4 인수 문서를 따른다. 종료 반복 재현은 중단했고 실제 제한/저장 정책 변경은 없다.
