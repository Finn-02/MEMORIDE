# B/A/C local UX integration candidate

This local candidate starts at cdd9ac7 (B implementation and shared REGION alignment already included). It adds the approved A/C Runner pair once, B S03 follow-up db7c92c once, and the optional C review test supplement once. It then carries the existing local A navigation capture, A02 product routing and C03 isolated rehearsal work without modifying the original working tree.

## App wiring
- CharacterSelectionScreen, NewIslandScreen and NewInventoryScreen are B's final screens, replacing A temporary character/island and legacy InventorySheet adapters.
- BGameService.regionVisitHistory is collected in AppRoot and passed as the immutable C world input; Loading/Unavailable are preserved.
- The SaveableStateHolder remains outside the restricted subtree. B's saveable tab and decoration selection survive actual restriction/background/configuration return; command callbacks still check owner, foreground, screen lease, generation and pending receipt.
- Only B computes and writes progress. Actual platform gate and UNKNOWN blocking remain. Rehearsal uses per-session isolated B stores, a synthetic DEMO-labelled destination and explicit USER_CONFIRMED arrival with null measurements. No real external navigation launch is implemented or certified by this rehearsal.
- PM result/save screens remain temporary A adapters pending PR #26. Navigation failure/retry remains guarded. B's island supporting text about automatic navigation describes intended behavior; actual external launch is still an A03 limitation.

## Verification
Windows AMD64; recorded JBR25 and committed Gradle9.3.1 wrapper.
Python310 -X utf8 scripts/check_all.py --android: Python66 / JUnit364, failures0 errors0 skipped0 / debug APK PASS. B tests130 included. Initial added revisit test used an invalid non-UUID seed ID, fixed to a valid test UUID and entire suite passed. Initial LF packaging failure was corrected before the passing run.
Gradle :app:compileReleaseKotlin PASS. This is compilation, not signed release installation.
Actual Pleos2.1/API34 x86_64, read-only/no-snapshot-save AVD: see REPORT.md in review ZIP for screenshots, exact tested branches, APK hash and final candidate SHA. No save reset or gate bypass.

## Remaining
PM shared fixture/merge-target coordination and PR #26 order; one final PR only to avoid duplicate B/alignment merging. Actual Galaxy, large-font acceptance, real external navigation/SDK and signed release installation/upgrade are not covered. No push/main merge/third-party messages by A.
