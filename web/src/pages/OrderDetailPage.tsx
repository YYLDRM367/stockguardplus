import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  collection,
  deleteDoc,
  doc,
  DocumentReference,
  onSnapshot,
  runTransaction,
  serverTimestamp
} from "firebase/firestore";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { Order, Party, Product } from "../types";

class InsufficientStockError extends Error {}

export function OrderDetailPage() {
  const { orgId } = useAuth();
  const { id } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState<Order | null>(null);
  const [parties, setParties] = useState<Party[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [confirmingApprove, setConfirmingApprove] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [approving, setApproving] = useState(false);
  const [approveError, setApproveError] = useState<string | null>(null);

  useEffect(() => {
    if (!orgId || !id) return;
    const unsubscribe = onSnapshot(doc(db, "organizations", orgId, "orders", id), (snapshot) => {
      setOrder(snapshot.exists() ? ({ id: snapshot.id, ...(snapshot.data() as Omit<Order, "id">) }) : null);
    });
    return unsubscribe;
  }, [orgId, id]);

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

  // Mirrors FirebaseOrderRepository.approveOrder on Android: one transaction
  // updates every line's product quantity and writes a matching Movement,
  // rejecting the whole approval if any sale line would go below zero.
  async function handleApprove() {
    if (!orgId || !id || !order) return;
    setApproving(true);
    setApproveError(null);
    const orgRef = doc(db, "organizations", orgId);
    const orderRef = doc(db, "organizations", orgId, "orders", id);

    try {
      await runTransaction(db, async (transaction) => {
        const orderSnapshot = await transaction.get(orderRef);
        const data = orderSnapshot.data();
        if (!data || data.status !== "draft") {
          throw new Error("Bu sipariş zaten onaylanmış.");
        }
        const lines: { productId: string; quantity: number }[] = data.lines || [];

        const productRefs = lines.map((l) => doc(orgRef, "products", l.productId));
        const productSnapshots = await Promise.all(productRefs.map((ref) => transaction.get(ref)));
        const movementRefs = lines.map(() => doc(collection(orgRef, "movements")));

        lines.forEach((line, index) => {
          const currentQuantity = (productSnapshots[index].data()?.quantity as number) ?? 0;
          let newQuantity: number;
          if (data.type === "purchase") {
            newQuantity = currentQuantity + line.quantity;
          } else {
            if (line.quantity > currentQuantity) {
              throw new InsufficientStockError(
                `Yetersiz stok (mevcut ${currentQuantity} adet, istenen ${line.quantity} adet).`
              );
            }
            newQuantity = currentQuantity - line.quantity;
          }

          transaction.update(productRefs[index] as DocumentReference, { quantity: newQuantity });
          transaction.set(movementRefs[index], {
            productId: line.productId,
            type: data.type === "purchase" ? "in" : "out",
            quantity: line.quantity,
            partyId: data.partyId,
            orderId: id,
            userId: orgId,
            timestamp: serverTimestamp()
          });
        });

        transaction.update(orderRef, { status: "approved", approvedAt: serverTimestamp() });
      });
    } catch (err) {
      setApproveError(err instanceof Error ? err.message : "Onaylanamadı.");
    } finally {
      setApproving(false);
    }
  }

  async function handleDelete() {
    if (!orgId || !id) return;
    await deleteDoc(doc(db, "organizations", orgId, "orders", id));
    navigate("/orders");
  }

  if (!order) {
    return <p className="empty-state">Bu sipariş artık mevcut değil.</p>;
  }

  const partyName = parties.find((p) => p.id === order.partyId)?.name || "";
  const productNameById = new Map(products.map((p) => [p.id, p.name]));
  const dateLabel = order.date ? new Date(order.date.seconds * 1000).toLocaleDateString("tr-TR") : "";
  const reference = [order.invoiceNumber, order.receiptNumber].filter(Boolean).join(" · ");

  return (
    <div>
      <div className="page-header">
        <h1>{dateLabel}</h1>
        {order.status === "draft" && (
          <div style={{ display: "flex", gap: 8 }}>
            <button className="primary-button" onClick={() => setConfirmingApprove(true)}>
              Onayla
            </button>
            <button className="danger-button" onClick={() => setConfirmingDelete(true)}>
              Sil
            </button>
          </div>
        )}
      </div>

      <div className="detail-grid">
        <div className="detail-field">
          <div className="label">Firma</div>
          <div className="value">{partyName}</div>
        </div>
        <div className="detail-field">
          <div className="label">Durum</div>
          <div className="value">
            <span className={`status-chip ${order.status === "approved" ? "in-stock" : "low-stock"}`}>
              {order.status === "approved" ? "Onaylandı" : "Taslak"}
            </span>
          </div>
        </div>
        {reference && (
          <div className="detail-field">
            <div className="label">Fatura/İrsaliye</div>
            <div className="value">{reference}</div>
          </div>
        )}
      </div>

      {approveError && <p className="error-text">{approveError}</p>}

      <h2 style={{ fontSize: "1rem", marginBottom: 10 }}>Sipariş satırları</h2>
      {order.lines.map((line, index) => (
        <div className="movement-row" key={index}>
          <span>{productNameById.get(line.productId) || line.productId}</span>
          <span>{line.quantity}</span>
        </div>
      ))}

      {confirmingApprove && (
        <div className="form-card" style={{ marginTop: 16 }}>
          <p>Bu siparişi onaylamak istediğine emin misin? Stok miktarları güncellenecek.</p>
          <div style={{ display: "flex", gap: 8 }}>
            <button
              className="primary-button"
              disabled={approving}
              onClick={() => {
                setConfirmingApprove(false);
                handleApprove();
              }}
            >
              Onayla
            </button>
            <button className="secondary-button" onClick={() => setConfirmingApprove(false)}>
              İptal
            </button>
          </div>
        </div>
      )}

      {confirmingDelete && (
        <div className="form-card" style={{ marginTop: 16 }}>
          <p>Bu taslak siparişi silmek istediğine emin misin?</p>
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
