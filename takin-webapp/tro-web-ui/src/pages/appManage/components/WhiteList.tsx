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
import CustomTable from 'src/components/custom-table';
import WhitelistSwitchService from 'src/pages/configCenter/whitelistSwitch/service';
import LinkMarkService from 'src/pages/linkMark/service';
import AppManageService from '../service';
import styles from './../index.less';
import AddWhiteListDrawer from './AddWhiteListDrawer';
import getWhiteListColumns from './WhiteListTableColomn';

interface Props {
  id?: string;
  detailData?: any;
  detailState?: any;
  action?: string;
}
interface State {
  isReload: boolean;
  whiteListList: any[];
  loading: boolean;
  selectedRowKeys: any[];
  allSystemFlow: any[];
  searchValues: any;
  total: number;
  searchParams: {
    current: number;
    pageSize: number;
  };
  whitelistSwitchStatus: number;
}
const WhiteList: React.FC<Props> = props => {
  const [state, setState] = useStateReducer<State>({
    isReload: false,
    whiteListList: null,
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
    },
    whitelistSwitchStatus: null
  });
  const { Search } = Input;
  const { confirm } = Modal;
  const { detailData, id, detailState, action } = props;
  const { selectedRowKeys } = state;

  useEffect(() => {
    // queryAllSystemFlow();
    querySwitchStatus();
  }, []);

  useEffect(() => {
    queryWhiteListList({ ...state.searchValues, ...state.searchParams });
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
    } = await AppManageService.openAndCloseWhiteList({
      ids,
      type: useYn,
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

  const showModal = (ids, useYn) => {
    let content;
    if (useYn === 0) {
      content = '??????????????????????????????????????????????????????';
    } else {
      content = '???????????????????????????????????????????????????';
    }

    confirm({
      content,
      title:
        useYn === 0 ? '??????????????????????????????' : '?????????????????????????????????????????????',
      okButtonProps: useYn === 0 ? { type: 'primary' } : { type: 'danger' },
      okText: useYn === 0 ? '????????????' : '????????????',
      onOk() {
        handleConfirm(ids, useYn);
      }
    });
  };

  /**
   * @name ?????????????????????
   */
  const queryWhiteListList = async values => {
    setState({
      loading: true
    });
    const {
      total,
      data: { success, data }
    } = await AppManageService.queryWhiteListList({
      applicationId: id,
      ...values
    });
    if (success) {
      setState({
        total,
        whiteListList: data,
        loading: false,
        selectedRowKeys: []
      });
      return;
    }
    setState({
      loading: false
    });
  };

  /**
   * @name ????????????????????????
   */
  const queryAllSystemFlow = async () => {
    const {
      data: { success, data }
    } = await LinkMarkService.querySystemFlow({});
    if (success) {
      setState({
        allSystemFlow:
          data &&
          data.map((item, key) => {
            return { label: item.systemProcessName, value: item.id };
          })
      });
      return;
    }
  };

  /**
   * @name ???????????????????????????
   */
  const querySwitchStatus = async () => {
    const {
      data: { data, success }
    } = await WhitelistSwitchService.queryWhitelistSwitchStatus({});
    if (success) {
      setState({
        whitelistSwitchStatus: data.switchFlag
      });
    }
  };

  const handleChangeSelectRows = value => {
    setState({
      selectedRowKeys: value
    });
  };

  const handleChangeAll = () => {
    const array =
      state.whiteListList &&
      state.whiteListList.map((item, k) => {
        return item.id;
      });
    if (
      state.whiteListList &&
      selectedRowKeys.length === state.whiteListList.length
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
    queryWhiteListList({
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
    queryWhiteListList({ ...state.searchParams });
  };

  const handleRefresh = () => {
    queryWhiteListList({
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
        {state.whitelistSwitchStatus === 0 && (
          <Alert
            type="warning"
            message={
              <p style={{ color: '#646676' }}>
                ???????????????????????????????????????????????????????????????????????????????????????????????????
              </p>}
            showIcon
            style={{ marginTop: 0, marginBottom: 22 }}
          />
        )}

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
                    ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                  </p>
                  <div>
                    <p>
                      <span className={styles.noteNum}>1</span>
                      <span className={styles.noteSubTitle}>???????????????</span>
                    </p>
                    <p className={styles.noteSubContent}>
                      ???????????????????????????????????????????????????
                    </p>
                    <p>
                      <span className={styles.noteNum}>2</span>
                      <span className={styles.noteSubTitle}>???????????????</span>
                    </p>
                    <p className={styles.noteSubContent}>
                      ??????????????????????????????????????????????????????
                    </p>
                    <p>
                      <span className={styles.noteNum}>3</span>
                      <span className={styles.noteSubTitle}>????????????</span>
                    </p>
                    <p className={styles.noteSubContent}>
                      ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    </p>
                  </div>
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
                <AddWhiteListDrawer
                  disabled={
                    detailState.switchStatus === 'OPENING' ||
                    detailState.switchStatus === 'CLOSING'
                      ? true
                      : false
                  }
                  title="???????????????"
                  action="add"
                  detailData={detailData}
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
          <Col span={4}>
            <CommonSelect
              style={{ width: '90%' }}
              placeholder="????????????:??????"
              dataSource={[
                { label: 'HTTP', value: 1 },
                { label: 'DUBBO', value: 2 }
                // { label: 'RABBITMQ', value: 3 }
              ]}
              onChange={value => handleChange('interfaceType', value)}
              value={state.searchValues.interfaceType}
            />
          </Col>
          <Col span={4}>
            <CommonSelect
              style={{ width: '90%' }}
              placeholder="??????:??????"
              dataSource={[
                { label: '?????????', value: 1 },
                { label: '?????????', value: 0 }
              ]}
              onChange={value => handleChange('useYn', value)}
              value={state.searchValues.useYn}
            />
          </Col>
          <Col span={6}>
            <Search
              placeholder="??????????????????"
              enterButton
              onSearch={value => handleChange('interfaceName', value)}
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
          rowKey="wlistId"
          rowSelection={{
            selectedRowKeys,
            onChange: value => handleChangeSelectRows(value)
          }}
          loading={state.loading}
          columns={getWhiteListColumns(
            state,
            setState,
            detailState,
            id,
            action,
            detailData
          )}
          dataSource={state.whiteListList ? state.whiteListList : []}
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
              onClick={() => showModal(state.selectedRowKeys, 1)}
              disabled={state.selectedRowKeys.length === 0 ? true : false}
            >
              ?????????????????????
            </Button>
          </AuthorityBtn>
        }
        {
          <AuthorityBtn
            isShow={btnAuthority && btnAuthority.appManage_6_enable_disable}
          >
            <Button
              type="link"
              style={{
                color:
                  state.selectedRowKeys.length === 0
                    ? 'rgba(17,187,213,0.45)'
                    : null
              }}
              onClick={() => showModal(state.selectedRowKeys, 0)}
              disabled={state.selectedRowKeys.length === 0 ? true : false}
            >
              ?????????????????????
            </Button>
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
export default WhiteList;
