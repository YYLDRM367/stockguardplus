import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { addDoc, collection, onSnapshot, serverTimestamp, Timestamp } from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { OrderType, Party, Product } from "../types";

interface LineInput {
  key: number;
  productId: string;
  quantity: string;
}

function todayInputValue() {
  return new Date().toISOString().slice(0, 10);
}

export function OrderFormPage() {
  const { orgId } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const type: OrderType = searchParams.get("type") === "sale" ? "sale" : "purchase";

  const [parties, setParties] = useState<Party[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [date, setDate] = useState(todayInputValue());
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [receiptNumber, setReceiptNumber] = useState("");
  const [partyId, setPartyId] = useState("");
  const [lines, setLines] = useState<LineInput[]>([{ key: 0, productId: "", quantity: "" }]);
  const [nextKey, setNextKey] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "parties"), (snapshot) => {
      setParties(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Party, "id">) })));
    });
    return unsubscribe;
  }, [orgId]);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "products"), (snapshot) => {
      setProducts(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Product, "id">) })));
    });
    return unsubscribe;
  }, [orgId]);

  function addLine() {
    setLines((prev) => [...prev, { key: nextKey, productId: "", quantity: "" }]);
    setNextKey((k) => k + 1);
  }

  function removeLine(key: number) {
    setLines((prev) => (prev.length <= 1 ? prev : prev.filter((l) => l.key !== key)));
  }

  function updateLine(key: number, field: "productId" | "quantity", value: string) {
    setLines((prev) => prev.map((l) => (l.key === key ? { ...l, [field]: value } : l)));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!orgId) return;
    if (partyId === "") {
      setError("Firma seçimi gerekli.");
      return;
    }
    const validLines = lines
      .map((l) => ({ productId: l.productId, quantity: parseInt(l.quantity, 10) || 0 }))
      .filter((l) => l.productId !== "" && l.quantity > 0);
    if (validLines.length === 0) {
      setError("En az bir sipariş satırı gerekli.");
      return;
    }

    setError(null);
    setSaving(true);
    try {
      const dateMillis = new Date(date).getTime();
      const ref = await addDoc(collection(db, "organizations", orgId, "orders"), {
        date: Timestamp.fromMillis(dateMillis),
        invoiceNumber: invoiceNumber.trim(),
        receiptNumber: receiptNumber.trim(),
        type,
        partyId,
        status: "draft",
        lines: validLines,
        userId: orgId,
        createdAt: serverTimestamp(),
        approvedAt: null
      });
      navigate(`/orders/${ref.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Kaydedilemedi.");
      setSaving(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>{type === "purchase" ? "Alış siparişi oluştur" : "Satış siparişi oluştur"}</h1>
      </div>

      <form className="form-card" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="date">Tarih</label>
          <input id="date" type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </div>

        <div className="field">
          <label htmlFor="party">Firma</label>
          <select id="party" value={partyId} onChange={(e) => setPartyId(e.target.value)} required>
            <option value="">Seçiniz</option>
            {parties.map((party) => (
              <option key={party.id} value={party.id}>
                {party.name}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="invoiceNumber">Fatura no</label>
          <input
            id="invoiceNumber"
            type="text"
            value={invoiceNumber}
            onChange={(e) => setInvoiceNumber(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="receiptNumber">İrsaliye no</label>
          <input
            id="receiptNumber"
            type="text"
            value={receiptNumber}
            onChange={(e) => setReceiptNumber(e.target.value)}
          />
        </div>

        <label style={{ marginBottom: 6, display: "block", fontWeight: 600 }}>Sipariş satırları</label>
        {lines.map((line) => (
          <div key={line.key} style={{ display: "flex", gap: 8, marginBottom: 8 }}>
            <select
              className="search-input"
              value={line.productId}
              onChange={(e) => updateLine(line.key, "productId", e.target.value)}
            >
              <option value="">Ürün seç</option>
              {products.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name}
                </option>
              ))}
            </select>
            <input
              type="number"
              min="1"
              placeholder="Adet"
              style={{ width: 90 }}
              value={line.quantity}
              onChange={(e) => updateLine(line.key, "quantity", e.target.value)}
            />
            <button
              type="button"
              className="secondary-button"
              disabled={lines.length <= 1}
              onClick={() => removeLine(line.key)}
            >
              Kaldır
            </button>
          </div>
        ))}
        <button type="button" className="toggle-link" style={{ marginBottom: 14 }} onClick={addLine}>
          + Satır ekle
        </button>

        {error && <p className="error-text">{error}</p>}

        <button type="submit" className="primary-button" disabled={saving}>
          {saving ? "Kaydediliyor..." : "Taslak olarak kaydet"}
        </button>
      </form>
    </div>
  );
}
