import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { addDoc, collection, doc, getDoc, onSnapshot, updateDoc } from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { Category, Product } from "../types";

export function ProductFormPage({ mode }: { mode: "add" | "edit" }) {
  const { orgId } = useAuth();
  const { id } = useParams();
  const navigate = useNavigate();

  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState("");
  const [sku, setSku] = useState("");
  const [barcode, setBarcode] = useState("");
  const [quantity, setQuantity] = useState("0");
  const [reorderPoint, setReorderPoint] = useState("0");
  const [categoryId, setCategoryId] = useState("");
  const [showAddCategory, setShowAddCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [loadingProduct, setLoadingProduct] = useState(mode === "edit");

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "categories"), (snapshot) => {
      setCategories(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Category, "id">) })));
    });
    return unsubscribe;
  }, [orgId]);

  useEffect(() => {
    if (mode !== "edit" || !orgId || !id) return;
    (async () => {
      const snapshot = await getDoc(doc(db, "organizations", orgId, "products", id));
      const data = snapshot.data() as Omit<Product, "id"> | undefined;
      if (data) {
        setName(data.name);
        setSku(data.sku);
        setBarcode(data.barcode || "");
        setQuantity(String(data.quantity));
        setReorderPoint(String(data.reorderPoint));
        setCategoryId(data.categoryId || "");
      }
      setLoadingProduct(false);
    })();
  }, [mode, orgId, id]);

  async function handleAddCategory() {
    if (!orgId || newCategoryName.trim() === "") return;
    const ref = await addDoc(collection(db, "organizations", orgId, "categories"), {
      name: newCategoryName.trim(),
      sortOrder: Date.now()
    });
    setCategoryId(ref.id);
    setNewCategoryName("");
    setShowAddCategory(false);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!orgId) return;
    if (name.trim() === "" || sku.trim() === "") {
      setError("Ürün adı ve SKU gerekli.");
      return;
    }
    setError(null);
    setSaving(true);
    try {
      if (mode === "add") {
        await addDoc(collection(db, "organizations", orgId, "products"), {
          name: name.trim(),
          sku: sku.trim(),
          barcode: barcode.trim(),
          quantity: parseInt(quantity, 10) || 0,
          reorderPoint: parseInt(reorderPoint, 10) || 0,
          categoryId
        });
        navigate("/products");
      } else if (id) {
        // Quantity is intentionally excluded here — it only changes through
        // a Purchase/Sales order, mirroring the Android app's rule.
        await updateDoc(doc(db, "organizations", orgId, "products", id), {
          name: name.trim(),
          sku: sku.trim(),
          barcode: barcode.trim(),
          reorderPoint: parseInt(reorderPoint, 10) || 0,
          categoryId
        });
        navigate(`/products/${id}`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Kaydedilemedi.");
    } finally {
      setSaving(false);
    }
  }

  if (loadingProduct) {
    return <p className="empty-state">Yükleniyor...</p>;
  }

  return (
    <div>
      <div className="page-header">
        <h1>{mode === "add" ? "Ürün ekle" : "Ürünü düzenle"}</h1>
      </div>

      <form className="form-card" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="name">Ürün adı</label>
          <input id="name" type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="sku">SKU</label>
          <input id="sku" type="text" value={sku} onChange={(e) => setSku(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="barcode">Barkod</label>
          <input id="barcode" type="text" value={barcode} onChange={(e) => setBarcode(e.target.value)} />
        </div>

        {mode === "add" ? (
          <div className="field">
            <label htmlFor="quantity">Miktar</label>
            <input
              id="quantity"
              type="number"
              min="0"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
            />
          </div>
        ) : (
          <p className="field-hint">
            Mevcut miktar: <strong>{quantity}</strong>. Değiştirmek için bir satın alma ya da satış
            siparişi oluştur.
          </p>
        )}

        <div className="field">
          <label htmlFor="reorderPoint">Yeniden sipariş noktası</label>
          <input
            id="reorderPoint"
            type="number"
            min="0"
            value={reorderPoint}
            onChange={(e) => setReorderPoint(e.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="category">Kategori</label>
          <select id="category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Kategorisiz</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        {showAddCategory ? (
          <div className="field">
            <label htmlFor="newCategory">Yeni kategori adı</label>
            <div style={{ display: "flex", gap: 8 }}>
              <input
                id="newCategory"
                type="text"
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
              />
              <button type="button" className="secondary-button" onClick={handleAddCategory}>
                Ekle
              </button>
            </div>
          </div>
        ) : (
          <button
            type="button"
            className="toggle-link"
            style={{ marginBottom: 14 }}
            onClick={() => setShowAddCategory(true)}
          >
            + Yeni kategori ekle
          </button>
        )}

        {error && <p className="error-text">{error}</p>}

        <button type="submit" className="primary-button" disabled={saving}>
          {saving ? "Kaydediliyor..." : "Kaydet"}
        </button>
      </form>
    </div>
  );
}
