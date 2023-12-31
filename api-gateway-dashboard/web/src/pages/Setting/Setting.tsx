import { PageContainer } from '@ant-design/pro-layout';
import { Card, Col, Form, Input, notification, Row } from 'antd';
import React, { useEffect } from 'react';
import { history, useIntl } from 'umi';

import ActionBar from '@/components/ActionBar';
import { getUrlQuery } from '@/helpers';
import { getGrafanaURL } from '@/pages/Dashboard/service';

import { updateMonitorURL } from './service';

const Setting: React.FC = () => {
  const [form] = Form.useForm();

  const isSuperAdmin = true;
  const isWorkspace = false;
  const canFetchGrafana = (isSuperAdmin && !isWorkspace) || isWorkspace;

  const { formatMessage } = useIntl();

  useEffect(() => {
    if (!canFetchGrafana) {
      return;
    }
    getGrafanaURL().then((url) => {
      form.setFieldsValue({
        grafanaURL: url,
      });
    });
  }, [canFetchGrafana]);

  const onSubmit = () => {
    form.validateFields().then((value) => {
      Promise.all([
        new Promise((resolve) => {
          if (canFetchGrafana) {
            updateMonitorURL(value.grafanaURL).then(resolve);
          }
          resolve();
        }),
      ]).then(() => {
        notification.success({
          message: formatMessage({
            id: 'page.setting.notification.update.configuration.successfully',
          }),
        });
        setTimeout(() => {
          const redirect = getUrlQuery('redirect');
          const currentHost = window.location.host;
          if (redirect) {
            const redirectUrl = decodeURIComponent(redirect);
            const pathArray = redirectUrl.split('/');
            const redirectHost = pathArray[2];
            if (currentHost === redirectHost) {
              let path = '';
              for (let i = 3; i < pathArray.length; i += 1) {
                path += '/';
                path += pathArray[i];
              }
              history.push(path);
            }
          } else {
            history.push('/');
          }
        }, 500);
      });
    });
  };

  return (
    <>
      <PageContainer title={formatMessage({ id: 'page.setting.pageContainer.title' })}>
        <Card>
          <Row>
            <Col span={10}>
              <Form form={form} labelCol={{ span: 7 }}>
                {canFetchGrafana && (
                  <Form.Item
                    label={formatMessage({ id: 'page.setting.form.item.grafanaURL' })}
                    name="grafanaURL"
                    extra={formatMessage({
                      id: 'page.setting.form.item.grafanaURL.inputHelpMessage',
                    })}
                    rules={[
                      {
                        pattern: new RegExp(/^https?:\/\//),
                        message: formatMessage({
                          id: 'page.setting.form.item.grafanaURL.inputErrorMessage',
                        }),
                      },
                    ]}
                  >
                    <Input />
                  </Form.Item>
                )}
              </Form>
            </Col>
          </Row>
        </Card>
      </PageContainer>
      <ActionBar step={1} lastStep={1} onChange={onSubmit} />
    </>
  );
};

export default Setting;
