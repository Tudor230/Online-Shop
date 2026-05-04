import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AdminOrdersComponent } from './orders';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { of, throwError } from 'rxjs';
import { createEnvironmentInjector, runInInjectionContext, EnvironmentInjector } from '@angular/core';

const createMockApi = () => ({
  getOrders: vi.fn(),
  getOrder: vi.fn(),
  updateOrderStatus: vi.fn(),
});

describe('AdminOrdersComponent', () => {
  let component: AdminOrdersComponent;
  let mockApi: ReturnType<typeof createMockApi>;
  let injector: EnvironmentInjector;

  beforeEach(() => {
    mockApi = createMockApi();
    injector = createEnvironmentInjector([
      { provide: AdminApiService, useValue: mockApi },
    ], null as any);

    component = runInInjectionContext(injector, () => new AdminOrdersComponent());
  });

  it('should load orders on init', () => {
    mockApi.getOrders.mockReturnValue(of({
      content: [{ id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null, currentStatus: 'PENDING', totalAmount: 100, createdAt: '2024-01-01' }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    }));

    component.ngOnInit();

    expect(component.orders().length).toBe(1);
    expect(component.loading()).toBe(false);
  });

  it('should set error when loading fails', () => {
    mockApi.getOrders.mockReturnValue(throwError(() => new Error('Load failed')));

    component.ngOnInit();

    expect(component.error()).toBe('Load failed');
    expect(component.loading()).toBe(false);
  });

  it('should open order detail', () => {
    const detail = {
      id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null,
      currentStatus: 'PENDING', subtotal: 100, discountAmount: 0, totalAmount: 100,
      shippingAddressId: 'a', billingAddressId: 'b',
      items: [], statusHistory: [], createdAt: '2024-01-01', updatedAt: '2024-01-01',
    };
    mockApi.getOrder.mockReturnValue(of(detail));

    component.openDetail({ id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null, currentStatus: 'PENDING', totalAmount: 100, createdAt: '2024-01-01' });

    expect(component.detailOrder()).toEqual(detail);
    expect(component.newStatus()).toBe('PENDING');
  });

  it('should show error when detail loading fails', () => {
    mockApi.getOrder.mockReturnValue(throwError(() => new Error('Detail error')));

    component.openDetail({ id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null, currentStatus: 'PENDING', totalAmount: 100, createdAt: '2024-01-01' });

    expect(component.detailOrder()).toBeNull();
    expect(component.actionError()).toBe('Detail error');
  });

  it('should update status and close modal on success', () => {
    const detail = {
      id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null,
      currentStatus: 'PENDING', subtotal: 100, discountAmount: 0, totalAmount: 100,
      shippingAddressId: 'a', billingAddressId: 'b',
      items: [], statusHistory: [], createdAt: '2024-01-01', updatedAt: '2024-01-01',
    };
    component.detailOrder.set(detail);
    component.newStatus.set('SHIPPED');
    component.statusNote.set('Shipped today');

    mockApi.updateOrderStatus.mockReturnValue(of(detail));
    mockApi.getOrders.mockReturnValue(of({
      content: [], totalElements: 0, totalPages: 1, number: 0, size: 20,
    }));

    component.updateStatus();

    expect(mockApi.updateOrderStatus).toHaveBeenCalledWith('1', { newStatus: 'SHIPPED', notes: 'Shipped today' });
    expect(component.detailOrder()).toBeNull();
  });

  it('should show error when status update fails', () => {
    const detail = {
      id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null,
      currentStatus: 'PENDING', subtotal: 100, discountAmount: 0, totalAmount: 100,
      shippingAddressId: 'a', billingAddressId: 'b',
      items: [], statusHistory: [], createdAt: '2024-01-01', updatedAt: '2024-01-01',
    };
    component.detailOrder.set(detail);
    component.newStatus.set('SHIPPED');

    mockApi.updateOrderStatus.mockReturnValue(throwError(() => new Error('Update failed')));

    component.updateStatus();

    expect(component.detailOrder()).not.toBeNull();
    expect(component.actionError()).toBe('Update failed');
  });

  it('should filter orders by status', () => {
    component.orders.set([
      { id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null, currentStatus: 'PENDING', totalAmount: 100, createdAt: '2024-01-01' },
      { id: '2', orderNumber: 'ORD-002', userId: null, guestEmail: null, currentStatus: 'SHIPPED', totalAmount: 200, createdAt: '2024-01-01' },
    ]);
    component.statusFilter.set('SHIPPED');

    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].orderNumber).toBe('ORD-002');
  });

  it('should filter orders by search query', () => {
    component.orders.set([
      { id: '1', orderNumber: 'ORD-001', userId: null, guestEmail: null, currentStatus: 'PENDING', totalAmount: 100, createdAt: '2024-01-01' },
      { id: '2', orderNumber: 'ORD-002', userId: null, guestEmail: 'test@example.com', currentStatus: 'SHIPPED', totalAmount: 200, createdAt: '2024-01-01' },
    ]);
    component.searchQuery.set('test@example.com');

    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].id).toBe('2');
  });
});
