# Android YouTube in-app playback fails (parked)

**Date:** 2026-08-23
**Status:** Root cause NOT yet found — investigation parked, tree reverted to clean
**Affects:** `ui/lessons/YouTubeLessonPlayer.kt`, all six free lessons
**Device:** OPPO CPH2363, Android 14, Android System WebView 150.0.7871.181

## Symptom

Opening any free lesson shows the fallback card
("Video không thể phát trong ứng dụng" / "Xem trên YouTube ↗")
instead of playing inline. iOS plays the same video IDs fine.

## Evidence gathered

| Check | Result |
|---|---|
| `onReady` / `onError` logging | `onReady` fires, then **immediately** `onError=UNKNOWN` |
| `UNKNOWN` meaning | Library maps 2/5/100/101/150; `UNKNOWN` = some **other** code (likely 153) |
| All 6 lesson videos via oembed | HTTP 200, public, correct titles — videos are fine |
| **Control video** `dQw4w9WgXcQ` in our app | **Fails identically** → not a per-video restriction |
| Innertube `playabilityStatus` probe | Useless — control video also returned UNPLAYABLE (bot-gated). Discarded. |
| Library version | 12.1.2 is the **latest** on Maven Central; no newer fix to pull |
| Library default `origin` | Already `https://www.youtube.com` — our explicit `.origin()` call is a **no-op** |
| Library WebView settings | Sets only `setJavaScriptEnabled` + `setMediaPlaybackRequiresUserGesture`; **never sets a User-Agent** |

## Hypotheses tested

1. **Per-video embed restriction** — DISPROVEN. Known-embeddable control video fails too.
2. **Hardcoded `.origin()` is wrong** — DISPROVEN. It equals the library default.
3. **Stock WebView UA (`; wv` token) trips YouTube's 2025 enforcement** — DISPROVEN.
   Stripping `wv` was verified applied on device
   (`UA after=Mozilla/5.0 (Linux; Android 14; CPH2363 Build/TP1A.220905.001) ...`)
   and the error was unchanged.

## Next step when resuming

Get the **raw numeric error code** — everything so far infers it. The last attempt wired a
`@JavascriptInterface` (`YTRaw`) plus `player.addEventListener('onError', ...)` via
`evaluateJavascript` on `onReady`, but produced no `js:` log lines — the injection itself
needs debugging first (likely the JS interface must be registered before page load, and/or
`player` is not reachable in the evaluated scope).

Once the real code is known:
- **153** → `web_embedded` enforcement; investigate referer/origin handling or a different player.
- Consider the iOS approach as reference: YouTubePlayerKit sets origin+referrer inside its own
  HTML builder and passes **no** explicit origin from app code.

## Reference

- iOS working implementation: `Views/Components/YouTubePlayerView.swift` (YouTubePlayerKit 2.0.5)
- Fallback UX already matches iOS, so the app degrades correctly while this is unresolved.
