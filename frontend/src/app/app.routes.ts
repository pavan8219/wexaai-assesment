import { Routes } from '@angular/router';

export const routes: Routes = [
    {
  path: 'investigation',
  loadComponent: () =>
    import('./pages/investigation/investigation.component')
      .then(m => m.InvestigationComponent)
}
];
