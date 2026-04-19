export type ProductSwatch =
  | 'primary'
  | 'primary-hover'
  | 'warning'
  | 'text-primary'
  | 'text-secondary'
  | 'surface-elevated';

export interface ProductColorOption {
  name: string;
  swatch: ProductSwatch;
}

export interface Product {
  id: string;
  category: string;
  title: string;
  rating: number;
  reviewCount: number;
  price: number;
  description: string;
  imagePlaceholder: string;
  imageGallery: readonly string[];
  availableColors: readonly ProductColorOption[];
}

export const mockProducts: readonly Product[] = [
  {
    id: 'ps5-console-shell',
    category: 'Console Mods',
    title: 'PS5 Console Shell',
    rating: 4.8,
    reviewCount: 312,
    price: 64.99,
    description: 'Custom matte shell plates with a precision fit that refresh your PS5 look in seconds.',
    imagePlaceholder: 'PS5 Shell Placeholder',
    imageGallery: ['Front View', 'Angled View', 'Back View', 'Setup View'],
    availableColors: [
      { name: 'Midnight', swatch: 'text-primary' },
      { name: 'Nebula Violet', swatch: 'primary' },
      { name: 'Slate', swatch: 'surface-elevated' }
    ]
  },
  {
    id: 'ps5-controller',
    category: 'Controllers',
    title: 'PS5 Controller',
    rating: 4.7,
    reviewCount: 524,
    price: 79.99,
    description: 'Adaptive trigger gaming controller tuned for low-latency sessions and long comfort.',
    imagePlaceholder: 'DualSense Placeholder',
    imageGallery: ['Hero Shot', 'Grip Detail', 'Trigger Detail', 'Desk Setup'],
    availableColors: [
      { name: 'Cosmic Red', swatch: 'warning' },
      { name: 'Starlight Blue', swatch: 'primary-hover' },
      { name: 'White', swatch: 'text-primary' }
    ]
  },
  {
    id: 'xbox-controller',
    category: 'Controllers',
    title: 'Xbox Controller',
    rating: 4.6,
    reviewCount: 409,
    price: 69.99,
    description: 'Textured wireless controller with ergonomic grip and responsive thumbstick precision.',
    imagePlaceholder: 'Xbox Pad Placeholder',
    imageGallery: ['Front View', 'Rear Triggers', 'Thumbsticks', 'Gaming Room'],
    availableColors: [
      { name: 'Carbon Black', swatch: 'surface-elevated' },
      { name: 'Pulse Blue', swatch: 'primary' },
      { name: 'Electric Volt', swatch: 'warning' }
    ]
  },
  {
    id: 'switch-2-joy-cons',
    category: 'Handheld Gear',
    title: 'Switch 2 Joy-Cons',
    rating: 4.9,
    reviewCount: 268,
    price: 89.99,
    description: 'Next-gen Joy-Cons with improved haptics and sharper motion tracking for party play.',
    imagePlaceholder: 'Joy-Cons Placeholder',
    imageGallery: ['Paired Set', 'Detached Pair', 'Charging Grip', 'Portable Setup'],
    availableColors: [
      { name: 'Neon Left', swatch: 'primary-hover' },
      { name: 'Neon Right', swatch: 'warning' },
      { name: 'Shadow Gray', swatch: 'text-secondary' }
    ]
  }
];
