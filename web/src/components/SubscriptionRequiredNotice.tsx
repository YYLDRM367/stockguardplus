// Google Play Billing purchases can only start on Android — unlike the
// mobile app's Paywall screen, there's nowhere on web to route a blocked
// write action to. This just explains that and points back to the app.
export function SubscriptionRequiredNotice({ onDismiss }: { onDismiss: () => void }) {
  return (
    <div className="form-card">
      <p>
        Bu işlem için aktif bir abonelik gerekiyor. Abonelikler yalnızca Android uygulaması üzerinden,
        Google Play ile başlatılabiliyor — mobil uygulamayı aç, bir plan seç ve aboneliğini başlat, sonra
        buradan da düzenleme yapabilirsin.
      </p>
      <button type="button" className="secondary-button" onClick={onDismiss}>
        Anladım
      </button>
    </div>
  );
}
