# MEMORIDE 1차 프로토타입 제출 결정

2026-11-14 KST. PM은 미확인 항목을 남긴 상태로 1차 프로토타입 제출 정리를 진행하기로 결정했다. 전체 제품 인수 HOLD를 PASS로 바꾸는 결정이 아니다.

## 고정한 산출물

- 빌드 소스: `17bea5438af9ea7b4fb3f3a431a19ab33777adc5`
- 검토한 통합 PR #29 HEAD: `8b532a342711ca76be00027834d4c3b4c0320932`
- 두 소스 사이 변경은 전달 문서 2개이며 앱 코드 차이는 없다.
- 파일: `독수리4형제_MEMORIDE_release.apk`, 58,201,427 bytes, versionName 0.1.0 / versionCode 6.
- SHA-256: `7f6f2ae9ee4cf74145368add969c8f978b5c5e8195a4fa9980d47273600cb311`.
- SHA-256은 수령 ZIP에서 APK 바이트를 추출해 직접 재계산했다. Git 빌드 소스는 A의 빌드 기록이며 APK에서 SHA를 역추출한 것이 아니다.

## 수용한 범위

같은 v6의 서명·업데이트 설치·내비 UI의 속초시청 목적지 설정·아이콘 복귀·사용자 도착 확인·기념품 3종/성장 0→1 증거를 1차 제출의 근거로 삼는다. 일반 Android UNKNOWN 차단은 정상 제한으로 안내한다. 시연용 수치와 수동 도착 정책도 README에 명시한다.

결과 닫기/섬 복귀·아이템/저장/재방문·창과 글자 전환·실차 및 장시간 안정성은 이번 v6에서 완료하지 않았다. QEMU c0000005와 기존 창 복원 FAIL의 원인은 미확정이다. 후속 재시도는 디스크 부족으로 APK 설치 전 중단됐다. 이 항목들은 전체 인수 HOLD와 함께 남기고 2차 준비에서 보완한다.

## 출처 정리

C가 제공한 지도 생성 도구·출처·재현 기록과 이미지 81개 대응표 및 ATTRIBUTION 보완을 반영한다. 앱 실행 코드·리소스·콘텐츠·패키징 설정은 수정하지 않는다. 지도 생성 도구 수정은 현재 벡터 스타일에 맞추는 작업이며 앱 빌드 중 자동 실행되지 않는다. C의 과거 재현 PASS는 보존하고, PM도 이번 Linux 환경에서 고정 원본 ZIP과 현재 XML에 대한 생성기 `--check` 바이트 일치를 별도로 확인했다. 이미지 81개는 수령 APK의 SHA-256을 대응표와 대조하고 APK 바이트의 Git blob SHA를 통합 HEAD와 대조해 모두 일치했다. B 원본의 과거 AI/외부 재료 이력 및 섬 이미지 13개의 최초 출처 확인은 미완료로 유지한다.

## 배포 상태와 남은 제출 작업

이전 기록: APK는 수령·해시 확인 완료, GitHub Releases 게시는 대기였다. 당시 연결의 전송 한도 때문에 58,201,427-byte APK 직접 업로드가 완료되지 않았다.

**2026-11-14 갱신 — GitHub Release 게시 완료.** PM Mac에서 기존 v6 APK를 재빌드·재서명 없이 그대로 첨부했다.

| 항목 | 결과 |
| --- | --- |
| Release | [https://github.com/Finn-02/MEMORIDE/releases/tag/v0.1.0-v6](https://github.com/Finn-02/MEMORIDE/releases/tag/v0.1.0-v6) · 제목 `MEMORIDE signed release v6` · 정식 게시(draft·prerelease 아님) · 2026-11-14 14:56 KST |
| 태그 | `v0.1.0-v6` → `17bea5438af9ea7b4fb3f3a431a19ab33777adc5` (APK 빌드 소스) |
| 첨부 파일 | 표시 이름(label) `독수리4형제_MEMORIDE_release.apk`. GitHub가 첨부 파일명의 한글을 지원하지 않아 실제 파일명·다운로드 이름은 `4._MEMORIDE_release.apk`이고, API로 이름을 바꿔도 같은 결과였다. 다운로드: https://github.com/Finn-02/MEMORIDE/releases/download/v0.1.0-v6/4._MEMORIDE_release.apk |
| 게시 전 원본 확인 | 58,201,427 bytes · SHA-256 `7f6f2ae9ee4cf74145368add969c8f978b5c5e8195a4fa9980d47273600cb311` · `io.kailand` versionName 0.1.0 / versionCode 6 · apksigner v2 검증 PASS, 서명 인증서 SHA-256 `8c842e72…3cebb`(팀 인증서 기록과 일치) |
| 게시 후 다운로드 확인 | 다시 내려받은 파일 58,201,427 bytes · SHA-256 `7f6f2ae9…600cb311` — **원본과 일치**. GitHub 첨부 파일 digest도 같은 값 |

게시는 v6 APK 배포 완료를 뜻하며 전체 제품 인수 HOLD와 위 미확인 범위는 그대로다. 접수 메일에는 위 Release 주소와 최종 main 커밋을 기재한다. 저장소 공개 전환과 접수 메일 발송은 이번 게시 작업에 포함하지 않았다. 서명키나 자격증명은 전달하지 않았다.

공식 기준: https://appdevchallenge.auto-hmg.io/guide/submission

- README에 앱 소개·실행·환경/설정·Pleos 버전/화면 규격·APK 배포 경로를 제공한다.
- 소스와 실행 가능한 release APK를 함께 제출하며, 큰 APK는 GitHub Releases를 사용한다.
- 접수 메일 발송과 저장소 공개 전환은 이번 소스 정리 작업에서 실행하지 않는다.
- 비공개 제출 후에는 11월 16일까지 공개하고 11월 23일까지 유지한다. 공개할 수 없으면 공식 안내의 소스+APK 압축파일 대체 제출을 따른다.
- 동의서/서명/개인정보는 저장소에 커밋하지 않는다.

파트 브랜치는 유지하고 병합 방식은 Create a merge commit을 사용한다. Actions를 켜거나 자동 병합을 설정하지 않는다.
