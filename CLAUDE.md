# CLAUDE.md — StockGuard+

**Status: pre-code.** This file records the decisions made before scaffolding
started. Nothing exists in this repo yet — the first work session should
scaffold the Android project against everything below, not re-derive it.

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
- `organizations/{orgId}/products/{productId}` — name, SKU, barcode,
  categoryId, unit, reorderPoint, photoUrl (optional)
- `organizations/{orgId}/categories/{categoryId}` — name, sortOrder
- `organizations/{orgId}/locations/{locationId}` — name (v1: single implicit
  location, schema ready for multi-warehouse)
- `organizations/{orgId}/stock/{productId_locationId}` — current quantity,
  kept as its own doc (not embedded in the product) for fast reads
- `organizations/{orgId}/movements/{movementId}` — productId, locationId,
  delta, type (`in`/`out`/`adjustment`), timestamp, userId

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
4. Product detail — stock history, barcode, adjust stock
5. Add/edit product
6. Category management (inside Settings: add/rename/delete — deleting
   reassigns affected products to "Uncategorized", no blocking dialog)
7. Quick stock in/out via barcode scan
8. Low-stock alerts (in-app list + push notification)
9. Settings — language, subscription status, account

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

## Release process

Google Play: bump `versionCode`, build the AAB, upload via Play Console.
Consider a staged rollout (10% → 50% → 100%) for anything riskier than a
copy change.

## Working with this repo

When picking up a new feature request, check the Roadmap first — most future
asks fit into the existing schema as an additive collection + screen, not a
rewrite. Treat the "Kağıt" palette and the localization rules as
non-negotiable; ask before deviating from either.
