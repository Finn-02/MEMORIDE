# A06 Pleos 이미지 revision 판정 정정

2026-11-06 KST / Part A / Windows AMD64.
앱·SDK 이미지·AVD·저장 변경 없이 C의 설치 보류 회신과 A 설치 메타데이터를 재확인했다.

## 잘못된 기준과 확인 사실

A가 source.properties의 `Pkg.Revision=1`만 읽고 이미지 전체를 revision1이라고 단정한 것은 잘못이다.
C에게 전달한 revision1 고정 요청은 철회한다. C가 설치를 멈춘 것은 기존 안내에 따른 것이며 C의 오류가 아니다.

| 근거 | 실제 확인값 |
|---|---|
| A 설치 package.xml | package path `system-images;android-34;pleos_car_emulator;x86_64`, revision major2 |
| A 설치 source.properties | Pkg.Revision1, API34, ABI x86_64 |
| 위 두 파일 생성/수정 시각 | 모두2026-11-04 17:05:23 KST |
| 현재 공식 배포 feed | 같은 package path, revision major2 |
| C 회신 | SDK Manager revision2, 미설치, 기기 시험 NOT RUN |

로컬 두 메타데이터의 revision은 불일치한다. 임의로 파일 값을 바꿔 맞추지 않는다.
A의 설치 package 메타데이터는 이미2이므로, 별도 revision1 이미지가 있어야 같은 시험을 할 수 있다는 전제는 성립하지 않는다.
현재 feed에서 별도 revision1 항목/공식 아카이브는 확인하지 못했다. 추측한 과거 URL을 전달하지 않는다.

## 공식 배포 정보

2026-11-06에 [공식 설치 안내](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)가 지정한 Windows/Linux feed를 직접 읽었다.

- Feed: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
- 제공 revision:2.
- Archive: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp_car_emulator.zip`
- Feed에 명시된 크기:4,087,751,964 bytes.
- Feed checksum: `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa`.
- checksum 요소에는 알고리즘 속성이 없다. 위40자리 값을 SHA256으로 표시하지 않는다.
- Feed XML 자체의 로컬 SHA256: `637b987b9d675bbeed9bd368e684a136867d2b62e34fcccf121a664294186adb`.

이는 feed 메타데이터 확인이다. 이번 작업에서4GB 아카이브를 다운로드하거나 본체 해시를 독립 검증하지 않았다.
C는 SDK Manager의 공식 feed 설치를 사용하며 약관 확인은 PC 사용자에게 맡긴다.

## 프로필

A 설치 이미지 안 `devices.xml`에 다음 프로필이 포함되어 있다.

- UI name: `Pleos Connect v2.1.0`, manufacturer42dot.
- Internal ID: `Pleos_Connect_v2_0`, tag android-automotive.
- devices.xml SHA256: `fbf2bccb13168ec903cab7464277eff2c7d20eb37a351d97b2a548286f285944`.
- 로컬 `avdmanager list device`에서도 동일 ID/name 확인, exit0.

공식 이미지 설치를 완료하고 Android Studio를 다시 열어 Automotive 목록 또는 avdmanager 목록을 확인한다.
UI 버전2.1.0과 내부 ID의v2_0는 다른 표기이며 프로필이 잘못됐다는 뜻이 아니다.
미완료 설치의 .installer만 있는 상태에서는 프로필 누락을 확정하지 않는다.
수동 위조/다른 일반 프로필 대체/프로필 SDK 파일 재배포 없이 먼저 공식 설치를 완료한다.

## 다음 비교와 한계

C는 공식 revision2를 설치하고 별도 AVD/기존 동일 APK로 진행한다.
전달한 읽기 전용 verify_image.py와 A_IMAGE_BASELINE.json으로 설치 package revision/API/ABI/프로필과
6개 파일(source.properties, devices.xml, build.prop, kernel-ranchu, ramdisk.img, advancedFeatures.ini) SHA256을 비교한다.
해시가 다르면 파일을 수정해 맞추지 말고 차이를 먼저 회신한다. 일치하면 기존 TEST_GUIDE의 D/P 시험을 진행한다.
이 표본 일치는 system.img/vendor.img 전체의 byte 동일성을 증명하지 않는다. 전체 이미지 동일성은 NOT VERIFIED로 남긴다.

기존 QEMU c0000005 두 건과 정상 D/P 사례는 그대로 유지한다. 이번 정정은 종료 원인 확정이나 해결이 아니다.
이번에는 앱/이미지 설치/AVD 재생성/추가 기어 시험을 하지 않았고 Python/JUnit/Android build를 재실행하지 않았다.
