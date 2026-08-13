# Android Port — Dan Thanh Guzheng

**Date:** 2026-08-13
**Status:** Approved design, ready for implementation planning

## Goal

Ship a native Android app with full feature parity to the existing SwiftUI iOS app,
driven by user demand for an Android version. The iOS app is not modified.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Stack | Native Kotlin + Jetpack Compose | SwiftUI maps near 1:1 to Compose; fastest path; best fidelity on tuner, player, and design — the pieces cross-platform runtimes handle worst |
| Scope | Full parity, all 5 tabs | Android users get what iOS users have; no feature explanation needed |
| Entitlements | Per-store, no cross-platform sync | Zero extra work, zero iOS changes; can add accounts later without breaking anything |
| Repo | Monorepo, new `android/` folder, no renames | Shared Firestore schema and design assets stay in one commit; renaming breaks `.xcodeproj` paths and project memory |
| Package name | `vn.hocdantranh.guzheng` | Correct reverse-DNS; permanent once published |
| minSdk / targetSdk | 24 / latest stable | API 24 covers ~99% of Vietnamese devices |
| Play account | Personal | Subject to 12-testers / 14-day closed test gate |

### Rejected alternatives

- **Flutter/React Native rewrite** — would rebuild the tuner, secure player, and IAP from
  scratch on a new runtime, then require a second risky migration of paying iOS customers.
  ~8–10 weeks to Android vs ~6. Single codebase is the only upside, and it is deferrable.
- **Kotlin Multiplatform** — shares ~1,000 lines of logic but requires restructuring a
  shipped iOS app now, for maybe 30% savings on future feature work.
- **Flutter for Android only** — same cost as a full rewrite, two languages forever,
  nothing shared. Strictly worse than a native port.
- **Full account system (Firebase Auth) for cross-platform purchases** — +2–3 weeks,
  requires an iOS release, and adds a login wall to an app that has none.

## Source of truth

The iOS app is ~5,750 lines of Swift across 27 files. It is the functional specification.
Firestore (`apphocdantranh` project) is shared unchanged — **no backend work in this port.**

## Architecture

MVVM, mirroring the iOS structure. `ObservableObject`/`@Published` → `ViewModel`/`StateFlow`.
`@AppStorage` → DataStore. Single Activity hosting Compose, Navigation Compose for routing.

```
AppHocDanTranh/                      git root, unchanged
├── HocDanTranh-Guzheng-DanThanh/    iOS, untouched
├── android/                         new
│   └── app/src/main/
│       ├── java/vn/hocdantranh/guzheng/
│       │   ├── DanThanhApp.kt       Application, Firebase init
│       │   ├── MainActivity.kt      single Activity, FLAG_SECURE
│       │   ├── ui/
│       │   │   ├── theme/           AppColors.swift → Color.kt
│       │   │   │                    AppFonts.swift  → Type.kt, Theme.kt
│       │   │   ├── components/      VintageComponents, LessonCard,
│       │   │   │                    LockBadge, PurchaseBottomSheet
│       │   │   ├── home/ videos/ tuner/ sheets/ settings/ lesson/
│       │   │   └── MainScaffold.kt  NavigationBar, 5 tabs
│       │   └── data/
│       │       ├── model/           Lesson, VideoItem, Sheet
│       │       ├── repo/            VideoRepository, SheetRepository
│       │       ├── billing/         BillingManager
│       │       ├── audio/           PitchEngine
│       │       └── prefs/           AppPrefs
│       └── res/
│           ├── values/strings.xml   Vietnamese (default)
│           ├── values-en/strings.xml
│           ├── raw/                 string_01..N.m4a, copied as-is
│           └── drawable/            exported from Assets.xcassets
├── design_handoff_guzheng_vintage/  shared by both apps
└── docs/
```

### Tab order (must match iOS)

Lessons → Video → Tuner → Sheet → Settings

## Dependency mapping

| iOS | Android | Notes |
|---|---|---|
| firebase-ios-sdk (Firestore) | `firebase-firestore-ktx` | same project, same collections, no backend work |
| StoreKit 2 | Play Billing Library 7 | rewritten; same product IDs |
| Tuna (YIN) | TarsosDSP (YIN) | same algorithm, different audio plumbing |
| AVPlayer / AVKit | Media3 ExoPlayer | + `FLAG_SECURE` |
| YouTubePlayerKit | `android-youtube-player` | equivalent |
| ThumbnailLoader (custom) | Coil | delete the custom loader, use the library |
| SKStoreReviewController | Play In-App Review API | direct equivalent |
| UpdateChecker (App Store lookup) | Play In-App Updates API | direct equivalent, cleaner |
| `.lproj` + `@AppStorage` | strings.xml + `AppCompatDelegate.setApplicationLocales` | see below |

### Localization

iOS currently mixes two mechanisms: `NSLocalizedString` via `.lproj` for most strings,
and a manual `isEnglish` ternary in `MainTabView` for tab labels. Android unifies on
`strings.xml` (`values/` = Vietnamese default, `values-en/` = English) with the in-app
toggle driven by `AppCompatDelegate.setApplicationLocales`. This preserves the user-facing
behavior — an explicit vi/en switch in Settings, not system-locale-following — while
removing the inconsistency.

## The three hard parts

Everything else is mechanical translation. These are where the effort and risk live.

### 1. Billing (~1 week)

Recreate the 11 one-time products in Play Console with **identical product IDs**
(`course_basic_1490k`, `song_divang_combo_150k`, `bundle_5songs_600k`, `lifetime_all_2990k`, …)
so entitlement logic stays symmetrical with iOS.

**Critical difference from StoreKit:** StoreKit 2 exposes `Transaction.currentEntitlements`
as an automatic source of truth. Play Billing does not. Play **auto-refunds any purchase not
acknowledged within 3 days.** `BillingManager` must:

- acknowledge immediately on every successful purchase, and
- sweep `queryPurchasesAsync()` on every app start for unacknowledged purchases.

This is the most common way indie Android apps silently lose revenue. It gets an explicit
test.

Products are non-consumable (never call `consumeAsync`) — correct for course content.
Prices set per-country in Play Console; VND is supported.

**Testing constraint:** billing cannot be tested on a local debug build. It requires an
upload to a Play track with license testers configured — an additional reason to open the
closed track in week 1.

### 2. Tuner (~1 week, highest variance)

TarsosDSP implements the same YIN algorithm as Tuna, so the pitch math is identical.
The Android-specific work is audio capture:

- **Audio source matters more than anything else.** Use `MediaRecorder.AudioSource.UNPROCESSED`
  where supported (check `AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED`), falling
  back to `VOICE_RECOGNITION`. Never `MIC` or `VOICE_COMMUNICATION` — their auto-gain and
  echo cancellation destroy pitch stability.
- `AudioRecord` at 44.1 kHz, buffer 2048–4096 frames.
- Runtime `RECORD_AUDIO` permission flow with rationale and "don't ask again" handling —
  iOS needs no equivalent.

The hard-won logic in the iOS rewrite carries over directly and de-risks this substantially:
no amplitude/level gate, trust the YIN probability rather than signal level.

**Requires 3–4 physical phones across price tiers.** The emulator microphone is useless for
pitch work.

### 3. Secure video (~4 days)

Media3 ExoPlayer with HLS for Cloudflare Stream, Compose watermark overlay matching the iOS
dynamic watermark, and `FLAG_SECURE` on the window — which blocks screenshots and screen
recording at OS level, genuinely stronger protection than iOS offers.

**Pre-existing gap:** `VideoManager.fetchSignedVideoURL` on iOS is still a placeholder
(`TODO: Implement actual Cloudflare API integration`) returning a constructed URL. Android
inherits this gap. It is not new work caused by the port, but neither platform can ship
secured lesson video until signed-URL generation is real.

## Timeline

~6 weeks, with the Play Console closed-test clock running in parallel.

| Week | Work | Play track |
|---|---|---|
| 0 (now) | Register Play Console ($25), add Android app to Firebase (`google-services.json`), recruit 12 testers | — |
| 1 | Scaffold, theme, navigation, 5-tab shell, i18n, DataStore | stub → closed track, **14-day clock starts** |
| 2 | Firestore repos + Lessons, Video, Sheet Music screens | |
| 3 | Billing: 11 products, acknowledge logic, purchase sheet, gating | license testers |
| 4 | Tuner: TarsosDSP, permission flow, real-device tuning | |
| 5 | Video player + watermark + YouTube, Settings, in-app review/update | |
| 6 | Device matrix, store listing, data safety form, content rating → production | clock expired |

Weeks 1–2 compress significantly with AI-assisted translation; the work is mechanical and
the iOS source is unambiguous. Weeks 3–4 do not compress — billing and pitch tuning are
empirical, gated on real Play Console state and real hardware.

## Risks

| Risk | Mitigation |
|---|---|
| Unacknowledged purchases auto-refunded after 3 days | Acknowledge on purchase + sweep on every app start; explicit test |
| Tuner unstable on budget Android hardware | `UNPROCESSED`/`VOICE_RECOGNITION` source; test on 3–4 real phones in week 4, not week 6 |
| 14-day closed test blocks launch | Open the track in week 1 with a stub build; recruit 12 testers in week 0 |
| Package name is permanent | Decided and recorded: `vn.hocdantranh.guzheng` |
| iOS buyers expect Android access | Out of scope by decision. Handle case-by-case; a Firestore manual-grant flag is a cheap later addition |
| Signed Cloudflare URLs unimplemented | Pre-existing on iOS; tracked separately, blocks secure video on both platforms |

## Out of scope

- Any change to the iOS app
- Cross-platform purchase sync, user accounts, Firebase Auth
- Backend or Firestore schema changes
- Tablet-specific layouts (phone layouts scale; revisit after launch)
