# Changelog

## 2.8.0

- Translated the entire app interface to English: introductory text, live preview, status, add/pause/resume controls, touch locking, settings, birthday information, placement instructions, dialog buttons, and toasts.
- Translated all 26 activity names, widget picker descriptions, control accessibility labels, and switch on/off states. English remains the app language on phones using another language.
- Moved interface copy into English Android string resources; translated sprite-loader diagnostics and the source catalogue labels.
- Added an Italian-phone locale regression on API levels 26 and 35 covering visible controls, activity names, accessibility descriptions, switch states, the widget preview, and placement help.
- Preserved animation playback, assets, touch locking, stored preferences, package name, and signing certificate. Version code 11 allows updating an existing installation.
- Published the English APK and complete source ZIP, with the animation showcase retained on the project homepage.

## 2.7.0

- On Android 12+, playback is fully resident. Removed the asynchronous frame request that could show a transparent loading view when returning to the home screen, even without the unnecessary cache update fixed earlier.
- Android 12/12L uses one ViewFlipper with stable children; Android 13+ uses native RemoteCollectionItems. Separate layouts replace the previous player during upgrade.
- Random routes close with an actual return walk, preserving idle → walk → idle → activity, facing direction, and musical notes. The character never crossfades.
- The finite sequence repeats and changes daily. Duration depends on size variants and speed; not every activity appears in each sequence.
- A total budget of 1600 frames, bounded responsive variants, and orientation fallback keep the complete Parcel below the tested 800 KiB threshold. Android 12 avoids repeated collection metadata and does not measure hidden children.
- Touch locking and view reapplication preserve the current frame. Android 8–11 retains the previous service and checkpoints.
- Added regressions on API levels 31 and 35 for returning from hidden pages, simulated screen-off visibility, a complete loop, immediate frame availability, payload limits, reapplication, and Android 12 touch events. A control reproduces an empty frame in the previous player without an app-scheduled invalidation.
- Version code 10; the same package and certificate allow upgrading from 2.6.

Results and limitations are documented in [VERIFICATION.md](VERIFICATION.md). Not tested on a physical Samsung launcher.

## 2.6.0

- Added a touch-lock toggle near the main controls, with a persistent setting and immediate updates to every placed widget.
- Locked taps on Mozart or the widget area do not open the app, including when playback is paused. Settings remain accessible from the app icon.
- Updated touch actions for existing views, cached frames, and upcoming frames while preserving the adapter, position, and sequence.
- Added a no-op collection touch event compatible with older Android handlers that cannot use a null template.
- Six new Robolectric cases on API levels 26 and 35 cover background taps, cached and upcoming frames, lock/unlock, paused widgets, persistence, and two widgets updated through the actual toggle.
- Version code 9; unchanged package and signing certificate. Sequences and checkpoints remain compatible with 2.4 and 2.5.

## 2.5.0

- Fixed an unnecessary collection refresh after service recreation that could discard a prefetched frame and display transparent loading when the screen woke.
- Limited startup warmup to the first five frames. Requesting a later frame cancels the refresh, even if frame zero had already scheduled it.
- Factory destruction permanently cancels its pending callback; loading and callback access are synchronized.
- Ten new Robolectric cases on API levels 26 and 35 cover the real cache/flipper, service recreation, restoration after frame zero, both startup frame rates, destruction, and an empty-frame control after invalidation. Four regressions failed before the fix.
- Sequence identity and checkpoints remain compatible with 2.4, preserving the cycle, poses, breathing, violin repair, mirroring, and notes.
- Version code 8; unchanged package and signing certificate.

The disappearance was reproduced through simulated service recreation and Android cache invalidation. Physical Samsung launcher testing remained outstanding.

## 2.4.0

- Added a two-second idle breathing cycle: the upper body rises and falls a few pixels while the feet stay fixed.
- Built frames by rigging original pixels, preserving character style and proportions.
- Removed the isolated violin fragment in frames 1 and 3 using only the transparent area of the generated repair. Character and instrument pixels remain unchanged.
- Verified fixed feet, actual torso motion, absence of disconnected fragments, and changes restricted to the defect.
- Preserved idle → walk → idle → activity, immediate transitions, mirrored poses, musical notes, and continuity in a 3×1 widget.
- Version code 7; unchanged package and signing certificate.

## 2.3.0

- Removed crossfades between animations; every pose remains fully opaque.
- Enforced idle → walk to a random destination → idle → random activity → idle, then repeat.
- Walking reaches its destination before resting, with continuity across widget batches.
- All poses and props remain mirrored after walking left until a later walk changes direction. Mirroring preserves the cell's position.
- Birthday events and preview selections follow the same cycle. Rapid taps replace the queued activity.
- Walking animates steps in place when the character completely fills the widget.
- Preserved all 26 activities, musical notes, checkpoints, and fixes for jumps in 3×1 widgets.
- Version code 6; unchanged package and signing certificate.

## 2.2.0

- Added 20 original animation sets and 80 frames generated from the Mozart reference, for 26 activities in total.
- Added violin, conducting, flute, quill writing, humming, tea, reading, a letter, a watch, wig adjustment, a minuet, stretching, yawning, napping, waving, a tricorn hat, a rose, a music box, travel, and applause.
- New sets play forward and backward, with registered sprite cells, aligned feet, and transparent margins.
- Introduced half-second entry/exit transitions and a visible idle between activities, including birthdays and manual selections. These transitions were later removed in 2.3.
- Added occasional musical notes, sometimes paired, rising and fading inside the character's bounds.
- Added all 26 preview controls; selecting an activity returns to the visible scene, and rapid taps replace the queued choice.
- Preserved immutable frames, stable adapters, checkpoints, and the 3×1 jump fixes.
- Version code 5; unchanged package and signing certificate.

## 2.1.0

- Fixed position jumps in wide, short widgets, including 3×1 layouts.
- Removed the fixed, centered pose from the temporary loading view.
- Made numbered frames immutable: prefetch and out-of-order requests cannot rewrite the sequence already playing.
- Preserved widget seeds and adapter identity during ordinary updates.
- Added bounded caching and checkpoints to reconstruct the same route after service recreation.
- Preserved Android's native timer, transparency, eight walking poses, and the absence of overlay/accessibility permissions.
- Passed 42 tests. Six jump regressions that failed in 2.0 passed in 2.1.

Physical Samsung launcher testing remained outstanding.

## 2.0.0

- Replaced the overlay with a native, transparent widget drawn by the launcher inside its own area.
- Removed the overlay, accessibility service, foreground service, and all requested permissions.
- Prepared random sequences in batches of roughly 45 seconds, continuing the engine state and making new choices for later batches.
- Added native playback at approximately 8 or 12 frames per second, with nearby frame prefetch and immediate transitions.
- Preserved eight walking poses, other activities, and special dates, using 36 transparent native resources.
- Added responsive variants for launcher-provided sizes without mandatory configuration.
- Migrated earlier versions while retaining the same package and certificate.

Smoothness and Samsung service binding remained to be checked on a physical phone.

## 1.1.0

- Regenerated walking with eight poses: alternating legs, bent knees, lifted feet, and ground contact. Steps follow distance, speed, and character size.
- Added a single home screen placement action, starting the service from the visible app and returning home after placement. Removed mandatory widget configuration.
- Added a default 2×2 widget, resizable in both directions down to a launcher cell, with the actual character in its preview.
- Removed the manual area editor and saved overrides from 1.0. Position and size follow the widget.
- Batched movement/resize events without repeatedly postponing scans.
- Canceling placement with no widgets present stops the prepared service.
- Passed 20 tests, completed build and lint, and retained the 1.0 signing certificate.

The launcher controls grid sizes and resize-handle visibility. The Samsung placement flow required physical testing; Android tests used Robolectric on API levels 26 and 35.
