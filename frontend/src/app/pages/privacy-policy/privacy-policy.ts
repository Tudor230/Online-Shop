import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

type PrivacyCategory = {
  readonly title: string;
  readonly examples: readonly string[];
};

type RetentionRule = {
  readonly dataType: string;
  readonly retention: string;
};

@Component({
  selector: 'app-privacy-policy-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './privacy-policy.html',
})
export class PrivacyPolicyPageComponent {
  readonly dataCategories: readonly PrivacyCategory[] = [
    {
      title: 'Information you provide directly',
      examples: [
        'Account details such as name, email, and phone number',
        'Shipping and billing addresses',
        'Order notes for custom builds, engraving, or assembly requests',
        'Messages you send to support',
      ],
    },
    {
      title: 'Information collected automatically',
      examples: [
        'Device, browser, and approximate location based on IP',
        'Pages visited, clicks, and checkout funnel events',
        'Referrer information and campaign identifiers',
        'Cookie or local storage identifiers',
      ],
    },
    {
      title: 'Information from partners',
      examples: [
        'Payment verification status from payment providers',
        'Delivery updates from shipping carriers',
        'Fraud prevention signals from security providers',
        'Aggregated audience insights from analytics services',
      ],
    },
  ];

  readonly usagePurposes: readonly string[] = [
    'Process and deliver orders, including custom hardware assembly workflows',
    'Provide account features, customer support, and warranty handling',
    'Prevent fraud, abuse, and unauthorized account access',
    'Improve product pages, search, and checkout through analytics',
    'Send service notifications and, if you opt in, marketing updates',
  ];

  readonly analyticsNotes: readonly string[] = [
    'We plan to use cookie-based analytics to understand how the store is used and where users drop off in key flows.',
    'Some analytics features may use third-party services that process pseudonymous identifiers and event data on our behalf.',
    'You can manage non-essential analytics cookies from Cookie settings. Essential cookies stay enabled because the store cannot function without them.',
    'Where required by law, we request consent before loading non-essential cookies.',
  ];

  readonly sharingCases: readonly string[] = [
    'Payment processors to authorize and settle transactions',
    'Shipping carriers to deliver packages and provide tracking updates',
    'Cloud, hosting, and support tooling providers acting as data processors',
    'Analytics and advertising partners, only in accordance with your preferences',
    'Authorities when legally required',
  ];

  readonly retentionRules: readonly RetentionRule[] = [
    {
      dataType: 'Order and invoice records',
      retention: 'Up to 10 years where tax/accounting law requires it',
    },
    {
      dataType: 'Account profile data',
      retention: 'Until account deletion request, plus a short backup window',
    },
    {
      dataType: 'Support and warranty requests',
      retention: 'Up to 3 years after issue closure for service continuity',
    },
    {
      dataType: 'Analytics cookie identifiers',
      retention: 'Typically 13 months or less, depending on tool settings',
    },
  ];

  readonly userRights: readonly string[] = [
    'Access a copy of your personal data',
    'Correct inaccurate or outdated information',
    'Request deletion where legal obligations do not require retention',
    'Restrict or object to certain processing activities',
    'Withdraw consent for optional cookies and marketing at any time',
    'Request data portability where applicable',
  ];
}
