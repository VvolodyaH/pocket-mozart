# Verification · Pocket Mozart 2.7.0

Application verification completed on October 12, 2026. Subsequent English documentation edits do not change the APK or application code.

## Results

- `:app:assembleDebug`: APK 2.7.0, version code 10, package `it.mozart.pocket`, minSdk 26 and targetSdk 35. Size: 14,504,588 bytes.
- `:app:testDebugUnitTest`: **76 tests, no failures, errors, or skipped tests**. This includes 28 JUnit tests and 48 Robolectric cases on API levels 26, 31, and 35.
- `:app:lintDebug`: **no errors**, with six existing warnings: three newer Android attributes, one backup rule, and two intentional image duplicates.
- Signing verified with `apksigner`, matching 2.6 and earlier versions. Certificate SHA-256: `62ef238df8c9a7b830f4cff8c2da1716a91478869eefa399d6950e89f0fe3ba5`.
- The manifest requests no permissions. The `BIND_REMOTEVIEWS` service remains for Android 8–11; Android 12+ uses resident frames.
- Sprites and atlases are unchanged from 2.6. Native graphics comparisons still cover all 108 frames across 26 poses.

## Reproduced cause

`AdapterViewFlipper` switches immediately to the requested item. If `RemoteViewsAdapter` cannot find it in its cache, it displays a transparent loading view while waiting for the service. A missing frame is sufficient; the unnecessary callback fixed in 2.5 is not required.

On API levels 31 and 35, `thePreviousServicePlayerCanBlankWithoutAnyWarmupInvalidation` prepares the previous player and Android cache with frames 400 and 401, then requests an uncached index. Without triggering an app invalidation, the framework displays an item without the character image. This demonstrates a mechanism consistent with the report; it does not identify the exact cause through logs from a physical Samsung phone.

## Android 12+ fix

All frames are delivered before playback. Android 12/12L uses one `ViewFlipper` with stable children and no entry/exit animations. Android 13+ uses `RemoteCollectionItems` with `AdapterViewFlipper`. Both paths remove the remote service and its loading view from playback.

The Android 12 path also avoids repeated application metadata serialization for each `RemoteCollectionItems` item. A 1600-item experiment reached about 3.55 MB. Nested `RemoteViews` instead share hierarchy data. The overall budget is 1600 frames across all variants, and tests check the complete Parcel below 800 KiB, leaving room beneath Binder's 1 MiB limit. Frames transfer references to packaged resources rather than individual bitmap buffers.

New layouts have IDs `0x7f040005` through `0x7f040008`, distinct from the previous animated layouts `0x7f040000` and `0x7f040004`. Upgrading can replace the previous view and abandon its service callbacks. Ordinary updates with unchanged configuration retain the adapter or stable children and the current index.

## Continuity and tradeoff

The resident sequence is finite. It repeats and changes with the local date at the first update of a new day. Each sequence selects some activities from the complete catalogue. With one or two size variants, it generally lasts around one to three minutes; more variants reduce the available time. Android 8–11 retains the previous player, without the new finite loop but with possible transparent loading delays.

Closing the loop does not instantly move Mozart back to the center. The final walk reaches the starting position, followed by idle and an activity, before playback returns to the opening idle. Opening and closing facing directions match. Musical notes finish before the wrap, without changing the character's opacity. Very large areas limit route distance according to the available time.

`ResidentTimelineTest` checks two frame intervals (83 and 125 ms), three budgets (400, 800, and 1600), four geometries (300×56, 40×40, 64×240, and 2000×2000), and eight seeds: **192 combinations**. Every frame pair, including last→first, respects speed, bounds, idle→walk→idle→activity order, and direction. Checks also cover notes, opening breathing, repeatability, the complete activity catalogue across fifty seeds, and birthdays.

## Resume, sizing, and touches

`ResidentPlaybackTest` serializes and reconstructs RemoteViews before applying them in the host. On API levels 31 and 35, at both frame rates, it traverses every frame and crosses the wrap boundary. Every 53 frames, it hides the window for six simulated seconds and shows it again. The index remains fixed during the pause, and the current view is visible, opaque, and immediately contains its drawable. No loading callback is completed to make the next frame available.

On API level 31, tests execute the real `ViewAnimator`. Robolectric's standard shadow stores a separate index from the native field read during resume, so it does not correctly represent this case. On API level 35, tests also verify that no service-backed `RemoteViewsAdapter` is present.

Transfer checks use one, two, four, and sixteen launcher-provided sizes. Beyond four, the renderer uses the landscape/portrait pair. Sequence geometry stays inside the supplied area, including a 3×1 row.

The Android 12 touch test mounts the view in an Activity, sends down/up events on the character, and verifies opening, locking, and unlocking. Index 160 and the same visible child survive reapplication; the next frame also remains locked. Unlocking retains index 161 and restores app opening. Earlier API level 26/35 tests cover collection templates, the background, paused widgets, persistence, and two widgets updated through the actual UI toggle.

Motion, legacy checkpoints, exact mirroring of every pose, breathing, note cleanup, startup, and placement tests remain in the suite. The five version 2.5 regressions remain on the legacy API level 26 path.

## Verification limits

**Not tested on a physical Samsung phone.** Robolectric checks the framework and serialization, but does not reproduce Samsung launcher compositing, gestures, memory management, or scheduling policies. Suspension is simulated through window visibility rather than physically switching off the display.

Resident playback removes service-loading delays. It cannot prevent a launcher from recreating a complete page or suspending rendering. Android 12 keeps more views in memory than the Android 13+ collection player. Device memory, initial placement time, power consumption, and smoothness under load require physical testing.

To check on a device, install using **Update**, keep a 3×1 widget, and repeatedly move from the page on its right back to Mozart's page. Also try screen-off/unlock, opening and closing apps, both frame rates, and touch locking. Watch at least one complete loop: returning to the starting point should happen by walking. If the launcher retains an older view after upgrading, pause and resume playback from the app to request a replacement.
