import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormsModule } from '@angular/forms';

interface CountryCode {
  code: string;
  label: string;
}

const COUNTRY_CODES: CountryCode[] = [
  { code: '+40', label: 'RO +40' },
  { code: '+1',  label: 'US +1' },
  { code: '+44', label: 'GB +44' },
  { code: '+49', label: 'DE +49' },
  { code: '+33', label: 'FR +33' },
  { code: '+39', label: 'IT +39' },
  { code: '+34', label: 'ES +34' },
  { code: '+31', label: 'NL +31' },
  { code: '+32', label: 'BE +32' },
  { code: '+43', label: 'AT +43' },
  { code: '+36', label: 'HU +36' },
  { code: '+48', label: 'PL +48' },
  { code: '+46', label: 'SE +46' },
  { code: '+358', label: 'FI +358' },
  { code: '+30', label: 'GR +30' },
  { code: '+7',  label: 'RU +7' },
  { code: '+380', label: 'UA +380' },
  { code: '+90', label: 'TR +90' },
  { code: '+81', label: 'JP +81' },
  { code: '+86', label: 'CN +86' },
];

function formatLocal(digits: string): string {
  if (!digits) return '';
  if (digits.length <= 3) return digits;
  if (digits.length <= 6) return digits.slice(0, 3) + ' ' + digits.slice(3);
  return digits.slice(0, 3) + ' ' + digits.slice(3, 6) + ' ' + digits.slice(6);
}

function stripNonDigits(value: string): string {
  return value.replace(/\D/g, '');
}

function parseFullNumber(full: string): { countryCode: string; localDigits: string } {
  const digits = stripNonDigits(full);
  for (const cc of [...COUNTRY_CODES].sort((a, b) => b.code.length - a.code.length)) {
    const prefix = stripNonDigits(cc.code);
    if (digits.startsWith(prefix)) {
      return { countryCode: cc.code, localDigits: digits.slice(prefix.length) };
    }
  }
  return { countryCode: '+40', localDigits: digits };
}

@Component({
  selector: 'app-phone-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="flex gap-2">
      <select
        class="select shrink-0 px-2.5 py-2 text-sm"
        [style.width]="'6.5rem'"
        [(ngModel)]="countryCode"
        (ngModelChange)="syncToControl()">
        @for (cc of countryCodes; track cc.code) {
          <option [value]="cc.code">{{ cc.label }}</option>
        }
      </select>
      <input
        #phoneField
        type="tel"
        class="input flex-1 px-3 py-2 text-sm"
        placeholder="7XX XXX XXX"
        [value]="displayText"
        (input)="onInput(phoneField.value, phoneField)"
        (keydown)="onKeydown($event)"
        (focus)="onFocus()"
        (blur)="onBlur()"
        (paste)="onPaste($event, phoneField)"
      />
    </div>
  `,
  host: { class: 'block' }
})
export class PhoneInputComponent implements OnInit {
  @Input({ required: true }) formControl!: FormControl<string>;

  readonly countryCodes = COUNTRY_CODES;
  countryCode = '+40';
  displayText = '';
  private rawDigits = '';
  private focused = false;

  ngOnInit(): void {
    const initial = this.formControl.value || '';
    if (initial) {
      const parsed = parseFullNumber(initial);
      this.countryCode = parsed.countryCode;
      this.rawDigits = parsed.localDigits;
    }
    this.updateDisplay();
  }

  onKeydown(event: KeyboardEvent): void {
    const allowed = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Tab', 'Home', 'End', 'Enter', 'Escape'];
    if (allowed.includes(event.key)) return;
    if (event.ctrlKey || event.metaKey) return;
    if (!/^\d$/.test(event.key)) { event.preventDefault(); return; }
    if (this.rawDigits.length >= 15) { event.preventDefault(); }
  }

  onInput(value: string, input: HTMLInputElement): void {
    let digits = stripNonDigits(value).slice(0, 15);
    if (digits === this.rawDigits) return;
    this.rawDigits = digits;
    this.displayText = this.focused ? this.rawDigits : formatLocal(this.rawDigits);
    if (input.value !== this.displayText) {
      input.value = this.displayText;
    }
    this.syncToControl();
  }

  onPaste(event: ClipboardEvent, input: HTMLInputElement): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text') ?? '';
    const digits = stripNonDigits(pasted).slice(0, 15);
    if (!digits) return;
    this.rawDigits = digits;
    this.displayText = this.focused ? this.rawDigits : formatLocal(this.rawDigits);
    input.value = this.displayText;
    this.syncToControl();
  }

  onFocus(): void {
    this.focused = true;
    this.displayText = this.rawDigits;
  }

  onBlur(): void {
    this.focused = false;
    this.displayText = formatLocal(this.rawDigits);
    this.formControl.markAsTouched();
  }

  syncToControl(): void {
    const combined = this.rawDigits ? this.countryCode + this.rawDigits : '';
    if (this.formControl.value !== combined) {
      this.formControl.setValue(combined);
      this.formControl.markAsDirty();
    }
  }

  private updateDisplay(): void {
    this.displayText = this.focused ? this.rawDigits : formatLocal(this.rawDigits);
  }
}
