import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminBulkActionRequest,
  AdminCategory,
  AdminCategoryCreateRequest,
  AdminDashboardStats,
  AdminOrderDetail,
  AdminOrderList,
  AdminOrderStatusUpdateRequest,
  AdminProductCreateRequest,
  AdminProductDetail,
  AdminProductList,
  AdminProductUpdateRequest,
  AdminRevenueChart,
  AdminUserCreateRequest,
  AdminUserDetail,
  AdminUserList,
  AdminUserUpdateRequest,
  PageResponse,
} from './admin.types';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly baseUrl = '/api/admin';

  constructor(private readonly http: HttpClient) {}

  // Dashboard
  getStats(): Observable<AdminDashboardStats> {
    return this.http.get<AdminDashboardStats>(`${this.baseUrl}/dashboard/stats`);
  }

  getRevenueChart(from: string, to: string): Observable<AdminRevenueChart[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<AdminRevenueChart[]>(`${this.baseUrl}/dashboard/revenue-chart`, { params });
  }

  // Users
  getUsers(page = 0, size = 20): Observable<PageResponse<AdminUserList>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminUserList>>(`${this.baseUrl}/users`, { params });
  }

  getUser(id: string): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${this.baseUrl}/users/${id}`);
  }

  createUser(request: AdminUserCreateRequest): Observable<AdminUserDetail> {
    return this.http.post<AdminUserDetail>(`${this.baseUrl}/users`, request);
  }

  updateUser(id: string, request: AdminUserUpdateRequest): Observable<AdminUserDetail> {
    return this.http.put<AdminUserDetail>(`${this.baseUrl}/users/${id}`, request);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/users/${id}`);
  }

  // Products
  getProducts(page = 0, size = 20): Observable<PageResponse<AdminProductList>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminProductList>>(`${this.baseUrl}/products`, { params });
  }

  getProduct(id: string): Observable<AdminProductDetail> {
    return this.http.get<AdminProductDetail>(`${this.baseUrl}/products/${id}`);
  }

  createProduct(request: AdminProductCreateRequest): Observable<AdminProductDetail> {
    return this.http.post<AdminProductDetail>(`${this.baseUrl}/products`, request);
  }

  updateProduct(id: string, request: AdminProductUpdateRequest): Observable<AdminProductDetail> {
    return this.http.put<AdminProductDetail>(`${this.baseUrl}/products/${id}`, request);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/products/${id}`);
  }

  bulkDeleteProducts(request: AdminBulkActionRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/products/bulk-delete`, request);
  }

  bulkActivateProducts(request: AdminBulkActionRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/products/bulk-activate`, request);
  }

  bulkDeactivateProducts(request: AdminBulkActionRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/products/bulk-deactivate`, request);
  }

  // Orders
  getOrders(page = 0, size = 20): Observable<PageResponse<AdminOrderList>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminOrderList>>(`${this.baseUrl}/orders`, { params });
  }

  getOrder(id: string): Observable<AdminOrderDetail> {
    return this.http.get<AdminOrderDetail>(`${this.baseUrl}/orders/${id}`);
  }

  updateOrderStatus(id: string, request: AdminOrderStatusUpdateRequest): Observable<AdminOrderDetail> {
    return this.http.put<AdminOrderDetail>(`${this.baseUrl}/orders/${id}/status`, request);
  }

  // Categories
  getCategories(): Observable<AdminCategory[]> {
    return this.http.get<AdminCategory[]>(`${this.baseUrl}/categories`);
  }

  getCategory(id: string): Observable<AdminCategory> {
    return this.http.get<AdminCategory>(`${this.baseUrl}/categories/${id}`);
  }

  createCategory(request: AdminCategoryCreateRequest): Observable<AdminCategory> {
    return this.http.post<AdminCategory>(`${this.baseUrl}/categories`, request);
  }

  updateCategory(id: string, request: AdminCategoryCreateRequest): Observable<AdminCategory> {
    return this.http.put<AdminCategory>(`${this.baseUrl}/categories/${id}`, request);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${id}`);
  }
}
