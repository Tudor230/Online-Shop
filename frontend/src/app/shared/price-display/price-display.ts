import { Component, Input } from '@angular/core';

const numberFormat = new Intl.NumberFormat('ro-RO', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

@Component({
  selector: 'app-price-display',
  standalone: true,
  template: `
    @if (variant === 'muted') {
      <span class="font-mono text-text-muted">{{ formattedValue }}&nbsp;<span class="font-sans text-text-muted/70 text-[0.8em]">{{ currencyCode }}</span></span>
    } @else {
      <span class="font-mono font-bold tracking-tight text-text-primary">{{ formattedValue }}</span><span class="font-sans text-text-muted/70 font-medium text-[0.8em]">&nbsp;{{ currencyCode }}</span>
    }
  `,
  host: { class: 'inline' }
})
export class PriceDisplayComponent {
  @Input({ required: true }) value!: number;
  @Input() currencyCode = 'RON';
  @Input() variant: 'default' | 'muted' = 'default';

  get formattedValue(): string {
    return numberFormat.format(this.value);
  }
}
