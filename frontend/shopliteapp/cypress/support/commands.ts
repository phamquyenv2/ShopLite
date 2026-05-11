/// <reference types="cypress" />

Cypress.Commands.add('login', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('input[type="email"], input[placeholder*="email" i]').type(email);
  cy.get('input[type="password"]').type(password);
  cy.get('button[type="submit"]').click();
});

Cypress.Commands.add('mockDashboard', () => {
  cy.intercept('GET', '**/api/v1/dashboard/**', {
    statusCode: 200,
    body: {
      revenue: 15000000,
      orderCount: 25,
      profit: 4500000,
    },
  }).as('getDashboard');
});

Cypress.Commands.add('mockProducts', () => {
  cy.intercept('GET', '**/api/v1/products**', {
    statusCode: 200,
    body: [
      { id: 1, name: 'Cà phê sữa', price: 25000, barcode: '1234567890' },
      { id: 2, name: 'Trà chanh', price: 20000, barcode: '0987654321' },
    ],
  }).as('getProducts');
});

Cypress.Commands.add('mockCategories', () => {
  cy.intercept('GET', '**/api/v1/categories**', {
    statusCode: 200,
    body: [
      { id: 1, name: 'Đồ uống' },
      { id: 2, name: 'Đồ ăn' },
    ],
  }).as('getCategories');
});

declare global {
  namespace Cypress {
    interface Chainable {
      login(email: string, password: string): Chainable<void>;
      mockDashboard(): Chainable<void>;
      mockProducts(): Chainable<void>;
      mockCategories(): Chainable<void>;
    }
  }
}
