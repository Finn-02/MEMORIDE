# B06-7L Android 자산

본 앱의 캐릭터는 갤럭시 검수를 거쳐 승인된 토리 v06, 모모 v01, 루루 v01을 사용한다. `AvatarView`는 저장된 캐릭터 ID와 표정 키, Catalog에서 조회한 의상으로 아래 승인 프레임 한 장을 고른다. PNG가 누락되면 같은 캐릭터의 해당 표정 기본 외형, idle 기본 외형, 플레이스홀더 순으로 표시한다.

B06-7K의 몸체 및 공통 표정·의상 PNG는 기존 Catalog와 회귀 기준을 위해 바이트를 보존한다. 몸체의 제작 원본은 아트 보관본의 `memoride_b067k_build_review.py`와 `B067K-review/b067k_*_source.blend`다. 이 세대의 PNG는 `AvatarView`의 외형 선택에 사용하지 않는다.

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

## 승인 캐릭터 프레임 36장

아트 보관본의 `tori_review_v06/tori_EDITABLE.blend`, `momo-lulu-review-v01/momo/momo_EDITABLE.blend`, `momo-lulu-review-v01/lulu/lulu_EDITABLE.blend`를 Blender 5.2.3에서 정면 투명 256×256 RGBA8로 렌더했다. 각 표정의 기본·목도리 프레임은 원본 렌더의 가림과 접촉 그림자를 담는다. 빵모자와 은행잎은 승인 캐릭터의 장면에 별도 3D 메시로 추가했다. 제작 원본은 아트 보관본 `Headwear-3D-Review-20261109/models`의 `tori_headwear_review.blend`, `momo_headwear_review.blend`, `lulu_headwear_review.blend`이며, `build_headwear.py`가 동일한 정면 카메라와 조명으로 의상별 beauty PNG를 렌더한다. 각 장식의 가림과 접촉 그림자는 완성 프레임에 포함된다. 삼성 갤럭시 검수에서 세 캐릭터의 36조합을 승인했다.

경로는 `app/src/main/assets/avatars/approved/<파일>`이고 APK 경로는 `assets/avatars/approved/<파일>`이다. 파일의 SHA-256은 승인 검수 PNG와 동일하다.

| 파일 | SHA-256 |
| --- | --- |
| `tori/idle__none.png` | `3e50006dbc05b9b81198589a5bbc78f8579ab1d023b646f7ae57a0a9e793a422` |
| `tori/idle__bread_hat.png` | `7797357c1140de61f0c426b061e905a7b4c1b46ab5ea4b078d3dc551637bb301` |
| `tori/idle__ginkgo_crown.png` | `3a44788a9a7aa890b194080e27b6f65f5903780eb0738a4878129e0fa1bd7cbe` |
| `tori/idle__wave_scarf.png` | `4357627aa14b3b7c0acc244307b98ccff498745ad77a0f07623cf251d972b7a8` |
| `tori/happy__none.png` | `796a146ef0199ff40465f7aa2851dda876451a3934705de053b03e5c129df611` |
| `tori/happy__bread_hat.png` | `886c6eeec46bab5ec7d42b463270498619861402d00df20c3d73f98935b96c9d` |
| `tori/happy__ginkgo_crown.png` | `3a0903110c5e3c8b4393d1b7d9162cd0586bd6c158446dbf0847fd0ada395555` |
| `tori/happy__wave_scarf.png` | `27aa4b36f996315ca5bfe19aa931708d092ec96f48fa3827fcc9ce6b6a01a046` |
| `tori/eat__none.png` | `8f76e8e4763b105e34cbd677815dd2a682a6fed2bf72ea2f79183c9c31bbfe6a` |
| `tori/eat__bread_hat.png` | `ba01bd59abd348dcb381fb3cf534da8c14666dabc3b1462dac5cd06e1217cdd6` |
| `tori/eat__ginkgo_crown.png` | `f7dae11b8ea06398f65f2ac38942ab2a25e81691d59e7bafbbbd908c13b88cc8` |
| `tori/eat__wave_scarf.png` | `3261df95e7e23e9606b0527e27d87e4d4d91aaddbfd8363ef90d3c3a10dd7672` |
| `momo/idle__none.png` | `bbd29693d5ef648f17abfd2d13017a7cd461ce50f2ebf634dec22456665e6494` |
| `momo/idle__bread_hat.png` | `64c3426a587982c82658b717cc9c4ed441745d7903ae2b7c3fda0badb39bc423` |
| `momo/idle__ginkgo_crown.png` | `d6189cd06128474b883f5fba9fb5cc91a6479797e628d286a36af5b6f70889cc` |
| `momo/idle__wave_scarf.png` | `1f8a64f1a3568aa7410c99042b8c2d1c22905320960215a8d9082920683b62f7` |
| `momo/happy__none.png` | `f550388caab8f88d543cde9969a244dcec9071849528d1e68162b39b19315ca7` |
| `momo/happy__bread_hat.png` | `e7dc04bf752ee92e82f3b72cf46ae92f8865def1a55c7cf7bb679441b040d269` |
| `momo/happy__ginkgo_crown.png` | `4b2bbe0959148eeb3877ac602c75e65772f55ada0a12fa8423e06ba0d43bafc9` |
| `momo/happy__wave_scarf.png` | `ff5c633638b282780f2ad4af08d58a1a32faae096f4c611a159f9a3e3d7d4216` |
| `momo/eat__none.png` | `4efed1e878e8dda61afc25e1dec0f60ec71eed60b20054eb646c7fe4be156458` |
| `momo/eat__bread_hat.png` | `07cd4daab7939ff5f5abe26cf53c4ce8885a56a6cc2364044525e8b7d0fac7a3` |
| `momo/eat__ginkgo_crown.png` | `e33892bf3b62ab5fa6300f0310edac24eb0ca0086f7ee39a708de08ab68fceb7` |
| `momo/eat__wave_scarf.png` | `0b406c20aff4e88dd4179b7261fafe0f59b623cdebb4c4a497729d734b9de662` |
| `lulu/idle__none.png` | `d07c083dbc8e680d67a87d3749e02b2de04d3f08dc0d3f96c92854ea480abcd9` |
| `lulu/idle__bread_hat.png` | `a45bfebfc2491b7356817d76be0ee08672fb6a5be4b25e2833e2d9e643e9d55b` |
| `lulu/idle__ginkgo_crown.png` | `a7db53cec821688c097c8bdab3ca2e65af89f58d3f4cba3d270766be8194ba41` |
| `lulu/idle__wave_scarf.png` | `098a9513aec9896ae656e06398fd7605d6809b746952ee5d63cfc74f2dd4ca72` |
| `lulu/happy__none.png` | `f60c5a468062e4e5561864620b3ae1a3d77f5b2a71856c272769b8f42f636ae8` |
| `lulu/happy__bread_hat.png` | `e5e3951f29b5680cf8275e604809db689008f293f702c7280113ebeb7f184393` |
| `lulu/happy__ginkgo_crown.png` | `fd08053002934e75f5e1214e3a34d5137ab698ffb1b3a607f105d7659c419df5` |
| `lulu/happy__wave_scarf.png` | `6604eb66ebb65d895dc895d74f4e66842c3e8cf5074e0f457a4c71c7beddb725` |
| `lulu/eat__none.png` | `c5e1afd1ff0d53be4b456b4fe72927c35289fef62488553e67a7f86f5a7c29d4` |
| `lulu/eat__bread_hat.png` | `04cb2afa0b4e544f3f618b0959225ff384692fa50059588259d549a78cf760dc` |
| `lulu/eat__ginkgo_crown.png` | `5be223a4d5eb56890203db344eb9aebe9a02be6e7f8c904b24f78ab5732623fe` |
| `lulu/eat__wave_scarf.png` | `0a06213454cfde01ef51e154a7936faa72edee7f5ed8d9fd3972aed61c702722` |
