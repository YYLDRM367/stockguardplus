import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { AppLayout } from "./layout/AppLayout";
import { LoginPage } from "./pages/LoginPage";
import { SubscriptionRequiredPage } from "./pages/SubscriptionRequiredPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ProductsPage } from "./pages/ProductsPage";
import { ProductFormPage } from "./pages/ProductFormPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";
import { CategoriesPage } from "./pages/CategoriesPage";
import { CompaniesPage } from "./pages/CompaniesPage";
import { OrdersPage } from "./pages/OrdersPage";
import { OrderFormPage } from "./pages/OrderFormPage";
import { OrderDetailPage } from "./pages/OrderDetailPage";
import { ReportsPage } from "./pages/ReportsPage";
import { SettingsPage } from "./pages/SettingsPage";
import { hasActiveAccess } from "./types";
import "./App.css";

function App() {
  const { user, loading, organization, orgLoading } = useAuth();

  if (loading || (user && orgLoading)) {
    return <p className="empty-state">Yükleniyor...</p>;
  }

  const entitled = user !== null && hasActiveAccess(organization);

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route
        element={
          !user ? (
            <Navigate to="/login" replace />
          ) : entitled ? (
            <AppLayout />
          ) : (
            <SubscriptionRequiredPage />
          )
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/products/new" element={<ProductFormPage mode="add" />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/products/:id/edit" element={<ProductFormPage mode="edit" />} />
        <Route path="/categories" element={<CategoriesPage />} />
        <Route path="/companies" element={<CompaniesPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/new" element={<OrderFormPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
