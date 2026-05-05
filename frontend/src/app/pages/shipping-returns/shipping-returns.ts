import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

type ShippingOption = {
  readonly name: string;
  readonly deliveryWindow: string;
  readonly cost: string;
  readonly bestFor: string;
};

@Component({
  selector: 'app-shipping-returns-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './shipping-returns.html',
})
export class ShippingReturnsPageComponent {
  readonly shippingOptions: readonly ShippingOption[] = [
    {
      name: 'Standard delivery',
      deliveryWindow: '2-4 business days after dispatch',
      cost: 'Free over EUR 149, otherwise EUR 6.90',
      bestFor: 'Most keyboard, mouse, and accessory orders',
    },
    {
      name: 'Priority delivery',
      deliveryWindow: '1-2 business days after dispatch',
      cost: 'EUR 14.90',
      bestFor: 'Urgent gifts, replacement hardware, tournament prep',
    },
    {
      name: 'Insured heavy shipment',
      deliveryWindow: '2-5 business days after dispatch',
      cost: 'Calculated at checkout by weight and destination',
      bestFor: 'Custom PC builds, monitors, and multi-item bundles',
    },
  ];

  readonly returnSteps: readonly string[] = [
    'Open your order in My Account and choose Start a return.',
    'Pick a reason and choose refund or replacement where available.',
    'Use the prepaid label we generate, then pack items safely with their accessories.',
    'Once we receive and inspect the package, we update you by email within one business day.',
  ];

  readonly nonReturnableItems: readonly string[] = [
    'Products customized to your request (laser engraving, custom cable length, paint finish)',
    'Opened software activation cards and digital license keys',
    'Consumables with broken seals (thermal paste syringes, cleaning kits, adhesive pads)',
    'Items damaged by liquid exposure, electrical misuse, or unsupported overvoltage modifications',
  ];
}
