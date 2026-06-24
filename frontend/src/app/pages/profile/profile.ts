import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, type AbstractControl, type ValidationErrors } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { KeycloakAuthService } from '../../core/auth/keycloak-auth.service';
import { ProfileApiService } from '../../core/profile/profile-api.service';
import { Address, Profile } from '../../core/profile/profile.types';
import { PhoneInputComponent } from '../../shared/phone-input/phone-input';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PhoneInputComponent],
  templateUrl: './profile.html'
})
export class ProfilePageComponent {
  private readonly profileApiService = inject(ProfileApiService);
  private readonly authState = inject(AuthStateService);
  private readonly keycloakAuthService = inject(KeycloakAuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly countries = [
    { code: 'RO', name: 'Romania' },
    { code: 'US', name: 'United States' },
    { code: 'GB', name: 'United Kingdom' },
    { code: 'DE', name: 'Germany' },
    { code: 'FR', name: 'France' },
    { code: 'IT', name: 'Italy' },
    { code: 'ES', name: 'Spain' },
    { code: 'NL', name: 'Netherlands' },
    { code: 'BE', name: 'Belgium' },
    { code: 'AT', name: 'Austria' },
    { code: 'HU', name: 'Hungary' },
    { code: 'PL', name: 'Poland' },
    { code: 'SE', name: 'Sweden' },
    { code: 'DK', name: 'Denmark' },
    { code: 'FI', name: 'Finland' },
    { code: 'NO', name: 'Norway' },
    { code: 'PT', name: 'Portugal' },
    { code: 'GR', name: 'Greece' },
    { code: 'CZ', name: 'Czech Republic' },
    { code: 'SK', name: 'Slovakia' },
    { code: 'BG', name: 'Bulgaria' },
    { code: 'HR', name: 'Croatia' },
    { code: 'SI', name: 'Slovenia' },
    { code: 'IE', name: 'Ireland' },
    { code: 'CH', name: 'Switzerland' },
    { code: 'UA', name: 'Ukraine' },
    { code: 'TR', name: 'Turkey' },
    { code: 'CA', name: 'Canada' },
    { code: 'AU', name: 'Australia' },
    { code: 'JP', name: 'Japan' },
    { code: 'CN', name: 'China' },
    { code: 'BR', name: 'Brazil' },
    { code: 'MX', name: 'Mexico' },
    { code: 'IN', name: 'India' },
    { code: 'KR', name: 'South Korea' },
    { code: 'SG', name: 'Singapore' },
    { code: 'NZ', name: 'New Zealand' },
    { code: 'ZA', name: 'South Africa' },
    { code: 'AE', name: 'United Arab Emirates' },
  ];

  private readonly postalPattern = /^[A-Za-z0-9\-\s]{3,20}$/;

  static phoneValidator(control: AbstractControl): ValidationErrors | null {
    const value = (control.value ?? '').replace(/\D/g, '');
    if (!value) return null;
    if (value.length < 7) return { phoneMinLength: true };
    return null;
  }

  readonly isAuthenticated = this.authState.isAuthenticated;
  readonly profile = signal<Profile | null>(null);

  readonly isLoadingProfile = signal(true);
  readonly profileError = signal<string | null>(null);

  readonly isSavingProfile = signal(false);
  readonly profileSaveError = signal<string | null>(null);

  readonly isCreatingAddress = signal(false);
  readonly addressCreateError = signal<string | null>(null);

  readonly shippingUpdateInProgressId = signal<string | null>(null);
  readonly billingUpdateInProgressId = signal<string | null>(null);
  readonly deleteInProgressId = signal<string | null>(null);
  readonly primarySelectionError = signal<string | null>(null);
  readonly deleteAddressError = signal<string | null>(null);

  readonly profileForm = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]]
  });

  readonly addressForm = this.formBuilder.nonNullable.group({
    recipientName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
    phoneNumber: ['', [ProfilePageComponent.phoneValidator]],
    addressLine1: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
    addressLine2: ['', [Validators.maxLength(255)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    state: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    postalCode: ['', [Validators.required, Validators.pattern(this.postalPattern), Validators.maxLength(20)]],
    country: ['', [Validators.required]],
  });

  readonly addresses = computed(() => this.profile()?.addresses ?? []);

  constructor() {
    if (!this.isAuthenticated()) {
      this.isLoadingProfile.set(false);
      return;
    }

    this.loadProfile();
  }

  async login(): Promise<void> {
    await this.keycloakAuthService.login();
  }

  saveProfileDetails(): void {
    if (this.profileForm.invalid || this.isSavingProfile()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.profileSaveError.set(null);
    this.isSavingProfile.set(true);

    this.profileApiService
      .updateProfile({
        firstName: this.profileForm.controls.firstName.value,
        lastName: this.profileForm.controls.lastName.value
      })
      .pipe(finalize(() => this.isSavingProfile.set(false)))
      .subscribe({
        next: (profile) => {
          this.applyProfile(profile);
          void this.refreshTokenAfterProfileUpdate();
        },
        error: () => this.profileSaveError.set('Could not save profile details. Please try again.')
      });
  }

  addAddress(): void {
    if (this.addressForm.invalid || this.isCreatingAddress()) {
      this.addressForm.markAllAsTouched();
      return;
    }

    this.addressCreateError.set(null);
    this.isCreatingAddress.set(true);

    this.profileApiService
      .createAddress({
        recipientName: this.addressForm.controls.recipientName.value,
        phoneNumber: this.addressForm.controls.phoneNumber.value,
        addressLine1: this.addressForm.controls.addressLine1.value,
        addressLine2: this.addressForm.controls.addressLine2.value,
        city: this.addressForm.controls.city.value,
        state: this.addressForm.controls.state.value,
        postalCode: this.addressForm.controls.postalCode.value,
        country: this.addressForm.controls.country.value
      })
      .pipe(finalize(() => this.isCreatingAddress.set(false)))
      .subscribe({
        next: (profile) => {
          this.applyProfile(profile);
          this.addressForm.reset({
            recipientName: '',
            phoneNumber: '',
            addressLine1: '',
            addressLine2: '',
            city: '',
            state: '',
            postalCode: '',
            country: ''
          });
        },
        error: () => this.addressCreateError.set('Could not add address. Please verify your fields and retry.')
      });
  }

  setPrimaryShipping(address: Address): void {
    if (this.shippingUpdateInProgressId()) {
      return;
    }

    this.primarySelectionError.set(null);
    this.shippingUpdateInProgressId.set(address.id);

    this.profileApiService
      .setPrimaryShipping({ addressId: address.id })
      .pipe(finalize(() => this.shippingUpdateInProgressId.set(null)))
      .subscribe({
        next: (profile) => this.applyProfile(profile),
        error: () => this.primarySelectionError.set('Could not update primary shipping address.')
      });
  }

  setPrimaryBilling(address: Address): void {
    if (this.billingUpdateInProgressId()) {
      return;
    }

    this.primarySelectionError.set(null);
    this.billingUpdateInProgressId.set(address.id);

    this.profileApiService
      .setPrimaryBilling({ addressId: address.id })
      .pipe(finalize(() => this.billingUpdateInProgressId.set(null)))
      .subscribe({
        next: (profile) => this.applyProfile(profile),
        error: () => this.primarySelectionError.set('Could not update primary billing address.')
      });
  }

  isPrimaryShipping(addressId: string): boolean {
    return this.profile()?.defaultShippingAddressId === addressId;
  }

  isPrimaryBilling(addressId: string): boolean {
    return this.profile()?.defaultBillingAddressId === addressId;
  }

  canDeleteAddress(addressId: string): boolean {
    return !this.isPrimaryShipping(addressId) && !this.isPrimaryBilling(addressId);
  }

  deleteAddress(address: Address): void {
    if (this.deleteInProgressId() || !this.canDeleteAddress(address.id)) {
      return;
    }

    this.deleteAddressError.set(null);
    this.deleteInProgressId.set(address.id);

    this.profileApiService
      .deleteAddress(address.id)
      .pipe(finalize(() => this.deleteInProgressId.set(null)))
      .subscribe({
        next: (profile) => this.applyProfile(profile),
        error: () => this.deleteAddressError.set('Could not delete address. Only non-primary addresses can be removed.')
      });
  }

  private loadProfile(): void {
    this.isLoadingProfile.set(true);
    this.profileError.set(null);

    this.profileApiService
      .getProfile()
      .pipe(finalize(() => this.isLoadingProfile.set(false)))
      .subscribe({
        next: (profile) => this.applyProfile(profile),
        error: () => this.profileError.set('Could not load your profile right now. Please refresh and retry.')
      });
  }

  private applyProfile(profile: Profile): void {
    this.profile.set(profile);
    this.profileForm.setValue({
      firstName: profile.firstName,
      lastName: profile.lastName
    });
  }

  private async refreshTokenAfterProfileUpdate(): Promise<void> {
    const tokenRefreshed = await this.keycloakAuthService.refreshUserToken();
    if (!tokenRefreshed) {
      this.profileSaveError.set('Profile saved, but session claims could not be refreshed. Please sign in again.');
    }
  }

  getAddressError(controlName: keyof typeof this.addressForm.controls): string | null {
    const control = this.addressForm.controls[controlName];
    if (!control.touched || !control.errors) {
      return null;
    }

    if (control.errors['required']) {
      return 'This field is required.';
    }
    if (control.errors['minlength']) {
      return 'Please enter at least two characters.';
    }
    if (control.errors['phoneMinLength']) {
      return 'Phone number is too short.';
    }
    if (control.errors['maxlength']) {
      return 'Please shorten this value.';
    }
    if (control.errors['pattern']) {
      return 'Please use a valid format.';
    }

    return 'Please enter a valid value.';
  }

  formatPhoneDisplay(phone: string | null): string {
    if (!phone) return '';
    const digits = phone.replace(/\D/g, '');
    let result = '+' + digits.slice(0, 3) + ' ';
    let remaining = digits.slice(3);
    while (remaining.length > 0) {
      result += remaining.slice(0, 3);
      remaining = remaining.slice(3);
      if (remaining.length > 0) result += ' ';
    }
    return result;
  }
}
