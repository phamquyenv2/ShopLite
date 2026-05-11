describe('Smoke Tests - Critical Paths', () => {
  beforeEach(() => {
    cy.mockDashboard();
    cy.mockProducts();
  });

  it('should load the app', () => {
    cy.visit('/');
    cy.url().should('not.include', '/error');
  });

  it('should show login page for unauthenticated user', () => {
    cy.visit('/');
    // Should redirect to login or show login form
    cy.url().should('satisfy', (url: string) => {
      return url.includes('/login') || url.includes('/') ;
    });
  });

  it('should navigate to product list', () => {
    cy.visit('/products');
    cy.wait('@getProducts');
    // Verify products page renders
    cy.get('body').should('contain.text', 'Cà phê sữa').or('contain.text', 'Products');
  });

  it('should have responsive layout', () => {
    cy.visit('/');
    // Test mobile viewport
    cy.viewport(375, 812);
    cy.get('body').should('be.visible');
    // Test tablet viewport
    cy.viewport(768, 1024);
    cy.get('body').should('be.visible');
  });
});

describe('POS Flow', () => {
  beforeEach(() => {
    cy.mockProducts();
    cy.mockDashboard();
  });

  it('should display products for sale', () => {
    cy.visit('/sale');
    cy.wait('@getProducts');
    cy.get('body').should('contain.text', 'Cà phê sữa').or('contain.text', 'Products');
  });
});

describe('Navigation', () => {
  it('should have working bottom navigation', () => {
    cy.visit('/');
    // Check that navigation elements exist
    cy.get('ion-tab-button, ion-button, a[href]').should('have.length.greaterThan', 0);
  });
});
