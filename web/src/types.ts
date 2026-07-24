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
