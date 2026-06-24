import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminAuditLog, PageResponse } from '../../../core/admin/admin.types';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.html',
})
export class AdminAuditLogsComponent implements OnInit {
  private readonly adminApiService = inject(AdminApiService);

  readonly logs = signal<AdminAuditLog[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  // Pagination
  readonly currentPage = signal(0);
  readonly pageSize = 20;
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(page = 0): void {
    this.loading.set(true);
    this.error.set(null);
    
    this.adminApiService.getAuditLogs(page, this.pageSize).subscribe({
      next: (response: PageResponse<AdminAuditLog>) => {
        this.logs.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load audit logs. Please try again.');
        this.loading.set(false);
        console.error('Error loading audit logs:', err);
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.loadLogs(page);
    }
  }

  formatDetails(details: string): string {
    try {
      const parsed = JSON.parse(details);
      return parsed.message || details;
    } catch {
      return details;
    }
  }
}

