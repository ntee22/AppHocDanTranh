# Android Tuner + paid-lesson scaffolding — design

Date: 2026-11-24. Approved in conversation by Kelvin.

## Task 1 — Android Tuner (parity with iOS `TunerView.swift`)

**Why:** the Tuner tab ships as "Đang phát triển"; Android users should get the same
tuner iOS has. Behaviour must match iOS; no hand-written pitch DSP.

**Approach (chosen):** TarsosDSP `core:2.5` (Maven repo `mvn.0110.be`) for YIN, fed by
`AudioRecord`. Same algorithm as iOS Tuna, buffer 4096, frames below −60 dBFS treated as
"no pitch". YIN threshold 0.20 instead of Tuna's 0.05: on the test phone 0.05 misread a
437.9 Hz tone as 146/109 Hz (sub-harmonics); 0.20 removed those frames. Rejected: writing YIN ourselves (Kelvin: use
a library); native/Oboe (no benefit at 10 fps).

Ported 1:1 from iOS:
- 21 strings, tone D, same Hz table and labels; green A strings.
- Range gate 55–1500 Hz; drop frames > ±200¢; in-tune = |¢| < 15.
- Auto-detect needs 3 consecutive frames on the same nearest string.
- Tap a string → manual lock + play `string_NN.m4a`; mic frames ignored while it plays;
  tap the locked string again → back to auto.
- Match: |¢| ≤ 5 for N consecutive frames (sensitivity low/medium/high = 7/5/3) →
  "✓ Đã đúng!", success haptic + short beep, 1.5 s cooldown, review-prompt counter +1.
- First-run guide popup (key `dt-tuner-guide-seen`), "?" reopens it.
- Settings gets the "Độ nhạy tuner" Low/Medium/High section (pref key `tunerSensitivity`).

Android-only additions: `RECORD_AUDIO` runtime permission with a denied-state card that
opens system settings; engine stops when the tab leaves composition or the app goes to
background.

**Review prompt:** Play In-App Review (`review-ktx`), same throttle as iOS ReviewManager:
prompt after 3 positive actions, ≥ 60 days apart. Written as a shared `ReviewPrompter`
so Lessons can call it later.

## Task 2 — paid lessons scaffolding (iOS + Android), shipped switched off

**Why:** lessons 8–16 will be sold later as one course. Build the code now so turning it
on is a flag flip + store setup + resubmission, while this release passes review and shows
users nothing about payment.

Lesson list 7–16 replaces the old 7–10. Lesson 7 free; 8–16 paid. All ten are
"Coming soon" today.

**Model change:** `isFree` used to mean "playable". Split into two fields:
- `isFree` — access tier (free vs part of the paid course).
- `isReleased` — the video exists.

One pure function decides what the user sees, identical on both platforms:

| isReleased | isFree | iapEnabled && owned | → state |
|---|---|---|---|
| false | any | any | COMING_SOON |
| true | true | any | FREE (playable) |
| true | false | true | PURCHASED (playable) |
| true | false | false, iapEnabled | LOCKED (shows purchase entry) |
| true | false | iapEnabled == false | COMING_SOON |

**Product:** single non-consumable `course_basic_1490k` (already in iOS StoreKit config);
`lifetime_all_2990k` also grants it on iOS.

**Switch:** compile-time `FeatureFlags.IAP_ENABLED = false` (Android) /
`FeatureFlags.iapEnabled = false` (iOS). Not a remote flag: Apple 2.3.1 forbids enabling
hidden features after review, and the first iOS IAP must be submitted with a binary anyway.
With the flag off: no billing connection, no StoreKit product fetch, no price, no buy or
restore button anywhere.

To enable later: create the product in both consoles, flip the flag, resubmit both apps.

**Android billing:** Play Billing `billing-ktx` wrapped in `BillingRepository`
(connect, query `course_basic_1490k`, purchase, acknowledge, restore via
`queryPurchasesAsync`). Purchase sheet + Settings "Restore purchases" row exist but are
only reachable when the flag is on.
