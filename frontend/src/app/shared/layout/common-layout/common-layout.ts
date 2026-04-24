import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from '../footer/footer';
import { HeaderComponent } from '../header/header';

@Component({
  selector: 'app-common-layout',
  standalone: true,
  imports: [HeaderComponent, RouterOutlet, FooterComponent],
  templateUrl: './common-layout.html'
})
export class CommonLayoutComponent {}

