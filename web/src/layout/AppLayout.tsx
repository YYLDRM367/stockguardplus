import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const navItems = [
  { to: "/", label: "Panel", end: true },
  { to: "/products", label: "Ürünler", end: false },
  { to: "/categories", label: "Kategoriler", end: false }
];

export function AppLayout() {
  const { user, signOut } = useAuth();

  return (
    <div className="app-shell">
      <header className="top-bar">
        <div className="top-bar-left">
          <span className="brand">StockGuard+</span>
          <nav className="nav-links">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="top-bar-right">
          <span className="user-email">{user?.email}</span>
          <button onClick={() => signOut()}>Çıkış yap</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
