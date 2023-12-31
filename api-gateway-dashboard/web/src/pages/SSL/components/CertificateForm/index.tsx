import { Form, Input, Tag } from 'antd';
import type { FormInstance } from 'antd/lib/form';
import React from 'react';
import { useIntl } from 'umi';

type CertificateFormProps = {
  mode: 'EDIT' | 'VIEW';
  form: FormInstance;
};

const CertificateForm: React.FC<CertificateFormProps> = ({ mode, form }) => {
  const { formatMessage } = useIntl();
  const renderSNI = () => {
    if (mode === 'VIEW') {
      return (
        <Form.Item label="SNI">
          {(form.getFieldValue('snis') || []).map((item: string) => (
            <Tag color="geekblue" key={item}>
              {item}
            </Tag>
          ))}
        </Form.Item>
      );
    }
    return null;
  };

  const renderExpireTime = () => {
    if (mode === 'VIEW') {
      return (
        <Form.Item
          label={formatMessage({ id: 'page.ssl.form.itemLabel.expireTime' })}
          name="expireTime"
          rules={[{ required: true, message: 'ExpireTime' }]}
        >
          <Input disabled={mode === 'VIEW'} />
        </Form.Item>
      );
    }
    return null;
  };

  return (
    <Form form={form} layout="horizontal" initialValues={form?.getFieldsValue()}>
      {renderSNI()}
      <Form.Item
        label={formatMessage({ id: 'page.ssl.form.itemLabel.cert' })}
        name="cert"
        rules={[
          {
            required: true,
            message: `${formatMessage({ id: 'component.global.pleaseCheck' })}${formatMessage({
              id: 'page.ssl.form.itemLabel.cert',
            })}`,
          },
          {
            min: 128,
            message: formatMessage({ id: 'page.ssl.form.itemRuleMessage.certValueLength' }),
          },
        ]}
      >
        <Input.TextArea
          rows={6}
          disabled={mode !== 'EDIT'}
          placeholder={formatMessage({ id: 'component.ssl.fields.cert.required' })}
        />
      </Form.Item>
      <Form.Item
        label={formatMessage({ id: 'page.ssl.form.itemLabel.privateKey' })}
        name="key"
        rules={[
          {
            required: true,
            message: `${formatMessage({ id: 'component.global.pleaseCheck' })}${formatMessage({
              id: 'page.ssl.form.itemLabel.privateKey',
            })}`,
          },
          {
            min: 128,
            message: formatMessage({ id: 'page.ssl.form.itemRuleMessage.privateKeyLength' }),
          },
        ]}
      >
        <Input.TextArea
          rows={6}
          disabled={mode !== 'EDIT'}
          placeholder={formatMessage({ id: 'component.ssl.fields.key.required' })}
        />
      </Form.Item>
      {renderExpireTime()}
    </Form>
  );
};

export default CertificateForm;
