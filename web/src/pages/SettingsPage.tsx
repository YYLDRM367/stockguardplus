import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";

const statusLabels: Record<string, string> = {
  trial: "Ücretsiz deneme",
  active: "Aktif",
  grace_period: "Ödeme sorunu — tekrar deneniyor",
  expired: "Süresi doldu",
  canceled: "İptal edildi"
};

const planLabels: Record<string, string> = {
  monthly: "Aylık",
  quarterly: "3 aylık",
  yearly: "Yıllık"
};

export function SettingsPage() {
  const { user, organization, deleteAccount } = useAuth();
  const [confirming, setConfirming] = useState(false);
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  async function handleDelete(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setDeleting(true);
    try {
      await deleteAccount(password);
      // Firebase auth state change will redirect to /login automatically.
    } catch (err) {
      setError(err instanceof Error ? err.message : "Hesap silinemedi.");
      setDeleting(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Ayarlar</h1>
      </div>

      <h2 style={{ fontSize: "1rem", marginBottom: 10 }}>Profil</h2>
      <div className="detail-grid" style={{ marginBottom: 24 }}>
        <div className="detail-field">
          <div className="label">E-posta</div>
          <div className="value">{user?.email}</div>
        </div>
      </div>

      <h2 style={{ fontSize: "1rem", marginBottom: 10 }}>Aboneliğim</h2>
      <div className="detail-grid" style={{ marginBottom: 24 }}>
        <div className="detail-field">
          <div className="label">Durum</div>
          <div className="value">
            {organization?.subscriptionStatus ? statusLabels[organization.subscriptionStatus] : "Aktif değil"}
          </div>
        </div>
        {organization?.subscriptionPlan && (
          <div className="detail-field">
            <div className="label">Plan</div>
            <div className="value">{planLabels[organization.subscriptionPlan]}</div>
          </div>
        )}
        {organization?.subscriptionExpiry && (
          <div className="detail-field">
            <div className="label">
              {organization.subscriptionStatus === "trial" ? "Deneme bitiş" : "Yenilenme"}
            </div>
            <div className="value">
              {new Date(organization.subscriptionExpiry.seconds * 1000).toLocaleDateString("tr-TR")}
            </div>
          </div>
        )}
      </div>
      <p className="field-hint" style={{ marginBottom: 24 }}>
        Abonelik yalnızca Android uygulaması üzerinden, Google Play ile yönetilir.
      </p>

      <h2 style={{ fontSize: "1rem", marginBottom: 10 }}>Hesabı sil</h2>
      {!confirming ? (
        <button className="danger-button" onClick={() => setConfirming(true)}>
          Hesabımı sil
        </button>
      ) : (
        <form className="form-card" onSubmit={handleDelete}>
          <p>
            Bu işlem geri alınamaz: tüm ürünler, kategoriler, firmalar, siparişler ve stok hareketleri kalıcı
            olarak silinir. Onaylamak için şifreni gir.
          </p>
          <div className="field">
            <label htmlFor="confirmPassword">Şifre</label>
            <input
              id="confirmPassword"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <div style={{ display: "flex", gap: 8 }}>
            <button type="submit" className="danger-button" disabled={deleting}>
              {deleting ? "Siliniyor..." : "Kalıcı olarak sil"}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => {
                setConfirming(false);
                setPassword("");
                setError(null);
              }}
            >
              İptal
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
