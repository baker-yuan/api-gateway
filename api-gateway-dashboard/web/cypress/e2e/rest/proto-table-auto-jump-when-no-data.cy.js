
context('Batch Create Proto And Delete Proto', () => {
  const selector = {
    id: '#id',
    content: '.view-lines',
    page_item: '.ant-pagination-item-2',
    draw: '.ant-drawer-content',
    deleteAlert: '.ant-modal-body',
    notificationCloseIcon: '.ant-notification-close-icon',
    notification: '.ant-notification-notice-message',
    table_row: '.ant-table-row',
  };

  const data = {
    createProtoSuccess: 'Create proto Successfully',
    deleteProtoSuccess: 'Delete proto Successfully',
  };

  before(() => {
    cy.login().then(() => {
      Array.from({ length: 11 }).forEach(async (value, key) => {
        const payload = {
          content: 'test',
          desc: '',
          id: `protoId${key}`,
        };
        cy.requestWithToken({ method: 'POST', payload, url: '/apisix/admin/proto' });
      });
    });
  });

  it('should delete last data and jump to first page', () => {
    cy.visit('/');
    cy.contains('Protocol Buffers').click();
    cy.get(selector.page_item).click();
    cy.wait(1000);
    cy.contains('Delete').click();
    cy.contains('button', 'Confirm').click();
    cy.get(selector.notification).should('contain', data.deleteProtoSuccess);
    cy.get(selector.notificationCloseIcon).click();
    cy.url().should('contains', '/proto/list?page=1&pageSize=10');
    cy.get(selector.table_row).should((proto) => {
      expect(proto).to.have.length(10);
    });
    cy.get('.ant-table-cell:contains(protoId)').each((elem) => {
      cy.requestWithToken({
        method: 'DELETE',
        url: `/apisix/admin/proto/${elem.text()}`,
      });
    });
  });
});
