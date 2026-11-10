# C 후속 인수 — 서명 v4 설치와 고정 debug Vulkan 비교

2026-11-08 KST / Part C / Windows 11 Home 10.0.26200 AMD64.
기록 기준 `part/c-c01-companion`, main 병합 SHA `49de99028e591b3e04a41343956878e5087a17f5`.
이 문서는 완료한 서로 다른 후보의 시험을 정리하며 새 앱 기능이나 최종 환경 승인을 추가하지 않는다.

## 서명 v4 교차 설치 — 완료·수용

A 선언 빌드 소스 `72b689da04c3ea16ea851c3607d6c9c578aa0781`,
versionName `0.1.0` / versionCode `4` / applicationId `io.kailand`.

- APK SHA256: `4574eaa15af0de1736bcaaf0e1b73b001054db3ed5ed5574fa821c79879af1fe`.
- 팀 공개 인증서 SHA256: `8c842e7249d09f67f425bc90ed944bbdbc6393444cbb0845bcfb1bc17193cebb`.
- C 독립 `verify_release.py` exit0. 해시·서명·패키지/버전·정식 Main·non-debuggable·debug 코드 제외 확인 PASS.
- 새 일반 API37/x86_64/user0 AVD에서 앱 부재 확인→최초 설치 Success→정식 Main COLD/Status ok→UNKNOWN 화면 관찰 PASS.
- 일반 Android의 UNKNOWN 차단은 정상 정책이다. 허용된 게임 진입·저장 보존·업데이트 설치·Pleos 인수로 확대하지 않는다.
- 이 C 최초 설치 결과는 A/PM이 검토·수용했다고 회신했다. 원격 반영 범위는 [A의 v4 인수 기록](A06_V4_RELEASE_ACCEPTANCE.md)을 따른다.

회신 ZIP `MEMORIDE_A06_C_SIGNED_V4_REPLY_20261107_153138_KST.zip`,
SHA256 `2583f0d1e4bd3148d248a7290430d1640d44b74650fc645ebdae7fb1dcc5e704`.
소스 SHA는 빌드 담당자 선언이며 APK에서 암호학적으로 역산한 값이 아니다.

## 고정 debug Vulkan-off 비교 — 회신 검토 완료

이 시험은 v4 인수와 별개다. 기존 `MEMORIDE_A06_C_COMPARE`와
고정 `685d107b5a10cd43b4887638d12e0445b3aef383` debug APK를 유지했다.
전달본과 설치 공개 base.apk SHA256 모두
`472761def4e579b4b850b5144ea69fa29faebc876d511743a1a0867580f5fcae`로 일치했다.

실제 Emulator 37.2.13/build16740729, 공식 Pleos2.1/API34/x86_64,
`Pleos_Connect_v2_0`, user10, RAM4096, SwiftShader/OpenGL ES3.0,
cold boot/no snapshot load-save 조건이다. 기존 기본 조건에 `-feature -Vulkan`을 추가했고
실제 disabled 로그를 확인했다. SDK/드라이버 변경·v4 교체·저장 초기화는 하지 않았다.
이미지 package.xml revision2/source.properties revision1과 6개 표본 해시 일치를 재확인했다.
이는 전체 system/vendor 이미지 동일성 증명이 아니다.

아래 시간은 host KST 첫/마지막 **표본 간격**이다. 정확한 기어 입력 시각이나 연속 텔레메트리가 아니다.
기어는 사용자가 공식 콘솔에서 수동으로 변경했다.

| 정상 조건 완료 표본 | D 간격 | P 간격 | 관찰 범위 |
|---|---|---|---|
| B0 앱 미실행 대조 | 136.995초 | 48.936초 | 앱 PID 없음, QEMU/ADB 생존, UXR80→0 |
| 앱 실행1 | 131.995초 | 45.841초 | D 플랫폼 차단, P 섬 복귀, 동일 PID, 저장 불변 |
| 앱 실행2 재개 | 136.589초 | 45.542초 | 재부팅 후 D 차단/P 섬 복귀, 동일 PID, 저장 불변 |
| 앱 실행3 | 131.560초 | 51.081초 | 재개 세션에서 D 차단/P 섬 복귀, 동일 PID, 저장 불변 |

정식 공개 UI로 USER_CONFIRMED 대전 REGION 원본1개·기념품3종·성장0·측정값null·pendingnull을 준비했다.
반복 중 새 여정/보상/초기화 없이 각 P 및 명시적 재부팅 뒤 debug run-as 바이너리 읽기로
전체 JSON과 711bytes 저장 파일 SHA256
`9dae290cda62c86ef19bcaf1d7a9d275feceafa352a312e5b138fcfdbd213bd3`의 불변을 확인했다.
기준 준비 중 사용자 Esc 정지 후 재개했으며 중복 저장하지 않았다.

### 정식 비교 집계에서 제외한 참고 이력

사용자가 잘못 조작했다고 확인한 중간 회차는 정상 비교 집계에서 제외한다.
그 회차의 QEMU APPCRASH `c0000005`(2026-11-08 00:26:34 KST, module unknown)는 별도 참고 근거로 보존했다.
동일 reportId의 Windows1000/1001 두 이벤트는 고유오류1건이다. 사용자는 수동 종료하지 않았다고 확인했다.
입력/자동 시험을 중단한 뒤 사용자가 명시적으로 새2회차 재개를 요청했다.
동일 Vulkan-off 조건에서 한 차례 재부팅하고 동일 저장 기준을 확인한 후 위 앱2·앱3을 완료했다.
첫 세션 앱16329/QEMU27068, 재개 세션 앱6488/QEMU31612이므로 연속3회 무오류라고 표시하지 않는다.
재개 후 입력 모드는 기본 PLEOS로 돌아왔고 저장/섬은 보존됐다. 원인 확정이나 안정성 해결을 주장하지 않는다.

종료 시 P·섬 화면·앱/QEMU 생존과 저장 불변을 확인하고 전용 로거만 종료했다.
AVD 정상 종료 요청 시험은 하지 않았으므로 `combase.dll/c0000602` 종료 비교는 NOT RUN이다.
기본 조건 앱 실행3회·실차·장시간 안정성·최신 v4 최종 시연 환경 인수도 이 결과에 포함하지 않는다.

회신 ZIP `MEMORIDE_C_VULKAN_OFF_REPLY_20261108_004006_KST.zip`,
SHA256 `785ba35cfd13ecf5e23cf2a217b5566b4a52c6b51769c883093b728af3bdb34d`.
환경·결과 양식·시각별 표본·저장 의미/해시·선별 로그·대표 화면과 별도 참고 이력을 포함하고 ZIP 무결성을 확인했다.
파트 1은 정상 조건에서 완료된 표본의 주행 제한·주차 복귀·저장 보존 근거를 수용했다.
최종 시연 환경은 A PC로 확정했고, A PC에서 서명 Release v4 최종 리허설을 완료했다.
QEMU 종료 원인은 미해결이다.

## 남은 범위

- 후속 별도 개발 AVD에서 수행한 C05 표시·화면 이탈 검사와 아직 남은 접근성/경쟁 조건은
  [추가 UI 검사](C05_UI_ACCESSIBILITY_FOLLOWUP.md)에 기록했다. 서명 후보/고정 Vulkan 비교와 분리한다.
- C05 전수 UI·접근성·활성 방문/reset 경쟁·옛 token 기기 재전달은 기존 NOT RUN 범위를 유지한다.
  A의 독립 핵심 시연3회·방문 분기 근거는 [A 기록](A06_V4_RELEASE_ACCEPTANCE.md)을 참조하되 C의 전수 재실행으로 세지 않는다.
- 추가 기기 시험은 대상 후보/환경/합성 데이터 범위를 분리한다. 이 문서 정리로 더 이상의 재시도나 초기화를 시작하지 않는다.

현재 main 동기화 때 실행한 C 검사(Python59/JUnit264/debug PASS)는 `49de990`의 별도 소프트웨어 근거다.
이번 기기 결과 기록에서 빌드·서명·배포·원격 push·자동 모니터링은 수행하지 않았다.
