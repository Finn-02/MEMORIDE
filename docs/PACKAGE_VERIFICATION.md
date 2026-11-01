# MEMORIDE 1 2 배포 검증

발행일은 2026년 11월 1일이다. 제공 자료의 검증 기록이며 앱 구현 완료 기록은 아니다.

## 확인한 범위

| 항목 | 결과 |
|---|---|
| 명칭과 범위 | MEMORIDE·Ride Becomes a Memory 반영. 카일랜드 섬 세계관과 기존 포함·제외 범위 유지. 상세 추억 서비스 추가 없음 |
| 고정 기준서 | references/frozen의 1.1 DOCX와 기존 1.1 MD는 원본과 바이트 일치 |
| 기술 식별자 | io.kailand·계약 초안·contentVersion 1.1.0·저장 schemaVersion 1·콘텐츠/fixture 원본 유지 |
| 역할과 WBS | 19개 작업, A20·B20·C20·PM10. 파트 1=A·2=B·3=C·PM=P, 실제 배정은 config/team.json의 null을 채움 |
| 문서 화면 | 명세 57쪽·컨셉 8쪽·파트 안내 6쪽을 현재 렌더에서 전부 확인. 명세 18장·표 35개·그림 9개. Word 글꼴과 버전에 따라 쪽 수는 달라질 수 있음 |
| 연결 검토 | 섬의 여정 준비, 지역 후보 선택, 비동기 브리핑 닫기, 오래된 acknowledge, reset 잠금·취소 대기 순서 보완 |
| 정적 도구 | Linux에서 check_all.py 수행. 카탈로그·5개 UI 샘플·파일 경로·MD 링크·import 휴리스틱과 도구 단위 시험 34개 통과 |
| 데이터 수량 | 지역 3·캐릭터 3·아이템 9·등록 자산 15·기본 도착 시나리오 6. 제공 PNG/SVG는 자체 도형 초안 |
| 미실행 차단 | 미검증 toolchain과 없는 app/wrapper는 Android 검사에서 exit 2. 미실행을 성공으로 기록하지 않음 |
| Actions | 수동 workflow_dispatch만, read 권한, Ubuntu 1 job, 15분 상한, push/PR 자동 실행 없음. 실제 비활성화·예산 설정은 PM 확인 필요 |
| 공식 근거 | 기존 1.1의 공개 안내 검토를 계승하고 이번 제출·개발환경·심사·공지 목록 및 현대차 공식 비전을 재확인. 시간 해석과 프로젝트 문구는 팀의 기획 관점 |
| GitHub | 원격 반영 범위·커밋·설정 상태는 REPOSITORY_STATUS.md로 구분. 원격 파일의 blob SHA와 로컬 바이트를 대조한 뒤 최종 배포 |
| ZIP | 최종 압축의 CRC와 파일별 SHA256, 별도 추출본의 check_all.py를 확인한 뒤 제공. 캐시·서명키·APK·강의 PDF 제외 |

1.1 컨셉 원본 SHA256:
`8216b893f81eb6cae8f6e835891fb00dfb3dfbe5f3c9ec5de01f814a0b89b288`

공식 빈 접수양식 SHA256:
`07be893d58b0e96ab7788f0d189d6829f0e59a2cee4bc18cb90ec37eeda2f5fd`

MANIFEST.sha256은 발행 시점의 파일을 확인하기 위한 목록이다. 개발 중 파일이 달라지는 것은 정상이며 매 PR마다 목록을 갱신할 필요는 없다.

## 아직 확인하지 않은 범위

Android 앱·Gradle wrapper·Kotlin 컴파일·APK, 실제 Pleos 부팅·NaviHelper 요청과 도착·주차/제한 입력, 팀의 macOS·Windows 빌드·설치, U/I/S/AC 및 X 시험은 NOT RUN이다. 이번에 추가한 U19·U20·I06도 구현 후 시험할 항목이다. Python 34개 시험은 이 Android 검증을 대신하지 않는다.

장비 성능·실제 개발 시간·사용자 수요·장기 리텐션·지역경제·갈등 감소·주행 안전·세계 최초성을 실증하지 않았다. 설계로 오류 가능성을 줄였지만 무결함·2주 완성·수상을 보장하는 자료는 아니다.

## 시작 직전 판단

파트 선택과 샘플 기반 준비를 시작할 수 있다. 파트 1의 A01 첫 공통 빌드와 D2 SDK 관찰이 실제 환경의 우선 확인이다. 다른 파트는 SDK 성공을 기다리지 않고 자기 순수 로직·화면을 개발한다. D3 한 지역 전체 흐름과 D8 첫 서명 release 설치를 앞당겨 가장 큰 불확실성을 줄인다.
