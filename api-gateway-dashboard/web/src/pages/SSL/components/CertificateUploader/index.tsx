import { UploadOutlined } from '@ant-design/icons';
import { Button, Form, Upload } from 'antd';
import type { UploadFile } from 'antd/lib/upload/interface';
import React from 'react';
import { useIntl } from 'umi';

import styles from '@/pages/SSL/style.less';

export type UploadType = 'PUBLIC_KEY' | 'PRIVATE_KEY';

type UploaderProps = {
  data: {
    publicKeyList: UploadFile[];
    privateKeyList: UploadFile[];
  };
  onSuccess: (
    data: Partial<SSLModule.UploadPrivateSuccessData & SSLModule.UploadPublicSuccessData>,
  ) => void;
  onRemove: (type: UploadType) => void;
};

const CertificateUploader: React.FC<UploaderProps> = ({ onSuccess, onRemove, data }) => {
  const { publicKeyList = [], privateKeyList = [] } = data;
  const [form] = Form.useForm();
  const { formatMessage } = useIntl();

  const genUploadFile = (name = ''): UploadFile => {
    return {
      uid: Math.random().toString(36).slice(2),
      name,
      status: 'done',
      size: 0,
      type: '',
    };
  };

  const parseCertificate = (file: File | Blob, fileName: string, type: UploadType) => {
    const fileReader = new FileReader();
    fileReader.readAsText(file);
    // eslint-disable-next-line func-names
    fileReader.onload = function (event) {
      const { result } = event.currentTarget as any;
      if (type === 'PUBLIC_KEY') {
        const uploadPublicData: SSLModule.UploadPublicSuccessData = {
          cert: result,
          publicKeyList: [genUploadFile(fileName)],
        };
        onSuccess(uploadPublicData);
      } else {
        const uploadprivateData: SSLModule.UploadPrivateSuccessData = {
          key: result,
          privateKeyList: [genUploadFile(fileName)],
        };
        onSuccess(uploadprivateData);
      }
    };
  };

  const beforeUpload = (file: File, fileList: File[], type: UploadType) => {
    parseCertificate(file, file.name, type);
    return false;
  };

  return (
    <Form form={form} layout="horizontal" className={styles.stepForm}>
      <Form.Item>
        <Upload
          className={styles.stepForm}
          onRemove={() => onRemove('PUBLIC_KEY')}
          fileList={publicKeyList}
          beforeUpload={(file, fileList) => beforeUpload(file, fileList, 'PUBLIC_KEY')}
        >
          <Button disabled={publicKeyList.length === 1}>
            <UploadOutlined /> {formatMessage({ id: 'page.ssl.button.uploadCert' })}
          </Button>
        </Upload>
      </Form.Item>
      <Form.Item>
        <Upload
          className={styles.stepForm}
          onRemove={() => onRemove('PRIVATE_KEY')}
          fileList={privateKeyList}
          beforeUpload={(file, fileList) => beforeUpload(file, fileList, 'PRIVATE_KEY')}
        >
          <Button disabled={privateKeyList.length === 1}>
            <UploadOutlined />{' '}
            {`${formatMessage({ id: 'page.ssl.upload' })}${formatMessage({
              id: 'page.ssl.form.itemLabel.privateKey',
            })}`}
          </Button>
        </Upload>
      </Form.Item>
    </Form>
  );
};
export default CertificateUploader;
