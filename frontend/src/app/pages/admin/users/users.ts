import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminUserList, PageResponse } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html'
})
export class AdminUsersComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  readonly users = signal<AdminUserList[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.api.getUsers(this.page()).subscribe({
      next: (res: PageResponse<AdminUserList>) => {
        this.users.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
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
    this.api.updateUser(user.id, { isActive: !user.isActive }).subscribe({
      next: () => this.loadUsers()
    });
  }

  deleteUser(id: string): void {
    if (!confirm('Delete this user?')) return;
    this.api.deleteUser(id).subscribe({
      next: () => this.loadUsers()
    });
  }
}
