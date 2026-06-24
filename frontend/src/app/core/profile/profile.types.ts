export interface Address {
  id: string;
  recipientName: string;
  phoneNumber: string | null;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface Profile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  defaultShippingAddressId: string | null;
  defaultBillingAddressId: string | null;
  addresses: Address[];
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
}

export interface CreateAddressRequest {
  recipientName: string;
  phoneNumber: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface SetPrimaryAddressRequest {
  addressId: string;
}
