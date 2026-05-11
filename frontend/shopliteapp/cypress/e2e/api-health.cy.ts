describe('API Health Checks', () => {
  const API_BASE = Cypress.env('API_BASE_URL') || 'http://localhost:8080';

  it('should respond to health endpoint', () => {
    cy.request({
      url: `${API_BASE}/actuator/health`,
      failOnStatusCode: false,
    }).then((response) => {
      expect(response.status).to.be.oneOf([200, 401, 403]);
    });
  });

  it('should have Swagger docs available', () => {
    cy.request({
      url: `${API_BASE}/v3/api-docs`,
      failOnStatusCode: false,
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.have.property('openapi');
    });
  });

  it('should require auth for protected endpoints', () => {
    cy.request({
      url: `${API_BASE}/api/v1/products`,
      failOnStatusCode: false,
    }).then((response) => {
      // Should return 401 when no token provided
      expect(response.status).to.be.oneOf([401, 403]);
    });
  });
});
