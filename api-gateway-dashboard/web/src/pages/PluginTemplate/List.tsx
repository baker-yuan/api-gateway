import { PlusOutlined } from '@ant-design/icons';
import { PageHeaderWrapper } from '@ant-design/pro-layout';
import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { Button, notification, Popconfirm, Select, Space, Tag } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import { history, useIntl } from 'umi';

import usePagination from '@/hooks/usePagination';

import { fetchLabelList, fetchList, remove } from './service';

const Page: React.FC = () => {
  const ref = useRef<ActionType>();
  const [labelList, setLabelList] = useState<LabelList>({});
  const { formatMessage } = useIntl();

  const { paginationConfig, savePageList } = usePagination();

  useEffect(() => {
    fetchLabelList().then(setLabelList);
  }, []);

  const handleTableActionSuccessResponse = (msgTip: string) => {
    notification.success({
      message: msgTip,
    });

    ref.current?.reload();
  };

  const columns: ProColumns<PluginTemplateModule.ResEntity>[] = [
    {
      title: 'ID',
      dataIndex: 'id',
      hideInSearch: true,
    },
    {
      title: formatMessage({ id: 'component.global.description' }),
      dataIndex: 'desc',
    },
    {
      title: formatMessage({ id: 'component.global.labels' }),
      dataIndex: 'labels',
      render: (_, record) => {
        return Object.keys(record.labels || {}).map((item) => (
          <Tag key={Math.random().toString(36).slice(2)}>
            {item}:{record.labels[item]}
          </Tag>
        ));
      },
      renderFormItem: (_, { type }) => {
        if (type === 'form') {
          return null;
        }

        return (
          <Select
            mode="tags"
            style={{ width: '100%' }}
            tagRender={(props) => {
              const { value, closable, onClose } = props;
              return (
                <Tag closable={closable} onClose={onClose} style={{ marginRight: 3 }}>
                  {value}
                </Tag>
              );
            }}
          >
            {Object.keys(labelList).map((key) => {
              return (
                <Select.OptGroup label={key} key={Math.random().toString(36).slice(2)}>
                  {(labelList[key] || []).map((value: string) => (
                    <Select.Option
                      key={Math.random().toString(36).slice(2)}
                      value={`${key}:${value}`}
                    >
                      {value}
                    </Select.Option>
                  ))}
                </Select.OptGroup>
              );
            })}
          </Select>
        );
      },
    },
    {
      title: formatMessage({ id: 'component.global.operation' }),
      valueType: 'option',
      render: (_, record) => (
        <>
          <Space align="baseline">
            <Button
              type="primary"
              onClick={() => {
                history.push(`/plugin-template/${record.id}/edit`);
              }}
              style={{ marginRight: 10 }}
            >
              {formatMessage({ id: 'component.global.edit' })}
            </Button>

            <Popconfirm
              title={formatMessage({ id: 'component.global.popconfirm.title.delete' })}
              onConfirm={() => {
                remove(record.id!).then(() => {
                  handleTableActionSuccessResponse(
                    `${formatMessage({ id: 'component.global.delete' })} ${formatMessage({
                      id: 'menu.pluginTemplate',
                    })} ${formatMessage({ id: 'component.status.success' })}`,
                  );
                });
              }}
              okText={formatMessage({ id: 'component.global.confirm' })}
              cancelText={formatMessage({ id: 'component.global.cancel' })}
            >
              <Button type="primary" danger>
                {formatMessage({ id: 'component.global.delete' })}
              </Button>
            </Popconfirm>
          </Space>
        </>
      ),
    },
  ];

  return (
    <PageHeaderWrapper title={formatMessage({ id: 'page.plugin.list' })}>
      <ProTable<PluginTemplateModule.ResEntity>
        actionRef={ref}
        rowKey="id"
        columns={columns}
        request={fetchList}
        pagination={{
          onChange: (page, pageSize?) => savePageList(page, pageSize),
          pageSize: paginationConfig.pageSize,
          current: paginationConfig.current,
        }}
        search={{
          searchText: formatMessage({ id: 'component.global.search' }),
          resetText: formatMessage({ id: 'component.global.reset' }),
        }}
        toolBarRender={() => [
          <Button type="primary" onClick={() => history.push('/plugin-template/create')}>
            <PlusOutlined />
            {formatMessage({ id: 'component.global.create' })}
          </Button>,
        ]}
      />
    </PageHeaderWrapper>
  );
};

export default Page;
