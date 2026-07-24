export interface Product {
  id: string;
  name: string;
  sku: string;
  barcode: string;
  quantity: number;
  reorderPoint: number;
  categoryId: string;
}

export type StockStatus = "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK";

export function productStatus(product: Product): StockStatus {
  if (product.quantity <= 0) return "OUT_OF_STOCK";
  if (product.quantity <= product.reorderPoint) return "LOW_STOCK";
  return "IN_STOCK";
}

export interface Organization {
  name: string;
  language: string;
  subscriptionPlan: string;
  subscriptionExpiry: string | null;
}

export interface Category {
  id: string;
  name: string;
  sortOrder: number;
}

export interface Party {
  id: string;
  name: string;
  address: string;
  phone1: string;
  phone2: string;
  email: string;
}

export type OrderType = "purchase" | "sale";
export type OrderStatus = "draft" | "approved";

export interface OrderLine {
  productId: string;
  quantity: number;
}

export interface Order {
  id: string;
  date: { seconds: number; nanoseconds: number } | null;
  invoiceNumber: string;
  receiptNumber: string;
  type: OrderType;
  partyId: string;
  status: OrderStatus;
  lines: OrderLine[];
  userId: string;
  createdAt: { seconds: number; nanoseconds: number } | null;
  approvedAt: { seconds: number; nanoseconds: number } | null;
}

export interface Movement {
  id: string;
  productId: string;
  type: "in" | "out";
  quantity: number;
  partyId: string;
  orderId: string;
  userId: string;
  timestamp: { seconds: number; nanoseconds: number } | null;
}

