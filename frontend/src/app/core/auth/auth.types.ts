export enum Role {
  ADMIN = 'ADMIN',
  SUPPORT = 'SUPPORT',
  CUSTOMER = 'CUSTOMER'
}

export interface AuthUser {
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

