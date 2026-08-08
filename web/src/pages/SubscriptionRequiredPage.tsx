import { useAuth } from "../auth/AuthContext";

// Google Play Billing purchases can only start on Android — this page never
// tries to collect payment itself, it just explains that and points back
// to the mobile app, then shows current status once one exists.
export function SubscriptionRequiredPage() {
  const { organization, signOut } = useAuth();
  const status = organization?.subscriptionStatus;

  const statusLabel =
    status === "expired"
      ? "Aboneliğinin süresi doldu."
      : status === "canceled"
        ? "Aboneliğin iptal edildi."
        : "Henüz aktif bir aboneliğin yok.";

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>StockGuard+</h1>
        <p className="subtitle">{statusLabel}</p>
        <p style={{ marginBottom: 16 }}>
          Abonelikler yalnızca Android uygulaması üzerinden, Google Play ile başlatılabiliyor. Mobil
          uygulamayı aç, bir plan seç ve 14 günlük ücretsiz denemeni başlat — abonen aktif olduğu anda
          buradan da tüm verilerine erişebilirsin.
        </p>
        <button type="button" className="secondary-button" onClick={() => signOut()}>
          Çıkış yap
        </button>
      </div>
    </div>
  );
}
