import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminDashboardStats, AdminRevenueChart } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './dashboard.html',
  providers: [DatePipe]
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly datePipe = inject(DatePipe);
  readonly today = new Date();
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
    const to = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const from = new Date(today);
    from.setDate(from.getDate() - 30);

    forkJoin({
      stats: this.api.getStats(),
      revenue: this.api.getRevenueChart(
        this.datePipe.transform(from, 'yyyy-MM-dd')!,
        this.datePipe.transform(to, 'yyyy-MM-dd')!
      )
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
