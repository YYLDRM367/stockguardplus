# CLAUDE.md — StockGuard+

**Status: Auth + Firestore wired, core loop works.** Sign up (email/password,
creates an org), sign in, sign out, Dashboard, Product list, Add/edit
product, Category management, Product detail (with movement history),
Company management ("Firmalar"), barcode scanning, and Purchase/Sales orders
are all backed by real Firestore data — verified end to end 2026-07-11.
**Stock quantity is never edited directly or one product at a time** — it
only changes when a multi-line Purchase Order (stock in, from a company) or
Sales Order (stock out, to a company) is approved. An order is created as a
`draft` (a required date, optional invoice/receipt number, one company, one
or more product+quantity lines) and is immutable after creation — wrong
orders get deleted and re-entered, not edited. Approving a draft is one
Firestore transaction that
updates every line's product quantity and writes a `Movement` record per
line (rejecting the whole approval if any Sales Order line would take a
product below zero), so per-product movement history and order-level
approval can never drift apart. This replaced an earlier single-product
"Stok Girişi/Çıkışı" design that didn't match how deliveries and invoices
actually arrive (multiple line items at once) — see "Data model" below.
Low-stock/out-of-stock alerts list (reachable from the Dashboard's stat
cards, not its own bottom tab) shows a real, filtered product list —
`AlertFilter` (`ALL`/`LOW_STOCK`/`OUT_OF_STOCK`) is passed through the
`Screen.Alerts` route so the "Low stock" and "Out of stock" cards each open
the list pre-filtered to just that status, instead of both showing the same
combined list. A **Reports** bottom tab (added 2026-07-13) queries
`movements` by a date range (start/end, Firestore range filter — the only
range filter, so no composite index needed) and then filters client-side by
type (in/out), company, and/or product, showing a running
total-in/total-out summary plus the matching movement rows. Google
Sign-In is declared in the stack but not implemented
yet (needs a SHA-1 fingerprint added in the Firebase console first).

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

- `organizations/{orgId}` — name, language, subscription state:
  `subscriptionStatus` (`trial`/`active`/`grace_period`/`expired`/`canceled`),
  `subscriptionPlan` (`monthly`/`quarterly`/`yearly`/`null`),
  `subscriptionExpiry` (Timestamp, current period end),
  `subscriptionPurchaseToken` (Play Billing purchase token, used to
  re-verify/look up the purchase), `subscriptionUpdatedAt` (Timestamp).
  These 5 fields are set at doc creation with default values (`trial`-less
  defaults — no field yet until a purchase happens) but after that are
  **only writable by Cloud Functions** (Admin SDK, which bypasses
  `firestore.rules`) — see "Subscriptions / billing" below.
  `firestore.rules` blocks the client from touching them on `update`.
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
- `organizations/{orgId}/orders/{orderId}` — date (required, user-picked via
  a Material3 date picker — the actual invoice/receipt date, distinct from
  `createdAt`), invoiceNumber and receiptNumber (both free text, both
  optional — the supplier/customer's own document numbers, not
  auto-generated), type (`purchase`/`sale`), partyId (required reference
  into `parties`), status (`draft`/`approved`), lines (embedded array of
  `{productId, quantity}` — not a subcollection, since an order is always
  read/written as one atomic unit), userId, createdAt, approvedAt. Created
  as `draft` with all its lines in one write via
  `FirebaseOrderRepository.createOrder`; drafts are **not editable** — a
  wrong draft is deleted and re-created, not patched.
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

**Dark theme** (added 2026-07-19): same token names, inverted for low-light
legibility — Background `#15171B`, Surface `#1D2025`, Text `#EAECEF`, Muted
`#8B93A0`, Border `#2E3238`, Accent `#7C8BC4` (lightened so it still passes
contrast on a dark ground), Good `#4FAE72`, Warn `#D9A548`, Bad `#E2635F`.
User picks System/Light/Dark in Settings (`ThemePreferences`, mirrors
`LocalePreferences`); unlike the language picker this needs no
`activity.recreate()` — colors are a `CompositionLocal`
(`LocalStockGuardPalette`) that recomposes instantly. The existing
`PaperBackground`/`PaperSurface`/etc. top-level `val`s were converted to
`@Composable` getter properties reading from that CompositionLocal, so every
screen that already referenced them as plain `Color` values kept working
unchanged — only `ui/theme/Color.kt` and `Theme.kt` needed to change.

Shape/type: 6dp corner radius, 1.5dp borders (bumped from the original 1dp
hairline 2026-07-18 — the row cards across Products/Orders/Reports/
Dashboard/Alerts read too faint at 1dp), no gradients, generous whitespace.
Typeface is the system default (Roboto) — don't introduce a custom font.
Quantities/SKUs use tabular figures wherever digits line up in a column.
Product name and quantity in the Products list row are `FontWeight.SemiBold`
(bumped 2026-07-19 at the user's request — the regular weight read too light
against the row's other text; they may revert this later, so don't remove
the old plain-weight option's context from history).

## MVP scope

1. Onboarding + sign-in (email/password + Google) with language picker
2. Dashboard — done: total/low-stock/out-of-stock stat cards (all three
   tappable — total goes to Products, the other two to the Alerts list),
   current date/time, and a "Recent activity" list of the 5 most recent
   stock movements (tap one to jump to that product) via
   `MovementRepository.observeRecentMovements`.
3. Product list — done: search-by-name text field plus a horizontally
   scrollable category filter row (All / Uncategorized / each category);
   both combine (`ProductListViewModel` filters client-side on the already
   live `observeProducts()` stream), stock-status badge
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
8. Low-stock alerts — done: in-app list reachable from the Dashboard's
   "Low stock"/"Out of stock" stat cards, each pre-filtered to that status
   (`AlertFilter`); push notification not started.
9. Settings — language picker (EN/TR, in-app override independent of device
   locale) done; Profile section shows the signed-in account's email
   (read-only) and an in-app **Delete account** action (Play Store policy
   requires this for any app with in-app account creation) — re-
   authenticates with the account password, deletes every Firestore
   collection under `organizations/{orgId}` (products, categories, parties,
   orders, movements, members) in batches, then deletes the Firebase Auth
   user itself. Subscription status still pending (no billing yet, see
   Roadmap).
10. Reports (bottom tab) — done. Quick ranges (Today/This week/This month)
    plus custom start/end date pickers, filters for movement type
    (in/out/all), company, and product, a total-in/total-out/movement-count
    summary, and the matching movement rows. Covers: incoming stock in a
    range, outgoing stock in a range, stock from/to a specific company in a
    range, and a single product's movement history in a range — all via
    filter combinations on one screen rather than separate report screens.
    CSV export (`ReportCsvExporter.kt`) shares the currently filtered rows
    via Android's share sheet — writes to `cacheDir/exports/` and hands off
    a `content://` URI through a `FileProvider` (declared in
    AndroidManifest.xml, paths in `res/xml/file_paths.xml`); opens directly
    in Excel or Google Sheets from whatever app the user shares it to.

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

## Subscriptions / billing

Started 2026-08-01. Decided model: **no free tier** — every account starts a
paid subscription right after sign-up, with a 14-day free trial built into
the subscription itself via Google Play Billing's own trial offer (so the
user's card is captured by Google Play up front; no app-side "trial without
a card" logic to build or to abuse). Three plans, one Play Console
subscription product with three base plans:

- Product ID: `stockguardplus_premium83`
- Base plan IDs: `monthly`, `quarterly`, `yearly` — each with a 14-day free
  trial offer phase configured in Play Console (Faz 1, done 2026-08-01).

This is the first feature in the project that needs a server component —
until now both the Android app and the web app talked to Firestore directly
with no backend. A client can't be trusted to write its own
"I'm subscribed" flag (anyone could edit their own org doc otherwise), and
Google reports subscription lifecycle events (renewal, cancellation, payment
failure) via server-to-server Real-time Developer Notifications that only a
backend can receive. So this requires Cloud Functions (Firebase Blaze plan)
for the first time: one callable Function the client calls right after a
purchase to verify the purchase token against the Play Developer API and
write `organizations/{orgId}`'s subscription fields, and one Pub/Sub-
triggered Function listening for RTDN events to keep that state correct
even when the app isn't open. `firestore.rules` already blocks the client
from writing those 5 fields directly (see "Data model" above) — the Cloud
Functions use the Admin SDK, which bypasses rules, so they're the only
writer.

Billing Library dependency (`com.android.billingclient:billing-ktx`) was
added to `app/build.gradle.kts` 2026-08-01 purely to unblock Play Console —
it refused to let a subscription product be created until an uploaded
build declared the `com.android.vending.BILLING` permission, which this
dependency adds via manifest merging. No purchase-flow code exists yet.

Full phased plan (Play Console setup → data model → Cloud Functions →
Android purchase flow + paywall → web read-only status display → testing →
Play Console submission) is written up in a durable artifact the user can
reference; ask the user for the link if picking this up fresh rather than
re-deriving the plan from scratch. Current status: Faz 1 (Play Console
product/plan/trial setup), Faz 2 (Firestore schema + `firestore.rules`,
deployed), and Faz 3 (Cloud Functions) are all done. `functions/` has
`verifyPurchase` (HTTPS callable, called by the Android app right after a
purchase — live at
`https://us-central1-stockguardplus.cloudfunctions.net/verifyPurchase`)
and `handleRtdn` (Pub/Sub-triggered on topic `play-subscriptions-rtdn`,
which Play Console's monetization setup is now wired to send Real-time
Developer Notifications to) — both call a shared helper that re-fetches
the purchase's current truth from the Play Developer API
(`purchases.subscriptionsv2.get`) rather than trusting notification
payloads. Both functions run as `stockguardplus@appspot.gserviceaccount.com`
(1st-gen default), which now has Play Console financial-data access and
Pub/Sub publish rights wired to it. Deployed 2026-08-02 using a temporary
Editor-role service account key — the narrower `firebase.sdkAdminServiceAgent`
key used for Hosting/Rules deploys lacks the Cloud Build/Artifact
Registry/Functions permissions a Functions deploy needs, and even the
Editor-role key couldn't grant the final `allUsers` → Cloud Functions
Invoker IAM binding on `verifyPurchase` (Editor excludes `setIamPolicy`
calls by design) — the user did that one step manually via Google Cloud
Console using their own (Owner-level) account. Faz 4 (Android purchase
flow + paywall) is also done as of 2026-08-02: `BillingRepository`
(`PlayBillingRepository`) wraps `BillingClient` to query the 3 base plans'
offers, launch the purchase flow, and acknowledge purchases, then calls
the `verifyPurchase` Cloud Function rather than trusting the client's own
view of entitlement; `OrganizationRepository` observes
`organizations/{orgId}`'s subscription fields live. Every signed-in user
now lands on a `Paywall` screen first (`NavStartViewModel` always routes
there when `currentOrgId != null`) — it checks `hasActiveAccess`
(trial/active/grace_period) and bounces straight to Dashboard if already
covered, otherwise shows the 3 plan cards; sign-up routes here too, since
there's no free tier. Settings has an "Aboneliğim" section (status,
renewal/trial-end date, a link to Play's subscription management page).
The old `subscriptionPlan: "free"` default was removed from both
`FirebaseAuthRepository.signUp` and web's `AuthContext.signUp` — an org
now has no entitlement at all until `verifyPurchase` writes real fields.
Faz 5 (web) is also done: `AuthContext` observes `organizations/{orgId}`
live (`organization`/`orgLoading`), `hasActiveAccess()` in `types.ts`
mirrors the Android extension property exactly, and `App.tsx` gates every
authenticated route on it — not entitled renders
`SubscriptionRequiredPage` (points back to the Android app, since Play
Billing purchases can't start on web) instead of the app shell. Settings
has a matching read-only "Aboneliğim" section. **Not yet deployed to
Hosting** — deliberately held back, since deploying it now would lock out
every existing web user (no one has completed a real purchase yet to
re-test against), so it's queued to go out together with/after Faz 6
testing confirms the Android purchase flow actually grants access. Not
started: end-to-end testing with license testers (Faz 6 — not yet
confirmed license testers are added in Play Console), Play Console
monetization submission declarations (Faz 7).

## Roadmap / deferred (do not build until asked)

- Multi-location warehouse UI (schema already supports it)
- Team members / roles (schema already supports it)
- Stock-value reporting (₺) — needs a unit price/cost field on Product
  first, which doesn't exist yet
- Further reports beyond the Reports screen's date/type/company/product
  filters (see MVP scope item 10): top-moving products ranking, an
  all-companies summary table (in one view instead of picking one company
  at a time), a category-level movement summary, and an approved-orders
  list report (order no./date/company/line count, not per-line movements)
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

## Web app (`web/`)

Started 2026-07-24 — a separate frontend in the same repo, sharing the exact
same Firebase project (Firestore + Auth) as the Android app. An account
created on one platform signs in and sees live, synced data on the other;
there is no separate backend or API, both clients talk to Firestore
directly under the same security rules.

Stack: Vite + React + TypeScript, `firebase` JS SDK, React Router. The
Firebase web config in `web/src/firebase.ts` is committed directly (not
gitignored like `google-services.json`) — a web API key isn't a secret, it's
visible in any browser's network tab regardless, and access is governed by
Firestore rules, not by hiding this file.

`web/src/auth/AuthContext.tsx`'s `signUp` deliberately mirrors
`FirebaseAuthRepository.signUp` on Android field-for-field (same
`organizations/{uid}` + `members/{uid}` batch write) so an account made on
web is indistinguishable from one made on mobile. `deleteAccount` in the
same file mirrors `FirebaseAuthRepository.deleteAccount` just as closely:
re-authenticate with password, batch-delete every Firestore collection
under the org, delete the org doc, then delete the Auth user.

Feature parity as of 2026-07-24 (built in this order, per the user's
request): Ürünler (Products, incl. add/edit/search/category filter),
Kategoriler (add/rename/delete, deleting reassigns affected products to
`categoryId=""` in one batch — same as `FirebaseCategoryRepository` on
Android), Firmalar (Companies — add/delete only, no rename, matching
Android), Siparişler (Orders — Purchase/Sale tabs, multi-line draft
creation, an approve action running one client-side `runTransaction` that
updates every line's product quantity and writes a matching Movement,
rejecting the whole approval if a sale line would go below zero — mirrors
`FirebaseOrderRepository.approveOrder`), Raporlar (quick/custom date range
+ type/company/product filters + totals + CSV download via a Blob/anchor,
simpler than Android's FileProvider share-sheet since a browser can just
trigger a direct download), and Ayarlar (profile email + delete account).
Barcode scanning is intentionally not built for web. Google Sign-In,
multi-language UI, and dark theme also don't exist on web yet — none have
been requested. Run it locally with `cd web && npm install && npm run dev`.

**Live at https://stockguardplus.web.app** (Firebase Hosting, free tier —
no custom domain needed unless/until wanted later, it can be attached to
this same Hosting site anytime without touching any code) — as of
2026-07-26 all 6 features above (Ürünler through Ayarlar) are deployed and
live, verified end to end. Deploy via `firebase deploy --only hosting`
from `web/` after `npm run build`; since this dev machine can't do an
interactive `firebase login` (see the `firestore.rules` note above for
why), deploys instead authenticate with a temporary service account key
set as `GOOGLE_APPLICATION_CREDENTIALS` — generate one from Firebase
Console → Project settings → Service accounts, delete it after the deploy
finishes. `web/firebase.json` rewrites every path to `/index.html` so
React Router's client-side routes don't 404 on a direct/refreshed load.

## Working with this repo

When picking up a new feature request, check the Roadmap first — most future
asks fit into the existing schema as an additive collection + screen, not a
rewrite. Treat the "Kağıt" palette and the localization rules as
non-negotiable; ask before deviating from either.
