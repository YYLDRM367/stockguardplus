const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");

admin.initializeApp();
const db = admin.firestore();

const PACKAGE_NAME = "com.stockguardplus.app";
// Keep in sync with the free trial offer's duration configured on each
// base plan in Play Console (see CLAUDE.md "Subscriptions / billing").
const TRIAL_DAYS = 7;

// Keep in sync with the base plan IDs configured in Play Console
// (stockguardplus_premium83 > monthly / quarterly / yearly — see
// CLAUDE.md "Subscriptions / billing").
function planFromBasePlanId(basePlanId) {
  if (basePlanId === "monthly" || basePlanId === "quarterly" || basePlanId === "yearly") {
    return basePlanId;
  }
  return null;
}

async function getAndroidPublisher() {
  const auth = new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"]
  });
  const authClient = await auth.getClient();
  return google.androidpublisher({ version: "v3", auth: authClient });
}

// Fetches the current truth for a purchase token from the Play Developer
// API and writes it onto the matching org doc. Used both right after a
// client-side purchase (verifyPurchase) and whenever Google notifies us of
// a lifecycle change (handleRtdn) — always re-fetching rather than trusting
// the notification payload keeps a single source of truth instead of two
// code paths that could drift apart.
async function syncSubscriptionForToken(purchaseToken, knownOrgId) {
  const publisher = await getAndroidPublisher();
  const res = await publisher.purchases.subscriptionsv2.get({
    packageName: PACKAGE_NAME,
    token: purchaseToken
  });
  const data = res.data;
  const lineItem = data.lineItems && data.lineItems[0];
  const basePlanId = lineItem?.offerDetails?.basePlanId || lineItem?.productId || null;
  const plan = planFromBasePlanId(basePlanId);
  const expiryTime = lineItem?.expiryTime ? new Date(lineItem.expiryTime) : null;
  const startTime = data.startTime ? new Date(data.startTime) : null;

  // Play's v2 API doesn't have a distinct "trial" state — a trial and a
  // paid period both report SUBSCRIPTION_STATE_ACTIVE. We infer trial by
  // checking whether we're still within the first 14 days of the
  // subscription's startTime, since our only offer is the 14-day trial.
  let status;
  switch (data.subscriptionState) {
    case "SUBSCRIPTION_STATE_ACTIVE": {
      const inTrialWindow =
        startTime && Date.now() - startTime.getTime() < TRIAL_DAYS * 24 * 60 * 60 * 1000;
      status = inTrialWindow ? "trial" : "active";
      break;
    }
    case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      status = "grace_period";
      break;
    case "SUBSCRIPTION_STATE_CANCELED":
      status = "canceled";
      break;
    default:
      // SUBSCRIPTION_STATE_ON_HOLD, _PAUSED, _EXPIRED, _PENDING, and
      // anything unrecognized all mean "no access".
      status = "expired";
  }

  let orgId = knownOrgId;
  if (!orgId) {
    const snapshot = await db
      .collection("organizations")
      .where("subscriptionPurchaseToken", "==", purchaseToken)
      .limit(1)
      .get();
    if (snapshot.empty) {
      console.warn("No org found for purchase token", purchaseToken);
      return;
    }
    orgId = snapshot.docs[0].id;
  }

  await db.collection("organizations").doc(orgId).update({
    subscriptionStatus: status,
    subscriptionPlan: plan,
    subscriptionExpiry: expiryTime ? admin.firestore.Timestamp.fromDate(expiryTime) : null,
    subscriptionPurchaseToken: purchaseToken,
    subscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

// Called by the Android app right after a Play Billing purchase completes.
exports.verifyPurchase = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in required.");
  }
  const purchaseToken = data.purchaseToken;
  if (!purchaseToken || typeof purchaseToken !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "purchaseToken is required.");
  }

  try {
    await syncSubscriptionForToken(purchaseToken, context.auth.uid);
  } catch (err) {
    console.error("verifyPurchase failed", err);
    throw new functions.https.HttpsError("internal", "Could not verify purchase.");
  }
  return { ok: true };
});

// Google Play Real-time Developer Notifications land here via Pub/Sub.
// The topic name must match what's configured in Play Console >
// Monetization setup > Real-time developer notifications.
exports.handleRtdn = functions.pubsub.topic("play-subscriptions-rtdn").onPublish(async (message) => {
  const payload = message.json;
  const notification = payload && payload.subscriptionNotification;
  if (!notification || !notification.purchaseToken) {
    console.log("RTDN with no subscriptionNotification, ignoring", JSON.stringify(payload));
    return;
  }

  try {
    await syncSubscriptionForToken(notification.purchaseToken, null);
  } catch (err) {
    console.error("handleRtdn failed", err);
  }
});
