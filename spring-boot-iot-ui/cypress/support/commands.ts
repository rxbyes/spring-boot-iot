// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace Cypress {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      interface Chainable<Subject> {
            login(email: string, password: string): Chainable<void>;
            logout(): Chainable<void>;
      }
}

// -- This is a parent command --
Cypress.Commands.add('login', (email, password) => {
      cy.log(`Logging in ${email}`);
});

// -- This is a child command --
Cypress.Commands.add('logout', () => {
      cy.log('Logging out');
});

// -- This is a dual command --
Cypress.Commands.add('drag', { prevSubject: 'element' }, (subject, options) => {
      cy.log(`Dragging ${subject}`);
});

// -- This will overwrite an existing command --
Cypress.Commands.overwrite('visit', (originalFn, url, options) => {
      return originalFn(url, options);
});
