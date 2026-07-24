# StockGuard+ Web

Web portal for StockGuard+, sharing the same Firebase project (Firestore +
Auth) as the Android app — an account created on mobile signs in here with
the same email/password and sees the same live data, and vice versa.

Stack: Vite + React + TypeScript, Firebase JS SDK, React Router.

## Running locally

```
npm install
npm run dev
```

## Current scope

- Sign up / sign in (mirrors the mobile app's `organizations/{uid}` +
  `members/{uid}` write on sign-up, so accounts are identical either way)
- Dashboard: live product count / low-stock / out-of-stock stats and a
  product list, read straight from `organizations/{orgId}/products` via
  `onSnapshot`

Not built yet: Categories, Companies, Orders, Reports, Settings, product
add/edit. See the root `CLAUDE.md` for the full data model this reads from.
