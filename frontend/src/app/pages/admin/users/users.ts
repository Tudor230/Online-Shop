import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminUserList, PageResponse } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './users.html'
})
export class AdminUsersComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly users = signal<AdminUserList[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly searchQuery = signal('');
  readonly syncing = signal(false);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.api.getUsers(this.page()).subscribe({
      next: (res: PageResponse<AdminUserList>) => {
        this.users.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Failed to load users');
        this.loading.set(false);
      }
    });
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadUsers();
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      this.loadUsers();
    }
  }

  toggleActive(user: AdminUserList): void {
    this.actionError.set(null);
    this.api.updateUser(user.id, { isActive: !user.isActive }).subscribe({
      next: () => this.loadUsers(),
      error: (err) => this.actionError.set(err?.message ?? 'Failed to update user status')
    });
  }

  deleteUser(id: string): void {
    if (!confirm('Delete this user?')) return;
    this.actionError.set(null);
    this.api.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => this.actionError.set(err?.message ?? 'Failed to delete user')
    });
  }

  syncFromKeycloak(): void {
    this.syncing.set(true);
    this.actionError.set(null);
    this.api.syncUsers().subscribe({
      next: (result) => {
        this.syncing.set(false);
        this.loadUsers();
        alert(`Sync complete: ${result.created} created, ${result.updated} updated`);
      },
      error: (err) => {
        this.syncing.set(false);
        this.actionError.set(err?.error?.message ?? err?.message ?? 'Failed to sync users');
      }
    });
  }

  filteredUsers(): AdminUserList[] {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.users();
    return this.users().filter(u =>
      u.email.toLowerCase().includes(q) ||
      u.firstName.toLowerCase().includes(q) ||
      u.lastName.toLowerCase().includes(q)
    );
  }
}
