import { useEffect, useMemo, useState } from "react";
import { collection, onSnapshot, query, where } from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { Movement, Party, Product } from "../types";

type QuickRange = "today" | "week" | "month" | "custom";
type TypeFilter = "all" | "in" | "out";

function startOfToday(): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}

function endOfToday(): Date {
  const d = new Date();
  d.setHours(23, 59, 59, 999);
  return d;
}

function toInputValue(d: Date): string {
  return d.toISOString().slice(0, 10);
}

export function ReportsPage() {
  const { orgId } = useAuth();
  const [quickRange, setQuickRange] = useState<QuickRange>("today");
  const [startDate, setStartDate] = useState(toInputValue(startOfToday()));
  const [endDate, setEndDate] = useState(toInputValue(endOfToday()));
  const [typeFilter, setTypeFilter] = useState<TypeFilter>("all");
  const [partyId, setPartyId] = useState("");
  const [productId, setProductId] = useState("");

  const [movements, setMovements] = useState<Movement[]>([]);
  const [loading, setLoading] = useState(true);
  const [parties, setParties] = useState<Party[]>([]);
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    if (!orgId) return;
    const unsubParties = onSnapshot(collection(db, "organizations", orgId, "parties"), (snapshot) => {
      setParties(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Party, "id">) })));
    });
    const unsubProducts = onSnapshot(collection(db, "organizations", orgId, "products"), (snapshot) => {
      setProducts(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Product, "id">) })));
    });
    return () => {
      unsubParties();
      unsubProducts();
    };
  }, [orgId]);

  function applyQuickRange(range: QuickRange) {
    setQuickRange(range);
    const now = new Date();
    let start: Date;
    if (range === "today") {
      start = startOfToday();
    } else if (range === "week") {
      start = new Date(now);
      const day = start.getDay();
      const diff = day === 0 ? 6 : day - 1; // week starts Monday
      start.setDate(start.getDate() - diff);
      start.setHours(0, 0, 0, 0);
    } else if (range === "month") {
      start = new Date(now.getFullYear(), now.getMonth(), 1);
    } else {
      return;
    }
    setStartDate(toInputValue(start));
    setEndDate(toInputValue(endOfToday()));
  }

  // Single range filter on timestamp — same trick as Android's
  // observeMovementsInRange — avoids a composite index, since type/party/
  // product are then filtered client-side below.
  useEffect(() => {
    if (!orgId) return;
    setLoading(true);
    const startMillis = new Date(startDate + "T00:00:00").getTime();
    const endMillis = new Date(endDate + "T23:59:59.999").getTime();
    const q = query(
      collection(db, "organizations", orgId, "movements"),
      where("timestamp", ">=", new Date(startMillis)),
      where("timestamp", "<=", new Date(endMillis))
    );
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Movement, "id">) }));
      list.sort((a, b) => (b.timestamp?.seconds ?? 0) - (a.timestamp?.seconds ?? 0));
      setMovements(list);
      setLoading(false);
    });
    return unsubscribe;
  }, [orgId, startDate, endDate]);

  const filteredMovements = useMemo(() => {
    return movements.filter((m) => {
      const typeMatches = typeFilter === "all" || m.type === typeFilter;
      const partyMatches = partyId === "" || m.partyId === partyId;
      const productMatches = productId === "" || m.productId === productId;
      return typeMatches && partyMatches && productMatches;
    });
  }, [movements, typeFilter, partyId, productId]);

  const summary = useMemo(() => {
    const totalIn = filteredMovements.filter((m) => m.type === "in").reduce((sum, m) => sum + m.quantity, 0);
    const totalOut = filteredMovements.filter((m) => m.type === "out").reduce((sum, m) => sum + m.quantity, 0);
    return { totalIn, totalOut, count: filteredMovements.length };
  }, [filteredMovements]);

  const partyNameById = new Map(parties.map((p) => [p.id, p.name]));
  const productNameById = new Map(products.map((p) => [p.id, p.name]));

  function downloadCsv() {
    const header = ["Tarih", "Ürün", "Tür", "Adet", "Firma"];
    const rows = filteredMovements.map((m) => [
      m.timestamp ? new Date(m.timestamp.seconds * 1000).toLocaleString("tr-TR") : "",
      productNameById.get(m.productId) || m.productId,
      m.type === "in" ? "Giriş" : "Çıkış",
      String(m.quantity),
      partyNameById.get(m.partyId) || ""
    ]);
    const csv = [header, ...rows].map((r) => r.map((v) => `"${v.replace(/"/g, '""')}"`).join(",")).join("\n");
    const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `stockguard-rapor-${startDate}_${endDate}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div>
      <div className="page-header">
        <h1>Raporlar</h1>
        <button className="link-button" onClick={downloadCsv} disabled={filteredMovements.length === 0}>
          CSV indir
        </button>
      </div>

      <div className="filters-row">
        <div className="chip-row">
          <button className={`chip ${quickRange === "today" ? "selected" : ""}`} onClick={() => applyQuickRange("today")}>
            Bugün
          </button>
          <button className={`chip ${quickRange === "week" ? "selected" : ""}`} onClick={() => applyQuickRange("week")}>
            Bu hafta
          </button>
          <button className={`chip ${quickRange === "month" ? "selected" : ""}`} onClick={() => applyQuickRange("month")}>
            Bu ay
          </button>
        </div>

        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input
            type="date"
            value={startDate}
            onChange={(e) => {
              setQuickRange("custom");
              setStartDate(e.target.value);
            }}
          />
          <input
            type="date"
            value={endDate}
            onChange={(e) => {
              setQuickRange("custom");
              setEndDate(e.target.value);
            }}
          />
        </div>

        <div className="chip-row">
          <button className={`chip ${typeFilter === "all" ? "selected" : ""}`} onClick={() => setTypeFilter("all")}>
            Tümü
          </button>
          <button className={`chip ${typeFilter === "in" ? "selected" : ""}`} onClick={() => setTypeFilter("in")}>
            Giriş
          </button>
          <button className={`chip ${typeFilter === "out" ? "selected" : ""}`} onClick={() => setTypeFilter("out")}>
            Çıkış
          </button>
        </div>

        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <select value={partyId} onChange={(e) => setPartyId(e.target.value)}>
            <option value="">Tüm firmalar</option>
            {parties.map((party) => (
              <option key={party.id} value={party.id}>
                {party.name}
              </option>
            ))}
          </select>
          <select value={productId} onChange={(e) => setProductId(e.target.value)}>
            <option value="">Tüm ürünler</option>
            {products.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="detail-grid" style={{ marginBottom: 16 }}>
        <div className="detail-field">
          <div className="label">Toplam giriş</div>
          <div className="value">{summary.totalIn}</div>
        </div>
        <div className="detail-field">
          <div className="label">Toplam çıkış</div>
          <div className="value">{summary.totalOut}</div>
        </div>
        <div className="detail-field">
          <div className="label">Hareket sayısı</div>
          <div className="value">{summary.count}</div>
        </div>
      </div>

      {loading ? (
        <p className="empty-state">Yükleniyor...</p>
      ) : filteredMovements.length === 0 ? (
        <p className="empty-state">Bu filtrelere uyan hareket yok.</p>
      ) : (
        filteredMovements.map((m) => (
          <div className="movement-row" key={m.id}>
            <div>
              <div>{productNameById.get(m.productId) || m.productId}</div>
              <div className="sku">
                {[
                  m.timestamp ? new Date(m.timestamp.seconds * 1000).toLocaleString("tr-TR") : "",
                  partyNameById.get(m.partyId) || ""
                ]
                  .filter(Boolean)
                  .join(" · ")}
              </div>
            </div>
            <span className={`qty ${m.type}`}>
              {m.type === "in" ? "+" : "-"}
              {m.quantity}
            </span>
          </div>
        ))
      )}
    </div>
  );
}
