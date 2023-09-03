/* eslint-disable no-undef */

context('Create and Delete Upstream With Custom CHash Key', () => {
  const selector = {
    name: '#name',
    description: '#desc',
    roundRobinSelect: '[title="Round Robin"]',
    varSelect: '[title="vars"]',
    defaultCHashKey: '[value="remote_addr"]',
    nodes_0_host: '#submitNodes_0_host',
    nodes_0_port: '#submitNodes_0_port',
    nodes_0_weight: '#submitNodes_0_weight',
    upstreamType: '.ant-select-item-option-content',
    hashPosition: '.ant-select-item-option-content',
    chash_key: '#key',
    notification: '.ant-notification-notice-message',
    nameSelector: '[title=Name]',
  };

  const data = {
    upstreamName: 'test_upstream',
    description: 'desc_by_autotest',
    custom_key: 'custom_key',
    new_key: 'new_key',
    ip: '127.0.0.1',
    port: '7000',
    weight: '1',
    createUpstreamSuccess: 'Create Upstream Successfully',
    configureUpstreamSuccess: 'Configure Upstream Successfully',
    deleteUpstreamSuccess: 'Delete Upstream Successfully',
  };

  beforeEach(() => {
    cy.login();
  });

  it('should create upstream with custom chash key', function () {
    cy.visit('/');
    cy.contains('Upstream').click();
    cy.contains('Create').click();

    cy.get(selector.name).type(data.upstreamName);
    cy.get(selector.description).type(data.description);

    cy.get(selector.roundRobinSelect).click();
    cy.get(selector.upstreamType).within(() => {
      cy.contains('CHash').click();
    });
    cy.get('[title="Key"]').should('exist');
    // Key is hidden when Hasn on select consumer
    cy.get(selector.varSelect).click();
    cy.get(selector.hashPosition).within(() => {
      cy.contains('consumer').click();
    });
    cy.get('[title="Key"]').should('not.exist');
    cy.get('#hash_on').click({ force: true });
    cy.get(selector.hashPosition).within(() => {
      cy.contains('cookie').click();
    });
    cy.get(selector.defaultCHashKey).click();
    cy.get(selector.defaultCHashKey).clear().type(data.custom_key);
    cy.get(selector.nodes_0_host).click();
    cy.get(selector.nodes_0_host).type(data.ip);
    cy.get(selector.nodes_0_port).clear().type(data.port);
    cy.get(selector.nodes_0_weight).clear().type(data.weight);

    cy.contains('Next').click();
    cy.contains('Submit').click();
    cy.get(selector.notification).should('contain', data.createUpstreamSuccess);
    cy.contains('.ant-table-cell', 'ID').should('be.visible');
    cy.url().should('contains', 'upstream/list');
  });

  it('should configure the upstream', function () {
    cy.visit('/');
    cy.contains('Upstream').click();

    cy.get(selector.nameSelector).type(data.upstreamName);
    cy.contains('Search').click();
    cy.contains(data.upstreamName).siblings().contains('Configure').click();

    cy.get(selector.chash_key).should('value', data.custom_key);
    cy.get(selector.chash_key).clear().type(data.new_key);

    cy.contains('Next').click();
    cy.contains('Submit').click({
      force: true,
    });

    cy.get(selector.notification).should('contain', data.configureUpstreamSuccess);
    cy.url().should('contains', 'upstream/list');
  });

  it('should delete the upstream', function () {
    cy.visit('/');
    cy.contains('Upstream').click();
    cy.contains(data.upstreamName).siblings().contains('Delete').click();
    cy.contains('button', 'Confirm').click();
    cy.get(selector.notification).should('contain', data.deleteUpstreamSuccess);
  });
});
