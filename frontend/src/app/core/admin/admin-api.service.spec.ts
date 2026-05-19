import { describe, it, expect, vi, beforeEach } from 'vitest';
import { of, throwError } from 'rxjs';
import { AdminApiService } from './admin-api.service';
import {
  AdminDashboardStats,
  AdminRevenueChart,
  AdminUserList,
  AdminProductList,
  AdminOrderList,
  AdminCategory,
  PageResponse,
} from './admin.types';

describe('AdminApiService', () => {
  let service: AdminApiService;
  let httpMock: any;

  beforeEach(() => {
    httpMock = {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    };
    service = new AdminApiService(httpMock);
  });

  describe('Dashboard', () => {
    it('should fetch stats', () => {
      const mockStats: AdminDashboardStats = {
        totalOrders: 10,
        totalUsers: 5,
        totalProducts: 20,
        lowStockCount: 2,
        pendingOrders: 3,
        totalRevenue: 1000.50,
      };
      httpMock.get.mockReturnValue(of(mockStats));

      service.getStats().subscribe((stats) => {
        expect(stats).toEqual(mockStats);
      });

      expect(httpMock.get).toHaveBeenCalledWith('http://localhost:8080/api/admin/dashboard/stats');
    });

    it('should fetch revenue chart with date params', () => {
      const mockRevenue: AdminRevenueChart[] = [
        { date: '2024-01-01', revenue: 100 },
      ];
      httpMock.get.mockReturnValue(of(mockRevenue));

      service.getRevenueChart('2024-01-01', '2024-01-31').subscribe((data) => {
        expect(data).toEqual(mockRevenue);
      });

      expect(httpMock.get).toHaveBeenCalledWith(
        'http://localhost:8080/api/admin/dashboard/revenue-chart',
        expect.objectContaining({
          params: expect.any(Object),
        })
      );
    });
  });

  describe('Users', () => {
    it('should fetch users with pagination', () => {
      const mockPage: PageResponse<AdminUserList> = {
        content: [],
        totalElements: 0,
        totalPages: 1,
        number: 0,
        size: 20,
      };
      httpMock.get.mockReturnValue(of(mockPage));

      service.getUsers(1, 10).subscribe((res) => {
        expect(res).toEqual(mockPage);
      });

      expect(httpMock.get).toHaveBeenCalledWith(
        'http://localhost:8080/api/admin/users',
        expect.any(Object)
      );
    });

    it('should delete a user', () => {
      httpMock.delete.mockReturnValue(of(void 0));

      service.deleteUser('user-1').subscribe(() => {
        expect(httpMock.delete).toHaveBeenCalledWith('http://localhost:8080/api/admin/users/user-1');
      });
    });

    it('should update user', () => {
      const mockUser: AdminUserList = {
        id: '1',
        email: 'test@test.com',
        firstName: 'Test',
        lastName: 'User',
        role: 'CUSTOMER',
        isActive: true,
        createdAt: '2024-01-01',
      };
      httpMock.put.mockReturnValue(of(mockUser));

      service.updateUser('1', { isActive: false }).subscribe((res) => {
        expect(res).toEqual(mockUser);
      });
    });
  });

  describe('Products', () => {
    it('should fetch products with pagination', () => {
      const mockPage: PageResponse<AdminProductList> = {
        content: [],
        totalElements: 0,
        totalPages: 1,
        number: 0,
        size: 20,
      };
      httpMock.get.mockReturnValue(of(mockPage));

      service.getProducts().subscribe((res) => {
        expect(res).toEqual(mockPage);
      });
    });

    it('should bulk delete products', () => {
      httpMock.post.mockReturnValue(of(void 0));

      service.bulkDeleteProducts({ ids: ['p1', 'p2'] }).subscribe(() => {
        expect(httpMock.post).toHaveBeenCalledWith(
          'http://localhost:8080/api/admin/products/bulk-delete',
          { ids: ['p1', 'p2'] }
        );
      });
    });
  });

  describe('Orders', () => {
    it('should fetch orders', () => {
      const mockPage: PageResponse<AdminOrderList> = {
        content: [],
        totalElements: 0,
        totalPages: 1,
        number: 0,
        size: 20,
      };
      httpMock.get.mockReturnValue(of(mockPage));

      service.getOrders().subscribe((res) => {
        expect(res).toEqual(mockPage);
      });
    });

    it('should update order status', () => {
      const mockOrder: AdminOrderList = {
        id: '1',
        orderNumber: 'ORD-001',
        userId: null,
        guestEmail: null,
        currentStatus: 'SHIPPED',
        totalAmount: 100,
        createdAt: '2024-01-01',
      };
      httpMock.put.mockReturnValue(of(mockOrder));

      service.updateOrderStatus('1', { newStatus: 'SHIPPED', notes: 'Shipped today' }).subscribe((res) => {
        expect(res.currentStatus).toBe('SHIPPED');
      });
    });
  });

  describe('Categories', () => {
    it('should fetch categories', () => {
      const mockCats: AdminCategory[] = [
        { id: '1', parentId: null, name: 'Electronics', slug: 'electronics' },
      ];
      httpMock.get.mockReturnValue(of(mockCats));

      service.getCategories().subscribe((res) => {
        expect(res).toEqual(mockCats);
      });
    });

    it('should create category', () => {
      const mockCat: AdminCategory = { id: '2', parentId: null, name: 'Books', slug: 'books' };
      httpMock.post.mockReturnValue(of(mockCat));

      service.createCategory({ name: 'Books', slug: 'books' }).subscribe((res) => {
        expect(res.name).toBe('Books');
      });
    });
  });

  describe('Error handling', () => {
    it('should propagate HTTP errors for stats', () => {
      const errorResponse = new Error('Network error');
      httpMock.get.mockReturnValue(throwError(() => errorResponse));

      service.getStats().subscribe({
        next: () => { throw new Error('should have errored'); },
        error: (err) => {
          expect(err.message).toBe('Network error');
        },
      });
    });
  });
});
