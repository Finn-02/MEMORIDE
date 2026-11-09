# B06-7L Android 자산

B06-7K 검수에서 승인한 토리·모모·루루 몸체 PNG를 B06-7L의 `char_*.png`로 사용한다. `memoride_b067k_build_review.py`와 `B067K-review/b067k_*_source.blend`가 제작 원본이다. Blender Eevee에서 고정 정사영 카메라로 256×256 RGBA 몸체를 렌더했다. 눈 기준 Y=126, 입 기준 Y=155, 발 기준 Y=247의 기존 좌표를 유지한다. Compose는 하나의 투명 캔버스에 몸체, 공통 표정, 공통 의상 순서로 그린다.

`create_island_base_v3.py`에서 기본 섬을 제작하고, `build_island_growth_v3.py`가 `island_base_v3.blend`의 카메라와 조명으로 길·꽃밭·휴식 시설을 각각 렌더했다. 장식은 `build_island_decor_b067b_visual.py`의 B06-7B 렌더를 사용한다. 아트 보관본의 `island_decor_b067b_visual.blend`와 `*_b067b_visual.png`에 대응하며, 앱에 포함된 장식 9개는 이름을 `_v3.png`로 맞춘 동일 바이트다. 섬·성장·슬롯 레이어는 1024×1024 RGBA 캔버스를 공유한다. 단계 0~3은 기본 섬에 필요한 성장 레이어를 누적한다. 배치 장식은 기존 Catalog `assetKey`와 저장 `slotId`를 `${assetKey}_${slotId}_v3.png`로 매핑한다.

제작 스크립트와 Blender 원본은 별도 아트 작업 보관본에 있다. Android 빌드는 저장소의 PNG를 그대로 패키징한다. `assets/starter/<파일>`은 앱에서 `assets/starter/<파일>`로 읽고 APK ZIP에서는 `assets/assets/starter/<파일>`에 있다. Catalog의 15개 경로와 섬 렌더러의 13개 경로가 아래 기준을 구성한다.

| PNG | SHA-256 |
| --- | --- |
| `char_lulu.png` | `685cce02cd7092472ea6d1833b354dd9a05a0b72a83e45e37f53b8f777c9f671` |
| `char_momo.png` | `7c0bb44e4f3f77d4e83cff8492d44d6206a603a3b03c8d2666301d1e3d4fdfe7` |
| `char_tori.png` | `7569a2e770ff8e8e44b9c9e2c137b6c5cd56f45245eba8bba919038f862cacb7` |
| `decor_bakery_sign.png` | `ea2a487c6a9c83cf9f2b85f54e347fd4c135fbf1e369b3962b1cac752c1f2573` |
| `decor_bakery_sign_slot_back_v3.png` | `3f172eb910a0a264fe87604526ee15e2b03c6b70214edd1d685b823cd0abaa20` |
| `decor_bakery_sign_slot_left_v3.png` | `02de50ace1fa4046e192ced797d2f81783b0d24e10baba9b5fb02e8a57c3f30e` |
| `decor_bakery_sign_slot_right_v3.png` | `dae8018c77527403c3c62739ca2c46547b848ff3f67730b04851c0bb134c3490` |
| `decor_ginkgo.png` | `7f00b4829e9858778d395d50f1ea02c1155b5babc285253b97671686381bf0e6` |
| `decor_ginkgo_slot_back_v3.png` | `c4bd47336f289130cb667f53030931a3647c70732bc8bde8b9cef22f689bd2f2` |
| `decor_ginkgo_slot_left_v3.png` | `5d75aba4fdc21bf7cf04d71dede4ba1ac4a2bfda5f5d6accec9914c032930884` |
| `decor_ginkgo_slot_right_v3.png` | `fa580f06d24671539869771d4a59be6694353697787e1fe457f24d274a3560bb` |
| `decor_shell.png` | `235ca5af8f882b2f5d5e7e949a73dc6e1fc87d7eb573f8daf6c4987a0cbd0890` |
| `decor_shell_slot_back_v3.png` | `7cc15121f1af3cded32807006d33df842459b4f712de7bcdffa07a2cb5d3a03d` |
| `decor_shell_slot_left_v3.png` | `a66721eae88e70b8012b931e5fd6d87ee94b2963553502306fe229e059dd1b57` |
| `decor_shell_slot_right_v3.png` | `2fe688d589cf964e91a7d2901e25c513484f3c4bdb0e8d684ca9949fc128a469` |
| `face_eat.png` | `0f8dbebe7851c29dfb6c490db2e2e6786fb56ac8cc68df556ee5341a630358d5` |
| `face_happy.png` | `046c829cde208cfa4079d836345c28d621a7a8214b076702c847d165683c58d8` |
| `face_idle.png` | `e27d2ee2e6de5307391270448377b594c4eb00ab760ebfc608c79865cdebf671` |
| `food_bread.png` | `bc05275f6eb2ca6d4cfd8a57a7cdc249347e49dd04b31412bbab4b7180344948` |
| `food_potato.png` | `b833d178a48ccd7f95c44b42ca0046d219dbf482bc65fc0050dcb07eff566745` |
| `food_sashimi.png` | `38a99f353009981da3bd40ffa6f989bb2034d15dd679f038c107090b22ff3f6c` |
| `growth_flowers_v3.png` | `1b9253dd09b6c5e4cfbe2fb335ab44ad62be254ad24fc9890a1220f6144e0b35` |
| `growth_path_v3.png` | `2a8254383b3752c9ef7feacf03e602fc623f40f6c96791a7a5a808f1302eed28` |
| `growth_rest_area_v3.png` | `7032f615a265913f203cbca633a7c738fd77d56b8784ca60b322c5d2ea00c973` |
| `island_base_v3.png` | `e04f7c8f44c143388d25d9186a296f54e5bb2b66bea7f58514b560974e7909cd` |
| `outfit_bread_hat.png` | `62102df42e2733da01a3f2e705e8c381fed52b320ff64ec3d07d4882ba5a38c5` |
| `outfit_ginkgo_crown.png` | `405911e9251a07f006157a0b7c41f10c37de6701393d6deb81ed83baced4e1f8` |
| `outfit_wave_scarf.png` | `11fda58ff2f96f0265c7adc9e329521e107e028a4fc9cf7c04cd6829b6cec721` |
