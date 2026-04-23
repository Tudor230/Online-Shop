import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private formBuilder = inject(FormBuilder);

  isLoginMode = signal(true);
  hide = signal(true);

  loginForm = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  registerForm = this.formBuilder.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    address: ['', Validators.required],
    age: ['', [Validators.required, Validators.min(1)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  onSubmitLogin() {
    this.loginForm.markAllAsTouched();

    if (this.loginForm.valid) {
      console.log('✅ === LOGIN ATTEMPT === ✅');
      console.log('Email:', this.loginForm.value.email);
      console.log('Password:', this.loginForm.value.password);

      console.log('Raw Payload:', JSON.stringify(this.loginForm.value, null, 2));
    } else {
      console.warn('❌ Login blocked: Form is invalid');
    }
  }

  onSubmitRegister() {
    this.registerForm.markAllAsTouched();

    if (this.registerForm.valid) {
      console.log('🚀 === NEW ACCOUNT CREATION === 🚀');
      console.log(
        'Name:',
        `${this.registerForm.value.firstName} ${this.registerForm.value.lastName}`,
      );
      console.log('Age:', this.registerForm.value.age);
      console.log('Address:', this.registerForm.value.address);
      console.log('Email:', this.registerForm.value.email);

      console.log('Raw Payload:', JSON.stringify(this.registerForm.value, null, 2));
    } else {
      console.warn('❌ Registration blocked: Form is invalid');
    }
  }
}
