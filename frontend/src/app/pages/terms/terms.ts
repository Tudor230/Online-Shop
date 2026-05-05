import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

type TermsBlock = {
  readonly title: string;
  readonly points: readonly string[];
};

@Component({
  selector: 'app-terms-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './terms.html',
})
export class TermsPageComponent {
  readonly highlights: readonly string[] = [
    'Guest checkout is available and account creation is optional.',
    'Custom orders begin assembly only after payment authorization and component confirmation.',
    'Returns, refunds, and warranty are handled according to our dedicated policy pages.',
  ];

  readonly termsBlocks: readonly TermsBlock[] = [
    {
      title: 'Using our store',
      points: [
        'You must provide accurate checkout and contact information.',
        'You are responsible for safeguarding your account credentials.',
        'You may not use the store to commit fraud, abuse services, or violate applicable law.',
      ],
    },
    {
      title: 'Products and custom configurations',
      points: [
        'We describe products and compatibility as clearly as possible, but minor visual or packaging differences may occur.',
        'For custom builds, compatibility checks are performed before assembly; we may contact you if changes are required.',
        'Performance results can vary depending on software, environment, and usage profile.',
      ],
    },
    {
      title: 'Pricing and payments',
      points: [
        'Displayed prices may include or exclude taxes depending on your region and legal requirements.',
        'We reserve the right to correct obvious pricing errors before order acceptance.',
        'Payment methods, anti-fraud checks, and authorization outcomes are handled by trusted payment partners.',
      ],
    },
    {
      title: 'Order acceptance and cancellation',
      points: [
        'Submitting an order does not guarantee acceptance; confirmation is sent once checks pass.',
        'Orders can be canceled before fulfillment starts; custom assembly orders may have narrower cancellation windows.',
        'If we cannot fulfill your order, we issue a full refund for unfulfilled items.',
      ],
    },
    {
      title: 'Liability and service availability',
      points: [
        'We provide the store and services with reasonable care but cannot guarantee uninterrupted availability at all times.',
        'To the extent permitted by law, our liability is limited to foreseeable damages directly caused by our breach.',
        'Nothing in these terms excludes rights that cannot be waived under consumer protection law.',
      ],
    },
  ];

  readonly prohibitedUses: readonly string[] = [
    'Interfering with store security, scraping protected data, or bypassing access controls',
    'Using bots to abuse inventory limits, pricing, promotions, or checkout flows',
    'Uploading malicious code, phishing content, or fake support messages',
    'Misusing social sharing, reviews, or messaging features to spread spam or harmful content',
  ];
}
