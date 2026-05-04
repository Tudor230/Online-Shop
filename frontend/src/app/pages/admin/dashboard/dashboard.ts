import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminDashboardStats, AdminRevenueChart } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html'
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly stats = signal<AdminDashboardStats | null>(null);
  readonly revenue = signal<AdminRevenueChart[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    const today = new Date();
    const from = new Date(today.getFullYear(), today.getMonth() - 1, 1);
    const to = new Date(today.getFullYear(), today.getMonth(), 0);

    forkJoin({
      stats: this.api.getStats(),
      revenue: this.api.getRevenueChart(from.toISOString().split('T')[0], to.toISOString().split('T')[0])
    }).subscribe({
      next: ({ stats, revenue }) => {
        this.stats.set(stats);
        this.revenue.set(revenue);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load dashboard data');
        this.loading.set(false);
      }
    });
  }
}
