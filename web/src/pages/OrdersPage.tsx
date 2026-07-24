import { useEffect, useState } from "react";
import { collection, onSnapshot, query, where } from "firebase/firestore";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { db } from "../firebase";
import type { Order, OrderType, Party } from "../types";

export function OrdersPage() {
  const { orgId } = useAuth();
  const [tab, setTab] = useState<OrderType>("purchase");
  const [orders, setOrders] = useState<Order[]>([]);
  const [parties, setParties] = useState<Party[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!orgId) return;
    const unsubscribe = onSnapshot(collection(db, "organizations", orgId, "parties"), (snapshot) => {
      setParties(snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Party, "id">) })));
    });
    return unsubscribe;
  }, [orgId]);

  useEffect(() => {
    if (!orgId) return;
    setLoading(true);
    const q = query(collection(db, "organizations", orgId, "orders"), where("type", "==", tab));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = snapshot.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Order, "id">) }));
      list.sort((a, b) => (b.createdAt?.seconds ?? 0) - (a.createdAt?.seconds ?? 0));
      setOrders(list);
      setLoading(false);
    });
    return unsubscribe;
  }, [orgId, tab]);

  const partyNameById = new Map(parties.map((p) => [p.id, p.name]));

  return (
    <div>
      <div className="page-header">
        <h1>Siparişler</h1>
        <Link className="link-button" to={`/orders/new?type=${tab}`}>
          + Sipariş oluştur
        </Link>
      </div>

      <div className="chip-row" style={{ marginBottom: 16 }}>
        <button className={`chip ${tab === "purchase" ? "selected" : ""}`} onClick={() => setTab("purchase")}>
          Alış
        </button>
        <button className={`chip ${tab === "sale" ? "selected" : ""}`} onClick={() => setTab("sale")}>
          Satış
        </button>
      </div>

      {loading ? (
        <p className="empty-state">Yükleniyor...</p>
      ) : orders.length === 0 ? (
        <p className="empty-state">Henüz sipariş yok.</p>
      ) : (
        <div className="product-list">
          {orders.map((order) => {
            const dateLabel = order.date ? new Date(order.date.seconds * 1000).toLocaleDateString("tr-TR") : "";
            const reference = [order.invoiceNumber, order.receiptNumber].filter(Boolean).join(" · ");
            const secondaryLine = [partyNameById.get(order.partyId) || "", reference]
              .filter(Boolean)
              .join(" · ");
            return (
              <Link className="product-row" to={`/orders/${order.id}`} key={order.id}>
                <div>
                  <div className="name">{dateLabel}</div>
                  {secondaryLine && <div className="sku">{secondaryLine}</div>}
                </div>
                <span className={`status-chip ${order.status === "approved" ? "in-stock" : "low-stock"}`}>
                  {order.status === "approved" ? "Onaylandı" : "Taslak"}
                </span>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
