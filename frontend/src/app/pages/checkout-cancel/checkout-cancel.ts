import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-checkout-cancel-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout-cancel.html'
})
export class CheckoutCancelPageComponent {}
