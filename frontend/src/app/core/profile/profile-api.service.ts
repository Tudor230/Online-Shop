import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { keycloakConfig } from '../../keycloak.config';
import {
  CreateAddressRequest,
  Profile,
  SetPrimaryAddressRequest,
  UpdateProfileRequest,
} from './profile.types';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly profileBaseUrl = `${keycloakConfig.backendApiUrl}/profile`;

  getProfile(): Observable<Profile> {
    return this.httpClient.get<Profile>(this.profileBaseUrl);
  }

  updateProfile(payload: UpdateProfileRequest): Observable<Profile> {
    return this.httpClient.patch<Profile>(this.profileBaseUrl, payload);
  }

  createAddress(payload: CreateAddressRequest): Observable<Profile> {
    return this.httpClient.post<Profile>(`${this.profileBaseUrl}/addresses`, payload);
  }

  setPrimaryShipping(payload: SetPrimaryAddressRequest): Observable<Profile> {
    return this.httpClient.patch<Profile>(`${this.profileBaseUrl}/primary-shipping`, payload);
  }

  setPrimaryBilling(payload: SetPrimaryAddressRequest): Observable<Profile> {
    return this.httpClient.patch<Profile>(`${this.profileBaseUrl}/primary-billing`, payload);
  }

  deleteAddress(addressId: string): Observable<Profile> {
    return this.httpClient.delete<Profile>(`${this.profileBaseUrl}/addresses/${addressId}`);
  }
}
