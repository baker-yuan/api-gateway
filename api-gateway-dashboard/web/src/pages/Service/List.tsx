import { PlusOutlined } from '@ant-design/icons';
import { PageHeaderWrapper } from '@ant-design/pro-layout';
import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { Button, notification, Popconfirm, Space } from 'antd';
import { omit } from 'lodash';
import React, { useRef, useState } from 'react';
import { history, useIntl } from 'umi';

import { RawDataEditor } from '@/components/RawDataEditor';
import { DELETE_FIELDS } from '@/constants';
import usePagination from '@/hooks/usePagination';

import { create, fetchList, remove, update } from './service';

const Page: React.FC = () => {
  const ref = useRef<ActionType>();
  const { formatMessage } = useIntl();
  const [visible, setVisible] = useState(false);
  const [rawData, setRawData] = useState<Record<string, any>>({});
  const [id, setId] = useState('');
  const [editorMode, setEditorMode] = useState<'create' | 'update'>('create');
  const { paginationConfig, savePageList, checkPageList } = usePagination();

  const [deleteLoading, setDeleteLoading] = useState('');

  const columns: ProColumns<ServiceModule.ResponseBody>[] = [
    {
      title: formatMessage({ id: 'component.global.id' }),
      dataIndex: 'id',
    },
    {
      title: formatMessage({ id: 'component.global.name' }),
      dataIndex: 'name',
    },
    {
      title: formatMessage({ id: 'component.global.description' }),
      dataIndex: 'desc',
    },
    {
      title: formatMessage({ id: 'component.global.operation' }),
      valueType: 'option',
      hideInSearch: true,
      render: (_, record) => (
        <>
          <Space align="baseline">
            <Button type="primary" onClick={() => history.push(`/service/${record.id}/edit`)}>
              {formatMessage({ id: 'component.global.edit' })}
            </Button>
            <Button
              type="primary"
              onClick={() => {
                setId(record.id);
                setRawData(omit(record, DELETE_FIELDS));
                setVisible(true);
                setEditorMode('update');
              }}
            >
              {formatMessage({ id: 'component.global.view' })}
            </Button>
            <Popconfirm
              title={formatMessage({ id: 'component.global.popconfirm.title.delete' })}
              onConfirm={() => {
                setDeleteLoading(record.id!);
                remove(record.id!)
                  .then(() => {
                    notification.success({
                      message: `${formatMessage({ id: 'component.global.delete' })} ${formatMessage(
                        {
                          id: 'menu.service',
                        },
                      )} ${formatMessage({ id: 'component.status.success' })}`,
                    });
                    checkPageList(ref);
                  })
                  .finally(() => {
                    setDeleteLoading('');
                  });
              }}
              okText={formatMessage({ id: 'component.global.confirm' })}
              cancelText={formatMessage({ id: 'component.global.cancel' })}
            >
              <Button type="primary" danger loading={record.id === deleteLoading}>
                {formatMessage({ id: 'component.global.delete' })}
              </Button>
            </Popconfirm>
          </Space>
        </>
      ),
    },
  ];

  return (
    <PageHeaderWrapper
      title={formatMessage({ id: 'page.service.list' })}
      content={formatMessage({ id: 'page.service.description' })}
    >
      <ProTable<ServiceModule.ResponseBody>
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
          <Button type="primary" onClick={() => history.push(`/service/create`)}>
            <PlusOutlined />
            {formatMessage({ id: 'component.global.create' })}
          </Button>,
          <Button
            type="default"
            onClick={() => {
              setVisible(true);
              setEditorMode('create');
              setRawData({});
            }}
          >
            {formatMessage({ id: 'component.global.data.editor' })}
          </Button>,
        ]}
      />
      <RawDataEditor
        visible={visible}
        type="service"
        readonly={false}
        data={rawData}
        onClose={() => {
          setVisible(false);
        }}
        onSubmit={(data: any) => {
          (editorMode === 'create' ? create(data) : update(id, data)).then(() => {
            setVisible(false);
            ref.current?.reload();
          });
        }}
      />
    </PageHeaderWrapper>
  );
};

export default Page;
