import { useEffect, useState } from "react";
import {
  addDoc,
  collection,
  doc,
  getDocs,
  onSnapshot,
  query,
  updateDoc,
  where,
  writeBatch
} from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { Category } from "../types";

export function CategoriesPage() {
  const { orgId } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [newName, setNewName] = useState("");
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "categories"), (snapshot) => {
      const list = snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Category, "id">) }));
      list.sort((a, b) => a.sortOrder - b.sortOrder);
      setCategories(list);
    });
    return unsubscribe;
  }, [orgId]);

  async function handleAdd() {
    if (!orgId || newName.trim() === "") return;
    await addDoc(collection(db, "organizations", orgId, "categories"), {
      name: newName.trim(),
      sortOrder: Date.now()
    });
    setNewName("");
  }

  function startRename(category: Category) {
    setRenamingId(category.id);
    setRenameValue(category.name);
  }

  async function confirmRename() {
    if (!orgId || !renamingId || renameValue.trim() === "") return;
    await updateDoc(doc(db, "organizations", orgId, "categories", renamingId), { name: renameValue.trim() });
    setRenamingId(null);
  }

  // Mirrors FirebaseCategoryRepository.deleteCategory on Android: reassign
  // affected products to categoryId = "" (Uncategorized) in the same batch
  // that deletes the category, so the two can never drift apart.
  async function handleDelete(categoryId: string) {
    if (!orgId) return;
    const affected = await getDocs(
      query(collection(db, "organizations", orgId, "products"), where("categoryId", "==", categoryId))
    );
    const batch = writeBatch(db);
    affected.docs.forEach((productDoc) => {
      batch.update(productDoc.ref, { categoryId: "" });
    });
    batch.delete(doc(db, "organizations", orgId, "categories", categoryId));
    await batch.commit();
  }

  return (
    <div>
      <div className="page-header">
        <h1>Kategoriler</h1>
      </div>

      <div className="filters-row">
        <div style={{ display: "flex", gap: 8 }}>
          <input
            className="search-input"
            type="text"
            placeholder="Yeni kategori adı"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleAdd()}
          />
          <button className="link-button" onClick={handleAdd}>
            Ekle
          </button>
        </div>
      </div>

      {categories.length === 0 ? (
        <p className="empty-state">Henüz kategori yok.</p>
      ) : (
        <div className="product-list">
          {categories.map((category) => (
            <div className="product-row" key={category.id}>
              {renamingId === category.id ? (
                <div style={{ display: "flex", gap: 8, flex: 1 }}>
                  <input
                    className="search-input"
                    type="text"
                    value={renameValue}
                    onChange={(e) => setRenameValue(e.target.value)}
                    autoFocus
                  />
                  <button className="link-button" onClick={confirmRename}>
                    Kaydet
                  </button>
                  <button className="secondary-button" onClick={() => setRenamingId(null)}>
                    İptal
                  </button>
                </div>
              ) : (
                <>
                  <span className="name">{category.name}</span>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button className="secondary-button" onClick={() => startRename(category)}>
                      Yeniden adlandır
                    </button>
                    <button className="danger-button" onClick={() => handleDelete(category.id)}>
                      Sil
                    </button>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
