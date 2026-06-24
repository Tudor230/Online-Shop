import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminApiService } from '../../../core/admin/admin-api.service';
import { AdminUserDetail } from '../../../core/admin/admin.types';

@Component({
  selector: 'app-admin-user-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-form.html'
})
export class AdminUserFormComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly userId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly email = signal('');
  readonly firstName = signal('');
  readonly lastName = signal('');
  readonly role = signal('CUSTOMER');
  readonly isActive = signal(true);
  readonly password = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.userId.set(id);
      this.loadUser(id);
    }
  }

  private loadUser(id: string): void {
    this.loading.set(true);
    this.api.getUser(id).subscribe({
      next: (user: AdminUserDetail) => {
        this.email.set(user.email);
        this.firstName.set(user.firstName);
        this.lastName.set(user.lastName);
        this.role.set(user.role);
        this.isActive.set(user.isActive);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? err?.message ?? 'Failed to load user');
        this.loading.set(false);
      }
    });
  }

  save(): void {
    const validationError = this.validate();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const id = this.userId();
    if (id) {
      this.api.updateUser(id, {
        firstName: this.firstName(),
        lastName: this.lastName(),
        role: this.role(),
        isActive: this.isActive(),
      }).subscribe({
        next: () => this.router.navigate(['/admin/users']),
        error: (err) => {
          this.error.set(err?.error?.message ?? err?.message ?? 'Failed to update user');
          this.saving.set(false);
        }
      });
    } else {
      this.api.createUser({
        email: this.email(),
        firstName: this.firstName(),
        lastName: this.lastName(),
        role: this.role(),
        password: this.password(),
      }).subscribe({
        next: () => this.router.navigate(['/admin/users']),
        error: (err) => {
          this.error.set(err?.error?.message ?? err?.message ?? 'Failed to create user');
          this.saving.set(false);
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/admin/users']);
  }

  private validate(): string | null {
    if (!this.firstName().trim()) return 'First name is required';
    if (!this.lastName().trim()) return 'Last name is required';
    const id = this.userId();
    if (!id) {
      if (!this.email().trim()) return 'Email is required';
      if (!this.password().trim()) return 'Password is required';
      if (this.password().length < 8) return 'Password must be at least 8 characters';
    }
    return null;
  }
}
