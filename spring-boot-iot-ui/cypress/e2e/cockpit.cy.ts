describe('Cockpit View', () => {
      beforeEach(() => {
            cy.visit('/');
      });

      it('renders cockpit view', () => {
            cy.get('.cockpit-view').should('exist');
      });

      it('renders role switcher', () => {
            cy.get('.role-switcher').should('exist');
            cy.get('.role-switcher button').should('have.length', 3);
      });

      it('renders risk trend chart', () => {
            cy.get('.risk-trend-chart').should('exist');
      });

      it('renders risk distribution chart', () => {
            cy.get('.risk-distribution-chart').should('exist');
      });

      it('renders early warning dashboard', () => {
            cy.get('.early-warning-dashboard').should('exist');
      });

      it('renders quick action cards', () => {
            cy.get('.quick-action-cards').should('exist');
            cy.get('.quick-action-card').should('have.length', 4);
      });
});
