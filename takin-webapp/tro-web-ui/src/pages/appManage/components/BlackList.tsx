/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  Alert,
  Button,
  Col,
  Icon,
  Input,
  message,
  Modal,
  Pagination,
  Popover,
  Row
} from 'antd';
import { CommonSelect, useStateReducer } from 'racc';
import React, { Fragment, useEffect } from 'react';
import AuthorityBtn from 'src/common/authority-btn/AuthorityBtn';
import TableTitle from 'src/common/table-title/TableTitle';
import CustomPopconfirm from 'src/components/custom-popconfirm/CustomPopconfirm';
import CustomTable from 'src/components/custom-table';
import WhitelistSwitchService from 'src/pages/configCenter/whitelistSwitch/service';
import LinkMarkService from 'src/pages/linkMark/service';
import AppManageService from '../service';
import styles from './../index.less';
import AddAndEditBlacklistDrawer from './AddAndEditBlackListDrawer';
import AddWhiteListDrawer from './AddWhiteListDrawer';
import getBlackListColumns from './BlackListTableColomn';
import getWhiteListColumns from './WhiteListTableColomn';

interface Props {
  id?: string;
  detailData?: any;
  detailState?: any;
  action?: string;
}
interface State {
  isReload: boolean;
  blackListList: any[];
  loading: boolean;
  selectedRowKeys: any[];
  allSystemFlow: any[];
  searchValues: any;
  total: number;
  searchParams: {
    current: number;
    pageSize: number;
  };
}
const BlackList: React.FC<Props> = props => {
  const [state, setState] = useStateReducer<State>({
    isReload: false,
    blackListList: null,
    loading: false,
    selectedRowKeys: [],
    allSystemFlow: null,
    searchValues: {
      interfaceType: undefined,
      useYn: undefined,
      interfaceName: null
    },
    total: 0,
    searchParams: {
      current: 0,
      pageSize: 10
    }
  });
  const { Search } = Input;
  const { confirm } = Modal;
  const { detailData, id, detailState, action } = props;
  const { selectedRowKeys } = state;

  useEffect(() => {
    // queryAllSystemFlow();
  }, []);

  useEffect(() => {
    queryBlackListList({ ...state.searchValues, ...state.searchParams });
  }, [state.isReload, state.searchParams.current, state.searchParams.pageSize]);
  const btnAuthority: any =
    localStorage.getItem('trowebBtnResource') &&
    JSON.parse(localStorage.getItem('trowebBtnResource'));
  /**
   * @name ????????????????????????????????????
   */
  const handleConfirm = async (ids, useYn) => {
    const {
      data: { data, success }
    } = await AppManageService.openAndCloseBlacklistList({
      ids,
      useYn,
      applicationId: id
    });
    if (success) {
      const txt = useYn === 0 ? '??????' : '??????';

      message.config({
        top: 150
      });
      message.success(`??????${txt}??????????????????`);
      setState({
        isReload: !state.isReload,
        selectedRowKeys: []
      });
    }
  };

  /**
   * @name ????????????????????????
   */
  const handleDelete = async ids => {
    const {
      data: { data, success }
    } = await AppManageService.deleteBlacklistList({
      ids,
      applicationId: id
    });
    if (success) {
      message.config({
        top: 150
      });
      message.success(`??????????????????????????????`);
      setState({
        isReload: !state.isReload,
        selectedRowKeys: []
      });
    }
  };
  /**
   * @name ?????????????????????
   */
  const queryBlackListList = async values => {
    setState({
      loading: true
    });
    const {
      total,
      data: { success, data }
    } = await AppManageService.queryBlackListList({
      applicationId: id,
      ...values
    });
    if (success) {
      setState({
        total,
        blackListList: data,
        loading: false,
        selectedRowKeys: []
      });
      return;
    }
    setState({
      loading: false
    });
  };

  const handleChangeSelectRows = value => {
    setState({
      selectedRowKeys: value
    });
  };

  const handleChangeAll = () => {
    const array =
      state.blackListList &&
      state.blackListList.map((item, k) => {
        return item.id;
      });
    if (
      state.blackListList &&
      selectedRowKeys.length === state.blackListList.length
    ) {
      setState({
        selectedRowKeys: []
      });
    } else {
      setState({
        selectedRowKeys: array
      });
    }
  };

  const handleChange = (key, value) => {
    setState({
      searchValues: { ...state.searchValues, [key]: value }
    });
    queryBlackListList({
      ...state.searchParams,
      ...state.searchValues,
      [key]: value
    });
  };

  const handleChangeInterfaceName = e => {
    setState({
      searchValues: { ...state.searchValues, interfaceName: e.target.value }
    });
  };

  const handleReset = () => {
    setState({
      searchValues: {
        interfaceType: undefined,
        useYn: undefined,
        interfaceName: null
      },
      selectedRowKeys: []
    });
    queryBlackListList({ ...state.searchParams });
  };

  const handleRefresh = () => {
    queryBlackListList({
      ...state.searchValues,
      ...state.searchParams
    });
  };

  const handleChangePage = async (current, pageSize) => {
    setState({
      searchParams: {
        pageSize,
        current: current - 1
      }
    });
  };

  const handlePageSizeChange = async (current, pageSize) => {
    setState({
      searchParams: {
        pageSize,
        current: 0
      }
    });
  };

  return (
    <Fragment>
      <div
        className={styles.tableWrap}
        style={{ height: document.body.clientHeight - 160 }}
      >
        <TableTitle
          title="?????????"
          tip={
            <Popover
              trigger="click"
              placement="rightTop"
              content={
                <div className={styles.note}>
                  <p className={styles.noteTitle}>???????????????</p>
                  <p className={styles.noteContent}>
                    ?????????????????????????????????redis???????????????key????????????key?????????????????????????????????????????????
                  </p>
                </div>}
            >
              <Icon
                type="question-circle"
                style={{
                  marginLeft: 8,
                  cursor: 'pointer'
                }}
              />
            </Popover>
          }
          extraNode={
            <AuthorityBtn
              isShow={btnAuthority && btnAuthority.appManage_2_create}
            >
              <div className={styles.addAction}>
                <AddAndEditBlacklistDrawer
                  detailData={detailData}
                  action="add"
                  titles="???????????????"
                  id={id}
                  onSccuess={() => {
                    setState({
                      isReload: !state.isReload
                    });
                  }}
                />
              </div>
            </AuthorityBtn>
          }
        />
        <Row type="flex" style={{ marginBottom: 20, marginTop: 20 }}>
          <Col span={6}>
            <Search
              placeholder="??????redis key"
              enterButton
              onSearch={value => handleChange('redisKey', value)}
              onChange={handleChangeInterfaceName}
              value={state.searchValues.interfaceName}
            />
          </Col>
          <Col span={2} style={{ marginLeft: 16, marginTop: 8 }}>
            <Button type="link" onClick={handleReset}>
              ??????
            </Button>
          </Col>
        </Row>
        <div style={{ textAlign: 'right' }}>
          <Icon
            type="redo"
            style={{ cursor: 'pointer' }}
            onClick={handleRefresh}
          />
        </div>

        <CustomTable
          rowKey="blistId"
          rowSelection={{
            selectedRowKeys,
            onChange: value => handleChangeSelectRows(value)
          }}
          loading={state.loading}
          columns={getBlackListColumns(
            state,
            setState,
            detailState,
            id,
            action,
            detailData
          )}
          dataSource={state.blackListList ? state.blackListList : []}
        />
      </div>
      <div
        style={{
          marginTop: 20,
          // textAlign: 'right',
          position: 'fixed',
          padding: '8px 40px',
          bottom: 0,
          right: 10,
          width: 'calc(100% - 178px)',
          backgroundColor: '#fff',
          boxShadow:
            '0px 2px 20px 0px rgba(92,80,133,0.15),0px -4px 8px 0px rgba(222,223,233,0.3)'
        }}
      >
        {
          <AuthorityBtn
            isShow={btnAuthority && btnAuthority.appManage_6_enable_disable}
          >
            <CustomPopconfirm
              title="?????????????????????"
              okColor="#FE7D61"
              onConfirm={() => handleConfirm(state.selectedRowKeys, 1)}
            >
              <Button
                type="link"
                style={{
                  marginRight: 16,
                  marginTop: 8,
                  color:
                    state.selectedRowKeys.length === 0
                      ? 'rgba(17,187,213,0.45)'
                      : null
                }}
                disabled={state.selectedRowKeys.length === 0 ? true : false}
              >
                ?????????????????????
              </Button>
            </CustomPopconfirm>
          </AuthorityBtn>
        }
        {
          <AuthorityBtn
            isShow={btnAuthority && btnAuthority.appManage_6_enable_disable}
          >
            <CustomPopconfirm
              title="?????????????????????"
              okColor="#FE7D61"
              onConfirm={() => handleConfirm(state.selectedRowKeys, 0)}
            >
              <Button
                type="link"
                style={{
                  marginRight: 16,
                  marginTop: 8,
                  color:
                    state.selectedRowKeys.length === 0
                      ? 'rgba(17,187,213,0.45)'
                      : null
                }}
                disabled={state.selectedRowKeys.length === 0 ? true : false}
              >
                ?????????????????????
              </Button>
            </CustomPopconfirm>
          </AuthorityBtn>
        }
        {
          <AuthorityBtn
            isShow={btnAuthority && btnAuthority.appManage_4_delete}
          >
            <CustomPopconfirm
              title="?????????????????????"
              okColor="#FE7D61"
              onConfirm={() => handleDelete(state.selectedRowKeys)}
            >
              <Button
                type="link"
                style={{
                  marginRight: 16,
                  marginTop: 8,
                  color:
                    state.selectedRowKeys.length === 0
                      ? 'rgba(17,187,213,0.45)'
                      : null
                }}
                disabled={state.selectedRowKeys.length === 0 ? true : false}
              >
                ?????????????????????
              </Button>
            </CustomPopconfirm>
          </AuthorityBtn>
        }
        <Pagination
          style={{ display: 'inline-block', float: 'right' }}
          total={state.total}
          current={state.searchParams.current + 1}
          pageSize={state.searchParams.pageSize}
          showTotal={(t, range) =>
            `??? ${state.total} ????????? ???${state.searchParams.current +
              1}??? / ??? ${Math.ceil(
              state.total / (state.searchParams.pageSize || 10)
            )}???`
          }
          showSizeChanger={true}
          onChange={(current, pageSize) => handleChangePage(current, pageSize)}
          onShowSizeChange={handlePageSizeChange}
        />
      </div>
    </Fragment>
  );
};
export default BlackList;
