export interface AdminDashboardStats {
  totalOrders: number;
  totalUsers: number;
  totalProducts: number;
  lowStockCount: number;
  pendingOrders: number;
  totalRevenue: number;
}

export interface AdminRevenueChart {
  date: string;
  revenue: number;
}

export interface AdminUserList {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  isActive: boolean;
  createdAt: string;
}

export interface AdminUserDetail {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  isActive: boolean;
  defaultShippingAddressId: string | null;
  defaultBillingAddressId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserCreateRequest {
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  password: string;
}

export interface AdminUserUpdateRequest {
  firstName?: string;
  lastName?: string;
  role?: string;
  isActive?: boolean;
}

export interface AdminProductList {
  id: string;
  sku: string;
  name: string;
  slug: string;
  basePrice: number;
  isActive: boolean;
  rating: number;
  reviewCount: number;
  imagePlaceholder: string;
  quantityAvailable: number;
  lowStockThreshold: number;
  categories: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminProductDetail {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description: string;
  basePrice: number;
  isActive: boolean;
  rating: number;
  reviewCount: number;
  imagePlaceholder: string;
  imageGallery: string[];
  categories: AdminCategory[];
  inventory: AdminInventory;
  createdAt: string;
  updatedAt: string;
}

export interface AdminProductCreateRequest {
  sku: string;
  name: string;
  slug: string;
  description?: string;
  basePrice: number;
  categoryIds?: string[];
  imagePlaceholder: string;
  imageGallery?: string[];
  initialQuantity?: number;
  lowStockThreshold?: number;
}

export interface AdminProductUpdateRequest {
  sku?: string;
  name?: string;
  slug?: string;
  description?: string;
  basePrice?: number;
  isActive?: boolean;
  categoryIds?: string[];
  imagePlaceholder?: string;
  imageGallery?: string[];
  quantityAvailable?: number;
  lowStockThreshold?: number;
}

export interface AdminInventory {
  quantityAvailable: number;
  lowStockThreshold: number;
}

export interface AdminCategory {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
}

export interface AdminCategoryCreateRequest {
  parentId?: string;
  name: string;
  slug: string;
}

export interface AdminOrderList {
  id: string;
  orderNumber: string;
  userId: string | null;
  guestEmail: string | null;
  currentStatus: string;
  totalAmount: number;
  createdAt: string;
}

export interface AdminOrderDetail {
  id: string;
  orderNumber: string;
  userId: string | null;
  guestEmail: string | null;
  currentStatus: string;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  shippingAddressId: string;
  billingAddressId: string;
  items: AdminOrderItem[];
  statusHistory: AdminOrderStatusHistory[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminOrderItem {
  productId: string;
  productName: string;
  productImage: string;
  quantity: number;
  unitPriceAtPurchase: number;
}

export interface AdminOrderStatusHistory {
  status: string;
  notes: string | null;
  createdAt: string;
}

export interface AdminOrderStatusUpdateRequest {
  newStatus: string;
  notes?: string;
}

export interface AdminBulkActionRequest {
  ids: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
