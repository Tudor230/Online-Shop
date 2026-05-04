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
  readonly detailOrder = signal<AdminOrderDetail | null>(null);
  readonly newStatus = signal('');
  readonly statusNote = signal('');

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.api.getOrders(this.page()).subscribe({
      next: (res: PageResponse<AdminOrderList>) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
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
    this.api.getOrder(order.id).subscribe({
      next: (detail) => {
        this.detailOrder.set(detail);
        this.newStatus.set(detail.currentStatus);
      }
    });
  }

  closeDetail(): void {
    this.detailOrder.set(null);
  }

  updateStatus(): void {
    const detail = this.detailOrder();
    if (!detail) return;
    this.api.updateOrderStatus(detail.id, { newStatus: this.newStatus(), notes: this.statusNote() || undefined }).subscribe({
      next: () => {
        this.closeDetail();
        this.loadOrders();
      }
    });
  }
}
