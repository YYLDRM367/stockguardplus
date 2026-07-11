# CLAUDE.md — StockGuard+

**Status: Auth + Firestore wired, core loop works.** Sign up (email/password,
creates an org), sign in, sign out, Dashboard, Product list, Add/edit
product, Category management, Product detail (with movement history),
Company management ("Firmalar"), barcode scanning, and Purchase/Sales orders
are all backed by real Firestore data — verified end to end 2026-07-11.
**Stock quantity is never edited directly or one product at a time** — it
only changes when a multi-line Purchase Order (stock in, from a company) or
Sales Order (stock out, to a company) is approved. An order is created as a
`draft` (order/invoice number, one company, one or more product+quantity
lines) and is immutable after creation — wrong orders get deleted and
re-entered, not edited. Approving a draft is one Firestore transaction that
updates every line's product quantity and writes a `Movement` record per
line (rejecting the whole approval if any Sales Order line would take a
product below zero), so per-product movement history and order-level
approval can never drift apart. This replaced an earlier single-product
"Stok Girişi/Çıkışı" design that didn't match how deliveries and invoices
actually arrive (multiple line items at once) — see "Data model" below.
Low-stock alerts list (reachable from the Dashboard's "Low stock" stat card,
not its own bottom tab) is still a placeholder screen. Google Sign-In is
declared in the stack but not implemented yet (needs a SHA-1 fingerprint
added in the Firebase console first).

## What this is

StockGuard+ is a native Android app for warehouse/inventory stock management,
aimed at small businesses. Goal: ship a simple MVP to Google Play, get store
approval, then iterate. Monetization is subscription-based from day one.

## Platform & stack

- **Android only for now** — no iOS. Revisit after the Android app has
  traction; don't build toward iOS parity until then.
- Kotlin + Jetpack Compose (UI), MVVM
- Kotlin Coroutines + Flow for async/state
- Navigation Compose
- Hilt for dependency injection
- Firebase: Firestore (data), Auth (email/password + Google Sign-In), Cloud
  Messaging (push for low-stock alerts), Analytics, Crashlytics
- Billing: **Google Play Billing Library directly** — no RevenueCat. Single
  platform doesn't need cross-store receipt unification; reconsider only if
  iOS gets added later.
- Barcode scanning: CameraX + ML Kit Barcode Scanning

## Data model (Firestore)

Everything is scoped under an organization (`orgId`) from day one, even
though v1 ships single-user per business — retrofitting multi-tenancy later
is expensive, doing it now is nearly free.

- `organizations/{orgId}` — name, language, subscription state (plan, expiry)
- `organizations/{orgId}/members/{userId}` — role (`owner`/`employee`); only
  `owner` is used in v1, schema is ready for team invites later

**v1 shortcut:** `orgId` is literally the owner's Firebase Auth `uid` — there
is no separate lookup collection to map a user to their org. This only works
because v1 has no team invites yet. When employee accounts are built (see
Roadmap), this needs a real `uid -> orgId` lookup and `firestore.rules` (repo
root) needs updating to match — right now its `orgId == request.auth.uid`
check would block any invited employee.
- `organizations/{orgId}/products/{productId}` — name, sku, barcode,
  quantity, reorderPoint, categoryId. `categoryId` is a real reference into
  `categories` (empty string = uncategorized); deleting a category reassigns
  its products to `categoryId = ""` via a batch write in
  `FirebaseCategoryRepository.deleteCategory`. `quantity` lives directly on
  the product doc (v1 simplification — no separate per-location `stock`
  collection yet; see multi-location note below) and is only ever changed
  by `FirebaseOrderRepository.approveOrder`, never written directly or from
  a per-product screen.
- `organizations/{orgId}/categories/{categoryId}` — name, sortOrder
- `organizations/{orgId}/parties/{partyId}` — name, address, phone1, phone2,
  email. A flat list of companies — supplier or customer, no type
  distinction (matches how small businesses actually use one contact list
  for both). Selected on every order; required, not optional.
- `organizations/{orgId}/orders/{orderId}` — orderNumber (free text, the
  supplier/customer's own invoice or PO number — not auto-generated), type
  (`purchase`/`sale`), partyId (required reference into `parties`), status
  (`draft`/`approved`), lines (embedded array of `{productId, quantity}` —
  not a subcollection, since an order is always read/written as one atomic
  unit), userId, createdAt, approvedAt. Created as `draft` with all its
  lines in one write via `FirebaseOrderRepository.createOrder`; drafts are
  **not editable** — a wrong draft is deleted and re-created, not patched.
  `FirebaseOrderRepository.approveOrder` runs one Firestore transaction that,
  for every line, updates that product's `quantity` (add for `purchase`,
  subtract for `sale`) and writes a matching `Movement` doc, then flips the
  order to `approved`; a `sale` line that would take a product below zero
  throws `InsufficientStockException` and aborts the *entire* order (no
  partial approval). Once `approved`, an order is immutable.
- `organizations/{orgId}/movements/{movementId}` — productId, type
  (`in`/`out`), quantity, partyId, orderId (which order produced this line),
  userId, timestamp (server timestamp). Write-only from
  `FirebaseOrderRepository.approveOrder` (one per approved order line) —
  there is no path that writes a movement outside of order approval.
  Movement history per product is fetched by `productId` only and sorted
  client-side (avoids needing a Firestore composite index for a `productId`
  + `timestamp` query).
- `organizations/{orgId}/locations/{locationId}` — not implemented yet
  (planned for multi-warehouse, see Roadmap); `quantity` above assumes a
  single implicit location until that lands.

Firestore security rules must filter every query by `orgId` — that's the
only real data-isolation boundary between businesses. Don't add a feature
that reads/writes without going through that filter.

## Design system — "Kağıt" direction

Light, minimal, corporate-trust mood. Chosen over four alternatives (dark
technical, warm boutique, dark navy/gold, warm industrial).

Colors:

| Token | Hex | Use |
|---|---|---|
| Background | `#F4F6F8` | screen background |
| Surface | `#FFFFFF` | cards, rows, inputs |
| Text | `#1F2933` | primary text |
| Muted | `#788492` | secondary text, labels |
| Border | `#E1E6EA` | hairline borders |
| Accent | `#3B4B8C` | primary actions, active states — used sparingly, not decorative |
| Good | `#2F855A` | in-stock |
| Warn | `#B7791F` | low stock |
| Bad | `#C53030` | out of stock |

Shape/type: 6dp corner radius, 1dp hairline borders, no gradients, generous
whitespace. Typeface is the system default (Roboto) — don't introduce a
custom font. Quantities/SKUs use tabular figures wherever digits line up in
a column.

## MVP scope

1. Onboarding + sign-in (email/password + Google) with language picker
2. Dashboard — total products, low-stock count, out-of-stock count
3. Product list — search, category filter, stock-status badge
4. Product detail — done: read-only quantity + per-product movement history.
   No in/out actions here — see item 7.
5. Add/edit product — both done; barcode field + scan-to-fill done; editing
   never touches quantity (see item 7)
6. Category management (inside Settings: add/rename/delete — deleting
   reassigns affected products to "Uncategorized", no blocking dialog)
6b. Company management (inside Settings, "Firmalar": add/delete — no
    rename yet). Required on every order.
7. Purchase Orders / Sales Orders (bottom tab "Orders", two sub-tabs) — done.
   Multi-line orders against a company, created as an immutable draft then
   approved (approval is the only way stock quantity changes — see "Data
   model"). Barcode scan on the product list jumps straight to a matched
   product's detail screen, or offers to create a new product with that
   barcode if nothing matches.
8. Low-stock alerts — in-app list still a placeholder screen (reachable from
   the Dashboard's "Low stock" stat card); push notification not started.
9. Settings — language picker (EN/TR, in-app override independent of device
   locale) done; subscription status still pending (no billing yet, see
   Roadmap)

Explicitly **out of scope for v1** — see Roadmap below. Don't build these
unless asked, even if they seem like a natural extension of a screen above.

## Localization

Target languages: **English and Turkish are fully translated for v1.**
Spanish, French, and German are planned but placeholder-only until
translated — see Roadmap.

Rules to follow in any UI code, starting now, even before those three extra
languages are translated:

- No hardcoded user-facing strings — everything goes through `strings.xml` /
  `values-<lang>/strings.xml`.
- No fixed-width text containers — German strings run ~30–40% longer than
  Turkish/English equivalents; layouts must reflow, never truncate.
- All numbers, dates, and currency go through `Locale`-aware formatters
  (`NumberFormat`, `DateFormat`) — never hand-build separators.
- Default language follows device locale; user can override it in Settings.

## Roadmap / deferred (do not build until asked)

- Multi-location warehouse UI (schema already supports it)
- Team members / roles (schema already supports it)
- Supplier + purchase-order tracking
- Reporting / analytics (stock-value trend, movement-history charts)
- CSV / Excel import-export
- Spanish, French, German translations
- iOS port

## Local setup (first run)

Android Studio, the Android SDK, and a real Gradle wrapper are already set
up on the dev machine and `gradlew assembleDebug` is verified working.
compileSdk/targetSdk are 35; the installed SDK also has platform 36.1 if a
future bump is needed.

1. Open this folder in **Android Studio** — it should sync without needing
   to regenerate the wrapper.
2. Firebase is wired: a project exists, `google-services.json` is in `app/`
   (gitignored — every dev machine needs their own copy from the Firebase
   console), Firestore and Email/Password Auth are enabled. `firestore.rules`
   (repo root) is deployed to the live Rules release as of 2026-07-11 (pushed
   via the Firebase Rules REST API using a temporary service account key,
   since the CLI's `firebase login` needs a real interactive browser session
   this dev setup doesn't have) — test mode is closed. Any future change to
   `firestore.rules` needs to be redeployed the same way (or via
   `firebase deploy --only firestore:rules` from a machine that can do
   interactive `firebase login`); editing the file alone does nothing.
3. Launcher icon is the real StockGuard+ mark as of 2026-07-11 (shield +
   boxes, indigo `#3B4B8C` on `#F4F6F8` — matches the Kağıt palette exactly):
   `drawable-nodpi/ic_launcher_foreground.png` (background-removed via a
   PowerShell/GDI+ color-key script, since this dev machine has neither
   ImageMagick nor Python/PIL) + `drawable/ic_launcher_background.xml` set to
   `#F4F6F8`. `store-assets/` (repo root, not part of the app build) holds
   the Play Console listing assets: `playstore_icon_512.png` (512×512) and
   `feature_graphic_1024x500.png` (center-cropped from the original
   1024×572 export to hit Play's exact 1024×500 spec). Store screenshots
   still need to be captured from the running app before submission.
4. `TopAppBar` needs `ExperimentalMaterial3Api` opt-in — this is enabled
   project-wide via `freeCompilerArgs` in `app/build.gradle.kts`, don't
   re-add per-function `@OptIn` annotations for it.
5. To install on a physical device: enable Developer Options + USB debugging
   on the phone, connect via USB, confirm the RSA prompt on the device, then
   `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Release process

`app/build.gradle.kts` reads release signing from `keystore.properties`
(repo root, gitignored) if it exists — see "Generate the release keystore"
below for the one-time setup. Without that file, `bundleRelease` still
compiles but produces an unsigned AAB Play Console will reject.

### Generate the release keystore (one-time, do this yourself — see why below)

The keystore signs every future update to this app. Lose it (or its
passwords) and you may permanently lose the ability to publish updates
under this app's identity — this is the one credential in this project
Claude should not generate and hold on your behalf. Two ways to create it:

**Android Studio (recommended — handles passwords without them touching
shell history or a terminal):**
1. Build menu → Generate Signed Bundle / APK → Android App Bundle → Next.
2. Under "Key store path", click **Create new...**
3. Pick a save path *outside* the repo (or inside it — it's gitignored
   either way, but outside is safer against an accidental `git add -A`),
   set a store password, key alias (e.g. `stockguardplus`), key password,
   validity (25+ years — Google itself recommends this so it outlives the
   app), and your org/name details for the certificate.
4. Finish creating the key, then continue the wizard: select `release`
   build variant → Finish. Studio builds a signed AAB directly.

**Command line (if you'd rather not use the Studio wizard):**
```
keytool -genkey -v -keystore stockguardplus-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias stockguardplus
```
`keytool` ships with the JDK (Android Studio's bundled JBR has it too, at
`C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`).

**Either way, immediately after:** save the keystore file and its two
passwords + alias in a password manager. Then create `keystore.properties`
in the repo root (gitignored, already wired into `build.gradle.kts`):
```
storeFile=C:\\path\\to\\stockguardplus-release.jks
storePassword=...
keyAlias=stockguardplus
keyPassword=...
```
From then on, `./gradlew bundleRelease` produces a signed AAB at
`app/build/outputs/bundle/release/app-release.aab`, ready to upload.

**Play App Signing:** on your *first* upload to Play Console, opt into
**Play App Signing** (it's the default now) — Google then holds the actual
signing key and your local keystore becomes an "upload key" instead. If you
ever lose the upload key, Play Console support can reset it; without Play
App Signing, losing the keystore is unrecoverable. Opt in.

### Every release after that

Bump `versionCode` (and `versionName` if it's a user-visible version bump)
in `app/build.gradle.kts`, `./gradlew bundleRelease`, upload the AAB via
Play Console. Consider a staged rollout (10% → 50% → 100%) for anything
riskier than a copy change.

## Working with this repo

When picking up a new feature request, check the Roadmap first — most future
asks fit into the existing schema as an additive collection + screen, not a
rewrite. Treat the "Kağıt" palette and the localization rules as
non-negotiable; ask before deviating from either.
