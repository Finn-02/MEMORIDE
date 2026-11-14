# C 월드맵 출처

2026-11-10 / Natural Earth 1:10m Admin 0 Countries 5.1.1의 ADM0_A3=KOR 레코드를 정적 Android 벡터로 변환했다.

- 소개: https://www.naturalearthdata.com/downloads/10m-cultural-vectors/10m-admin-0-countries/
- 원본: https://naturalearth.s3.amazonaws.com/10m_cultural/ne_10m_admin_0_countries.zip
- 원본 ZIP SHA256: `ce1ac7036499a0edd641fbc093cd209a98f96a49d2eca8480aaacad35138a7f6`
- 이용 조건: https://www.naturalearthdata.com/about/terms-of-use/ (public domain, 수정/상업 이용 가능; 이번 작업에서 본문 확인)
- 재현: `py -3 scripts/build_world_map.py <원본ZIP경로>`
- 결과: `app/src/main/res/drawable/memoride_korea_map.xml`, 53개 링/3060개 좌표.

정적 도감용 지형이다. 원본 축척의 작은 섬·해안 일반화/누락 가능성이 있으며 측량·내비·행정 경계 판별·실제 방문 인증용이 아니다.
시연용 등록 지역의 위치는 기존 Catalog 위도·경도를 같은 투영으로 배치한다. 주행 경로/가상 도로를 그려 넣지 않는다.
PM의 최종 지도/콘텐츠 출처 검토는 별도 인계한다. 다른 원본으로 교체하면 출처·투영·아이콘 위치를 함께 갱신해야 한다.
인터넷·지도 SDK·API 키·유료 서비스 없이 작동한다. 원본 세계 지도 ZIP이나 캐시는 앱에 넣지 않는다.

## 2026-11-13 최종 후보 스타일 재현

검사 기준은 A 후보 `a342825ea02550f4cb90d346494b748cae605cbb`다. 생성기가 현재 벡터의 채움 `#FFF6E0`, 외곽선 `#EBCF96`, 두께 `3`을 재현하도록 정렬했다. 지형·투영·53개 링/3060개 좌표는 유지했다.

- 생성기 SHA-256: `c964ebfd91837d1b739cb210823af81e8c99cf546ddbf66571c4b434d9d3e556`
- 결과 벡터 SHA-256: `5be83ba3e703a820bae7c517139ee600613f5bc25e8eb9e8aa43bb2ff72e6ad5`
- UTF-8/LF 출력으로 독립 생성 2회와 A 후보 파일의 바이트 일치를 확인했다.
- 검사: `py -3 scripts/build_world_map.py <원본ZIP경로> --check`
- 별도 출력: `py -3 scripts/build_world_map.py <원본ZIP경로> --output <후보XML경로>`
- 잘못된 색상의 파일은 `--check`에서 실패하며 덮어쓰지 않는 것을 확인했다.

상세 명령·결과는 [MAP_REPRODUCTION.json](MAP_REPRODUCTION.json)에 기록한다. AI 코딩 보조를 사용했으며 지도 이미지 생성형 AI는 사용하지 않았다. 이 재현 검사는 Android 표시·터치·주행 안전 검증이 아니다.

위 재현 PASS와 독립 생성 2회는 C의 2026-11-13 실행 기록이다. 이번 PM 제출 정리에서도 고정 해시의 원본 ZIP과 최신 통합 HEAD의 XML에 대해 `--check`를 실행해 53개 링/3060개 좌표 및 위 결과 SHA-256의 바이트 일치를 확인했다. Android 기기 재검수 결과는 아니다.
