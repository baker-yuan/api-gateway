import { PageContainer } from '@ant-design/pro-layout';
import { Card, Form, notification, Steps } from 'antd';
import React, { useEffect, useState } from 'react';
import { history, useIntl } from 'umi';

import ActionBar from '@/components/ActionBar';
import PluginPage from '@/components/Plugin';
import { transformLabelValueToKeyValue } from '@/helpers';

import Preview from './components/Preview';
import Step1 from './components/Step1';
import { create, fetchItem, update } from './service';

const Page: React.FC = (props) => {
  const [step, setStep] = useState(1);
  const [plugins, setPlugins] = useState<PluginComponent.Data>({});
  const [form1] = Form.useForm();
  const { formatMessage } = useIntl();

  useEffect(() => {
    const { id } = (props as any).match.params;
    if (id) {
      fetchItem(id).then(({ data }) => {
        const { desc, labels = {}, ...rest } = data;
        form1.setFieldsValue({
          id,
          desc,
          custom_normal_labels: Object.keys(labels).map((key) => `${key}:${labels[key]}`),
        });
        setPlugins(rest.plugins);
      });
    }
  }, []);

  const onSubmit = () => {
    const { desc, custom_normal_labels } = form1.getFieldsValue();
    const labels: Record<string, string> = {};
    transformLabelValueToKeyValue(custom_normal_labels || []).forEach(
      ({ labelKey, labelValue }) => {
        labels[labelKey] = labelValue;
      },
    );
    const data = { desc, labels, plugins } as PluginTemplateModule.Entity;

    const { id } = (props as any).match.params;
    (id ? update(id, data) : create(data))
      .then(() => {
        notification.success({
          message: `${
            id
              ? formatMessage({ id: 'component.global.edit' })
              : formatMessage({ id: 'component.global.create' })
          } ${formatMessage({ id: 'menu.pluginTemplate' })} ${formatMessage({
            id: 'component.status.success',
          })}`,
        });
        history.push('/plugin-template/list');
      })
      .catch(() => {
        setStep(3);
      });
  };

  const onStepChange = (nextStep: number) => {
    if (step === 1) {
      form1.validateFields().then(() => {
        setStep(nextStep);
      });
    } else if (nextStep === 3) {
      setStep(3);
    } else if (nextStep === 4) {
      onSubmit();
    } else {
      setStep(nextStep);
    }
  };

  return (
    <>
      <PageContainer
        title={`${
          (props as any).match.params.id
            ? formatMessage({ id: 'component.global.edit' })
            : formatMessage({ id: 'component.global.create' })
        } ${formatMessage({ id: 'menu.pluginTemplate' })}`}
      >
        <Card bordered={false}>
          <Steps current={step - 1} style={{ marginBottom: 30 }}>
            <Steps.Step
              title={formatMessage({ id: 'component.global.steps.stepTitle.basicInformation' })}
            />
            <Steps.Step
              title={formatMessage({ id: 'component.global.steps.stepTitle.pluginConfig' })}
            />
            <Steps.Step title={formatMessage({ id: 'component.global.steps.stepTitle.preview' })} />
          </Steps>

          {step === 1 && <Step1 form={form1} />}
          {step === 2 && (
            <PluginPage
              initialData={plugins}
              onChange={setPlugins}
              referPage="route"
              schemaType="route"
            />
          )}
          {step === 3 && <Preview form1={form1} plugins={plugins} />}
        </Card>
      </PageContainer>
      <ActionBar step={step} lastStep={3} onChange={onStepChange} />
    </>
  );
};

export default Page;
