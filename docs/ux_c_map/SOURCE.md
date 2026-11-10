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
