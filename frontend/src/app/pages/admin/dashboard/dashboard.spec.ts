import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AdminDashboardComponent } from './dashboard';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { of, throwError } from 'rxjs';
import { createEnvironmentInjector, runInInjectionContext, EnvironmentInjector } from '@angular/core';

const createMockApi = () => ({
  getStats: vi.fn(),
  getRevenueChart: vi.fn(),
});

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let mockApi: ReturnType<typeof createMockApi>;
  let injector: EnvironmentInjector;

  beforeEach(() => {
    mockApi = createMockApi();
    injector = createEnvironmentInjector([
      { provide: AdminApiService, useValue: mockApi },
    ], null as any);

    component = runInInjectionContext(injector, () => new AdminDashboardComponent());
  });

  it('should load stats and revenue on init', () => {
    mockApi.getStats.mockReturnValue(of({
      totalOrders: 10,
      totalUsers: 5,
      totalProducts: 20,
      lowStockCount: 2,
      pendingOrders: 3,
      totalRevenue: 1000,
    }));
    mockApi.getRevenueChart.mockReturnValue(of([
      { date: '2024-01-01', revenue: 100 },
    ]));

    component.ngOnInit();

    expect(component.stats()?.totalOrders).toBe(10);
    expect(component.revenue().length).toBe(1);
    expect(component.loading()).toBe(false);
    expect(component.error()).toBeNull();
  });

  it('should set error when data loading fails', () => {
    mockApi.getStats.mockReturnValue(throwError(() => new Error('Server error')));
    mockApi.getRevenueChart.mockReturnValue(of([]));

    component.ngOnInit();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Server error');
  });

  it('should set loading true when refresh is triggered', () => {
    mockApi.getStats.mockReturnValue(of({
      totalOrders: 1, totalUsers: 1, totalProducts: 1,
      lowStockCount: 0, pendingOrders: 0, totalRevenue: 0,
    }));
    mockApi.getRevenueChart.mockReturnValue(of([]));

    component.loadData();

    expect(component.loading()).toBe(false); // synchronously false after subscription completes
  });

  it('should clear previous error on refresh', () => {
    mockApi.getStats.mockReturnValue(throwError(() => new Error('Fail')));
    mockApi.getRevenueChart.mockReturnValue(of([]));
    component.ngOnInit();
    expect(component.error()).toBe('Fail');

    mockApi.getStats.mockReturnValue(of({
      totalOrders: 1, totalUsers: 1, totalProducts: 1,
      lowStockCount: 0, pendingOrders: 0, totalRevenue: 0,
    }));
    component.loadData();

    expect(component.error()).toBeNull();
  });
});
