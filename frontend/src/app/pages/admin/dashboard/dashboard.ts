import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
    this.loadStats();
    this.loadRevenue();
  }

  private loadStats(): void {
    this.api.getStats().subscribe({
      next: (s) => this.stats.set(s),
      error: () => this.error.set('Failed to load stats')
    });
  }

  private loadRevenue(): void {
    const today = new Date();
    const from = new Date(today.getFullYear(), today.getMonth() - 1, 1);
    const to = new Date(today.getFullYear(), today.getMonth(), 0);

    this.api.getRevenueChart(from.toISOString().split('T')[0], to.toISOString().split('T')[0]).subscribe({
      next: (data) => {
        this.revenue.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load revenue chart');
        this.loading.set(false);
      }
    });
  }
}
