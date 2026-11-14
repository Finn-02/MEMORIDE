# PR26 PM screens: A connection

PM source: origin/part/pm-p01-briefing db5a1eb, merged locally with ancestry retained. A base6675b07. Main and original dirty A working tree are preserved.

- ProductScreenSlots now calls RegionBriefingScreen / ArrivalSaveScreen, with applyInsets=false because GameAppRoot owns the outer inset shell.
- A supplies first-visit avatar and B read-only island scene on revisit via PM scene slot. PM does not import B. Existing shared DTO/slot signatures and game save schema unchanged.
- Result island/character callbacks still go through ReceiptCloseCoordinator and B acknowledgeReceipt, preserve the displayed original while awaiting publication, and only route after success/current owner/gate/lease/generation checks. PM handles button/Back deduplication; failure keeps result available for retry.
- Failed save close returns to island while retaining ManualJourneyRuntime original input. Island retry reopens the same save flow. Retry/dismiss read current runtime busy state rather than a stale composed boolean.

Windows JBR25 / wrapper9.3.1 / Python3.10: check_all.py --android Python66/JUnit376(failure/error/skip0)/debug PASS. compileReleaseKotlin PASS; not signed release installation. New A tests exercise PM actions with actual A/B ack failures and both destinations, and buttons/Back racing one suspended acknowledgement.
Actual Pleos observations and exact final source/APK hashes are recorded in the review ZIP REPORT.md. Actual ack storage failure and full Saving-state input races were tested at JVM boundaries, not injected into a real PM screen. Large font, physical Galaxy, real external navigation and signed release remain separate.

PM merge-route coordination is required before final main merge of overlapping PR26/28/29. It is not a prerequisite to A internal wiring or local verification. No automatic main merge.
