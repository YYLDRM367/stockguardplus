import { useEffect, useState } from "react";
import { collection, onSnapshot } from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import { productStatus, type Product } from "../types";

export function DashboardPage() {
  const { user, orgId, signOut } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(
      collection(db, "organizations", orgId, "products"),
      (snapshot) => {
        setProducts(
          snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as Omit<Product, "id">) }))
        );
        setLoading(false);
      }
    );
    return unsubscribe;
  }, [orgId]);

  const total = products.length;
  const lowStock = products.filter((p) => productStatus(p) === "LOW_STOCK").length;
  const outOfStock = products.filter((p) => productStatus(p) === "OUT_OF_STOCK").length;

  return (
    <div className="page">
      <div className="top-bar">
        <h1>StockGuard+</h1>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <span style={{ fontSize: "0.85rem", color: "var(--paper-muted)" }}>{user?.email}</span>
          <button onClick={() => signOut()}>Çıkış yap</button>
        </div>
      </div>

      <div className="content">
        <div className="stat-row">
          <div className="stat-card">
            <div className="value">{total}</div>
            <div className="label">Ürün</div>
          </div>
          <div className="stat-card">
            <div className="value" style={{ color: "var(--stock-warn)" }}>
              {lowStock}
            </div>
            <div className="label">Az Stok</div>
          </div>
          <div className="stat-card">
            <div className="value" style={{ color: "var(--stock-bad)" }}>
              {outOfStock}
            </div>
            <div className="label">Tükendi</div>
          </div>
        </div>

        {loading ? (
          <p className="empty-state">Yükleniyor...</p>
        ) : products.length === 0 ? (
          <p className="empty-state">Henüz ürün yok. Mobil uygulamadan ekleyebilirsin.</p>
        ) : (
          <div className="product-list">
            {products.map((product) => {
              const status = productStatus(product);
              const statusLabel =
                status === "OUT_OF_STOCK" ? "Tükendi" : status === "LOW_STOCK" ? "Az Kaldı" : "Stokta";
              const statusClass =
                status === "OUT_OF_STOCK" ? "out-of-stock" : status === "LOW_STOCK" ? "low-stock" : "in-stock";
              return (
                <div className="product-row" key={product.id}>
                  <div>
                    <div className="name">{product.name}</div>
                    <div className="sku">{product.sku}</div>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <span style={{ fontWeight: 600 }}>{product.quantity}</span>
                    <span className={`status-chip ${statusClass}`}>{statusLabel}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
