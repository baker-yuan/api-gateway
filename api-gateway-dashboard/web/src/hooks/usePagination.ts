import type { ActionType } from '@ant-design/pro-table';
import type { PageInfo } from '@ant-design/pro-table/lib/typing';
import querystring from 'query-string';
import type { MutableRefObject } from 'react';
import { useEffect, useState } from 'react';
import { history, useLocation } from 'umi';

export default function usePagination() {
  const location = useLocation();
  const [paginationConfig, setPaginationConfig] = useState({ pageSize: 10, current: 1 });
  useEffect(() => {
    const { page = 1, pageSize = 10 } = querystring.parse(location.search);
    setPaginationConfig({ pageSize: Number(pageSize), current: Number(page) });
  }, [location.search]);

  const savePageList = (page = 1, pageSize = 10) => {
    history.replace(`${location.pathname}?page=${page}&pageSize=${pageSize}`);
  };

  const checkPageList = (ref: MutableRefObject<ActionType | undefined>) => {
    const { current, pageSize, total } = ref.current?.pageInfo as PageInfo;
    if (current > pageSize / total && current > 1) {
      savePageList(paginationConfig.current - 1, paginationConfig.pageSize);
    } else {
      ref.current?.reload();
    }
  };

  return { paginationConfig, savePageList, checkPageList };
}
