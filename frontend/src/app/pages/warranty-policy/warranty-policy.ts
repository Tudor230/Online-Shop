import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

type WarrantyCoverage = {
  readonly category: string;
  readonly period: string;
  readonly details: string;
};

@Component({
  selector: 'app-warranty-policy-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './warranty-policy.html'
})
export class WarrantyPolicyPageComponent {
  readonly coverageByCategory: readonly WarrantyCoverage[] = [
    {
      category: 'Custom desktop builds',
      period: '24 months',
      details: 'Covers assembly defects, power delivery faults, and component failures under normal use.'
    },
    {
      category: 'Monitors and displays',
      period: '24 months',
      details: 'Covers panel, backlight, and controller defects. Pixel policy follows manufacturer thresholds.'
    },
    {
      category: 'Peripherals and accessories',
      period: '12 months',
      details: 'Covers switches, sensors, cable connectors, and charging electronics.'
    },
    {
      category: 'Battery-powered devices',
      period: '12 months battery / 24 months device',
      details: 'Battery wear from normal charge cycles is excluded unless performance drops below warranty specs.'
    }
  ];

  readonly claimSteps: readonly string[] = [
    'Start a claim from My Account and choose Warranty request.',
    'Share a short issue description and upload photos or a quick video.',
    'Our technicians respond within one business day with troubleshooting or return instructions.',
    'If confirmed, we repair, replace, or refund based on stock and repair feasibility.'
  ];

  readonly notCovered: readonly string[] = [
    'Damage from drops, liquid exposure, fire, or unauthorized electrical modifications',
    'Cosmetic wear that does not affect function (minor scratches, keycap shine, paint fade)',
    'Failures caused by unsupported firmware, mining BIOS flashes, or overclock settings outside validated profiles',
    'Normal consumable wear (fans, thermal compound, batteries) beyond expected service life'
  ];
}

