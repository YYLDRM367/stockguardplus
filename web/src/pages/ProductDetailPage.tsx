import { useEffect, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { collection, deleteDoc, doc, onSnapshot, query, where } from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import { productStatus, type Category, type Movement, type Product } from "../types";

export function ProductDetailPage() {
  const { orgId } = useAuth();
  const { id } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState<Product | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [movements, setMovements] = useState<Movement[]>([]);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  useEffect(() => {
    if (!orgId || !id) return;
    const unsubscribe = onSnapshot(doc(db, "organizations", orgId, "products", id), (snapshot) => {
      if (snapshot.exists()) {
        setProduct({ id: snapshot.id, ...(snapshot.data() as Omit<Product, "id">) });
      } else {
        setProduct(null);
      }
    });
    return unsubscribe;
  }, [orgId, id]);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "categories"), (snapshot) => {
      setCategories(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Category, "id">) })));
    });
    return unsubscribe;
  }, [orgId]);

  useEffect(() => {
    if (!orgId || !id) return;
    const q = query(collection(db, "organizations", orgId, "movements"), where("productId", "==", id));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Movement, "id">) }));
      list.sort((a, b) => (b.timestamp?.seconds ?? 0) - (a.timestamp?.seconds ?? 0));
      setMovements(list.slice(0, 20));
    });
    return unsubscribe;
  }, [orgId, id]);

  async function handleDelete() {
    if (!orgId || !id) return;
    await deleteDoc(doc(db, "organizations", orgId, "products", id));
    navigate("/products");
  }

  if (!product) {
    return <p className="empty-state">Bu ürün artık mevcut değil.</p>;
  }

  const status = productStatus(product);
  const statusLabel = status === "OUT_OF_STOCK" ? "Tükendi" : status === "LOW_STOCK" ? "Az Kaldı" : "Stokta";
  const statusClass = status === "OUT_OF_STOCK" ? "out-of-stock" : status === "LOW_STOCK" ? "low-stock" : "in-stock";
  const categoryName = categories.find((c) => c.id === product.categoryId)?.name || "Kategorisiz";

  return (
    <div>
      <div className="page-header">
        <h1>{product.name}</h1>
        <div style={{ display: "flex", gap: 8 }}>
          <Link className="secondary-button" to={`/products/${product.id}/edit`}>
            Düzenle
          </Link>
          <button className="danger-button" onClick={() => setConfirmingDelete(true)}>
            Sil
          </button>
        </div>
      </div>

      <div className="detail-grid">
        <div className="detail-field">
          <div className="label">SKU</div>
          <div className="value">{product.sku}</div>
        </div>
        <div className="detail-field">
          <div className="label">Durum</div>
          <div className="value">
            <span className={`status-chip ${statusClass}`}>{statusLabel}</span>
          </div>
        </div>
        <div className="detail-field">
          <div className="label">Kategori</div>
          <div className="value">{categoryName}</div>
        </div>
        <div className="detail-field">
          <div className="label">Yeniden sipariş noktası</div>
          <div className="value">{product.reorderPoint}</div>
        </div>
        <div className="detail-field">
          <div className="label">Miktar</div>
          <div className="value">{product.quantity}</div>
        </div>
      </div>

      <h2 style={{ fontSize: "1rem", marginBottom: 10 }}>Stok Hareketleri</h2>
      {movements.length === 0 ? (
        <p className="empty-state">Henüz hareket yok.</p>
      ) : (
        movements.map((movement) => (
          <div className="movement-row" key={movement.id}>
            <span>{movement.type === "in" ? "Giriş" : "Çıkış"}</span>
            <span className={`qty ${movement.type}`}>
              {movement.type === "in" ? "+" : "-"}
              {movement.quantity}
            </span>
          </div>
        ))
      )}

      {confirmingDelete && (
        <div className="form-card" style={{ marginTop: 16 }}>
          <p>Bu ürünü silmek istediğine emin misin? Bu işlem geri alınamaz.</p>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="danger-button" onClick={handleDelete}>
              Sil
            </button>
            <button className="secondary-button" onClick={() => setConfirmingDelete(false)}>
              İptal
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
