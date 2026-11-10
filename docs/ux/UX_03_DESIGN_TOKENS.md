> 최신 적용 지침: `PLEOS_OFFICIAL_UX_UI_IMPLEMENTATION.md`와 화면 대조표를 우선 확인하세요. 아래 토큰은 원본 디자인 값이며 공식 기준 통과 인증이 아닙니다.

# UX 03 · 디자인 토큰 (코스탈 모닝)

기준일: 2026-11-10 · Figma `컬러 가이드 · 코스탈 모닝 (개발용)` 구역과 같은 값 · 기계용 값: `design_tokens.json`

- 주간 모드 기준이에요. 야간 모드는 맨 아래 '야간 후보'를 같은 토큰 이름에 값만 바꿔 쓰세요.
- 그라데이션 방향: '위→아래'는 세로, '왼→오'는 가로예요.
- 투명도는 %로 적었어요.

## 색

| 토큰 | 값 | 쓰는 곳 |
|---|---|---|
| `bg/sky` | 위→아래 #E3F2FF 0% → #F4F8FF 50% → #FFF6EA 100% | 모든 앱 화면 배경 |
| `bg/demo` | 위→아래 #1F4E79 → #13233A | 시연용 연결 화면(실제 앱 아님) |
| `overlay/dim` | #13233A 45% | 대화상자 · 안내 창 뒤 |
| `surface/card` | #FFFFFF | 카드, 보조 버튼, 아이콘 버튼 |
| `surface/panel` | #FFFFFF 92% + `shadow/panel` | 인벤토리 패널, 월드맵 상세, 안내 창 |
| `surface/subtle` | #EEF6FB | 숫자 타일(여정 기록 · 방문 횟수) |
| `surface/locked` | #F3F7FA | 잠긴 아이템, 비활성 목적지 카드 |
| `surface/placeholder` | 위→아래 #EAF6FC → #DDEFF8 | 이미지 자리 |
| `surface/placeholder-warm` | 위→아래 #EAF6FC → #FFE7D1 | 기념품 이미지 자리 |
| `surface/illustration` | 위→아래 #BFE8F2 → #FFE7D1 | 캐릭터 · 지역 일러스트 자리 |
| `primary/navy` | #1F4E79 | 주요 색 |
| `primary/ocean` | #2E86AB | 그라데이션 끝, 선택 테두리 |
| `primary/gradient` | 왼→오 #1F4E79 → #2E86AB | 주요 버튼, 선택 탭, 아이템 행동 버튼 |
| `primary/sky` | #38BDF8 | 진행 막대 시작 |
| `progress/gradient` | 왼→오 #38BDF8 → #1F4E79 | 섬 성장 막대, 저장 중 진행 표시 |
| `progress/empty` | #D6E6F0 | 남은 단계, 진행 바탕 |
| `accent/coral` | #FF8A5B | 캐릭터, 발견 지역 아이콘(25%) |
| `accent/coral-deep` | #FF5E62 | 캐릭터 그라데이션 끝, 선택 지역 |
| `accent/character` | 위→아래 #FF8A5B → #FF5E62 | 캐릭터 자리(그림 전) |
| `accent/chip-bg` | #FF8A5B 14% | 캐릭터 칩, 결과 칩 배경 |
| `accent/chip-text` | #B8431F | 칩 글자 (4.86:1) |
| `scene/sea` | 위→아래 #DDF3FB → #A8DCEB | 섬 장면 배경 |
| `scene/wave` | #FFFFFF 35% | 섬 둘레 물결 |
| `scene/sand` | #F6E3B4 | 섬, 캐릭터 발판 |
| `scene/sand-light` | #FFF1D0 | 섬 윗면 |
| `scene/tree` | 잎 #4CAF50 · #2E7D32 / 기둥 #8D6E63 | 나무 |
| `scene/slot` | #2E8FB8 채움 12% + 점선 2px(6/6) | 장식 슬롯 |
| `stage/sky` | 위→아래 #DDF3FB → #FFF3E3 | 인벤토리 캐릭터 크게 보기 배경 |
| `map/sea` | 위→아래 #BFE8F2 → #9ED3E6 | 월드맵 바다 |
| `map/land` | #FFF8E6, 테두리 #E8D5A6 | 월드맵 땅 |
| `map/road` | #F0DDB0, 4px | 월드맵 도로 |
| `text/primary` | #13233A | 제목 · 본문 · 보조 버튼 글자 |
| `text/secondary` | #5B6B82 | 설명 · 라벨 · 단위 |
| `text/on-primary` | #FFFFFF (보조 설명 78%) | 진한 버튼 · 선택 탭 위 |
| `text/disabled` | #8FA6BE | 잠김 · 점선 테두리 전용 (글자 X) |
| `border/default` | #D6E2EE 1.5px | 보조 버튼, 아이콘 버튼, 선택 안 된 탭 |
| `border/card` | #E3ECF4 1px | 카드 |
| `border/selected` | #2E86AB 2.5~3px | 마무리 가능 목적지 카드, 사용 중 아이템 |

## 효과

| 토큰 | 값 | 쓰는 곳 |
|---|---|---|
| `shadow/panel` | #1F4E79 12% · x0 y16 · 흐림 40 | 패널, 안내 창 |
| `shadow/card` | #1F4E79 8% · x0 y8 · 흐림 20 | 카드 |
| `glow/primary` | #2E86AB 35% · x0 y10 · 흐림 28 | 주요 버튼 |
| `glow/character` | #FF5E62 35% · x0 y8 · 흐림 24 | 캐릭터, 선택 지역 |

## 모서리

주요 버튼 22 · 보조 버튼 18 · 아이콘 버튼 16 · 탭 14 · 아이템 · 목적지 카드 20 · 캐릭터 카드 · 패널 · 안내 창 24 · 장면(섬 · 지도) 20 · 칩 999

## 글꼴 (Noto Sans KR)

| 용도 | 굵기 | 크기(px, 임시) |
|---|---|---|
| 화면 제목 | Bold | 28~36 |
| 결과 제목 | Bold | 36 |
| 버튼 | Bold | 18~28 |
| 큰 숫자 | Bold | 26~44 |
| 라벨 | Medium | 14~16 |
| 본문 | Regular | 15~20 |

2026-11-10 공식 UI 개요 재확인: 중요 정보 기본 텍스트 높이5mm 이상·보조3mm 이상, 문단3줄 이하. 기존 중요4mm 안내는 정정합니다. 실제 물리 높이는 논리dp/density만으로 인증하지 말고 공식 Typography와 대상 표시 환경에서 확인하세요.

## 대비 체크 (Pleos 기준 4.5:1)

| 조합 | 대비 | 판정 |
|---|---|---|
| #13233A / 배경 #F4F8FF | 14.83 | 통과 |
| #5B6B82 / 흰 카드 | 5.43 | 통과 |
| 흰 글자 / #1F4E79 | 8.66 | 통과 |
| 흰 글자 / #2E86AB | 4.11 | 미달: 굵은 글자 예외로 통과 처리 금지. 실제 글자/아이콘 배경 대비 보정 필요 |
| #B8431F / 칩 배경 #FFEFE8 | 4.86 | 통과 |
| 흰 글자 / #FF8A5B | 2.32 | 미달: 캐릭터 위에 글자를 올리지 않음 |

## Compose 예시 (참고용, 실제 위치는 A의 theme 코드)

```kotlin
object MemorideColors {
    val Navy = Color(0xFF1F4E79)
    val Ocean = Color(0xFF2E86AB)
    val Sky = Color(0xFF38BDF8)
    val Coral = Color(0xFFFF8A5B)
    val CoralDeep = Color(0xFFFF5E62)
    val ChipText = Color(0xFFB8431F)
    val TextPrimary = Color(0xFF13233A)
    val TextSecondary = Color(0xFF5B6B82)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSubtle = Color(0xFFEEF6FB)
    val SurfaceLocked = Color(0xFFF3F7FA)
    val BorderDefault = Color(0xFFD6E2EE)
    val BorderCard = Color(0xFFE3ECF4)
    val ProgressEmpty = Color(0xFFD6E6F0)
    val Sand = Color(0xFFF6E3B4)
}
val BgSky = Brush.verticalGradient(listOf(Color(0xFFE3F2FF), Color(0xFFF4F8FF), Color(0xFFFFF6EA)))
val PrimaryGradient = Brush.horizontalGradient(listOf(MemorideColors.Navy, MemorideColors.Ocean))
val ProgressGradient = Brush.horizontalGradient(listOf(MemorideColors.Sky, MemorideColors.Navy))
val CharacterGradient = Brush.verticalGradient(listOf(MemorideColors.Coral, MemorideColors.CoralDeep))
```

## 야간 후보 (컬러 시안 A · 미드나잇 드라이브)

| 토큰 | 야간 값 |
|---|---|
| `bg/sky` | 위→아래 #0B1026 → #1B1F4B → #2B1E5C |
| `primary/gradient` | 왼→오 #22D3EE → #818CF8 (글자 #0B1026) |
| `accent/coral` | #FFD166 |
| `text/primary` | #F5F7FF |
| `text/secondary` | #A9B4D6 |
| `surface/panel` | #FFFFFF 8% + 배경 흐림 30 |
