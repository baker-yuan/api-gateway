/* eslint-disable */

describe('Plugin Schema Test', () => {
  const timeout = 5000;
  const cases = require('../../fixtures/plugin-dataset.json');
  const domSelector = require('../../fixtures/selector.json');
  const data = require('../../fixtures/data.json');
  const pluginList = Object.keys(cases);

  before(() => {
    cy.clearLocalStorageSnapshot();
    cy.login();
    cy.saveLocalStorage();
  });

  beforeEach(() => {
    cy.restoreLocalStorage();
  });

  it('can visit plugin market', () => {
    cy.visit('/');
    cy.get('#root > div > section > aside > div > div:nth-child(1) > ul', { timeout })
      .contains('Plugin')
      .click();
    cy.get('.ant-pro-table > div > div > div.ant-pro-table-list-toolbar', { timeout })
      .contains('Enable')
      .click();
    cy.url().should('include', '/plugin/market');
  });

  describe('test plugin cases', () => {
    let globalPluginNames;

    before(function () {
      cy.login();
      cy.visit('/plugin/market');
      cy.saveLocalStorage();

      cy.get('main.ant-layout-content', { timeout })
        .find('div.ant-card-head span', { timeout })
        .then((cards) => Array.from(cards).map((card) => card.innerText))
        .then((pluginNames) => {
          globalPluginNames = pluginNames;
        });
    });

    beforeEach(() => {
      cy.restoreLocalStorage();
    });

    pluginList
      .map((name) => ({ name, cases: cases[name].filter((v) => v.type !== 'consumer') }))
      .filter(({ cases }) => cases.length > 0)
      .forEach(({ name, cases }) => {
        cases.forEach((c, i) => {
          it(`${name} plugin #${i + 1} case`, () => {
            if (globalPluginNames.includes(name)) {
              cy.configurePlugin({ name, content: c });
            } else {
              cy.log(`${name} not a global plugin, skipping`);
            }
          });
        });
      });

    it('should edit the plugin', function () {
      cy.visit('/plugin/list');

      cy.get(domSelector.refresh).click();
      cy.contains('Configure').click();
      cy.get(domSelector.monacoScroll).should('exist');
      cy.get(domSelector.disabledSwitcher).click();
      cy.contains('button', 'Submit').click();
    });

    it('should delete plugin list', function () {
      cy.visit('/plugin/list');
      cy.get(domSelector.refresh).click();
      cy.get(domSelector.paginationOptions).click();
      cy.contains('50 / page').should('be.visible').click();
      cy.get(domSelector.fiftyPerPage).should('exist');
      cy.location('href').should('include', 'pageSize=50');

      cy.get(domSelector.deleteButton, { timeout })
        .should('exist')
        .each(($el) => {
          cy.wrap($el).click().click({ timeout });
          cy.contains('button', 'Confirm').click({ force: true });
          cy.get(domSelector.notification).should('contain', data.deletePluginSuccess);
          cy.get(domSelector.notificationCloseIcon).click().should('not.exist');
        });

      // check if plugin list is empty
      cy.get(domSelector.empty).should('be.visible');
    });

    it('click plugin Edit button the existing configuration should display', () => {
      cy.visit('/');
      cy.contains('Consumer').click();
      cy.get('.ant-empty-normal').should('be.visible');
      cy.contains('Create').click();

      cy.get('#username').type('test');
      cy.contains('Next').click();
      cy.contains('.ant-card', 'client-control').within(() => {
        cy.contains('Enable').click({
          force: true,
        });
      });
      cy.focused('.ant-drawer-content').should('exist');
      cy.get('.view-zones').should('exist');
      cy.contains('max_body_size').should('be.visible');
      cy.window().then((window) => {
        window.monacoEditor.setValue(JSON.stringify({ max_body_size: 1024 }));
        cy.contains('button', 'Submit').click();
      });
      cy.wait(3000);
      cy.contains('.ant-card', 'client-control').within(() => {
        cy.contains('Enable').click({
          force: true,
        });
      });
      cy.focused('.ant-drawer-content').should('exist');
      cy.contains('1024').should('be.visible');
    });
  });
});
