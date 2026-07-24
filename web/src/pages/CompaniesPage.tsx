import { useEffect, useState, type FormEvent } from "react";
import { addDoc, collection, deleteDoc, doc, onSnapshot } from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { Party } from "../types";

export function CompaniesPage() {
  const { orgId } = useAuth();
  const [parties, setParties] = useState<Party[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [phone1, setPhone1] = useState("");
  const [phone2, setPhone2] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "parties"), (snapshot) => {
      const list = snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Party, "id">) }));
      list.sort((a, b) => a.name.localeCompare(b.name));
      setParties(list);
    });
    return unsubscribe;
  }, [orgId]);

  function resetForm() {
    setName("");
    setAddress("");
    setPhone1("");
    setPhone2("");
    setEmail("");
    setError(null);
    setShowForm(false);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!orgId) return;
    if (name.trim() === "") {
      setError("Firma adı gerekli.");
      return;
    }
    setSaving(true);
    try {
      await addDoc(collection(db, "organizations", orgId, "parties"), {
        name: name.trim(),
        address: address.trim(),
        phone1: phone1.trim(),
        phone2: phone2.trim(),
        email: email.trim()
      });
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Kaydedilemedi.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(partyId: string) {
    if (!orgId) return;
    await deleteDoc(doc(db, "organizations", orgId, "parties", partyId));
  }

  return (
    <div>
      <div className="page-header">
        <h1>Firmalar</h1>
        {!showForm && (
          <button className="link-button" onClick={() => setShowForm(true)}>
            + Firma ekle
          </button>
        )}
      </div>

      {showForm && (
        <form className="form-card" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="name">Firma adı</label>
            <input id="name" type="text" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="address">Adres</label>
            <input id="address" type="text" value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="phone1">Telefon 1</label>
            <input id="phone1" type="text" value={phone1} onChange={(e) => setPhone1(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="phone2">Telefon 2</label>
            <input id="phone2" type="text" value={phone2} onChange={(e) => setPhone2(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="email">E-posta</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>

          {error && <p className="error-text">{error}</p>}

          <div style={{ display: "flex", gap: 8 }}>
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? "Kaydediliyor..." : "Kaydet"}
            </button>
            <button type="button" className="secondary-button" onClick={resetForm}>
              İptal
            </button>
          </div>
        </form>
      )}

      {parties.length === 0 ? (
        <p className="empty-state">Henüz firma yok.</p>
      ) : (
        <div className="product-list">
          {parties.map((party) => (
            <div className="product-row" key={party.id}>
              <div>
                <div className="name">{party.name}</div>
                {(party.phone1 || party.email) && (
                  <div className="sku">{[party.phone1, party.email].filter(Boolean).join(" · ")}</div>
                )}
              </div>
              <button className="danger-button" onClick={() => handleDelete(party.id)}>
                Sil
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
