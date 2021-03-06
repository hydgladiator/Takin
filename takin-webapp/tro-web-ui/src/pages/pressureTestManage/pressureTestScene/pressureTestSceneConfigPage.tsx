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

import { Button, message } from 'antd';
import { WrappedFormUtils } from 'antd/lib/form/Form';
import { connect } from 'dva';
import _ from 'lodash';
import { useStateReducer } from 'racc';
import React, { useEffect } from 'react';
import CustomSkeleton from 'src/common/custom-skeleton';
import FormCardMultiple from 'src/components/form-card-multiple';
import { BasePageLayout } from 'src/components/page-layout';
import BusinessActivityService from 'src/pages/businessActivity/service';
import router from 'umi/router';
import BaseInfo from './components/BaseInfo';
import BusinessActivityConfig from './components/BusinessActivityConfig';
import DataVerificationConfig from './components/DataVerificationConfig';
import PressureConfig from './components/PressureConfig';
import SLAConfig from './components/SLAConfig';
import { TestMode } from './enum';
import styles from './index.less';
import PressureTestSceneService from './service';
interface Props {
  location?: { query?: any };
  dictionaryMap?: any;
}
interface PressureTestSceneConfigState {
  form: any;
  fileList: any[];
  bussinessActiveList: any[];
  selectedBussinessActivityIds: any[];
  selectedBussinessActiveList: any[];
  uploadFileNum: number;
  detailData: any;
  configType: Number;
  businessFlowId: String;
  pressureMode: Number;
  concurrenceNum: Number;
  ipNum: Number;
  minIpNum: Number;
  testMode: Number;
  maxIpNum: Number;
  pressureTestTime: any;
  lineIncreasingTime: any;
  stepIncreasingTime: any;
  step: Number;
  stepChartsData: any;
  flag: boolean;
  indexss: Number;
  estimateFlow: Number;
  downloadUrl: string;
  loading: boolean;
  initList: any;
  businessFlowList: any[];
  bussinessActivityAndScriptList: any[];
  bussinessFlowAndScriptList: any[];
  missingDataScriptList: any;
  businessList: any;
  tpsNum: any;
}
declare var serverUrls: string;
const PressureTestSceneConfig: React.FC<Props> = props => {
  const [state, setState] = useStateReducer<PressureTestSceneConfigState>({
    form: null as WrappedFormUtils,
    fileList: [],
    bussinessActiveList: null,
    selectedBussinessActiveList: null,
    selectedBussinessActivityIds: null,
    uploadFileNum: 0,
    detailData: {} as any,
    /** ???????????? */
    configType: 1,
    /** ????????????id */
    businessFlowId: undefined,
    /** ???????????? */
    pressureMode: 1,
    /** ???????????? */
    testMode: TestMode.????????????,
    /** ??????????????? */
    concurrenceNum: undefined,
    /** ????????????IP??? */
    ipNum: undefined,
    /** ????????????IP????????? */
    minIpNum: 0,
    /** ????????????IP???Da??? */
    maxIpNum: undefined,
    /** ???????????? */
    pressureTestTime: { time: undefined, unit: 'm' },
    /** ???????????????????????? */
    lineIncreasingTime: { time: undefined, unit: 'm' },
    /** ????????????(??????) */
    stepIncreasingTime: { time: undefined, unit: 'm' },
    /** ???????????? */
    step: undefined,
    stepChartsData: null,
    flag: false,
    indexss: 0,
    estimateFlow: null,
    downloadUrl: null,
    loading: false,
    initList: [
      {
        businessActivityId: '',
        businessActivityName: '',
        scriptId: '',
        targetTPS: '',
        targetRT: '',
        targetSuccessRate: '',
        targetSA: ''
      }
    ],
    businessFlowList: [],
    bussinessActivityAndScriptList: [],
    bussinessFlowAndScriptList: [],
    missingDataScriptList: [],
    businessList: [],
    tpsNum: null
  });

  const { location, dictionaryMap } = props;
  const { query } = location;
  const { action, id } = query;

  useEffect(() => {
    queryBussinessActive();
    queryBussinessActivityAndScript();
    queryBussinessFlowAndScript();
    queryBigFileDownload();
    if (action === 'edit') {
      queryPressureTestSceneDetail(id);
    }
  }, []);

  const formDataSource = [
    BaseInfo(state, setState, props),
    BusinessActivityConfig(state, setState, props),
    PressureConfig(state, setState, props),
    SLAConfig(state, setState, props)
  ];

  /**
   * @name ?????????????????????????????????
   */

  const objectValueHasEmpty = object => {
    let isEmpty = false;
    Object.keys(object).forEach(item => {
      if (
        object[item] === null ||
        object[item] === '' ||
        object[item] === undefined
      ) {
        isEmpty = true;
      }
    });
    if (isEmpty) {
      // ????????????
      return true;
    }
    return false;
  };

  const isLengthMoreThanZero = (arr, filterTerm: any) => {
    let isLength = false;

    const filterArray = arr.filter(item => {
      return item === filterTerm;
    });
    if (filterArray.length > 0) {
      isLength = true;
    }
    if (isLength) {
      return true;
    }
    return false;
  };

  const handleSubmit = async () => {
    state.form.validateFields(async (err, values) => {
      if (err) {
        message.error('????????????????????????');
        return false;
      }

      let result;
      if (state.pressureMode === 1) {
        result = {
          ...values
        };
      } else if (state.pressureMode === 2) {
        result = {
          ...values,
          increasingTime: values.lineIncreasingTime
        };
        delete result.lineIncreasingTime;
      } else if (state.pressureMode === 3) {
        result = {
          ...values,
          increasingTime: values.stepIncreasingTime
        };
      }

      if (state.configType === 1) {
        result = {
          ...result
        };
      }

      if (state.configType === 2) {
        result = {
          ...result,
          businessFlowId:
            result.businessFlow[0] && Number(result.businessFlow[0]),
          scriptId: result.businessFlow[1]
        };
      }
      delete result.businessFlow;

      if (state.testMode !== TestMode.???????????????) {
        if (
          !result.pressureTestTime ||
          !result.pressureTestTime.time ||
          !result.pressureTestTime.unit
        ) {
          message.info('?????????????????????');
          return;
        }

        if (
          state.pressureMode === 2 &&
          (!state.lineIncreasingTime ||
            !state.lineIncreasingTime.time ||
            !state.lineIncreasingTime.unit)
        ) {
          message.info('?????????????????????');
          return;
        }

        if (
          state.pressureMode === 3 &&
          (!result.stepIncreasingTime ||
            !result.stepIncreasingTime.time ||
            !result.stepIncreasingTime.unit)
        ) {
          message.info('?????????????????????');
          return;
        }
      }

      /**
       * @name ??????????????????????????????????????????
       */
      if (result.businessActivityConfig) {
        const businessActivityConfigArr = result.businessActivityConfig.map(
          item => {
            if (objectValueHasEmpty(item)) {
              return true;
            }
            return false;
          }
        );
      }

      /**
       * @name ??????SLA????????????????????????????????????
       */
      if (result.stopCondition) {
        const stopConditionArr = result.stopCondition.map(item => {
          if (
            !item.ruleName ||
            !item.businessActivity ||
            (item.businessActivity && item.businessActivity.length === 0) ||
            objectValueHasEmpty(item.rule)
          ) {
            return true;
          }
          return false;
        });
        if (isLengthMoreThanZero(stopConditionArr, true)) {
          message.error('?????????SLA?????????????????????');
          return;
        }
      }
      /**
       * @name ??????SLA????????????????????????????????????
       */
      if (result.warningCondition && result.warningCondition.length > 0) {
        const warningConditionArr = result.warningCondition.map(item => {
          /** ????????????????????????????????????????????????[] */
          if (
            !item.ruleName &&
            (!item.businessActivity ||
              (item.businessActivity && item.businessActivity.length === 0)) &&
            !item.rule.condition &&
            !item.rule.during &&
            !item.rule.indexInfo &&
            !item.rule.times
          ) {
            return true;
          }
          return false;
        });

        if (isLengthMoreThanZero(warningConditionArr, true)) {
          result = {
            ...result,
            warningCondition: []
          };
        }
      }

      if (result.warningCondition && result.warningCondition.length > 0) {
        const warningConditionArr = result.warningCondition.map(item => {
          if (
            !item.ruleName ||
            !item.businessActivity ||
            (item.businessActivity && item.businessActivity.length === 0) ||
            objectValueHasEmpty(item.rule)
          ) {
            return true;
          }
          return false;
        });

        if (isLengthMoreThanZero(warningConditionArr, true)) {
          message.error('?????????SLA?????????????????????');
          return;
        }
      }

      setState({
        loading: true
      });
      /**
       * @name ??????????????????
       */
      if (action === 'add') {
        const {
          data: { success, data }
        } = await PressureTestSceneService.addPressureTestScene(result);
        if (success) {
          setState({
            loading: false
          });
          message.success('????????????????????????');
          router.push('/pressureTestManage/pressureTestScene');
        }
        setState({
          loading: false
        });
      }

      /**
       * @name ??????????????????
       */
      if (action === 'edit') {
        const {
          data: { success, data }
        } = await PressureTestSceneService.editPressureTestScene({
          ...result,
          id
        });
        if (success) {
          setState({
            loading: false
          });
          message.success('????????????????????????');
          router.push('/pressureTestManage/pressureTestScene');
        }
        setState({
          loading: false
        });
      }
    });
  };

  /**
   * @name ????????????????????????
   */
  const queryBussinessActive = async () => {
    const {
      data: { success, data }
    } = await PressureTestSceneService.queryBussinessActive({});
    if (success) {
      setState({
        bussinessActiveList:
          data &&
          data.map(item => {
            return { label: item.businessActiveName, value: item.id };
          })
      });
    }
  };

  /**
   * @name ?????????????????????????????????
   */
  const queryBussinessActivityAndScript = async () => {
    const {
      data: { success, data }
    } = await PressureTestSceneService.queryBussinessActivityAndScript({});
    if (success) {
      setState({
        bussinessActivityAndScriptList:
          data &&
          data.map((item, k) => {
            return {
              id: item.id,
              name: item.name,
              disabled: item.scriptList ? false : true,
              scriptList: item.scriptList
            };
          })
      });
    }
  };

  /**
   * @name ?????????????????????????????????
   */
  const queryBussinessFlowAndScript = async () => {
    const {
      data: { success, data }
    } = await PressureTestSceneService.queryBussinessFlowAndScript({});
    if (success) {
      setState({
        bussinessFlowAndScriptList:
          data &&
          data.map((item, k) => {
            return {
              id: item.id,
              name: item.name,
              disabled: item.scriptList ? false : true,
              scriptList: item.scriptList
            };
          })
      });
    }
  };

  /**
   * @name ?????????????????????????????????
   */
  const queryBigFileDownload = async () => {
    const {
      data: { success, data }
    } = await PressureTestSceneService.getBigFileDownload({});
    if (success) {
      setState({
        downloadUrl: data.url
      });
    }
  };

  /**
   * @name ????????????????????????
   */
  const queryMissingDataScriptList = async values => {
    const {
      data: { success, data }
    } = await BusinessActivityService.queryMissingDataScriptList({ ...values });
    if (success) {
      setState({
        missingDataScriptList: data
      });
    }
  };

  /**
   * @name ????????????????????????
   */
  const queryPressureTestSceneDetail = async value => {
    const {
      data: { success, data }
    } = await PressureTestSceneService.getPressureTestSceneDetail({
      id: value
    });
    if (success) {
      setState({
        configType: data.configType,
        businessFlowId: data.businessFlowId,
        initList:
          data.configType !== null
            ? data.businessActivityConfig
            : state.initList,
        detailData: data,
        fileList: data.uploadFile,
        pressureMode: data.pressureMode,
        selectedBussinessActiveList: data.businessActivityConfig,
        selectedBussinessActivityIds:
          data.businessActivityConfig &&
          data.businessActivityConfig.map((item, k) => {
            return item.businessActivityId;
          }),
        pressureTestTime: data.pressureTestTime,
        ipNum: data.ipNum,
        testMode: data.pressureType,
        concurrenceNum: data.concurrenceNum,
        tpsNum: getTpsNum(
          data.businessActivityConfig,
          data.pressureType,
          data.concurrenceNum
        ),
        step: data.step,

        stepIncreasingTime:
          data.pressureMode === 3
            ? data.increasingTime
            : { time: undefined, unit: 'm' },
        lineIncreasingTime:
          data.pressureMode === 2
            ? data.increasingTime
            : { time: undefined, unit: 'm' }
      });
    }
  };

  const getTpsNum = (list, testMode, concurrenceNum) => {
    setState({
      businessList: list
    });
    const isEmpty = list.find(item => !item.targetTPS);
    if (isEmpty) {
      return;
    }
    let tpsNum = 0;
    list.forEach(item => {
      tpsNum += item.targetTPS;
    });

    minMax(testMode, tpsNum, concurrenceNum);
    return tpsNum;
  };

  const minMax = async (testMode, tpsNum, concurrenceNum) => {
    let params = null;
    if (testMode === 0) {
      params = {
        concurrenceNum
      };
    } else {
      params = {
        tpsNum
      };
    }
    const {
      data: { success, data }
    } = await PressureTestSceneService.getMaxMachineNumber({ ...params });
    if (success) {
      setState({
        ipNum: data.min,
        minIpNum: data.min,
        maxIpNum: data.max
      });
    }
  };

  return (action !== 'add' && JSON.stringify(state.detailData) !== '{}') ||
    action === 'add' ? (
    <BasePageLayout title={'??????????????????'}>
      <FormCardMultiple
        commonFormProps={{
          rowNum: 1,
          formProps: {
            className: styles.formCard
          }
        }}
        dataSource={formDataSource}
        getForm={form => setState({ form })}
      />
      <Button
        type="primary"
        onClick={() => handleSubmit()}
        loading={state.loading}
      >
        ??????
      </Button>
    </BasePageLayout>
  ) : (
    <CustomSkeleton />
  );
};
export default connect(({ common }) => ({ ...common }))(
  PressureTestSceneConfig
);
