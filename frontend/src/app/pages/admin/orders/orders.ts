import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminOrderList, AdminOrderDetail, PageResponse } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './orders.html'
})
export class AdminOrdersComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly orders = signal<AdminOrderList[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly detailOrder = signal<AdminOrderDetail | null>(null);
  readonly newStatus = signal('');
  readonly statusNote = signal('');
  readonly statusFilter = signal('');
  readonly searchQuery = signal('');

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.getOrders(this.page()).subscribe({
      next: (res: PageResponse<AdminOrderList>) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load orders');
        this.loading.set(false);
      }
    });
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadOrders();
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      this.loadOrders();
    }
  }

  openDetail(order: AdminOrderList): void {
    this.actionError.set(null);
    this.api.getOrder(order.id).subscribe({
      next: (detail) => {
        this.detailOrder.set(detail);
        this.newStatus.set(detail.currentStatus);
      },
      error: (err) => this.actionError.set(err?.message ?? 'Failed to load order details')
    });
  }

  closeDetail(): void {
    this.detailOrder.set(null);
    this.actionError.set(null);
  }

  updateStatus(): void {
    const detail = this.detailOrder();
    if (!detail) return;
    this.actionError.set(null);
    this.api.updateOrderStatus(detail.id, { newStatus: this.newStatus(), notes: this.statusNote() || undefined }).subscribe({
      next: () => {
        this.closeDetail();
        this.loadOrders();
      },
      error: (err) => this.actionError.set(err?.message ?? 'Failed to update order status')
    });
  }

  filteredOrders(): AdminOrderList[] {
    let result = this.orders();
    const q = this.searchQuery().toLowerCase().trim();
    const status = this.statusFilter();

    if (status) {
      result = result.filter(o => o.currentStatus === status);
    }
    if (q) {
      result = result.filter(o =>
        o.orderNumber.toLowerCase().includes(q) ||
        (o.guestEmail ?? '').toLowerCase().includes(q)
      );
    }
    return result;
  }
}
