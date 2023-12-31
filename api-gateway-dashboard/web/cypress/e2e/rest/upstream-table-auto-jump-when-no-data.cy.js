/* eslint-disable no-undef */

context('Table Auto Jump When No Data', () => {
  const selector = {
    id: '#id',
    name: '#name',
    nodes_0_host: '#submitNodes_0_host',
    page_item: '.ant-pagination-item-2',
    deleteAlert: '.ant-modal-body',
    notificationCloseIcon: '.ant-notification-close-icon',
    notification: '.ant-notification-notice-message',
    table_row: ' .ant-table-row',
  };

  const data = {
    createUpstreamSuccess: 'Create Upstream Successfully',
    deleteUpstreamSuccess: 'Delete Upstream Successfully',
  };

  beforeEach(() => {
    cy.login().then(() => {
      Array.from({ length: 11 }).forEach((value, key) => {
        const payload = {
          name: `upstreamName${key}`,
          type: 'roundrobin',
          pass_host: 'pass',
          scheme: 'http',
          timeout: {
            connect: 6,
            send: 6,
            read: 6,
          },
          keepalive_pool: {
            size: 320,
            idle_timeout: 60,
            requests: 1000,
          },
          nodes: {
            '127.0.0.1': 1,
          },
        };
        cy.requestWithToken({ method: 'POST', payload, url: `/apisix/admin/upstreams` });
      });
    });
  });

  it('should delete the upstream', () => {
    cy.visit('/');
    cy.contains('Upstream').click();
    cy.get(selector.page_item).click();
    cy.wait(1000);
    cy.contains('upstreamName').siblings().contains('Delete').click();
    cy.contains('button', 'Confirm').click();
    cy.get(selector.notification).should('contain', data.deleteUpstreamSuccess);
    cy.get('.ant-notification-close-x').click();
    cy.url().should('contains', '/upstream/list?page=1&pageSize=10');
    cy.get(selector.table_row).should((upstream) => {
      expect(upstream).to.have.length(10);
    });
    cy.get('.ant-table-cell:contains(upstreamName)').each((elem) => {
      cy.requestWithToken({
        method: 'DELETE',
        url: `/apisix/admin/upstreams/${elem.prev().text()}`,
      });
    });
  });
});
