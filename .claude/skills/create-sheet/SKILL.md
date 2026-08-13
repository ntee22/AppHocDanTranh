---
name: create-sheet
description: Use when adding, reordering, or hiding sheet music in the Dan Thanh Guzheng app's Firestore `sheets` collection — triggered by a sheet title plus Google Drive image links, "add this sheet", "put this at the top", or a new sheet not appearing in the app.
---

# Create Sheet

## Overview

Writes sheet-music documents straight into the `sheets` collection of Firebase project
`apphocdantranh`, so new sheets appear in the app with no code change and no App Store
release. The app reads this collection live.

## When to Use

- Partner supplies a sheet title + Google Drive image link(s) + free/paid
- A sheet needs to move to the top of the library
- A sheet must be hidden without deleting it (`isVisible: false`)
- A sheet was added but isn't showing in the app (see Silent Failures)

**Not for:** changing how sheets are *displayed* — that's `SheetMusicView.swift`.

## Setup

Requires a Firebase admin key at `.secrets/sa.json` (repo root, gitignored, `chmod 600`).
Get one from Firebase Console → Project Settings → Service Accounts → Generate new
private key. There is no `firebase`/`gcloud` CLI on this machine; these scripts use
`firebase-admin` over Node.

```bash
cd .claude/skills/create-sheet/scripts && npm install   # first run only
```

## Schema

Exactly five fields. Match it exactly — extra fields are ignored, missing ones break the doc.

| Field | Type | Notes |
|---|---|---|
| `title` | string | Display name |
| `pages` | array\<string\> | Image URLs, in page order |
| `isFree` | bool | Defaults `true` if absent |
| `isVisible` | bool | **Must be `true`** or the app hides the doc |
| `order` | number | **Must be a whole number**; ascending = top to bottom |

Doc IDs follow `sheet_0NN`, zero-padded, continuing from the highest existing.

## Silent Failures

Both of these make a sheet vanish with **no error anywhere** — the usual cause of
"I added it but it's not showing":

1. **`isVisible` not exactly `true`** — filtered client-side (`SheetMusicView.swift:89`).
2. **Fractional `order`** (e.g. `15.5`) — parsed as `as? Int`, so the doc is dropped
   (`SheetMusicView.swift:93`). Integers only, unless the app is patched first.

A third, subtler one: a Drive file **not shared "Anyone with the link"** returns an HTML
login page with HTTP 200, which renders as a blank sheet. `add-sheets.js` checks
content-type to catch this, and refuses to commit if any image fails.

## Ordering

New sheets go on top using non-positive integers below every existing order
(`0`, `-1`, `-2`, …). Existing docs are never renumbered. `add-sheets.js` computes this
automatically; the entry listed **first** ends up **topmost**.

Note the original `sheet_001`–`sheet_017` are ordered reverse-alphabetically (Z→A,
orders 1–17). Top-placement was a deliberate choice to avoid touching the app, and it
breaks that scheme.

**Inserting between two existing sheets needs fractional orders, which requires an app
patch first.** In `SheetMusicView.swift`: make `order` a `Double` in `SheetItem`, accept
both `Int` and `Double` when parsing, and bump `cacheKey` to `cached_sheets_v2` (the
`Codable` shape changes, so v1 cache fails to decode). Then relax the
`Number.isInteger` check in `add-sheets.js` in the same step, or the script will keep
refusing the input. Don't write a fractional order before that ships.

`VideoRepository.swift:96` looks like the pattern to copy but **is not** — it does
`order = Int(f)` into an `Int` field, truncating `15.5` to `15` and tying with
`sheet_015`. Keep `order` a `Double` end to end.

If mid-list inserts become routine, the durable alternative is a one-time renumber to
spaced integers (10, 20, 30, …), which removes the app-patch dependency permanently. For
a position near the tail, renumbering just the two or three docs below it is cheaper than
a release — a full-collection rewrite is not required.

## Procedure

1. Write the input JSON (any path; defaults to `scripts/new-sheets.json`):

```json
[
  {
    "title": "Em thua cô ta",
    "isFree": true,
    "pages": [
      "https://drive.google.com/file/d/FILE_ID_PAGE_1/view?usp=sharing",
      "https://drive.google.com/file/d/FILE_ID_PAGE_2/view?usp=sharing"
    ]
  }
]
```

Raw Drive share links are correct — `directImageURL()` (`SheetMusicView.swift:17`)
converts them to `lh3.googleusercontent.com/d/ID` at load time.

2. **Dry run, and show the partner the output.** Writes nothing; prints the image check,
   every planned doc, and the resulting order sequence.

```bash
node add-sheets.js                    # or: node add-sheets.js /path/to/input.json
```

3. Commit only after the plan looks right and all images pass:

```bash
node add-sheets.js --commit
```

It writes via `batch.create()`, so a doc-ID collision fails loudly instead of
overwriting an existing sheet, and it re-reads the collection afterward to verify.

4. Report the resulting ids and orders.

## Auditing

`node inspect.js` prints every doc with its order and field types, and flags any doc the
app is silently skipping. Use it first when a sheet isn't appearing.

## Common Mistakes

| Mistake | Consequence |
|---|---|
| Committing without a dry run | Unreviewed writes to the live production database |
| Using `.set()` instead of `.create()` | Silently overwrites an existing sheet |
| Renumbering existing docs to insert at top | Needless mass write; use negative orders |
| Fractional `order` to insert mid-list | Sheet disappears until the app is patched |
| Forgetting `isVisible: true` | Sheet disappears |
| Assuming HTTP 200 means the image works | Restricted Drive files 200 with HTML |

## Notes

- The app caches sheets in `UserDefaults` (`cached_sheets_v1`) and calls `loadCache()`
  before fetching, so on launch the old list flashes briefly before new sheets appear.
  Expected, not a bug.
- `.secrets/sa.json` is a **full-admin** credential for the whole Firebase project.
  Only ever use it to touch `sheets`, and never print its contents or commit it.
