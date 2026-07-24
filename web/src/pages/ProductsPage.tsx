import { useEffect, useMemo, useState } from "react";
import { collection, onSnapshot } from "firebase/firestore";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import { productStatus, type Category, type Product } from "../types";

export function ProductsPage() {
  const { orgId } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  // null = all, "" = uncategorized, otherwise a real category id
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "products"), (snapshot) => {
      setProducts(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as Omit<Product, "id">) })));
      setLoading(false);
    });
    return unsubscribe;
  }, [orgId]);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "categories"), (snapshot) => {
      setCategories(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as Omit<Category, "id">) })));
    });
    return unsubscribe;
  }, [orgId]);

  const filteredProducts = useMemo(() => {
    return products.filter((product) => {
      const matchesCategory = selectedCategoryId === null || product.categoryId === selectedCategoryId;
      const matchesSearch =
        search.trim() === "" || product.name.toLowerCase().includes(search.trim().toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }, [products, search, selectedCategoryId]);

  return (
    <div>
      <div className="page-header">
        <h1>Ürünler</h1>
        <Link className="link-button" to="/products/new">
          + Ürün ekle
        </Link>
      </div>

      <div className="filters-row">
        <input
          className="search-input"
          type="text"
          placeholder="Ürün ara"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <div className="chip-row">
          <button
            className={`chip ${selectedCategoryId === null ? "selected" : ""}`}
            onClick={() => setSelectedCategoryId(null)}
          >
            Tümü
          </button>
          <button
            className={`chip ${selectedCategoryId === "" ? "selected" : ""}`}
            onClick={() => setSelectedCategoryId("")}
          >
            Kategorisiz
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              className={`chip ${selectedCategoryId === category.id ? "selected" : ""}`}
              onClick={() => setSelectedCategoryId(category.id)}
            >
              {category.name}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <p className="empty-state">Yükleniyor...</p>
      ) : filteredProducts.length === 0 ? (
        <p className="empty-state">
          {products.length === 0 ? "Henüz ürün yok. Eklemek için yukarıdaki + butonuna dokun." : "Bu arama/filtreye uyan ürün yok."}
        </p>
      ) : (
        <div className="product-list">
          {filteredProducts.map((product) => {
            const status = productStatus(product);
            const statusLabel =
              status === "OUT_OF_STOCK" ? "Tükendi" : status === "LOW_STOCK" ? "Az Kaldı" : "Stokta";
            const statusClass =
              status === "OUT_OF_STOCK" ? "out-of-stock" : status === "LOW_STOCK" ? "low-stock" : "in-stock";
            return (
              <Link className="product-row" to={`/products/${product.id}`} key={product.id}>
                <div>
                  <div className="name">{product.name}</div>
                  <div className="sku">{product.sku}</div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <span style={{ fontWeight: 600 }}>{product.quantity}</span>
                  <span className={`status-chip ${statusClass}`}>{statusLabel}</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
